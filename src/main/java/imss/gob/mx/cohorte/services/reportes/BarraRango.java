package imss.gob.mx.cohorte.services.reportes;

/**
 * Dónde se dibujan la franja del rango y la marca del resultado dentro de la barra.
 *
 * <p>Es la única parte del reporte cuya <b>geometría sale del dato</b> y no del
 * diseño. Todo lo demás en una plantilla lleva sus milímetros escritos; aquí la
 * franja y la marca se colocan en porcentajes que dependen del rango de esa persona
 * y de su resultado, así que se calculan al imprimir.</p>
 *
 * <h3>La escala</h3>
 *
 * <p>El rango ocupa siempre <b>la mitad central</b> de la barra: con dos límites, la
 * franja va del 25 % al 75 %. No se toma una escala «bonita» —60 a 140 para una
 * glucosa de 70 a 99— porque esa la elige una persona mirando el analito, y aquí hay
 * que servir a cualquier parámetro que alguien dé de alta mañana, incluidos los que
 * no tienen convención.</p>
 *
 * <p>Con un solo límite no hay amplitud de la que partir, así que la escala va de
 * cero al doble del límite: un techo de 200 deja la franja del 0 % al 50 %, y un piso
 * de 50 la deja del 50 % al 100 %. Se lee igual de bien y sigue siendo previsible.</p>
 *
 * <h3>Por qué la marca se recorta a los extremos</h3>
 *
 * <p>Un resultado muy alejado —una glucosa de 400 con techo en 99— caería fuera de la
 * barra y no se dibujaría. La marca se sujeta al 2 % y al 98 % para que siempre se
 * vea: <b>que el valor esté fuera de la escala no puede hacer que desaparezca</b>,
 * que es precisamente el caso en el que más importa verlo.</p>
 */
public record BarraRango(double franjaIzquierda, double franjaAncho, double marca,
                         boolean marcaVisible) {

    /** Lo que se deja a cada lado para que la marca no se salga del dibujo. */
    private static final double TOPE_MINIMO = 2;
    private static final double TOPE_MAXIMO = 98;

    /**
     * Calcula la barra, o null si no hay nada que dibujar.
     *
     * <p>Sin rango no hay barra: una franja sin referencia no dice nada, y una marca
     * suelta sobre una pista vacía se leería como una posición que significa algo.</p>
     */
    public static BarraRango de(Double valor, RangoReferencia.Rango rango) {
        if (rango == null) return null;
        Double min = rango.min();
        Double max = rango.max();
        if (min == null && max == null) return null;

        double escala0;
        double escala1;
        if (min != null && max != null) {
            double amplitud = max - min;
            if (amplitud <= 0) return null;          // rango invertido o de un punto
            escala0 = min - amplitud / 2;
            escala1 = max + amplitud / 2;
        } else {
            double limite = min != null ? min : max;
            if (limite <= 0) return null;            // sin escala positiva no hay barra
            escala0 = 0;
            escala1 = limite * 2;
        }

        double total = escala1 - escala0;
        double izquierda = min != null ? porcentaje(min, escala0, total) : 0;
        double derecha = max != null ? porcentaje(max, escala0, total) : 100;

        double posicion = 0;
        boolean visible = false;
        if (valor != null) {
            visible = true;
            posicion = Math.max(TOPE_MINIMO,
                       Math.min(TOPE_MAXIMO, porcentaje(valor, escala0, total)));
        }

        return new BarraRango(izquierda, Math.max(0, derecha - izquierda), posicion, visible);
    }

    private static double porcentaje(double v, double escala0, double total) {
        return (v - escala0) / total * 100;
    }

    /** Redondeado a una décima: más precisión no cambia un píxel y alarga el HTML. */
    public String franjaIzquierdaCss() { return redondear(franjaIzquierda); }
    public String franjaAnchoCss()     { return redondear(franjaAncho); }
    public String marcaCss()           { return redondear(marca); }

    private static String redondear(double v) {
        return String.valueOf(Math.round(v * 10) / 10.0);
    }
}
