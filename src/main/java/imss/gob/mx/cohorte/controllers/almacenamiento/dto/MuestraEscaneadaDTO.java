package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Respuesta de una etiqueta resuelta por el lector de códigos.
 *
 * <p>No basta con devolver la muestra: la pantalla tiene que <em>llevar</em> al
 * usuario hasta ella, y para eso necesita saber dos cosas que la muestra sola no
 * dice.</p>
 *
 * <p>La primera, si es una alícuota, porque en el panel las alícuotas viven
 * plegadas dentro de su muestra padre y hay que desplegar esa fila antes de que
 * la buscada exista en el DOM. La segunda, si la muestra queda fuera de la vista
 * por omisión, porque entonces hay que encender el histórico o el usuario vería
 * un listado vacío después de un escaneo correcto.</p>
 */
@Getter
@Builder
@AllArgsConstructor
public class MuestraEscaneadaDTO {

    private MuestraResponseDTO muestra;

    /** Id de la muestra padre cuando la escaneada es una alícuota; null si es padre. */
    private Long idMuestraPadre;

    /**
     * La muestra no aparece en la vista por omisión: la institución ni es
     * propietaria ni la tiene en su poder, solo participó en un traslado pasado.
     */
    private boolean requiereHistorico;
}
