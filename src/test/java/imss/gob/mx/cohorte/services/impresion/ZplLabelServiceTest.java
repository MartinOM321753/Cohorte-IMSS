package imss.gob.mx.cohorte.services.impresion;

import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.DisposicionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.TipoCodigo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * El ZPL que sale de aquí va directo al cabezal de la Zebra, sin vista previa que
 * lo revise. Estas pruebas fijan lo que se venía haciendo mal: el origen heredado
 * de trabajos anteriores, el ancho de impresión mayor que el cabezal, el número
 * de etiquetas por avance tomado de la configuración de hoja, y el contenido sin
 * nada que lo contuviera dentro del recuadro.
 */
class ZplLabelServiceTest {

    private final ZplLabelService service = new ZplLabelService();

    /** Rollo de 3 carriles, etiqueta 33x22 mm a 203 dpi. */
    private ConfiguracionEtiqueta configRollo() {
        ConfiguracionEtiqueta c = new ConfiguracionEtiqueta();
        c.setAnchoMm(33.0);
        c.setAltoMm(22.0);
        c.setDpi(203);
        c.setEtiquetasPorFila(3);
        c.setCarrilesRollo(3);
        c.setAnchoCabezalMm(104.0);
        c.setMargenIzquierdoMm(2.5);
        c.setMargenSuperiorMm(2.0);
        c.setTipoCodigo(TipoCodigo.CODE_128);
        c.setModuloCodigo(6);
        c.setAnchoBarraCodigo(2);
        c.setTamanoFuenteNombre(16);
        c.setTamanoFuenteEtiqueta(16);
        c.setDisposicion(DisposicionEtiqueta.NOMBRE_CODIGO_ETIQUETA);
        return c;
    }

    private String zplDeDocumento(ConfiguracionEtiqueta config, boolean marco) {
        var doc = new imss.gob.mx.cohorte.modules.documentos.Documento();
        doc.setEtiqueta("D26-01-C-00001.PDF");
        doc.setNombreOriginal("consentimiento.pdf");
        return service.generarZplDocumentos(java.util.List.of(doc), config, false, marco);
    }

    @Test
    @DisplayName("emite ^LH explícito para no heredar el origen de la impresora")
    void emiteLabelHome() {
        String zpl = zplDeDocumento(configRollo(), false);
        assertTrue(zpl.contains("^LH0,0"),
                "sin ^LH el formato hereda el origen guardado en la impresora y el margen izquierdo se corre");
        assertTrue(zpl.contains("^LT0"), "^LT también sobrevive entre trabajos y hay que reiniciarlo");
    }

    @Test
    @DisplayName("^LH refleja el desplazamiento de calibración configurado")
    void labelHomeUsaCalibracion() {
        ConfiguracionEtiqueta c = configRollo();
        c.setOffsetLhXDots(24);
        c.setOffsetLhYDots(-8);
        assertTrue(zplDeDocumento(c, false).contains("^LH24,-8"));
    }

    @Test
    @DisplayName("^PW nunca excede el ancho del cabezal")
    void anchoImpresionAcotado() {
        ConfiguracionEtiqueta c = configRollo();
        // 3 carriles x 33 mm = 99 mm, que cabe en el cabezal de 104 mm.
        // Se calcula igual que la entidad, truncando, no redondeando.
        int anchoEtiquetaDots = (int) (33.0 / 25.4 * 203);
        assertEquals(3 * anchoEtiquetaDots, anchoDePW(zplDeDocumento(c, false)),
                "con 3 carriles de 33 mm el ancho pedido cabe y debe respetarse");

        // Con 6 carriles se piden 198 mm sobre un cabezal de 104: hay que recortar
        c.setCarrilesRollo(6);
        int cabezalDots = (int) (104.0 / 25.4 * 203);
        assertEquals(cabezalDots, anchoDePW(zplDeDocumento(c, false)),
                "pedir mas ancho que el cabezal hace que la Zebra recorte por su cuenta y corra el origen");
    }

    @Test
    @DisplayName("agrupa por carriles del rollo, no por columnas de la hoja")
    void agrupaPorCarriles() {
        ConfiguracionEtiqueta c = configRollo();
        c.setEtiquetasPorFila(3);   // columnas de la hoja Avery
        c.setCarrilesRollo(1);      // pero el rollo es de un solo carril

        var muestras = new java.util.ArrayList<imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra>();
        for (int i = 0; i < 3; i++) {
            var m = new imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra();
            m.setEtiqueta("SG/00000" + i + "/I1F4-L1");
            muestras.add(m);
        }

        String zpl = service.generarZplLote(muestras, c, false);
        assertEquals(3, contar(zpl, "^XA"),
                "un rollo de un carril consume una etiqueta por avance, no tres");
    }

    @Test
    @DisplayName("el marco de depuración se emite solo cuando se pide")
    void marcoOpcional() {
        assertFalse(zplDeDocumento(configRollo(), false).contains("^GB"));
        assertTrue(zplDeDocumento(configRollo(), true).contains("^GB"));
    }

