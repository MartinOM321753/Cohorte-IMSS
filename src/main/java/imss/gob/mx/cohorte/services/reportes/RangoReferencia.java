package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.persona.Persona;

/**
 * El rango de referencia que aplica a un participante y en qué situación queda su
 * resultado frente a él.
 *
 * <p>Un parámetro numérico guarda dos rangos, uno para mujeres y otro para hombres, y
 * cuál de los dos aplica depende de quién se midió. Hasta ahora esta comparación solo
 * existía en el navegador, dentro de las gráficas del expediente; los resultados de
 * examen sí traían el dato resuelto del servidor, pero los de estudio no. Un reporte
 * impreso no puede depender de un cálculo que vive en una pantalla, así que aquí está
 * una vez, en el servidor, para los dos.</p>
 *
 * <h3>Por qué hay un margen y no sólo un límite</h3>
 *
 * <p>El reporte que se entrega al participante distingue entre estar un poco fuera y
 * estar lo bastante fuera como para hablarlo con un médico. Esa frontera no se puede
 * deducir del rango: depende del analito. Un colesterol total de 214 con techo en 200
 * hay que revisarlo, y un hematocrito de 34 con piso en 36 es una diferencia menor,
 * aunque en proporción se salgan casi lo mismo.</p>
 *
 * <p>Por eso el margen es un dato de cada analito, no una fórmula. Cuando no está
 * configurado se usa {@link #FRACCION_MARGEN} de la amplitud del rango, que es un
 * punto de partida razonable y deja el reporte funcionando desde el primer día — pero
 * la intención es que quien define el protocolo lo fije analito por analito.</p>
 */
public final class RangoReferencia {

    private RangoReferencia() {}

    /**
     * Cuánto del rango se admite como diferencia menor cuando nadie ha fijado el
     * margen de ese analito: una décima parte.
     *
     * <p>Con rango de 70 a 99, el margen sale de 2.9: un 101 queda «ligeramente
     * fuera» y un 110 pasa a «revisar».</p>
     */
    public static final double FRACCION_MARGEN = 0.10;

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
        return new Rango(min, max, parametro.getMargenRevision());
    }

    /**
     * ¿El valor queda fuera? Devuelve false cuando no hay con qué comparar — ni valor,
     * ni rango—, porque «no se sabe» no es «está mal».
     */
    public static boolean fueraDeRango(Double valor, Rango rango) {
        return estado(valor, rango).fuera();
    }

    /**
     * En qué situación queda el valor.
     *
     * <p>Sin valor o sin rango es {@link EstadoResultado#SIN_DATO}, nunca EN_RANGO:
     * dar por bueno lo que no se ha podido comparar es exactamente el error que
     * convierte un hueco en un resultado.</p>
     */
    public static EstadoResultado estado(Double valor, Rango rango) {
        if (valor == null || rango == null) return EstadoResultado.SIN_DATO;

        double exceso = 0;
        if (rango.min() != null && valor < rango.min()) {
            exceso = rango.min() - valor;
        } else if (rango.max() != null && valor > rango.max()) {
            exceso = valor - rango.max();
        }
        if (exceso == 0) return EstadoResultado.EN_RANGO;

        return exceso <= rango.margenEfectivo()
                ? EstadoResultado.LIGERAMENTE_FUERA
                : EstadoResultado.REVISAR;
    }

    /** Hacia qué lado se sale, en las palabras del reporte. Vacío si no se sale. */
    public static String sentido(Double valor, Rango rango) {
        if (valor == null || rango == null) return "";
        if (rango.min() != null && valor < rango.min()) return "Por abajo";
        if (rango.max() != null && valor > rango.max()) return "Por arriba";
        return "";
    }

    /** Texto legible del rango, para la columna de referencia. */
    public static String texto(Rango rango) {
        if (rango == null) return "";
        if (rango.min() != null && rango.max() != null) return numero(rango.min()) + " – " + numero(rango.max());
        if (rango.min() != null) return numero(rango.min()) + " o más";
        return "menor a " + numero(rango.max());
    }

    /** Quita el ".0" de los enteros: «120» se lee mejor que «120.0» en un reporte. */
    private static String numero(Double d) {
        if (d == null) return "";
        return d == Math.floor(d) && !d.isInfinite()
                ? String.valueOf(d.longValue())
                : String.valueOf(d);
    }

    /**
     * Un rango con su margen.
     *
     * @param margen cuánto se puede pasar del límite y seguir siendo una diferencia
     *               menor, en las unidades del propio analito; null para deducirlo
     */
    public record Rango(Double min, Double max, Double margen) {

        /** Un rango sin margen configurado. */
        public Rango(Double min, Double max) {
            this(min, max, null);
        }

        /**
         * El margen que se aplica de verdad.
         *
         * <p>Configurado manda, incluso si es cero: un cero significa «cualquier
         * diferencia hay que revisarla», y es una decisión legítima —el colesterol
         * total del protocolo funciona así—, no un campo sin llenar.</p>
         *
         * <p>Sin configurar se deduce: de la amplitud si hay dos límites, y del
         * propio límite si sólo hay uno, porque de un rango abierto no hay amplitud
         * que tomar.</p>
         */
        public double margenEfectivo() {
            if (margen != null) return Math.abs(margen);
            if (min != null && max != null) return Math.abs(max - min) * FRACCION_MARGEN;
            Double unico = min != null ? min : max;
            return unico == null ? 0 : Math.abs(unico) * FRACCION_MARGEN;
        }
    }
}
