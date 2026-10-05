package imss.gob.mx.cohorte.services.almacenamiento.procesamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.lote.Lote;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;

import java.util.List;

/**
 * Lo que dejó procesar un participante: las muestras padre (los tubos primarios),
 * las alícuotas generadas y los lotes en que quedaron agrupadas.
 */
public record ResultadoProcesamiento(
        List<Muestra> padres,
        List<Muestra> alicuotas,
        List<Lote> lotes
) {
}
