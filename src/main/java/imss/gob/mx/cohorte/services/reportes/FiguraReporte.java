package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Dibuja las figuras del diseño con lo que el motor de PDF sabe pintar.
 *
 * <p>Nada de SVG: este motor no lo entiende sin una extensión que arrastraría un
 * procesador de XML entero. Lo que sí reconoce, comprobado buscándolo en sus
 * fuentes, es el radio por esquina, los trazos {@code dashed}, {@code dotted} y
 * {@code double}, y {@code transform} con {@code rotate}. No reconoce
 * {@code opacity} ni {@code box-shadow}.</p>
 *
 * <p>De ahí salen las decisiones raras de aquí: el triángulo y las puntas de flecha
 * se construyen con el truco de los bordes —una caja sin tamaño cuyos laterales son
 * transparentes— y el rombo con un cuadrado girado. Todo acaba siendo rectángulos,
 * que es lo único que el editor y el papel dibujan igual.</p>
 *
 * <p>El gemelo en TypeScript está en {@code lib/figuras.ts}. Si divergen, la vista
 * previa vuelve a mentir.</p>
 */
public final class FiguraReporte {

    private FiguraReporte() {}

    private static final String COLOR_TRAZO = "#333333";
    private static final double GROSOR_TRAZO_MM = 0.3;

    /** El contenido de una figura: sus piezas ya en HTML, en milímetros. */
    public static String html(JsonNode el) {
        double ancho = el.path("anchoMm").asDouble(0);
        double alto = el.path("altoMm").asDouble(0);
        String forma = el.path("forma").asText("rectangulo");
        String color = color(el, "colorBorde", COLOR_TRAZO);
        String trazo = estiloBorde(el.path("estiloBorde").asText("solid"));
        double grosor = el.path("grosorBordeMm").asDouble(0);

        return switch (forma) {
            case "linea" -> caja(linea(ancho, alto, grosorTrazo(el), color, trazo));
            case "flecha" -> caja(String.join("",
                    flecha(ancho, alto, grosorTrazo(el), color, trazo,
                           el.path("punta").asText("fin"))));
            case "triangulo" -> caja(triangulo(ancho, alto, color(el, "relleno", color)));
            case "rombo" -> caja(rombo(ancho, alto, el, grosor, color, trazo));
            case "elipse" -> caja(
                    "<div style=\"position:absolute;left:0;top:0;width:100%;height:100%;"
                    + "background:" + color(el, "relleno", "transparent") + ";"
                    + (grosor > 0 ? "border:" + grosor + "mm " + trazo + " " + color + ";" : "")
                    + "border-radius:50%;\"></div>");
            default -> caja(rectangulo(el, grosor, color, trazo));
        };
    }

    // ── Piezas ───────────────────────────────────────────────────────────────

    private static String rectangulo(JsonNode el, double grosor, String color, String trazo) {
        JsonNode radios = el.path("radios");
        double unico = el.path("radioMm").asDouble(0);

        // Sin `radios` manda el valor antiguo, que redondeaba las cuatro por igual.
        double si = radio(radios, "supIzq", unico);
        double sd = radio(radios, "supDer", unico);
        double id = radio(radios, "infDer", unico);
        double ii = radio(radios, "infIzq", unico);

        return "<div style=\"position:absolute;left:0;top:0;width:100%;height:100%;"
                + "background:" + color(el, "relleno", "transparent") + ";"
                + (grosor > 0 ? "border:" + grosor + "mm " + trazo + " " + color + ";" : "")
                + "border-top-left-radius:" + si + "mm;"
                + "border-top-right-radius:" + sd + "mm;"
                + "border-bottom-right-radius:" + id + "mm;"
                + "border-bottom-left-radius:" + ii + "mm;\"></div>";
    }

    private static String rombo(JsonNode el, double ancho, double alto,
                                double grosor, String color, String trazo) {
        return rombo(ancho, alto, el, grosor, color, trazo);
    }

    private static String rombo(double ancho, double alto, JsonNode el,
                                double grosor, String color, String trazo) {
        // Se encoge por raíz de dos para que las puntas no se salgan al girar.
        double lado = 1 / Math.sqrt(2);
        double w = ancho * lado;
        double h = alto * lado;
        return "<div style=\"position:absolute;"
                + "left:" + ((ancho - w) / 2) + "mm;top:" + ((alto - h) / 2) + "mm;"
                + "width:" + w + "mm;height:" + h + "mm;"
                + "background:" + color(el, "relleno", "transparent") + ";"
                + (grosor > 0 ? "border:" + grosor + "mm " + trazo + " " + color + ";" : "")
                + "transform:rotate(45deg);\"></div>";
    }

