package imss.gob.mx.cohorte.services.impresion;

import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.DisposicionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.TipoCodigo;

import java.util.ArrayList;
import java.util.List;

/**
 * Maquetado del contenido de una etiqueta dentro de su recuadro, en dots.
 *
 * Es el equivalente para la Zebra de lo que {@code layoutEtiqueta.ts} hace para
 * la impresión por navegador: decide qué tan grande sale cada elemento para que
 * todo quepa, y devuelve posiciones ya resueltas. Quien emite el ZPL solo coloca.
 *
 * Antes no existía nada así. El generador iba bajando un cursor vertical sin
 * comprobar nunca si se había pasado del alto de la etiqueta, y el código de
 * barras se centraba con un ancho que podía exceder el de la etiqueta: por la
 * derecha se salía sin que nada lo advirtiera. Además la misma lógica estaba
 * escrita dos veces —una para muestras y otra para documentos— y ya habían
 * empezado a divergir.
 */
public final class MaquetadoEtiquetaZpl {

    private MaquetadoEtiquetaZpl() {}

    public enum Tipo { NOMBRE, CODIGO, ETIQUETA }

    /** Un elemento ya colocado, en dots relativos a la esquina de la etiqueta. */
    public record Elemento(
            Tipo tipo,
            int x,
            int y,
            int ancho,
            int alto,
            /** Tamaño de fuente en dots. Solo en NOMBRE y ETIQUETA. */
            int fuente,
            /** Módulo o ancho de barra ya reducido. Solo en CODIGO. */
            int escala
    ) {}

    public record AreaUtil(int x, int y, int ancho, int alto) {}

    public record Resultado(
            AreaUtil areaUtil,
            List<Elemento> elementos,
            /**
             * El contenido no cabe ni en su tamaño mínimo. Se emite igual, pero
             * recortado a la caja: nunca se dibuja fuera. Sirve para avisar que
             * la etiqueta es demasiado chica para lo que se le pidió.
             */
            boolean desbordado,
            boolean codigoReducido,
            boolean fuenteReducida
    ) {}

    /** Por debajo de esto la letra deja de leerse impresa. */
    private static final int MIN_FUENTE_DOTS = 8;

    /** El símbolo más chico que una lectora reconoce con holgura. */
    private static final int MIN_ESCALA = 1;

    /** Módulos en blanco que Code 128 exige a cada lado para poder leerse. */
    private static final int ZONA_MUDA_MODULOS = 10;

