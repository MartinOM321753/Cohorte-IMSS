package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EstudioMuestraRequestDTO {

    @NotNull(message = "El tipo de estudio de muestra es requerido")
    private Long idTipoEstudioMuestra;

    @NotBlank(message = "El UUID del usuario que realiza es requerido")
    private String usuarioRealizaUUID;

    @NotNull(message = "La fecha del estudio es requerida")
    @PastOrPresent(message = "La fecha del estudio no puede ser futura")
    private LocalDate fechaEstudio;

    @Size(max = 500)
    private String observaciones;

    /** Volumen/masa consumida de la muestra (opcional). */
    private Double cantidadConsumida;

    /** Unidad del consumo: "mL", "µL", "mg", "g" (opcional). */
    @Size(max = 20)
    private String unidadConsumida;

    @Valid
    private List<ResultadoEstudioMuestraRequestDTO> resultados;
}
