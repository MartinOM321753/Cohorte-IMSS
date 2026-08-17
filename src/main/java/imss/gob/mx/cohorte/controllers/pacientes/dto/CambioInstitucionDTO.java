package imss.gob.mx.cohorte.controllers.pacientes.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** DTOs del cambio de institución de un participante. */
public class CambioInstitucionDTO {

    private CambioInstitucionDTO() {}

    /** Un tipo de registro que ata al participante a su institución actual. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Vinculo {
        private String tipo;
        private String etiqueta;
        private long cantidad;
    }

    /**
     * Respuesta de la consulta de elegibilidad. Cuando no se puede mover se
     * devuelve el detalle con conteos, no solo la negativa: quien reasigna
     * necesita saber qué revisar.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Elegibilidad {
        private boolean puedeCambiar;
        private List<Vinculo> vinculos;
        private String motivo;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CambioRequest {
        @NotNull(message = "La institución destino es obligatoria")
        private Long idInstitucion;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReasignacionRequest {
        @NotEmpty(message = "Debe indicar al menos un participante")
        private List<String> uuids;

        @NotNull(message = "La institución destino es obligatoria")
        private Long idInstitucion;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResultadoReasignacion {
        private String uuid;
        private String folio;
        private boolean movido;
        private String motivo;
    }

    /** Resumen del lote: lo que interesa de un vistazo, más el detalle. */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResumenReasignacion {
        private int solicitados;
        private int movidos;
        private int rechazados;
        private List<ResultadoReasignacion> detalle;
    }
}
