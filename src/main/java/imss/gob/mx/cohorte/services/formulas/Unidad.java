package imss.gob.mx.cohorte.services.formulas;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Una unidad de medida, con lo necesario para convertirla.
 *
 * <p>El catálogo del sistema guarda la unidad como texto libre, y ahí conviven las
 * unidades de verdad con cosas que no lo son. Esta clase reconoce las que sí lo son
 * y les asigna qué miden y cuánto valen respecto a la unidad de referencia de su
 * dimensión; a las demás las deja pasar tal cual, con su nombre intacto.</p>
 *
 * <p><b>No reconocer una unidad no es un error.</b> El valor se sigue pudiendo usar
 * en una fórmula: lo único que no se puede hacer con él es ofrecer un cambio de
 * unidad, porque no hay factor que aplicar. Rechazarlo sería imponer un criterio que
 * no nos toca.</p>
 *
 * <p>Se conservan unidades que hoy ningún parámetro usa —gramos, mililitros,
 * minutos— porque el cambio de unidad se ofrece contra este catálogo, no contra lo
 * que esté capturado. Sin g/dL, por ejemplo, no habría forma de pasar la albúmina
 * desde los mg/dL en que está declarada.</p>
 */
public record Unidad(String nombre, Dimension dimension, BigDecimal haciaCanonica) {

    /** Un valor sin unidad declarada. No es lo mismo que una unidad no reconocida. */
    public static final Unidad NINGUNA = new Unidad("", Dimension.ADIMENSIONAL, BigDecimal.ONE);

    /**
     * Hay unidad, pero no tiene nombre.
     *
     * <p>Es lo que sale de multiplicar o dividir dos cantidades con unidad: metros al
     * cuadrado, o kilogramos entre metros al cuadrado, no están en ningún catálogo y no
     * se pueden nombrar sin montar un álgebra de dimensiones entera.</p>
     *
     * <p>Hace falta distinguirla de {@link #NINGUNA} porque las dos se comportan al
     * revés al seguir operando: multiplicar por un número pelado conserva la unidad del
     * otro lado, y multiplicar por algo que sí tiene unidad no la conserva. Sin esta
     * distinción, «peso ÷ estatura²» acabaría etiquetado como kilogramos.</p>
     */
    public static final Unidad DERIVADA = new Unidad("", Dimension.DESCONOCIDA, BigDecimal.ONE);

    /**
     * Las unidades conocidas, en el orden en que conviene ofrecerlas.
     *
     * <p>El factor lleva a la unidad de referencia de cada dimensión: metro,
     * kilogramo, segundo, milímetro de mercurio, litro. Cuál sea la de referencia da
     * igual mientras todas las de una dimensión usen la misma.</p>
     */
    private static final List<Unidad> CONOCIDAS = List.of(
            // ── Longitud ────────────────────────────────────────────────────────
            unidad("mm",    Dimension.LONGITUD, "0.001"),
            unidad("cm",    Dimension.LONGITUD, "0.01"),
            unidad("m",     Dimension.LONGITUD, "1"),

            // ── Masa ────────────────────────────────────────────────────────────
            unidad("mg",    Dimension.MASA, "0.000001"),
            unidad("g",     Dimension.MASA, "0.001"),
            unidad("kg",    Dimension.MASA, "1"),

            // ── Tiempo ──────────────────────────────────────────────────────────
            unidad("ms",       Dimension.TIEMPO, "0.001"),
            unidad("segundos", Dimension.TIEMPO, "1"),
            unidad("minutos",  Dimension.TIEMPO, "60"),
            unidad("horas",    Dimension.TIEMPO, "3600"),
            unidad("días",     Dimension.TIEMPO, "86400"),
            unidad("años",     Dimension.TIEMPO, "31557600"),

            // ── Presión ─────────────────────────────────────────────────────────
            unidad("mmHg",  Dimension.PRESION, "1"),
            unidad("kPa",   Dimension.PRESION, "7.50062"),

            // ── Volumen ─────────────────────────────────────────────────────────
            unidad("mL",     Dimension.VOLUMEN, "0.001"),
            unidad("Litros", Dimension.VOLUMEN, "1"),

            // ── Flujo ───────────────────────────────────────────────────────────
            unidad("L/s",   Dimension.FLUJO, "60"),
            unidad("L/min", Dimension.FLUJO, "1"),

            // ── Concentración ───────────────────────────────────────────────────
            // Están las dos porque el reporte de salud imprime la albúmina en g/dL
            // y el catálogo la declara en mg/dL.
            unidad("mg/dL", Dimension.CONCENTRACION, "1"),
            unidad("g/dL",  Dimension.CONCENTRACION, "1000"),

            // ── Resto, sin pariente con quien convertirse ────────────────────────
            unidad("U/L",       Dimension.ACTIVIDAD_ENZIMATICA, "1"),
            unidad("10³/µL",    Dimension.CONTEO_POR_VOLUMEN, "1"),
            unidad("LPM",       Dimension.FRECUENCIA, "1"),
            unidad("mm/h",      Dimension.VELOCIDAD, "1"),
            unidad("kg/m²",     Dimension.MASA_POR_SUPERFICIE, "1"),
            unidad("kcal/día",  Dimension.ENERGIA_POR_TIEMPO, "1"),
            unidad("dB/m",      Dimension.ATENUACION, "1"),
            unidad("Ω",         Dimension.RESISTENCIA_ELECTRICA, "1"),
            unidad("%",         Dimension.ADIMENSIONAL, "1"),
            unidad("Puntos",    Dimension.ADIMENSIONAL, "1")
    );

