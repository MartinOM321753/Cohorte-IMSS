package imss.gob.mx.cohorte.services.reportes;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que los iconos lleguen de verdad al papel.
 *
 * <p>Esto no es una formalidad. Un icono es un glifo de una tipografía, y si el
 * motor no consigue cargarla cae a las catorce fuentes base del formato PDF, donde
 * esos códigos no existen: el documento sale igual, sin error, con un hueco donde
 * iba el icono. Es exactamente la clase de fallo que nadie ve hasta imprimir.</p>
 *
 * <p>Por eso se comprueba contra el PDF ya generado y no contra el HTML.</p>
 */
class IconosEnPdfTest {

    private final ReportePdfService pdf = new ReportePdfService();

    /** «home» en Material Symbols. */
    private static final String HOME = "e88a";

    private String paginaCon(String cuerpo) {
        return """
            <html><head><meta charset="utf-8"/><style>
              @page { size: 100mm 60mm; margin: 0; }
              body { margin: 0; }
              .icono { font-family: 'Iconos'; }
            </style></head><body>%s</body></html>
            """.formatted(cuerpo);
    }

    @Test
    @DisplayName("la tipografía de iconos está en el classpath")
    void laFuenteEstaEmpaquetada() {
        // Si falta, todo lo demás falla en silencio: el PDF sale sin los iconos.
        assertThat(getClass().getResource(ReportePdfService.FUENTE_ICONOS))
                .as("la tipografía de iconos tiene que viajar dentro del jar")
                .isNotNull();
    }

    @Test
    @DisplayName("la tipografía queda incrustada, y solo con los glifos usados")
    void elIconoSeIncrusta() throws Exception {
        byte[] bytes = pdf.aPdf(paginaCon(
                "<span class=\"icono\" style=\"font-size:40pt;\">&#xE88A;</span>"));

        try (PDDocument doc = Loader.loadPDF(bytes)) {
            PDResources recursos = doc.getPage(0).getResources();

            List<String> incrustadas = new ArrayList<>();
            for (COSName nombre : recursos.getFontNames()) {
                PDFont fuente = recursos.getFont(nombre);
                if (fuente.isEmbedded()) incrustadas.add(fuente.getName());
            }

            // Sin incrustar, el documento se genera igual y el icono sale como un
            // hueco: es un fallo que no da ningún error.
            assertThat(incrustadas)
                    .as("la tipografía de iconos tiene que viajar dentro del PDF")
                    .anyMatch(n -> n.contains("MaterialSymbols"));
        }

        // Y el documento entero pesa mucho menos que la tipografía completa, porque
        // solo se incrustan los glifos usados. Sin ese recorte, cada reporte con un
        // icono cargaría con casi un mega de dibujos que nadie mira.
        assertThat(bytes.length).isLessThan(400_000);
    }

