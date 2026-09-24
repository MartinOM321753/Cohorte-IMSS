package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Previsualización del lote: qué se va a crear con el volumen capturado, qué
 * sobra y qué alternativas hay para el sobrante.
 *
 * <p>Sale del mismo {@code PlanificadorAlicuotas} que ejecuta la creación, así
 * que lo que la pantalla promete y lo que el servidor crea no pueden divergir.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanAlicuotasResponseDTO {

    private Integer numeroAlicuotasConfiguradas;
    private Double volumenAlicuota;
    private String unidad;
    /** Lo que haría falta para el lote completo: configuradas × volumen. */
    private Double totalRequerido;
    /** Lo que la muestra padre puede comprometer ahora mismo. */
    private Double valorDisponible;
    private Integer alicuotasCompletas;
    /** Lo que queda tras llenar las completas. */
    private Double remanente;
    /** Huecos del tubo que quedarían sin usar. */
    private Integer lugaresRestantes;
    private Boolean alcanzaLoteCompleto;
    /** Si el remanente cabe en una alícuota incompleta. */
    private Boolean puedeAlojarParcial;
    /** Alícuotas del lote que ya existen: si es mayor que 0, esto es una continuación. */
    private Integer slotsOcupados;
    /** Huecos del tubo que quedan por llenar. */
    private Integer slotsLibres;
    /** Explicación redactada para mostrar tal cual. */
    private String mensaje;
    /** Repartos posibles; el primero es el sugerido por omisión. */
    private List<OpcionDistribucionDTO> opciones;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OpcionDistribucionDTO {
        /** Clave estable para la pantalla: {@code SOLO_COMPLETAS}, {@code PARCIALES_1}, … */
        private String clave;
        private String descripcion;
        private List<Double> volumenes;
        private Integer totalAlicuotas;
        /** Lo que quedaría en la muestra padre con este reparto. */
        private Double remanenteEnPadre;
    }
}
