package imss.gob.mx.cohorte.controllers.DTO;

import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import lombok.*;

import java.util.ArrayList;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PisosDTO {

    private  Long idRefrigerador;
    private ArrayList<PisoRefrigerador> pisos;

}
