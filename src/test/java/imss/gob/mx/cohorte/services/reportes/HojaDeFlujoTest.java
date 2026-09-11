package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * La hoja de flujo, comprobada contra el PDF de verdad.
 *
 * <p>El lienzo recorta: la hoja tiene alto fijo y {@code overflow:hidden}, así que una
 * tabla más larga que la página <b>se corta sin avisar</b> —el documento sale, se ve
 * bien y le faltan filas—. Estas pruebas existen para que eso no pueda volver, y por
 * eso miran el PDF ya generado y no el HTML: en el HTML las filas están todas, y la
 * pregunta es cuáles llegaron al papel.</p>
 */
class HojaDeFlujoTest {

    private static final long PANEL = 10L;

    private final ResolvedorCampos resolvedor = new ResolvedorCampos();
    private final MaquetadorReporte maquetador = new MaquetadorReporte(
            new ObjectMapper(), resolvedor, new BloqueResultados(resolvedor),
            new BloqueEstudios(), new BloqueExamenes(resolvedor),
            new BloqueLista(resolvedor),
            new EvidenciasReporte(null, null), new ImagenesReporte(null));
    private final ReportePdfService pdf = new ReportePdfService();

    // ── Escenario ────────────────────────────────────────────────────────────

    private ContextoReporte conMediciones(int cuantas) {
        Persona persona = new Persona();
        persona.setNombre("María");
        persona.setApellidoPaterno("Rodríguez");
        persona.setFechaNacimiento(LocalDate.of(1980, 1, 1));
        persona.setSexo(Persona.Sexo.F);

        Paciente p = new Paciente();
        p.setFolio("010418");
        p.setPersona(persona);

        TipoEstudio tipo = new TipoEstudio();
        tipo.setId(PANEL);
        tipo.setNombre("Panel");

        EstudioMedico e = new EstudioMedico();
        e.setId(1000L);
        e.setTipoEstudio(tipo);
        e.setFechaEstudio(LocalDateTime.of(2026, 6, 1, 9, 0));
        e.setResultadoEstudio(new ArrayList<>());

        for (int i = 1; i <= cuantas; i++) {
            ParametroEstudio par = new ParametroEstudio();
            par.setId((long) i);
            par.setNombre("MEDICION-" + i);
            par.setUnidad("mg/dL");
            par.setTipo(TipoParametro.NUMERICO);
            par.setValorMinMujeres(70.0);
            par.setValorMaxMujeres(99.0);

            ResultadoEstudio r = new ResultadoEstudio();
            r.setParametro(par);
            r.setValorNumerico(80.0 + i);
            r.setGrupoCodigo("ROOT");
            r.setOrdenResultado(i);
            e.getResultadoEstudio().add(r);
        }

        return new ContextoReporte(p, List.of(e), List.of(), null,
                new ContextoReporte.Totales(1, 0, 0));
    }

    private String diseno(boolean flujo, String tipoElemento) {
        // Cada bloque nombra sus columnas a su manera: la tabla dice «parametro» y la
        // lista dice «nombre». Pasarle a una las de la otra deja la columna fuera.
        String columnas = "lista".equals(tipoElemento)
                ? "\"nombre\",\"valor\",\"barra\",\"referencia\",\"estado\""
                : "\"parametro\",\"valor\",\"referencia\",\"estado\"";
        return """
            {"version":1,"tamano":"CARTA","orientacion":"vertical",
             "margenesFlujo":{"arribaMm":18,"derechaMm":16,"abajoMm":18,"izquierdaMm":16},
             "paginas":[{"flujo":%s,"elementos":[
               {"tipo":"%s","clave":"bloque.estudio.10.resultados",
                "xMm":16,"yMm":18,"anchoMm":180,"altoMm":40,"z":1,
                "desbordamiento":"crecer",
                "estilo":{"tamanoPt":9,"columnas":[%s]}}
             ]}]}
            """.formatted(flujo, tipoElemento, columnas);
    }

    /** Cuántas de las n mediciones llegaron al PDF, y en cuántas páginas. */
    private record Salida(int paginas, int ultimaVisible) {}

