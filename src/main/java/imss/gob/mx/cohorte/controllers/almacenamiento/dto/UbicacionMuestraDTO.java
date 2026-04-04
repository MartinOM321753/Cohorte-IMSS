package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UbicacionMuestraDTO {
    private Long idPosicionCaja;
    private String fila;
    private String columna;
    private String codigoCaja;
    private String numeroPiso;
    private String codigoRefrigerador;
}
