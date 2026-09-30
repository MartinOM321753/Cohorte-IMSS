package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TuboMuestraResumenDTO {
    private Long id;
    private String nombre;
    private String prefijoCodigo;
    private Integer numeroAlicuotas;
    /** El modal de alícuotas necesita la receta completa para planificar el lote. */
    private Double volumenAlicuota;
    /** Volumen individual de cada alícuota, en orden. Vacía = tubo uniforme. */
    private List<Double> volumenesAlicuota;
    private String unidadVolumen;
    private Boolean generacionAutomatica;
    private Boolean permiteAlicuotaParcial;
}
