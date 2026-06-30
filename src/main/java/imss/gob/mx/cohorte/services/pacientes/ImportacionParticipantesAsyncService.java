package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.controllers.pacientes.dto.ImportResultDTO;
import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImportacionParticipantesAsyncService {

    private final PacienteImportService pacienteImportService;
    private final EmailService emailService;

    /**
     * Corre en un hilo separado (ver AsyncConfig.notificacionesExecutor) para
     * que el endpoint de importación responda de inmediato sin esperar a que
     * se procesen las filas del archivo. Al terminar, notifica por correo al
     * usuario que inició la carga.
     */
    @Async("notificacionesExecutor")
    public void procesarYNotificar(byte[] contenido, String nombreArchivo, Institucion institucion,
                                    String emailDestino, String nombreDestino) {
        ImportResultDTO resultado;
        try {
            resultado = pacienteImportService.importar(contenido, nombreArchivo, institucion);
        } catch (Exception e) {
            log.error("Fallo la importación masiva de participantes: {}", e.getMessage(), e);
            enviarCorreoError(emailDestino, nombreDestino, e.getMessage());
            return;
        }
        log.info("Importación masiva de participantes finalizada: {} exitosos, {} errores, {} duplicados, {} advertencias",
                resultado.getExitosos(), resultado.getErrores(), resultado.getDuplicados(), resultado.getAdvertencias());
        enviarCorreoCompletado(emailDestino, nombreDestino, resultado);
    }

    private void enviarCorreoCompletado(String email, String nombre, ImportResultDTO resultado) {
        if (email == null || email.isBlank()) {
            log.warn("No se pudo notificar la importación: el usuario no tiene email registrado");
            return;
        }
        Context ctx = new Context();
        ctx.setVariable("nombre", nombre);
        ctx.setVariable("totalFilas", resultado.getTotalFilas());
        ctx.setVariable("exitosos", resultado.getExitosos());
        ctx.setVariable("errores", resultado.getErrores());
        ctx.setVariable("duplicados", resultado.getDuplicados());
        ctx.setVariable("detalleErrores", resultado.getDetalleErrores());
        ctx.setVariable("advertencias", resultado.getAdvertencias());
        ctx.setVariable("detalleAdvertencias", resultado.getDetalleAdvertencias());
        emailService.enviar(email, "Carga de participantes completada", "email/carga-participantes-completada", ctx);
    }

    private void enviarCorreoError(String email, String nombre, String motivo) {
        if (email == null || email.isBlank()) return;
        Context ctx = new Context();
        ctx.setVariable("nombre", nombre);
        ctx.setVariable("motivo", motivo);
        emailService.enviar(email, "Error en la carga de participantes", "email/carga-participantes-error", ctx);
    }
}