    @Test
    @DisplayName("ninguna coordenada cae fuera del recuadro de la etiqueta")
    void contenidoDentroDelRecuadro() {
        ConfiguracionEtiqueta c = configRollo();
        // Etiqueta diminuta con fuentes y modulo enormes: el peor caso.
        c.setAnchoMm(20.0);
        c.setAltoMm(10.0);
        c.setCarrilesRollo(1);
        c.setModuloCodigo(20);
        c.setAnchoBarraCodigo(8);
        c.setTamanoFuenteNombre(60);
        c.setTamanoFuenteEtiqueta(60);

        String zpl = zplDeDocumento(c, false);
        int anchoDots = (int) (20.0 / 25.4 * 203);
        int altoDots = (int) (10.0 / 25.4 * 203);

        Matcher m = Pattern.compile("\\^FO(-?\\d+),(-?\\d+)").matcher(zpl);
        int encontrados = 0;
        while (m.find()) {
            int x = Integer.parseInt(m.group(1));
            int y = Integer.parseInt(m.group(2));
            encontrados++;
            assertTrue(x >= 0 && x < anchoDots,
                    "x=" + x + " fuera del recuadro de " + anchoDots + " dots");
            assertTrue(y >= 0 && y < altoDots,
                    "y=" + y + " fuera del recuadro de " + altoDots + " dots");
        }
        assertTrue(encontrados > 0, "deberia haberse emitido algun elemento");
    }

    @Test
    @DisplayName("el maquetado reduce el símbolo antes que la letra")
    void reduceSimboloAntesQueLetra() {
        ConfiguracionEtiqueta c = configRollo();
        c.setAnchoMm(25.0);
        c.setAnchoBarraCodigo(8);

        var r = MaquetadoEtiquetaZpl.calcular(c, "Juan Perez", "D26-01-C-00001.PDF", null);
        assertTrue(r.codigoReducido(), "el simbolo no cabia y debio achicarse");
        assertFalse(r.fuenteReducida(),
                "achicar la letra no angosta el codigo: no debe tocarse por un problema de ancho");
    }

    // ── Acomodo por carriles ────────────────────────────────────────────────

    private imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra muestra(String etiqueta) {
        var m = new imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra();
        m.setEtiqueta(etiqueta);
        return m;
    }

    @Test
    @DisplayName("cada etiqueta se dibuja en el carril que le tocó")
    void acomodoRespetaElCarril() {
        ConfiguracionEtiqueta c = configRollo();   // 3 carriles, 33 mm
        int labelW = c.getAnchoDots();

        // Carril 0 vacío, carril 1 con la etiqueta, carril 2 vacío
        String zpl = service.generarZplAcomodado(
                java.util.Arrays.asList(null, muestra("SG/000001/I1F4-L1"), null), c, false);

        assertEquals(1, contar(zpl, "^XA"), "es una sola fila de avance");

        Matcher m = Pattern.compile("\\^FO(\\d+),").matcher(zpl);
        boolean alguno = false;
        while (m.find()) {
            int x = Integer.parseInt(m.group(1));
            assertTrue(x >= labelW && x < 2 * labelW,
                    "x=" + x + " deberia caer en el carril 1 (" + labelW + " a " + (2 * labelW) + ")");
            alguno = true;
        }
        assertTrue(alguno, "no se dibujo nada");
    }

    @Test
    @DisplayName("una fila enteramente vacía no gasta rollo")
    void filaVaciaNoSeImprime() {
        ConfiguracionEtiqueta c = configRollo();
        String zpl = service.generarZplAcomodado(java.util.Arrays.asList(
                muestra("A/000001/I1F4-L1"), null, null,   // fila 1: tiene contenido
                null, null, null,                          // fila 2: vacía, se omite
                null, null, muestra("B/000002/I1F4-L1")    // fila 3: tiene contenido
        ), c, false);

        assertEquals(2, contar(zpl, "^XA"),
                "solo deben emitirse las dos filas con etiquetas; la vacía gastaría una vuelta de rollo para nada");
    }

    @Test
    @DisplayName("el acomodo se corta en filas del tamaño del rollo")
    void acomodoSeCortaPorCarriles() {
        ConfiguracionEtiqueta c = configRollo();   // 3 carriles
        var slots = new java.util.ArrayList<imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra>();
        for (int i = 0; i < 7; i++) slots.add(muestra("M/00000" + i + "/I1F4-L1"));

        // 7 etiquetas en 3 carriles -> 3 avances (3 + 3 + 1)
        assertEquals(3, contar(service.generarZplAcomodado(slots, c, false), "^XA"));
    }

    private int anchoDePW(String zpl) {
        Matcher m = Pattern.compile("\\^PW(\\d+)").matcher(zpl);
        assertTrue(m.find(), "no se emitio ^PW");
        return Integer.parseInt(m.group(1));
    }

    private int contar(String texto, String aguja) {
        int n = 0, i = 0;
        while ((i = texto.indexOf(aguja, i)) != -1) { n++; i += aguja.length(); }
        return n;
    }
}
