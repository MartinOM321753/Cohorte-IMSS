package imss.gob.mx.cohorte.controllers.catalogo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ItemCatalogo {
    private Long id;
    private String nombre;
    private boolean existeEnDestino;
    private int hijos;
}
