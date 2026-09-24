package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;

import java.util.List;

/**
 * Una ventana del listado de muestras, con lo necesario para pedir la de al lado.
 *
 * <p>Las alícuotas viajan aparte de las tarjetas: la pantalla las dibuja
 * plegadas dentro de su padre, y mezclarlas en la misma lista haría que
 * «veinte» significara veinte filas unas veces y veinte tarjetas otras, según
 * cuántas alícuotas tuviera cada muestra.</p>
 *
 * @param muestras           las tarjetas de esta página, de la más reciente a la más antigua
 * @param alicuotas          las alícuotas visibles de esas tarjetas
 * @param cursorInicio       posición de la primera fila; sirve para pedir hacia arriba
 * @param cursorFin          posición de la última fila; sirve para pedir hacia abajo
 * @param hayAnteriores      si queda algo más reciente que esta página
 * @param haySiguientes      si queda algo más antiguo que esta página
 * @param total              cuántas tarjetas cumplen los criterios en total
 * @param huerfanasDevueltas cuántas alícuotas ajenas y fuera del biobanco hay tras el filtro
 */
public record PaginaMuestras(
        List<Muestra> muestras,
        List<Muestra> alicuotas,
        String cursorInicio,
        String cursorFin,
        boolean hayAnteriores,
        boolean haySiguientes,
        long total,
        long huerfanasDevueltas) {
}
