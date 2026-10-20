package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteFolioProyeccion;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reasignación de institución por rango de folios, en segundo plano.
 *
 * <p>Cada participante se mueve en su propia transacción: si uno no se puede mover
 * (tiene estudios, por ejemplo) no revierte a los demás, y el motivo queda en el
 * resumen. El resultado llega por correo, porque un rango grande tarda lo suficiente
 * como para que nadie espere con la pantalla abierta.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReasignacionRangoAsyncService {

    private static final int MAX_DETALLE_CORREO = 500;
    private static final int EJEMPLOS_POR_MOTIVO = 10;
    private static final int CADA_CUANTOS_LOG = 500;

    private final PacienteRepository pacienteRepository;
    private final PacienteService pacienteService;
    private final ParticipanteTitularidadService participanteTitularidadService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PlatformTransactionManager transactionManager;

    /**
     * Corre en el executor {@code reasignacionesExecutor}. Los datos del usuario
     * (instituciones visibles, destino, correo) se resuelven antes, en la petición:
     * el hilo de fondo no tiene la sesión de seguridad.
     */
    @Async("reasignacionesExecutor")
    public void procesarYNotificar(int folioDesde, int folioHasta, List<Long> visibles,
                                   Institucion destino, String emailDestino, String nombreDestino) {
        String rango = formatear(folioDesde) + " - " + formatear(folioHasta);
        log.info("Reasignación por rango {} hacia institución {} iniciada", rango, destino.getId());

        Resumen resumen;
        try {
            resumen = reasignar(folioDesde, folioHasta, visibles, destino);
        } catch (Exception e) {
            log.error("Falló la reasignación por rango {}: {}", rango, e.getMessage(), e);
            enviarCorreoError(emailDestino, nombreDestino, folioDesde, folioHasta, e.getMessage());
            return;
        }

        log.info("Reasignación por rango {} finalizada: {} movidos, {} no movidos, {} ya en destino",
                rango, resumen.movidos, resumen.noMovidos.size(), resumen.yaEnDestino);
        enviarCorreoCompletado(emailDestino, nombreDestino, folioDesde, folioHasta, destino, resumen);
    }

    private Resumen reasignar(int folioDesde, int folioHasta, List<Long> visibles, Institucion destino) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        List<PacienteFolioProyeccion> candidatos =
                pacienteRepository.findEnRangoDeFolios(folioDesde, folioHasta, visibles);

        Resumen resumen = new Resumen(candidatos.size());
        int procesados = 0;

        for (PacienteFolioProyeccion c : candidatos) {
            procesados++;
            if (destino.getId().equals(c.getIdInstitucion())) {
                resumen.yaEnDestino++;
                continue;
            }

            String motivo;
            try {
                motivo = tx.execute(status -> intentarMover(c.getUuid(), destino));
            } catch (Exception e) {
                log.warn("Error al mover el folio {}: {}", c.getFolio(), e.getMessage());
                motivo = "Error al guardar el cambio; no se movió";
            }

            if (motivo == null) {
                resumen.movidos++;
            } else {
                resumen.noMovidos.add(new Fallo(c.getFolio(), motivo));
            }

            if (procesados % CADA_CUANTOS_LOG == 0) {
                log.info("Reasignación por rango: {} de {} revisados", procesados, candidatos.size());
            }
        }
        return resumen;
    }

    /** Devuelve null si se movió; si no, el motivo por el que no se pudo. */
    private String intentarMover(String uuid, Institucion destino) {
        Paciente paciente = pacienteRepository.findByUuid(uuid).orElse(null);
        if (paciente == null) {
            return "El participante ya no existe";
        }
        if (destino.getId().equals(paciente.getInstitucion().getId())) {
            return "Ya pertenece a la institución destino";
        }

        List<ParticipanteTitularidadService.Vinculo> vinculos =
                participanteTitularidadService.vinculosQueImpidenCambio(uuid);
        if (!vinculos.isEmpty()) {
            return "Tiene " + participanteTitularidadService.describir(vinculos)
                    + " registrados en su institución actual";
        }

        paciente.setInstitucion(destino);
        paciente.setFechaActualizacion(LocalDateTime.now());
        pacienteService.guardar(paciente);

        // La cuenta de acceso, si existe, sigue al participante (misma regla que la reasignación individual).
        if (paciente.getPersona() != null) {
            userRepository.findByPersona_Id(paciente.getPersona().getId()).ifPresent(cuenta -> {
                cuenta.setInstitucion(destino);
                userRepository.save(cuenta);
            });
        }
        return null;
    }

    // ─── Correos ─────────────────────────────────────────────────────────────

    private void enviarCorreoCompletado(String email, String nombre, int folioDesde, int folioHasta,
                                        Institucion destino, Resumen resumen) {
        if (email == null || email.isBlank()) {
            log.warn("No se pudo notificar la reasignación por rango: el usuario no tiene email registrado");
            return;
        }

        Map<String, List<String>> porMotivo = new LinkedHashMap<>();
        for (Fallo f : resumen.noMovidos) {
            porMotivo.computeIfAbsent(f.getMotivo(), k -> new ArrayList<>()).add(f.getFolio());
        }
        List<GrupoMotivo> grupos = porMotivo.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()))
                .map(e -> new GrupoMotivo(e.getKey(), e.getValue().size(),
                        String.join(", ", e.getValue().stream().limit(EJEMPLOS_POR_MOTIVO).toList())))
                .toList();

        Context ctx = new Context();
        ctx.setVariable("nombre", nombre);
        ctx.setVariable("folioDesde", formatear(folioDesde));
        ctx.setVariable("folioHasta", formatear(folioHasta));
        ctx.setVariable("destinoNombre", destino.getNombre());
        ctx.setVariable("encontrados", resumen.encontrados);
        ctx.setVariable("movidos", resumen.movidos);
        ctx.setVariable("yaEnDestino", resumen.yaEnDestino);
        ctx.setVariable("noMovidos", resumen.noMovidos.size());
        ctx.setVariable("grupos", grupos);
        ctx.setVariable("detalle", resumen.noMovidos.stream().limit(MAX_DETALLE_CORREO).toList());
        ctx.setVariable("detalleTruncado", resumen.noMovidos.size() > MAX_DETALLE_CORREO);

        emailService.enviar(email,
                "Reasignación por rango completada (" + formatear(folioDesde) + " - " + formatear(folioHasta) + ")",
                "email/reasignacion-rango-completada", ctx);
    }

    private void enviarCorreoError(String email, String nombre, int folioDesde, int folioHasta, String motivo) {
        if (email == null || email.isBlank()) return;
        Context ctx = new Context();
        ctx.setVariable("nombre", nombre);
        ctx.setVariable("folioDesde", formatear(folioDesde));
        ctx.setVariable("folioHasta", formatear(folioHasta));
        ctx.setVariable("motivo", motivo);
        emailService.enviar(email, "Error en la reasignación por rango", "email/reasignacion-rango-error", ctx);
    }

    private static String formatear(int folio) {
        return String.format("%06d", folio);
    }

    // ─── Tipos internos ──────────────────────────────────────────────────────

    private static final class Resumen {
        final int encontrados;
        int movidos;
        int yaEnDestino;
        final List<Fallo> noMovidos = new ArrayList<>();

        Resumen(int encontrados) {
            this.encontrados = encontrados;
        }
    }

    @Getter
    @AllArgsConstructor
    public static class Fallo {
        private final String folio;
        private final String motivo;
    }

    @Getter
    @AllArgsConstructor
    public static class GrupoMotivo {
        private final String motivo;
        private final int cantidad;
        private final String ejemplos;
    }
}
