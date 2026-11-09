package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class EstudioMuestraBatchRequestDTO {

    @NotNull(message = "La lista de estudios es requerida")
    @Size(min = 1, message = "Debe incluir al menos un estudio")
    @Valid
    private List<EstudioMuestraRequestDTO> estudios;
}
