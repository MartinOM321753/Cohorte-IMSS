package imss.gob.mx.cohorte.services.formulas;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dónde cae un valor entre los cortes de una tabla de percentiles.
 *
 * <p>Son cuentas puras: reciben los once cortes ya escogidos para la edad y el sexo
 * del participante y no saben de dónde salieron. Por eso viven con el evaluador y no
 * con las tablas.</p>
 *
 * <h2>Lo que la tabla no dice no se inventa</h2>
 * <p>La tabla da once puntos, del P5 al P95. Entre dos cortes se interpola en línea
 * recta, que es lo más que se puede afirmar. Fuera de ellos no hay número: un valor
 * por debajo del P5 está «por debajo del P5», no en el percentil 3, y extrapolar sería
 * fabricar un dato clínico.</p>
 */
public final class Percentiles {

    /** Los percentiles que marcan los cortes, en el orden de la tabla. */
    public static final int[] PERCENTILES = {5, 10, 20, 30, 40, 50, 60, 70, 80, 90, 95};

    private Percentiles() {}

    /**
     * El percentil por interpolación lineal entre los dos cortes vecinos.
     *
     * @return {@code null} si el valor queda fuera del P5–P95 o los cortes no son once
     */
    public static BigDecimal interpolar(BigDecimal valor, List<BigDecimal> cortes) {
        if (valor == null || !cortesValidos(cortes)) return null;

        int ultimo = PERCENTILES.length - 1;
        if (valor.compareTo(cortes.get(0)) < 0 || valor.compareTo(cortes.get(ultimo)) > 0) return null;

        for (int i = 0; i < ultimo; i++) {
            BigDecimal abajo = cortes.get(i);
            BigDecimal arriba = cortes.get(i + 1);
            if (valor.compareTo(arriba) > 0) continue;

            BigDecimal ancho = arriba.subtract(abajo);
            BigDecimal base = BigDecimal.valueOf(PERCENTILES[i]);
            if (ancho.signum() == 0) return base;

            BigDecimal pasos = BigDecimal.valueOf(PERCENTILES[i + 1] - PERCENTILES[i]);
            BigDecimal fraccion = valor.subtract(abajo).divide(ancho, Magnitud.PRECISION);
            return base.add(fraccion.multiply(pasos));
        }
        return BigDecimal.valueOf(PERCENTILES[ultimo]);
    }

    /**
     * El tramo en que cae, escrito: «P40–P50», «&lt; P5», «&gt; P95». Un valor que
     * coincide con un corte se nombra por él: «P50».
     *
     * @return {@code null} si los cortes no son once
     */
    public static String banda(BigDecimal valor, List<BigDecimal> cortes) {
        if (valor == null || !cortesValidos(cortes)) return null;

        int ultimo = PERCENTILES.length - 1;
        if (valor.compareTo(cortes.get(0)) < 0) return "< P" + PERCENTILES[0];
        if (valor.compareTo(cortes.get(ultimo)) > 0) return "> P" + PERCENTILES[ultimo];

        for (int i = 0; i <= ultimo; i++) {
            if (valor.compareTo(cortes.get(i)) == 0) return "P" + PERCENTILES[i];
            if (i < ultimo && valor.compareTo(cortes.get(i + 1)) < 0) {
                return "P" + PERCENTILES[i] + "–P" + PERCENTILES[i + 1];
            }
        }
        return null;
    }

    private static boolean cortesValidos(List<BigDecimal> cortes) {
        return cortes != null && cortes.size() == PERCENTILES.length
                && cortes.stream().allMatch(java.util.Objects::nonNull);
    }
}
