package imss.gob.mx.cohorte.controllers.examenes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ResultadoExamenRequestDTO {

    @NotBlank(message = "El UUID del paciente es obligatorio")
    private String pacienteUUID;

    @NotBlank(message = "El UUID del usuario de registro es obligatorio")
    private String usuarioRegistroUUID;

    @NotNull(message = "El ID del examen es obligatorio")
    private Long idExamen;

    @NotNull(message = "El valor obtenido es obligatorio")
    private Double valorObtenido;

    private String observaciones;

    @NotNull(message = "La fecha del resultado es obligatoria")
    @PastOrPresent(message = "La fecha del resultado no puede ser futura")
    private LocalDateTime fechaResultado;
}
