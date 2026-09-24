package imss.gob.mx.cohorte.modules.estudios.resultados;

import java.util.Comparator;

/**
 * En qué orden se presentan los resultados de un estudio ya guardado.
 *
 * <p>Manda el orden que hoy tiene el parámetro en el catálogo, no el que tenía el
 * formulario el día de la captura. La fila guarda {@code ordenResultado}, pero ese
 * número identifica el grupo al que pertenece el resultado —en la captura normal
 * es cero para todos—, así que nunca fue una posición por parámetro: dentro de un
 * grupo el desempate lo hacía el id, o sea el orden de inserción. Ese lugar lo
 * toma ahora el orden configurado, y el id se queda al final para lo que él no
 * resuelve.</p>
 *
 * <p>Vive aquí, y no en cada lugar que arma una tabla, porque el expediente, el
 * reporte del participante y el PDF tienen que coincidir: si cada uno ordenara por
 * su cuenta, reacomodar el catálogo movería unas pantallas y otras no.</p>
 *
 * <p>Un parámetro retirado de uso conserva su lugar —sigue en el catálogo, con su
 * orden—. Al final solo caen las filas que se quedaron sin parámetro.</p>
 */
public final class OrdenDeResultados {

    private OrdenDeResultados() {}

    public static final Comparator<ResultadoEstudio> POR_CATALOGO =
            Comparator
                    .comparing((ResultadoEstudio r) -> r.getGrupoCodigo() == null ? "" : r.getGrupoCodigo())
                    .thenComparing(r -> r.getOrdenResultado() == null ? 0 : r.getOrdenResultado())
                    .thenComparing(r -> r.getParametro() == null || r.getParametro().getOrden() == null
                            ? Integer.MAX_VALUE : r.getParametro().getOrden())
                    .thenComparing(r -> r.getId() == null ? Long.MAX_VALUE : r.getId());
}
