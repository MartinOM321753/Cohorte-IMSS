package imss.gob.mx.cohorte.services.formulas;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Un número con su unidad, o la ausencia de un número.
 *
 * <p>Es el tipo con el que trabaja el motor de fórmulas. El número va en aritmética
 * decimal exacta y nunca en punto flotante: un índice de masa corporal de 24.95
 * redondeado a una décima da 25.0, que cruza el umbral de sobrepeso. En un documento
 * clínico el redondeo no es cosmético, cambia la clasificación.</p>
 *
 * <p>La unidad viaja pegada al número porque sin ella el número miente sin avisar.
 * Con la estatura guardada en centímetros, <code>peso ÷ estatura²</code> da 0.0025 en
 * lugar de 24.98: la fórmula está bien escrita y el resultado está mal, y no hay nada
 * sospechoso que detectar en la operación.</p>
 *
 * <p><b>Ausente</b> es un estado propio, distinto de cero. Si el participante no
 * tiene el peso registrado, su índice de masa corporal no es cero: no se puede
 * calcular. La ausencia se propaga por toda la fórmula hasta la celda, que queda en
 * blanco. Un cero propagado se imprimiría como un dato, y sería falso.</p>
 */
public record Magnitud(BigDecimal valor, Unidad unidad, String valorTexto) {

    /**
     * Precisión de trabajo para las divisiones.
     *
     * <p>Hace falta porque una división puede no terminar —un tercio no tiene
     * representación decimal finita— y sin un límite la operación falla. Treinta y
     * cuatro cifras dejan margen de sobra por encima de cualquier medición clínica,
     * de modo que el redondeo final al imprimir nunca depende de este.</p>
     */
    public static final MathContext PRECISION = MathContext.DECIMAL128;

    private static final Magnitud SIN_DATO = new Magnitud(null, Unidad.NINGUNA, null);

    /** No hay valor con qué calcular. */
    public static Magnitud sinDato() {
        return SIN_DATO;
    }

    public static Magnitud de(BigDecimal valor, Unidad unidad) {
        if (valor == null) return sinDato();
        return new Magnitud(valor, unidad != null ? unidad : Unidad.NINGUNA, null);
    }

    /**
     * Un valor de texto: la opcion elegida en un parametro de opciones.
     *
     * <p>No se puede sumar ni dividir —y el evaluador lo deja en blanco si alguien lo
     * intenta— pero si se puede comparar. Es lo que permite escribir
     * {@code si(manoDominante = 'Derecha', …, …)}, que en el catalogo de esta cohorte
     * es un caso real: la dinamometria se reporta de la mano dominante.</p>
     */
    public static Magnitud deTexto(String texto) {
        if (texto == null || texto.isBlank()) return sinDato();
        return new Magnitud(null, Unidad.NINGUNA, texto.trim());
    }

    public static Magnitud de(BigDecimal valor, String textoUnidad) {
        return de(valor, Unidad.de(textoUnidad));
    }

    /**
     * Desde el {@code Double} en que la base guarda los resultados.
     *
     * <p>Se convierte con {@code BigDecimal.valueOf}, que pasa por la representación
     * decimal del número. El constructor directo haría lo contrario —traer el valor
     * binario exacto— y un 0.1 capturado se volvería
     * 0.1000000000000000055511151231257827, que es fiel a lo que guarda la máquina y
     * no a lo que escribió quien capturó.</p>
     */
    public static Magnitud de(Double valor, String textoUnidad) {
        if (valor == null || valor.isNaN() || valor.isInfinite()) return sinDato();
        return de(BigDecimal.valueOf(valor), Unidad.de(textoUnidad));
    }

    public boolean ausente() {
        return valor == null && valorTexto == null;
    }

    public boolean presente() {
        return !ausente();
    }

    /** Lleva texto y no numero: solo sirve para comparar. */
    public boolean esTexto() {
        return valorTexto != null;
    }

    /**
     * La misma cantidad expresada en otra unidad.
     *
     * <p>Si no hay forma de convertir —dimensiones distintas, o alguna unidad que no
     * se reconoció— el resultado es <b>ausente</b>, no el valor sin tocar. Devolver
     * el número original con la etiqueta de la otra unidad sería imprimir 165 metros
     * donde hay 165 centímetros: un dato falso con aspecto de bueno. Vale más la
     * celda en blanco.</p>
     *
     * <p>Por la pantalla este caso no debería llegar nunca, porque el selector solo
     * ofrece unidades de la misma dimensión.</p>
     */
    public Magnitud en(Unidad destino) {
        // Un texto no tiene unidad que convertir; se queda como esta.
        if (esTexto()) return this;
        if (ausente() || destino == null) return sinDato();
        if (unidad.equals(destino)) return this;
        if (!unidad.convertibleA(destino)) return sinDato();

        BigDecimal canonica = valor.multiply(unidad.haciaCanonica());
        return new Magnitud(canonica.divide(destino.haciaCanonica(), PRECISION), destino, null);
    }

    /**
     * El número tal como se imprime.
     *
     * <p>Aquí, y solo aquí, se redondea. Todo el cálculo anterior corre con la
     * precisión completa, y la comparación contra un rango de referencia también:
     * redondear antes de comparar es lo que convierte un 24.95 en sobrepeso.</p>
     *
     * <p>Se redondea con la regla de la mitad hacia arriba, que es la que espera
     * quien lee un resultado clínico. Sin decimales indicados se imprime el valor
     * como está, sin ceros de relleno.</p>
     */
    public String texto(Integer decimales) {
        if (ausente()) return "";
        if (esTexto()) return valorTexto;
        if (decimales == null) return valor.stripTrailingZeros().toPlainString();
        return valor.setScale(decimales, RoundingMode.HALF_UP).toPlainString();
    }

    /** El número con su unidad, como se lee en una celda: «24.98 kg/m²». */
    public String textoConUnidad(Integer decimales) {
        String numero = texto(decimales);
        if (numero.isEmpty()) return "";
        String simbolo = unidad.nombre();
        return simbolo.isBlank() ? numero : numero + " " + simbolo;
    }

    @Override
    public String toString() {
        return ausente() ? "(sin dato)" : textoConUnidad(null);
    }
}
