package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PisoResumenDTO {
    private Long id;
    private String numeroPiso;
    private int totalPosiciones;
    private int posicionesOcupadas;
    private int posicionesLibres;
    private double porcentajeUso;
}
