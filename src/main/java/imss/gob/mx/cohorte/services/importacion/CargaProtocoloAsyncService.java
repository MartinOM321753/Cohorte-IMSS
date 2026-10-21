package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CargaProtocoloAsyncService {

    private final CargaMasivaProtocoloService cargaProtocoloService;
    private final EmailService emailService;
    private final UserRepository userRepository;

    /**
     * @param securityContext capturado en el hilo HTTP con
     *        {@code SecurityContextHolder.getContext()} antes de lanzar el async.
     */
    @Async("cargasExecutor")
    public void confirmarYNotificar(byte[] contenido, Long idProtocolo,
                                     Map<String, Long> hojaProtocolo,
                                     String fechaPorOmision,
                                     SecurityContext securityContext,
                                     String emailSolicitante, String nombreSolicitante) {
        SecurityContextHolder.setContext(securityContext);
        try {
            ejecutar(contenido, idProtocolo, hojaProtocolo, fechaPorOmision,
                    emailSolicitante, nombreSolicitante);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void ejecutar(byte[] contenido, Long idProtocolo,
                           Map<String, Long> hojaProtocolo, String fechaPorOmision,
                           String emailSolicitante, String nombreSolicitante) {
        List<String> emailsRoot = userRepository.findAllByRol_RoleAndActivoTrue("ROOT").stream()
                .filter(u -> u.getPersona() != null && u.getPersona().getEmail() != null
                        && !u.getPersona().getEmail().isBlank())
                .map(u -> u.getPersona().getEmail())
                .distinct()
                .toList();

        ResultadoCargaProtocolo resultado;
        try {
            if (hojaProtocolo != null) {
                resultado = cargaProtocoloService.confirmarMultiHojaDesdeBytes(
                        contenido, hojaProtocolo, fechaPorOmision);
            } else {
                resultado = cargaProtocoloService.confirmarDesdeBytes(
                        contenido, idProtocolo, fechaPorOmision);
            }
        } catch (Exception e) {
            log.error("Fallo la carga masiva por protocolo: {}", e.getMessage(), e);
            enviarCorreoError(emailSolicitante, nombreSolicitante, e.getMessage());
            for (String rootEmail : emailsRoot) {
                if (!rootEmail.equalsIgnoreCase(emailSolicitante)) {
                    enviarCorreoError(rootEmail, nombreSolicitante, e.getMessage());
                }
            }
            return;
        }

        log.info("Carga masiva por protocolo finalizada: {} alicuotas, {} lotes, {} padres",
                resultado.alicuotas(), resultado.lotes(), resultado.padres());

        enviarCorreoCompletado(emailSolicitante, nombreSolicitante, resultado);
        for (String rootEmail : emailsRoot) {
            if (!rootEmail.equalsIgnoreCase(emailSolicitante)) {
                enviarCorreoCompletado(rootEmail, nombreSolicitante, resultado);
            }
        }
    }

    private void enviarCorreoCompletado(String email, String nombreSolicitante,
                                         ResultadoCargaProtocolo resultado) {
        if (email == null || email.isBlank()) return;
        Context ctx = new Context();
        ctx.setVariable("nombre", nombreSolicitante);
        ctx.setVariable("alicuotas", resultado.alicuotas());
        ctx.setVariable("lotes", resultado.lotes());
        ctx.setVariable("padres", resultado.padres());
        ctx.setVariable("procesamientos", resultado.procesamientos());
        ctx.setVariable("ubicadas", resultado.ubicadas());
        ctx.setVariable("transferencias", resultado.transferencias());
        ctx.setVariable("avisos", resultado.avisos());
        ctx.setVariable("tieneAvisos", resultado.avisos() != null && !resultado.avisos().isEmpty());
        emailService.enviar(email, "Carga masiva de muestras completada",
                "email/carga-protocolo-completada", ctx);
    }

    private void enviarCorreoError(String email, String nombreSolicitante, String motivo) {
        if (email == null || email.isBlank()) return;
        Context ctx = new Context();
        ctx.setVariable("nombre", nombreSolicitante);
        ctx.setVariable("motivo", motivo);
        emailService.enviar(email, "Error en la carga masiva de muestras",
                "email/carga-protocolo-error", ctx);
    }
}
