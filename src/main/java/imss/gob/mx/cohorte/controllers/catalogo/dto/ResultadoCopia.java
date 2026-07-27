package imss.gob.mx.cohorte.controllers.catalogo.dto;

import imss.gob.mx.cohorte.modules.catalogo.TipoCatalogo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class ResultadoCopia {
    private TipoCatalogo catalogo;
    private int copiados;
    private int omitidos;
    private List<String> detalleOmitidos;
}
