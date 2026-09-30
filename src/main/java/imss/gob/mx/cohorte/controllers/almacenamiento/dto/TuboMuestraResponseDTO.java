package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TuboMuestraResponseDTO {
    private Long id;
    private String nombre;
    private String prefijoCodigo;
    private Integer numeroAlicuotas;
    private Double volumenAlicuota;
    /** Volumen individual de cada alícuota, en orden. Vacía = tubo uniforme. */
    private List<Double> volumenesAlicuota;
    private String unidadVolumen;
    private String destinoSugerido;
    private Integer orden;
    private Boolean activo;
    /** Si al registrar una muestra con este tubo las alícuotas se crean solas. */
    private Boolean generacionAutomatica;
    /** Si se admite cerrar el lote con una alícuota incompleta. */
    private Boolean permiteAlicuotaParcial;
}
