package imss.gob.mx.cohorte.controllers.pruebaescalon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PruebaEscalonRequestDTO {

    @NotBlank(message = "El UUID del paciente es obligatorio")
    private String pacienteUUID;

    @NotBlank(message = "El UUID del usuario es obligatorio")
    private String usuarioRealizaUUID;

    @NotNull(message = "La fecha de estudio es obligatoria")
    private LocalDate fechaEstudio;
}
