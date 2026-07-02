package imss.gob.mx.cohorte.controllers.catalogo.dto;

import imss.gob.mx.cohorte.modules.catalogo.TipoCatalogo;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SeleccionCatalogo {

    @NotNull(message = "El tipo de catálogo es requerido")
    private TipoCatalogo tipo;

    private List<Long> ids;
}
