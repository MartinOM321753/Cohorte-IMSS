package imss.gob.mx.cohorte.controllers.pacientes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Lo que UNA institución registró a un participante que ya no gestiona.
 *
 * <p>No es el expediente: es solo lo propio. El historial completo de ese
 * participante le corresponde a su institución dueña, no a quien alguna vez lo
 * atendió.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MisRegistrosParticipanteDTO {

    private String pacienteUuid;
    private String folio;
    private String nombreCompleto;
    /** Sede a la que pertenece hoy el participante. */
    private String institucionActualNombre;

    private List<Registro> estudios;
    private List<Registro> muestras;
    private List<Registro> citas;
    private List<Registro> somatometrias;
    private List<Registro> examenes;

    /** Fila mínima para listar; el detalle se abre por el módulo correspondiente. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Registro {
        private Long id;
        private LocalDateTime fecha;
        private String descripcion;
    }
}
