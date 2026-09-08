package imss.gob.mx.cohorte.services.formulas;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Contesta las variables de una fórmula con los datos de un participante.
 *
 * <p>Es la pieza que une el motor —que no sabe de participantes— con los datos. Recibe
 * de fuera cómo resolver una clave del catálogo, y devuelve la función que el
 * evaluador necesita: dado el nombre corto que usa la fórmula, la cantidad ya lista
 * para calcular.</p>
 *
 * <p>Se mantiene aquí, sin importar nada del módulo de reportes, para que el mismo
 * enlace sirva a la emisión de un reporte, a la descarga de datos y a la vista previa
 * del editor. Cada uno pasa su forma de resolver claves y el resto es común.</p>
 *
 * <p><b>La conversión ocurre en este punto</b>, una sola vez por variable y antes de
 * que el evaluador toque nada. Cuando la fórmula pide la estatura en metros y el dato
 * está en centímetros, lo que entra al cálculo son los metros.</p>
 */
public final class EnlaceDeVariables {

    private EnlaceDeVariables() {}

    /**
     * @param variables    las declaradas en la fórmula
     * @param porClave     cómo sacar el dato de una clave del catálogo
     * @return con qué contestar cada nombre; ausente para las que no se resuelvan
     */
    public static Function<String, Magnitud> para(List<VariableFormula> variables,
                                                  Function<String, Magnitud> porClave) {
        Map<String, VariableFormula> declaradas = new HashMap<>();
        if (variables != null) {
            for (VariableFormula v : variables) declaradas.put(v.nombre(), v);
        }

        return nombre -> {
            VariableFormula v = declaradas.get(nombre);

            // Un nombre que la fórmula usa pero nadie declaró. Se contesta ausente y la
            // celda queda vacía; que además no se pueda guardar así lo dice el
            // validador, que es quien tiene que avisar antes de llegar aquí.
            if (v == null || porClave == null) return Magnitud.sinDato();

            Magnitud dato = porClave.apply(v.clave());
            if (dato == null || dato.ausente()) return Magnitud.sinDato();

            return v.unidad() == null ? dato : dato.en(v.unidad());
        };
    }
}