    /**
     * Cómo está escrita cada unidad en el catálogo del sistema.
     *
     * <p>La clave va normalizada —sin acentos, en minúsculas y sin espacios—, así que
     * «Milímetros», «milimetros» y «MILÍMETROS» caen en la misma entrada. Aquí se
     * resuelven también los sinónimos que trae el catálogo: «metros» junto a «m»,
     * «Latidos por minuto LPM» junto a «LPM».</p>
     */
    private static final Map<String, Unidad> POR_TEXTO = indexar();

    private static Unidad unidad(String nombre, Dimension d, String factor) {
        return new Unidad(nombre, d, new BigDecimal(factor));
    }

    private static Map<String, Unidad> indexar() {
        Map<String, Unidad> mapa = new LinkedHashMap<>();
        for (Unidad u : CONOCIDAS) mapa.put(normalizar(u.nombre()), u);

        // Sinónimos tal como aparecen escritos en el catálogo del sistema.
        alias(mapa, "mm",       "milimetros", "milimetro");
        alias(mapa, "cm",       "centimetros", "centimetro");
        alias(mapa, "m",        "metros", "metro");
        alias(mapa, "kg",       "kilogramos", "kilogramo");
        alias(mapa, "g",        "gramos", "gramo");
        alias(mapa, "ms",       "milisegundos", "milisegundo");
        alias(mapa, "segundos", "segundo", "seg", "s");
        alias(mapa, "minutos",  "minuto", "min");
        alias(mapa, "horas",    "hora", "h");
        alias(mapa, "años",     "año", "anios", "anio");
        alias(mapa, "días",     "dia", "dias");
        alias(mapa, "LPM",      "latidos por minuto lpm", "latidos por minuto");
        alias(mapa, "Litros",   "litro", "l");
        alias(mapa, "mL",       "mililitros", "mililitro");
        alias(mapa, "10³/µL",   "10³ / μl", "10^3/ul", "miles por microlitro");
        alias(mapa, "kg/m²",    "kg/m^2");
        alias(mapa, "Puntos",   "punto", "puntaje");
        return mapa;
    }

    private static void alias(Map<String, Unidad> mapa, String nombre, String... comoSeEscribe) {
        Unidad u = mapa.get(normalizar(nombre));
        for (String texto : comoSeEscribe) mapa.putIfAbsent(normalizar(texto), u);
    }

    /**
     * La unidad que corresponde a un texto del catálogo.
     *
     * <p>Sin texto se devuelve {@link #NINGUNA}. Con un texto que no está en la
     * lista se devuelve una unidad que conserva ese nombre y queda marcada como
     * desconocida: se imprime igual que siempre y solo pierde la conversión.</p>
     */
    public static Unidad de(String texto) {
        if (texto == null || texto.isBlank()) return NINGUNA;
        Unidad conocida = POR_TEXTO.get(normalizar(texto));
        return conocida != null
                ? conocida
                : new Unidad(texto.trim(), Dimension.DESCONOCIDA, BigDecimal.ONE);
    }

    /**
     * Las unidades que se le pueden ofrecer a quien inserta una variable.
     *
     * <p>Solo las de la misma dimensión, la propia incluida. Una unidad desconocida
     * no tiene alternativas: devolverla sola es lo honesto, porque no hay ningún
     * factor con el que llevarla a otra parte.</p>
     */
    public List<Unidad> intercambiables() {
        if (sinParientes()) return List.of(this);
        List<Unidad> mismas = new ArrayList<>();
        for (Unidad u : CONOCIDAS) {
            if (u.dimension() == dimension) mismas.add(u);
        }
        return mismas.isEmpty() ? List.of(this) : List.copyOf(mismas);
    }

    /** ¿Se puede pasar de esta a la otra sin inventar nada? */
    public boolean convertibleA(Unidad otra) {
        if (otra == null) return false;
        if (this.equals(otra)) return true;
        if (sinParientes() || otra.sinParientes()) return false;
        return dimension == otra.dimension();
    }

    /**
     * Las dimensiones donde cambiar de unidad no significa nada.
     *
     * <p>Una desconocida no tiene factor. Y en las adimensionales conviven cosas que
     * comparten el no llevar unidad física pero no se parecen en nada: un porcentaje,
     * un puntaje de escala y un valor sin unidad. Ofrecer pasar de «%» a «Puntos»
     * sería ofrecer un disparate.</p>
     */
    private boolean sinParientes() {
        return dimension == Dimension.DESCONOCIDA || dimension == Dimension.ADIMENSIONAL;
    }

    /** Solo el nombre; es lo que se imprime junto al número. */
    @Override
    public String toString() {
        return nombre;
    }

    /**
     * Minúsculas, sin acentos y sin espacios sobrantes.
     *
     * <p>El signo de micro se unifica antes que nada: existe como «µ» y como «μ»
     * —dos caracteres distintos que se dibujan igual— y el catálogo usa uno mientras
     * que aquí se escribe el otro. Sin esto, «10³ / μL» no encontraría su entrada.</p>
     */
    private static String normalizar(String texto) {
        String microUnificada = texto.trim().replace('µ', 'μ');
        String sinAcentos = Normalizer
                .normalize(microUnificada.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.replaceAll("\\s+", " ").replace(" / ", "/");
    }
}