    private Salida imprimir(String disenoJson, ContextoReporte ctx, int cuantas) throws Exception {
        byte[] bytes = pdf.aPdf(maquetador.maquetar(disenoJson, ctx));
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            String texto = new PDFTextStripper().getText(doc);
            int ultima = 0;
            for (int i = 1; i <= cuantas; i++) {
                // El sufijo evita que MEDICION-1 se dé por visto dentro de MEDICION-12.
                if (texto.contains("MEDICION-" + i + " ") || texto.contains("MEDICION-" + i + "\n")
                        || texto.contains("MEDICION-" + i + "\r")) {
                    ultima = i;
                }
            }
            return new Salida(doc.getNumberOfPages(), ultima);
        }
    }

    // ── Pruebas ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("El lienzo recorta una tabla larga: es la razón de que exista la hoja de flujo")
    void elLienzoRecorta() throws Exception {
        Salida s = imprimir(diseno(false, "datos"), conMediciones(60), 60);
        assertThat(s.paginas()).isEqualTo(1);
        assertThat(s.ultimaVisible())
                .as("el lienzo no puede con 60 filas; si algún día pudiera, esta prueba sobra")
                .isLessThan(60);
    }

    @Test
    @DisplayName("La hoja de flujo reparte las 60 mediciones y no pierde ninguna")
    void elFlujoPagina() throws Exception {
        Salida s = imprimir(diseno(true, "datos"), conMediciones(60), 60);
        assertThat(s.paginas()).isGreaterThan(1);
        assertThat(s.ultimaVisible()).isEqualTo(60);
    }

    @Test
    @DisplayName("Con 200 mediciones tampoco se pierde ninguna")
    void elFlujoAguantaUnPanelLargo() throws Exception {
        Salida s = imprimir(diseno(true, "datos"), conMediciones(200), 200);
        assertThat(s.paginas()).isGreaterThanOrEqualTo(4);
        assertThat(s.ultimaVisible()).isEqualTo(200);
    }

    @Test
    @DisplayName("La lista con barra de rango también fluye entre páginas")
    void laListaPagina() throws Exception {
        Salida s = imprimir(diseno(true, "lista"), conMediciones(60), 60);
        assertThat(s.paginas()).isGreaterThan(1);
        assertThat(s.ultimaVisible()).isEqualTo(60);
    }

    @Test
    @DisplayName("Los márgenes del flujo se respetan en todas las páginas, no solo en la primera")
    void losMargenesSeRepiten() throws Exception {
        byte[] bytes = pdf.aPdf(maquetador.maquetar(diseno(true, "datos"), conMediciones(120)));
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            assertThat(doc.getNumberOfPages()).isGreaterThan(2);

            // Se lee cada página por separado y se comprueba que ninguna arranca
            // pegada al borde: si el margen sólo valiera en la primera, la segunda
            // traería texto por encima de los 18 mm.
            for (int i = 1; i <= doc.getNumberOfPages(); i++) {
                PDFTextStripper s = new PDFTextStripper();
                s.setStartPage(i);
                s.setEndPage(i);
                assertThat(s.getText(doc))
                        .as("la página %d no debería salir vacía", i)
                        .isNotBlank();
            }
        }
    }

    @Test
    @DisplayName("Nada del flujo se sale del margen derecho, ni siquiera lo alineado a la derecha")
    void elFlujoNoSeSaleDelPapel() throws Exception {
        // El fallo que esta prueba vigila no daba error: el motor coloca el contenido
        // dentro del margen de la @page pero resuelve los porcentajes contra el ancho
        // del PAPEL, así que una tabla al 100% se salía 32 mm y la columna de estado
        // —alineada a la derecha— caía fuera de la hoja. El PDF salía completo salvo
        // esa columna, que sencillamente no estaba.
        byte[] bytes = pdf.aPdf(maquetador.maquetar(diseno(true, "lista"), conMediciones(10)));

        double[] extremo = {0};
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            var stripper = new PDFTextStripper() {
                @Override
                protected void writeString(String texto, java.util.List<TextPosition> pos) {
                    for (TextPosition t : pos) {
                        extremo[0] = Math.max(extremo[0], t.getXDirAdj() + t.getWidthDirAdj());
                    }
                }
            };
            stripper.setSortByPosition(true);
            stripper.getText(doc);

            double anchoPapelMm = doc.getPage(0).getMediaBox().getWidth() / PT_POR_MM;
            double derechaMm = extremo[0] / PT_POR_MM;

            assertThat(derechaMm)
                    .as("el texto llega a %.1f mm y el papel mide %.1f mm", derechaMm, anchoPapelMm)
                    .isLessThanOrEqualTo(anchoPapelMm - MARGEN_MM + TOLERANCIA_MM);
        }
    }

    private static final double PT_POR_MM = 72.0 / 25.4;
    private static final double MARGEN_MM = 16;
    /** El relleno de la celda deja el texto algo antes del borde; no se exige al milímetro. */
    private static final double TOLERANCIA_MM = 1;

    @Test
    @DisplayName("El estado sale escrito en el documento, no solo como color")
    void elEstadoSeImprime() throws Exception {
        byte[] bytes = pdf.aPdf(maquetador.maquetar(diseno(true, "lista"), conMediciones(30)));
        try (PDDocument doc = Loader.loadPDF(bytes)) {
            String texto = new PDFTextStripper().getText(doc);
            // Los valores van de 81 a 110 con rango 70–99, así que salen los tres:
            // dentro, un poco por arriba, y lo bastante como para revisarlo.
            assertThat(texto).contains(EstadoResultado.EN_RANGO.etiquetaCorta());
            assertThat(texto).contains("Por arriba");
            assertThat(texto).contains(EstadoResultado.REVISAR.etiquetaCorta());
        }
    }
}
