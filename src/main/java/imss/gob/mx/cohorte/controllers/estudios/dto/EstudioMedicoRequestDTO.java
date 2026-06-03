package imss.gob.mx.cohorte.controllers.estudios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.Valid;
import lombok.*;


import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class EstudioMedicoRequestDTO {

    @NotBlank
    private String pacienteUUID;

    @NotBlank
    private String usuarioRealizaUUID;

    @NotNull
    private Long idTipoEstudio;

    @NotNull
    @PastOrPresent(message = "La fecha del estudio no puede ser futura")
    private LocalDate fechaEstudio;

    private String observaciones;

    @Valid
    private List<ResultadoEstudioRequestDTO> resultados;
}
