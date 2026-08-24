package imss.gob.mx.cohorte.services.importacion;

import java.util.List;

/**
 * Lo que devuelve el lector: encabezados y filas, todo como texto.
 *
 * <p>Deliberadamente tonto. El lector no sabe qué es un alias, un folio ni un
 * parametro: se limita a entregar la tabla tal como venia en el archivo, ya
 * saneada. Interpretarla es trabajo de cada importador, y por eso este mismo
 * resultado sirve igual para estudios que para examenes.</p>
 *
 * <p>Todo llega como String, incluidos los numeros y las fechas. Convertirlos
 * aqui obligaria a adivinar el formato sin saber a que parametro corresponde la
 * columna, que es justo lo que produce fechas cambiadas de mes y decimales mal
 * interpretados.</p>
 *
 * @param encabezados en el orden del archivo
 * @param filas       cada fila tiene tantas celdas como encabezados; las que
 *                    faltaban en el archivo llegan como cadena vacia, nunca null,
 *                    para que quien recorra la tabla no tenga que comprobarlo
 * @param numerosDeFila numero de fila real dentro del archivo (base 1, contando
 *                    el encabezado), para poder senalar el problema donde el
 *                    usuario lo va a buscar: en su hoja de calculo
 */
public record TablaLeida(
        List<String> encabezados,
        List<List<String>> filas,
        List<Integer> numerosDeFila
) {
    public int totalFilas() {
        return filas.size();
    }

    public boolean vacia() {
        return filas.isEmpty();
    }
}
