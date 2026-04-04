package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MuestraRequestDTO {

    @NotBlank(message = "La etiqueta es obligatoria")
    @Size(max = 50, message = "Etiqueta máximo 50 caracteres")
    private String etiqueta;

    private Double valor;

    @Size(max = 50, message = "Unidad máximo 50 caracteres")
    private String unidad;

    private LocalDateTime fechaRecoleccion;

    @Size(max = 200, message = "Observaciones máximo 200 caracteres")
    private String observaciones;

    @NotBlank(message = "El UUID del paciente es obligatorio")
    private String pacienteUUID;

    @NotBlank(message = "El UUID del usuario que recolecta es obligatorio")
    private String usuarioRecolectaUUID;

    private Long idPosicionCaja;
}
