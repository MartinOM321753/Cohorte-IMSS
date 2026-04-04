package imss.gob.mx.cohorte.controllers.estudios.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EstudioMedicoRequestDTO {

    @NotBlank
    private String pacienteUUID;

    @NotBlank
    private String usuarioRealizaUUID;

    @NotNull
    private Long idTipoEstudio;

    @NotNull
    private LocalDate fechaEstudio;

    private String observaciones;

    private List<ResultadoEstudioRequestDTO> resultados;
}
