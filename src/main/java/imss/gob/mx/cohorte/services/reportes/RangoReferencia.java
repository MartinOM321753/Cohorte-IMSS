package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.persona.Persona;

/**
 * El rango de referencia que aplica a un participante y si un valor se sale de él.
 *
 * <p>Un parámetro numérico guarda dos rangos, uno para mujeres y otro para hombres, y
 * cuál de los dos aplica depende de quién se midió. Hasta ahora esta comparación solo
 * existía en el navegador, dentro de las gráficas del expediente; los resultados de
 * examen sí traían el dato resuelto del servidor, pero los de estudio no. Un reporte
 * impreso no puede depender de un cálculo que vive en una pantalla, así que aquí está
 * una vez, en el servidor, para los dos.</p>
 */
public final class RangoReferencia {

    private RangoReferencia() {}

    /** El rango aplicable, o null si el parámetro no define ninguno para ese sexo. */
    public static Rango de(ParametroEstudio parametro, Persona.Sexo sexo) {
        if (parametro == null) return null;
        // Sin sexo registrado no se elige rango: inventar uno sería peor que no marcar
        // nada, porque un "fuera de rango" falso en un documento clínico se lee como un
        // hallazgo.
        if (sexo == null) return null;

        Double min = sexo == Persona.Sexo.F ? parametro.getValorMinMujeres() : parametro.getValorMinHombres();
        Double max = sexo == Persona.Sexo.F ? parametro.getValorMaxMujeres() : parametro.getValorMaxHombres();
        if (min == null && max == null) return null;
        return new Rango(min, max);
    }

    /**
     * ¿El valor queda fuera? Devuelve false cuando no hay con qué comparar — ni valor,
     * ni rango—, porque «no se sabe» no es «está mal».
     */
    public static boolean fueraDeRango(Double valor, Rango rango) {
        if (valor == null || rango == null) return false;
        if (rango.min() != null && valor < rango.min()) return true;
        return rango.max() != null && valor > rango.max();
    }

    /** Texto legible del rango, para la columna de referencia. */
    public static String texto(Rango rango) {
        if (rango == null) return "";
        if (rango.min() != null && rango.max() != null) return numero(rango.min()) + " – " + numero(rango.max());
        if (rango.min() != null) return "≥ " + numero(rango.min());
        return "≤ " + numero(rango.max());
    }

    /** Quita el ".0" de los enteros: «120» se lee mejor que «120.0» en un reporte. */
    private static String numero(Double d) {
        if (d == null) return "";
        return d == Math.floor(d) && !d.isInfinite()
                ? String.valueOf(d.longValue())
                : String.valueOf(d);
    }

    public record Rango(Double min, Double max) {}
}