    @Test
    @DisplayName("el documento se abre y el glifo queda como texto seleccionable")
    void elPdfSeAbreYConservaElGlifo() throws Exception {
        byte[] bytes = pdf.aPdf(paginaCon(
                "<span class=\"icono\" style=\"font-size:40pt;\">&#xE88A;</span>"));

        try (PDDocument doc = Loader.loadPDF(bytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(1);

            // Sale como carácter, no como dibujo: es lo que confirma que el motor
            // encontró el glifo en vez de dejar el hueco.
            String texto = new PDFTextStripper().getText(doc);
            assertThat(texto).contains(String.valueOf((char) Integer.parseInt(HOME, 16)));
        }
    }

    @Test
    @DisplayName("varios iconos distintos caben en la misma página")
    void variosIconos() throws Exception {
        byte[] bytes = pdf.aPdf(paginaCon(
                "<span class=\"icono\">&#xE88A;</span>"
                + "<span class=\"icono\">&#xE87D;</span>"
                + "<span class=\"icono\">&#xE0BE;</span>"));

        try (PDDocument doc = Loader.loadPDF(bytes)) {
            String texto = new PDFTextStripper().getText(doc);
            assertThat(texto).contains("").contains("").contains("");
        }
    }

    @Test
    @DisplayName("un elemento de icono del diseño llega al papel")
    void iconoDesdeElDiseno() throws Exception {
        // El camino real: un elemento del diseño, no HTML escrito a mano.
        String diseno = """
            {"version":1,"tamano":"CARTA","orientacion":"vertical",
             "margenes":{"superiorMm":10,"derechoMm":10,"inferiorMm":10,"izquierdoMm":10},
             "paginas":[{"id":"p1","elementos":[
               {"id":"i1","tipo":"icono","codigo":"e88a","nombre":"home",
                "xMm":20,"yMm":20,"anchoMm":15,"altoMm":15,"z":1,"color":"#0b6b4f"}]}]}
            """;

        var resolvedor = new ResolvedorCampos();
        var maquetador = new MaquetadorReporte(
                new com.fasterxml.jackson.databind.ObjectMapper(), resolvedor,
                new BloqueResultados(resolvedor), new BloqueEstudios(),
                new BloqueExamenes(resolvedor), new BloqueLista(resolvedor), new EvidenciasReporte(null, null),
                new ImagenesReporte(null));

        var persona = new imss.gob.mx.cohorte.modules.persona.Persona();
        persona.setNombre("Ana");
        var paciente = new imss.gob.mx.cohorte.modules.paciente.Paciente();
        paciente.setPersona(persona);
        var ctx = new ContextoReporte(paciente, java.util.List.of(), java.util.List.of(),
                null, new ContextoReporte.Totales(0, 0, 0));

        String html = maquetador.maquetar(diseno, ctx);
        assertThat(html).contains("&#xE88A;").contains("Iconos");

        try (PDDocument doc = Loader.loadPDF(pdf.aPdf(html))) {
            assertThat(new PDFTextStripper().getText(doc))
                    .contains(String.valueOf((char) 0xE88A));
        }
    }

    @Test
    @DisplayName("un código inventado no ensucia el documento")
    void codigoInvalidoSeIgnora() {
        // El código viene del diseño; sin validarlo, cualquier cadena acabaría dentro
        // de una entidad HTML del documento.
        var resolvedor = new ResolvedorCampos();
        var maquetador = new MaquetadorReporte(
                new com.fasterxml.jackson.databind.ObjectMapper(), resolvedor,
                new BloqueResultados(resolvedor), new BloqueEstudios(),
                new BloqueExamenes(resolvedor), new BloqueLista(resolvedor), new EvidenciasReporte(null, null),
                new ImagenesReporte(null));

        String diseno = """
            {"version":1,"tamano":"CARTA","orientacion":"vertical",
             "margenes":{"superiorMm":10,"derechoMm":10,"inferiorMm":10,"izquierdoMm":10},
             "paginas":[{"id":"p1","elementos":[
               {"id":"i1","tipo":"icono","codigo":"nada;</div><script>",
                "xMm":20,"yMm":20,"anchoMm":15,"altoMm":15,"z":1}]}]}
            """;

        var persona = new imss.gob.mx.cohorte.modules.persona.Persona();
        persona.setNombre("Ana");
        var paciente = new imss.gob.mx.cohorte.modules.paciente.Paciente();
        paciente.setPersona(persona);
        var ctx = new ContextoReporte(paciente, java.util.List.of(), java.util.List.of(),
                null, new ContextoReporte.Totales(0, 0, 0));

        String html = maquetador.maquetar(diseno, ctx);
        assertThat(html).doesNotContain("<script>");
    }

    @Test
    @DisplayName("el texto normal sigue saliendo junto a los iconos")
    void textoNormalConvive() throws Exception {
        // Registrar una familia nueva no puede haberse llevado por delante la de
        // siempre; los acentos son lo primero que se rompe cuando eso pasa.
        byte[] bytes = pdf.aPdf(paginaCon(
                "<span class=\"icono\">&#xE88A;</span><span>Institución</span>"));

        try (PDDocument doc = Loader.loadPDF(bytes)) {
            assertThat(new PDFTextStripper().getText(doc)).contains("Institución");
        }
    }
}
