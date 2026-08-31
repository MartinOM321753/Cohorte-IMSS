package imss.gob.mx.cohorte.services.reportes;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Que el motor de PDF aguante lo que un reporte real le va a pedir.
 *
 * <p>Se comprueba antes de construir nada encima, porque cambiar de motor después de
 * tener el diseñador hecho sería carísimo. Tres cosas que un reporte clínico en español
 * necesita y que no se pueden dar por sentadas:</p>
 *
 * <ol>
 *   <li>Los acentos y la eñe salen legibles, no como signos raros.</li>
 *   <li>Una tabla más larga que la hoja parte página sola en vez de recortarse.</li>
 *   <li>Las posiciones absolutas en milímetros se respetan — es lo que produce el
 *       diseñador, donde cada elemento lleva sus coordenadas.</li>
 * </ol>
 */
class ReportePdfServiceTest {

    private final ReportePdfService servicio = new ReportePdfService();

    private String htmlCon(String cuerpo) {
        return """
            <html><head><meta charset="utf-8"/><style>
              @page { size: 215.9mm 279.4mm; margin: 0; }
              body { margin: 0; font-family: sans-serif; font-size: 10pt; }
              .abs { position: absolute; }
              table { border-collapse: collapse; width: 180mm; }
              td, th { border: 0.2mm solid #333; padding: 1.5mm; }
              thead { display: table-header-group; }
            </style></head><body>
            """ + cuerpo + "</body></html>";
    }

    private String texto(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private int paginas(byte[] pdf) throws Exception {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            return doc.getNumberOfPages();
        }
    }

    @Test
    @DisplayName("Genera un PDF válido")
    void generaUnPdfValido() throws Exception {
        byte[] pdf = servicio.aPdf(htmlCon("<p>Reporte</p>"));

        assertTrue(pdf.length > 0, "El PDF no puede venir vacío");
        assertEquals('%', (char) pdf[0], "Un PDF empieza por %PDF");
        assertEquals(1, paginas(pdf));
    }

    /**
     * El caso que decide si esta librería sirve para el proyecto: un reporte del IMSS
     * está lleno de acentos, y si el motor los pierde no hay nada que hacer con él.
     */
    @Test
    @DisplayName("Los acentos y la eñe sobreviven al PDF")
    void losAcentosSobreviven() throws Exception {
        String frase = "Institución médica: evaluación clínica del participante señalado";
        byte[] pdf = servicio.aPdf(htmlCon("<p>" + frase + "</p>"));

        String extraido = texto(pdf);
        assertTrue(extraido.contains("Institución"), "Se perdió la ó: " + extraido);
        assertTrue(extraido.contains("médica"),      "Se perdió la é: " + extraido);
        assertTrue(extraido.contains("clínica"),     "Se perdió la í: " + extraido);
        assertTrue(extraido.contains("señalado"),    "Se perdió la ñ: " + extraido);
    }

    /**
     * Un estudio puede tener 40 parámetros y no caben en una hoja. La tabla tiene que
     * continuar en la siguiente, no recortarse — y el encabezado debe repetirse, que
     * es para lo que está {@code display: table-header-group}.
     */
    @Test
    @DisplayName("Una tabla más larga que la hoja parte página y repite el encabezado")
    void laTablaLargaPartePagina() throws Exception {
        StringBuilder filas = new StringBuilder();
        for (int i = 1; i <= 60; i++) {
            filas.append("<tr><td>Parámetro ").append(i)
                 .append("</td><td>").append(i * 1.5)
                 .append("</td><td>mg/dL</td></tr>");
        }
        String tabla = "<table><thead><tr><th>Parámetro</th><th>Valor</th><th>Unidad</th></tr></thead>"
                + "<tbody>" + filas + "</tbody></table>";

        byte[] pdf = servicio.aPdf(htmlCon(tabla));

        assertTrue(paginas(pdf) > 1, "60 filas no caben en una hoja carta");
        String extraido = texto(pdf);
        assertTrue(extraido.contains("Parámetro 1"),  "Falta la primera fila");
        assertTrue(extraido.contains("Parámetro 60"), "Se recortó el final en vez de paginar");
    }

    /**
     * El diseñador coloca cada elemento con coordenadas propias. Si el motor ignorara
     * la posición absoluta, todo el enfoque se cae.
     */
    @Test
    @DisplayName("Respeta las posiciones absolutas en milímetros")
    void respetaPosicionesAbsolutas() throws Exception {
        String cuerpo = """
            <div class="abs" style="left:20mm; top:15mm;">Membrete</div>
            <div class="abs" style="left:20mm; top:250mm;">Pie de página</div>
            """;
        byte[] pdf = servicio.aPdf(htmlCon(cuerpo));

        assertEquals(1, paginas(pdf), "Dos elementos posicionados caben en una hoja");
        String extraido = texto(pdf);
        assertTrue(extraido.contains("Membrete"));
        assertTrue(extraido.contains("Pie de página"));
        assertTrue(extraido.indexOf("Membrete") < extraido.indexOf("Pie de página"),
                "El de arriba debe extraerse antes que el de abajo");
    }

    @Test
    @DisplayName("Un HTML roto falla con un motivo legible, no con una traza cruda")
    void htmlRotoFallaConMotivo() {
        ReportePdfService.ReporteNoGeneradoException e = assertThrows(
                ReportePdfService.ReporteNoGeneradoException.class,
                () -> servicio.aPdf("<html><body><p>sin cerrar"));
        assertNotNull(e.getMessage());
    }
}
