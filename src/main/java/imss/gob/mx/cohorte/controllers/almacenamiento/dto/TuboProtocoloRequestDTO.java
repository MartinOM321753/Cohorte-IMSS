package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuboProtocoloRequestDTO {

    @NotBlank(message = "El nombre del tubo es obligatorio")
    @Size(max = 100, message = "Nombre máximo 100 caracteres")
    private String nombre;

    @Size(max = 20, message = "Prefijo máximo 20 caracteres")
    private String prefijoCodigo;

    /** GUARDAR | ESTUDIO | ALICUOTAR. Por omisión GUARDAR. */
    private AccionTubo accion;

    private Integer orden;

    private Boolean activo;

    // ── Receta (solo relevante si accion == ALICUOTAR) ──────────────────────────

    /** Tipo de muestra resultante del proceso (sangre total → suero). */
    private Long idTipoResultante;

    @Min(value = 0, message = "El número de alícuotas no puede ser negativo")
    private Integer numeroAlicuotas;

    private Double volumenAlicuota;

    private List<Double> volumenesAlicuota;

    @Size(max = 20, message = "Unidad de volumen máximo 20 caracteres")
    private String unidadVolumen;

    @Size(max = 100, message = "Destino sugerido máximo 100 caracteres")
    private String destinoSugerido;

    private Boolean agruparEnLote;

    private Boolean generacionAutomatica;

    private Boolean permiteAlicuotaParcial;
}