    public static Resultado calcular(ConfiguracionEtiqueta config, String nombre,
                                      String etiqueta, String codigoData) {
        int labelW = config.getAnchoDots();
        int labelH = config.getAltoDots();

        AreaUtil area = new AreaUtil(
                config.getMargenIzquierdoDots(),
                config.getMargenSuperiorDots(),
                Math.max(0, labelW - config.getMargenIzquierdoDots() - config.getMargenDerechoDots()),
                Math.max(0, labelH - config.getMargenSuperiorDots() - config.getMargenInferiorDots())
        );

        String datos = (codigoData != null && !codigoData.isEmpty()) ? codigoData : etiqueta;
        int escalaConfigurada = Math.max(MIN_ESCALA, escalaConfigurada(config));

        int escala = escalaConfigurada;
        Medida medida = armar(config, nombre, etiqueta, datos, escala, 1.0, area);

        // Achicar el símbolo: es lo único que reduce el ancho, y de paso el alto.
        while (!cabe(medida, area) && escala > MIN_ESCALA) {
            escala--;
            medida = armar(config, nombre, etiqueta, datos, escala, 1.0, area);
        }

        // La letra solo se achica cuando lo que falta es ALTO. Reducirla porque
        // el símbolo no cabe a lo ancho estropearía el texto sin resolver nada.
        double factorFuente = 1.0;
        while (!cabeAlto(medida, area) && factorFuente > 0.5) {
            factorFuente = Math.round((factorFuente - 0.05) * 100) / 100.0;
            medida = armar(config, nombre, etiqueta, datos, escala, factorFuente, area);
        }

        List<Elemento> elementos = new ArrayList<>();
        int fondo = area.y() + area.alto();
        int y = area.y();
        for (int i = 0; i < medida.piezas.size(); i++) {
            Pieza p = medida.piezas.get(i);
            int ancho = p.ancho > 0 ? Math.min(p.ancho, area.ancho()) : area.ancho();
            int x = area.x() + Math.max(0, (area.ancho() - ancho) / 2);

            // Un elemento que ya no alcanza dentro del recuadro NO se emite.
            //
            // En una hoja bastaría con recortarlo, porque lo que sobresale cae en
            // el papel y se ve feo pero inocuo. En un rollo no: la Zebra dibuja en
            // coordenadas del formato, y lo que se pase del alto de la etiqueta se
            // imprime sobre la siguiente. Perder una línea es preferible a estropear
            // la etiqueta de al lado. El desborde queda avisado en el resultado.
            if (y >= fondo) break;

            int alto = Math.min(p.alto, fondo - y);
            elementos.add(new Elemento(p.tipo, x, y, ancho, alto, p.fuente, p.escala));
            y += p.alto + (i < medida.piezas.size() - 1 ? p.gap : 0);
        }

        return new Resultado(area, elementos, !cabe(medida, area),
                escala < escalaConfigurada, factorFuente < 1.0);
    }

    // ── Interno ─────────────────────────────────────────────────────────────

    private record Pieza(Tipo tipo, int alto, int ancho, int anchoNecesario, int gap,
                          int fuente, int escala) {}

    private record Medida(List<Pieza> piezas, int altoTotal, int anchoMax) {}

    private static boolean cabeAlto(Medida m, AreaUtil a) { return m.altoTotal <= a.alto(); }
    private static boolean cabeAncho(Medida m, AreaUtil a) { return m.anchoMax <= a.ancho(); }
    private static boolean cabe(Medida m, AreaUtil a) { return cabeAlto(m, a) && cabeAncho(m, a); }

    /**
     * Escala con que se pide el símbolo. En los códigos de dos dimensiones la
     * fija el módulo; en Code 128 la fija el ancho de barra, porque el módulo va
     * a la altura de las barras ({@code ^BCN,<modulo*10>}).
     */
    private static int escalaConfigurada(ConfiguracionEtiqueta config) {
        if (config.getTipoCodigo() == TipoCodigo.CODE_128) {
            return config.getAnchoBarraCodigo() != null ? config.getAnchoBarraCodigo() : 2;
        }
        return config.getModuloCodigo();
    }

    private static Medida armar(ConfiguracionEtiqueta config, String nombre, String etiqueta,
                                 String datos, int escala, double factorFuente, AreaUtil area) {
        List<Pieza> piezas = new ArrayList<>();

        for (Tipo tipo : orden(config.getDisposicion())) {
            switch (tipo) {
                case NOMBRE -> {
                    if (!Boolean.TRUE.equals(config.getMostrarNombre())) continue;
                    int f = fuente(config.getTamanoFuenteNombre(), factorFuente);
                    piezas.add(new Pieza(tipo, f, 0, 0, gap(config.getEspaciadoNombre(), 4), f, 0));
                }
                case ETIQUETA -> {
                    if (!Boolean.TRUE.equals(config.getMostrarEtiqueta())) continue;
                    int f = fuente(config.getTamanoFuenteEtiqueta(), factorFuente);
                    piezas.add(new Pieza(tipo, f, 0, 0, gap(config.getEspaciadoEtiqueta(), 4), f, 0));
                }
                case CODIGO -> {
                    if (!Boolean.TRUE.equals(config.getMostrarCodigo())) continue;
                    TipoCodigo t = config.getTipoCodigo();
                    int ancho = anchoCodigo(t, config.getModuloCodigo(), escala, datos.length());
                    int alto = altoCodigo(t, config.getModuloCodigo(), escala, datos.length());
                    // Los lineales necesitan además zona muda; los de dos
                    // dimensiones la llevan dentro del propio símbolo.
                    int necesario = t == TipoCodigo.CODE_128
                            ? ancho + 2 * ZONA_MUDA_MODULOS * escala
                            : ancho;
                    piezas.add(new Pieza(tipo, alto, ancho, necesario,
                            gap(config.getEspaciadoCodigo(), 10), 0, escala));
                }
            }
        }

        // La separación va entre elementos, no después del último: ese sobrante
        // robaba altura útil sin corresponder a nada visible.
        int altoTotal = 0;
        int anchoMax = 0;
        for (int i = 0; i < piezas.size(); i++) {
            Pieza p = piezas.get(i);
            altoTotal += p.alto + (i < piezas.size() - 1 ? p.gap : 0);
            anchoMax = Math.max(anchoMax, p.anchoNecesario);
        }
        return new Medida(piezas, altoTotal, anchoMax);
    }

