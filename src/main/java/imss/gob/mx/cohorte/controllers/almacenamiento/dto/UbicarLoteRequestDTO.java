package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.application.almacenamiento.MuestraApplicationService.UbicacionAlicuotaDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Qué alícuota va a qué hueco, para ubicar un lote completo de una vez.
 *
 * <p>La lista llega ya resuelta desde la pantalla porque es la que tiene la
 * rejilla de la caja pintada y sabe qué huecos están libres. El servidor no
 * necesita conocer la regla de llenado —por fila, por columna, saltando los
 * ocupados—, que además va a cambiar con el uso; solo tiene que comprobar que
 * cada hueco sigue libre y es del biobanco correcto, y aplicarlo todo junto.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UbicarLoteRequestDTO {

    private List<UbicacionAlicuotaDTO> asignaciones;
}