    private static String triangulo(double ancho, double alto, String relleno) {
        return "<div style=\"position:absolute;left:0;top:0;width:0;height:0;"
                + "border-left:" + (ancho / 2) + "mm solid transparent;"
                + "border-right:" + (ancho / 2) + "mm solid transparent;"
                + "border-bottom:" + alto + "mm solid " + relleno + ";\"></div>";
    }

    /**
     * Una línea, dibujada como el borde superior de una caja sin alto.
     *
     * <p>No como un rectángulo de color: un relleno no puede ser punteado, un borde
     * sí. Es lo que permite ofrecer trazo discontinuo.</p>
     */
    private static String linea(double ancho, double alto, double grosor,
                                String color, String trazo) {
        return "<div style=\"position:absolute;left:0;"
                + "top:" + ((alto - grosor) / 2) + "mm;"
                + "width:" + ancho + "mm;height:0;"
                + "border-top:" + grosor + "mm " + trazo + " " + color + ";\"></div>";
    }

    private static List<String> flecha(double ancho, double alto, double grosor,
                                       String color, String trazo, String punta) {
        double largoPunta = Math.max(grosor * 3, 2);
        double medioAlto = Math.max(grosor * 2, 1.4);

        boolean alInicio = "inicio".equals(punta) || "ambas".equals(punta);
        boolean alFin = "fin".equals(punta) || "ambas".equals(punta) || punta.isBlank();

        double desde = alInicio ? largoPunta : 0;
        double hasta = ancho - (alFin ? largoPunta : 0);

        List<String> piezas = new ArrayList<>();
        // El trazo se acorta bajo la punta: si llegara hasta el vértice asomaría por
        // delante, que es lo que delata una flecha mal montada.
        piezas.add("<div style=\"position:absolute;left:" + desde + "mm;"
                + "top:" + ((alto - grosor) / 2) + "mm;"
                + "width:" + Math.max(0, hasta - desde) + "mm;height:0;"
                + "border-top:" + grosor + "mm " + trazo + " " + color + ";\"></div>");

        if (alFin) {
            piezas.add("<div style=\"position:absolute;left:" + hasta + "mm;"
                    + "top:" + (alto / 2 - medioAlto) + "mm;width:0;height:0;"
                    + "border-top:" + medioAlto + "mm solid transparent;"
                    + "border-bottom:" + medioAlto + "mm solid transparent;"
                    + "border-left:" + largoPunta + "mm solid " + color + ";\"></div>");
        }
        if (alInicio) {
            piezas.add("<div style=\"position:absolute;left:0;"
                    + "top:" + (alto / 2 - medioAlto) + "mm;width:0;height:0;"
                    + "border-top:" + medioAlto + "mm solid transparent;"
                    + "border-bottom:" + medioAlto + "mm solid transparent;"
                    + "border-right:" + largoPunta + "mm solid " + color + ";\"></div>");
        }
        return piezas;
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private static String caja(String contenido) {
        return "<div style=\"position:relative;width:100%;height:100%;\">" + contenido + "</div>";
    }

    private static double grosorTrazo(JsonNode el) {
        double g = el.path("grosorBordeMm").asDouble(0);
        return g > 0 ? g : GROSOR_TRAZO_MM;
    }

    private static double radio(JsonNode radios, String campo, double porDefecto) {
        return radios.isMissingNode() ? porDefecto : radios.path(campo).asDouble(porDefecto);
    }

    /** Solo los cuatro que el motor dibuja de verdad; lo demás sale continuo. */
    private static String estiloBorde(String valor) {
        return switch (valor) {
            case "dashed", "dotted", "double" -> valor;
            default -> "solid";
        };
    }

    /** Solo colores con la forma que produce el editor; lo demás iría al style tal cual. */
    private static String color(JsonNode el, String campo, String porDefecto) {
        String v = el.path(campo).asText("");
        return v.matches("#[0-9a-fA-F]{3,8}") ? v : porDefecto;
    }
}
