package imss.gob.mx.cohorte.controllers.impresion.dto;

import java.util.List;

/**
 * Un archivo tabular leido para imprimirse como etiquetas.
 *
 * <p>No hay ninguna interpretacion de por medio: el servidor no sabe que
 * significa cada columna ni le hace falta. Devuelve la tabla tal como se ve en la
 * hoja de calculo y es la pantalla la que decide que columnas entran en la
 * etiqueta y en que orden.</p>
 *
 * <p>Nada de esto se guarda. Cada impresion parte de subir el archivo otra vez, y
 * por eso el DTO lleva todo lo que la pantalla necesita en una sola respuesta.</p>
 *
 * @param encabezados    en el orden del archivo
 * @param filas          una etiqueta por fila; cada fila trae tantas celdas como
 *                       encabezados, con cadena vacia donde no habia dato
 * @param numerosDeFila  numero de fila real dentro del archivo, para poder
 *                       senalar un problema donde el usuario lo va a buscar
 * @param avisos         lo que conviene que el usuario sepa antes de imprimir;
 *                       ninguno impide hacerlo
 */
public record TablaEtiquetasDTO(
        List<String> encabezados,
        List<List<String>> filas,
        List<Integer> numerosDeFila,
        List<String> avisos
) {
    public int totalFilas() {
        return filas.size();
    }
}
