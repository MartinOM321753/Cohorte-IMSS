package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuboMuestraRequestDTO {

    @NotBlank(message = "El nombre del tubo es obligatorio")
    @Size(max = 100, message = "Nombre máximo 100 caracteres")
    private String nombre;

    @Size(max = 20, message = "Prefijo máximo 20 caracteres")
    private String prefijoCodigo;

    @Min(value = 0, message = "El número de alícuotas no puede ser negativo")
    private Integer numeroAlicuotas = 0;

    private Double volumenAlicuota;

    @Size(max = 20, message = "Unidad de volumen máximo 20 caracteres")
    private String unidadVolumen;

    @Size(max = 100, message = "Destino sugerido máximo 100 caracteres")
    private String destinoSugerido;

    private Integer orden;

    private Boolean activo = true;

    /**
     * Valor por omisión de la generación de alícuotas al registrar.
     *
     * <p>Sin valor por defecto a propósito: {@code null} significa «no lo
     * menciono», y la actualización lo respeta. Ponerle {@code TRUE} aquí haría
     * que cualquier petición que omitiera el campo —por ejemplo la que solo
     * activa o desactiva el tubo— devolviera a automático un tubo configurado
     * en manual, sin que nadie lo pidiera.</p>
     */
    private Boolean generacionAutomatica;

    /** Si el lote puede cerrarse con una alícuota incompleta. Ver la nota de arriba. */
    private Boolean permiteAlicuotaParcial;
}
