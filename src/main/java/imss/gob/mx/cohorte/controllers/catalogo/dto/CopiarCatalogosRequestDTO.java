package imss.gob.mx.cohorte.controllers.catalogo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CopiarCatalogosRequestDTO {

    @NotBlank(message = "La institución origen es requerida")
    private String uuidInstitucionOrigen;

    @NotBlank(message = "La institución destino es requerida")
    private String uuidInstitucionDestino;

    @NotEmpty(message = "Debe seleccionar al menos un catálogo")
    @Valid
    private List<SeleccionCatalogo> catalogos;
}