    private static int fuente(Integer configurada, double factor) {
        int base = configurada != null ? configurada : 16;
        return Math.max(MIN_FUENTE_DOTS, (int) Math.round(base * factor));
    }

    private static int gap(Integer valor, int porOmision) {
        return valor != null ? valor : porOmision;
    }

    private static List<Tipo> orden(DisposicionEtiqueta disposicion) {
        return switch (disposicion) {
            case CODIGO_NOMBRE_ETIQUETA -> List.of(Tipo.CODIGO, Tipo.NOMBRE, Tipo.ETIQUETA);
            case CODIGO_ETIQUETA -> List.of(Tipo.CODIGO, Tipo.ETIQUETA);
            case NOMBRE_ETIQUETA_CODIGO -> List.of(Tipo.NOMBRE, Tipo.ETIQUETA, Tipo.CODIGO);
            default -> List.of(Tipo.NOMBRE, Tipo.CODIGO, Tipo.ETIQUETA);
        };
    }

    /**
     * Ancho del símbolo en dots.
     *
     * En Code 128 cada carácter ocupa 11 módulos, más arranque, dígito de control
     * y cierre: 11n + 35 en el peor caso, el del subconjunto B. Ese total va
     * multiplicado por el ancho de barra, no por el módulo. Es una cota superior
     * —con dígitos seguidos el codificador compacta y sale más angosto—, y para
     * decidir si cabe conviene que lo sea.
     */
    static int anchoCodigo(TipoCodigo tipo, int modulo, int escala, int largoDatos) {
        if (tipo == TipoCodigo.CODE_128) {
            return (11 * largoDatos + 35) * escala;
        }
        return tamanoBidimensional(tipo, escala, largoDatos);
    }

    /**
     * Alto del símbolo en dots. En Code 128 lo marca {@code ^BC} (modulo*10) y no
     * el ancho: usar el ancho aquí empujaba el texto siguiente muy por debajo.
     */
    static int altoCodigo(TipoCodigo tipo, int modulo, int escala, int largoDatos) {
        if (tipo == TipoCodigo.CODE_128) {
            return modulo * 10;
        }
        return tamanoBidimensional(tipo, escala, largoDatos);
    }

    private static int tamanoBidimensional(TipoCodigo tipo, int escala, int largoDatos) {
        return switch (tipo) {
            case DATAMATRIX -> {
                int celdas = (int) Math.ceil(Math.sqrt(largoDatos * 8.0));
                yield Math.max(celdas * escala, escala * 12);
            }
            case QR_CODE -> {
                int celdas = (int) Math.ceil(Math.sqrt(largoDatos * 10.0));
                yield Math.max(celdas * escala, escala * 15);
            }
            default -> 100;
        };
    }
}
