package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cómo se dibujan las figuras en el documento.
 *
 * <p>Lo que más importa aquí no es el aspecto sino <b>con qué</b> se dibuja: este
 * motor entiende CSS 2.1 con algunos añadidos, y las formas se construyen solo con
 * lo que se comprobó que reconoce —radio por esquina, trazos discontinuos y
 * {@code transform}—. Un descuido que colara {@code opacity} o un {@code <svg>}
 * saldría bien en el editor y en blanco en el papel.</p>
 */
class FiguraReporteTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode figura(String extra) {
        String json = "{\"tipo\":\"figura\",\"anchoMm\":40,\"altoMm\":20"
                + (extra.isBlank() ? "" : "," + extra) + "}";
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("el rectángulo redondea cada esquina por su cuenta")
    void esquinasPorSeparado() {
        String html = FiguraReporte.html(figura(
                "\"forma\":\"rectangulo\",\"relleno\":\"#ffffff\","
                + "\"radios\":{\"supIzq\":5,\"supDer\":0,\"infDer\":3,\"infIzq\":0}"));

        assertThat(html).contains("border-top-left-radius:5.0mm");
        assertThat(html).contains("border-top-right-radius:0.0mm");
        assertThat(html).contains("border-bottom-right-radius:3.0mm");
        assertThat(html).contains("border-bottom-left-radius:0.0mm");
    }

    @Test
    @DisplayName("un diseño con el radio antiguo redondea las cuatro por igual")
    void radioAntiguoSigueValiendo() {
        // Los diseños hechos antes de que se pudiera redondear esquina por esquina
        // solo traen `radioMm`; tienen que seguir viéndose igual.
        String html = FiguraReporte.html(figura("\"forma\":\"rectangulo\",\"radioMm\":4"));

        assertThat(html).contains("border-top-left-radius:4.0mm");
        assertThat(html).contains("border-bottom-right-radius:4.0mm");
    }

    @Test
    @DisplayName("la línea se dibuja como borde, para poder ser punteada")
    void lineaPunteada() {
        String html = FiguraReporte.html(figura(
                "\"forma\":\"linea\",\"estiloBorde\":\"dotted\",\"grosorBordeMm\":0.5,"
                + "\"colorBorde\":\"#112233\""));

        // Un rectángulo relleno no puede ser punteado; un borde sí.
        assertThat(html).contains("border-top:0.5mm dotted #112233");
        assertThat(html).contains("height:0;");
    }

    @Test
    @DisplayName("un trazo desconocido cae en continuo")
    void trazoDesconocido() {
        // `groove` y compañía dependen de sombreado y a un cuarto de milímetro no se
        // distinguen; dejarlos pasar daría un borde que el motor pinta a su manera.
        String html = FiguraReporte.html(figura("\"forma\":\"linea\",\"estiloBorde\":\"groove\""));

        assertThat(html).contains("solid");
        assertThat(html).doesNotContain("groove");
    }

    @Test
    @DisplayName("la flecha lleva punta y el trazo se acorta debajo")
    void flechaConPunta() {
        String html = FiguraReporte.html(figura(
                "\"forma\":\"flecha\",\"grosorBordeMm\":0.5,\"punta\":\"fin\","
                + "\"colorBorde\":\"#000000\""));

        // La punta es una caja sin tamaño con dos lados transparentes.
        assertThat(html).contains("border-left:2.0mm solid #000000");
        assertThat(html).contains("solid transparent");
        // Y el trazo no llega al final: si llegara, asomaría por delante del vértice.
        assertThat(html).doesNotContain("width:40.0mm;height:0");
    }

    @Test
    @DisplayName("la flecha puede llevar punta en los dos extremos")
    void flechaDoble() {
        String html = FiguraReporte.html(figura("\"forma\":\"flecha\",\"punta\":\"ambas\""));

        assertThat(html).contains("border-left:");
        assertThat(html).contains("border-right:");
    }

    @Test
    @DisplayName("el triángulo se hace con bordes, no con svg")
    void trianguloConBordes() {
        String html = FiguraReporte.html(figura("\"forma\":\"triangulo\",\"relleno\":\"#aabbcc\""));

        assertThat(html).contains("border-bottom:20.0mm solid #aabbcc");
        assertThat(html).contains("width:0;height:0");
        assertThat(html).doesNotContain("<svg");
    }

    @Test
    @DisplayName("el rombo es un cuadrado girado que cabe en su caja")
    void romboGirado() {
        String html = FiguraReporte.html(figura("\"forma\":\"rombo\",\"relleno\":\"#aabbcc\""));

        assertThat(html).contains("transform:rotate(45deg)");
        // Encogido por raíz de dos: si midiera lo mismo que la caja, las puntas se
        // saldrían al girar.
        assertThat(html).contains("width:28.2");
    }

    @Test
    @DisplayName("ninguna figura usa lo que el motor no entiende")
    void nadaFueraDeLoSoportado() {
        for (String forma : new String[] {"rectangulo", "elipse", "linea", "flecha", "triangulo", "rombo"}) {
            String html = FiguraReporte.html(figura("\"forma\":\"" + forma + "\",\"grosorBordeMm\":0.4"));

            assertThat(html).as(forma).doesNotContain("opacity");
            assertThat(html).as(forma).doesNotContain("box-shadow");
            assertThat(html).as(forma).doesNotContain("<svg");
            assertThat(html).as(forma).doesNotContain("<canvas");
        }
    }

    @Test
    @DisplayName("un color con mala pinta no entra en el documento")
    void colorInvalidoSeIgnora() {
        // El color va directo a un atributo style: dejar pasar cualquier cadena
        // permitiría colar ahí lo que fuera.
        String html = FiguraReporte.html(figura(
                "\"forma\":\"rectangulo\",\"relleno\":\"red;background:url(http://x)\""));

        assertThat(html).doesNotContain("url(");
        assertThat(html).contains("background:transparent");
    }
}
