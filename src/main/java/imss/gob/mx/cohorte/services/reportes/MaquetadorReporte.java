package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte el diseño de una plantilla, con los datos del participante, en el HTML
 * que se imprime.
 *
 * <p>Es el gemelo del lienzo del editor: los dos dibujan lo mismo a partir del mismo
 * JSON, uno en pantalla y otro para el PDF. Por eso ambos se limitan a posición
 * absoluta, colores planos y tablas — el motor de PDF entiende CSS 2.1, y usar aquí
 * algo que allá no existe haría que la vista previa mintiera.</p>
 *
 * <p>Las medidas del diseño están en milímetros y aquí se emiten en milímetros: no
 * hay conversión que pueda desajustarse.</p>
 */
@Service
@AllArgsConstructor
public class MaquetadorReporte {

    /** Marcador de campo dentro de un texto: {{clave}}, con espacios tolerados. */
    private static final Pattern MARCADOR = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    private final ObjectMapper objectMapper;
    private final ResolvedorCampos resolvedor;
    private final BloqueResultados bloqueResultados;
    private final BloqueEstudios bloqueEstudios;
    private final EvidenciasReporte evidencias;

    public String maquetar(String disenoJson, ContextoReporte contexto) {
        JsonNode diseno = leer(disenoJson);

        double anchoMm = medida(diseno, true);
        double altoMm = medida(diseno, false);

        StringBuilder html = new StringBuilder(8192);
        html.append("<html><head><meta charset=\"utf-8\"/><style>")
            .append(estilos(anchoMm, altoMm))
            .append("</style></head><body>");

        JsonNode paginas = diseno.path("paginas");
        List<JsonNode> repetidos = elementosRepetidos(paginas);

        for (int i = 0; i < paginas.size(); i++) {
            html.append("<div class=\"hoja\">");
            // Los que se repiten van primero para quedar debajo: un membrete no debe
            // taparle nada al contenido de la página.
            if (i > 0) for (JsonNode el : repetidos) html.append(elemento(el, contexto));
            for (JsonNode el : ordenados(paginas.get(i).path("elementos"))) {
                html.append(elemento(el, contexto));
            }
            html.append("</div>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    // ── Elementos ────────────────────────────────────────────────────────────

    private String elemento(JsonNode el, ContextoReporte ctx) {
        String caja = "position:absolute;"
                + "left:" + mm(el, "xMm") + ";top:" + mm(el, "yMm") + ";"
                + "width:" + mm(el, "anchoMm") + ";height:" + mm(el, "altoMm") + ";"
                + "z-index:" + el.path("z").asInt(0) + ";";

        return switch (el.path("tipo").asText("")) {
            case "texto"  -> texto(el, caja, ctx);
            case "imagen" -> imagen(el, caja);
            case "figura" -> figura(el, caja);
            case "datos"  -> datos(el, caja, ctx);
            default -> "";
        };
    }

    private String texto(JsonNode el, String caja, ContextoReporte ctx) {
        String contenido = sustituirMarcadores(el.path("contenido").asText(""), ctx);
        String estilo = caja
                + "font-size:" + el.path("tamanoPt").asDouble(10) + "pt;"
                + "font-weight:" + (el.path("negrita").asBoolean(false) ? "bold" : "normal") + ";"
                + "font-style:" + (el.path("cursiva").asBoolean(false) ? "italic" : "normal") + ";"
                + "color:" + color(el, "color", "#111111") + ";"
                + "text-align:" + el.path("alineacion").asText("left") + ";"
                + "line-height:" + el.path("interlineado").asDouble(1.35) + ";"
                + "overflow:hidden;";
        return "<div style=\"" + estilo + "\">" + Html.escaparConSaltos(contenido) + "</div>";
    }

    private String imagen(JsonNode el, String caja) {
        String url = el.path("url").asText("");
        if (url.isBlank()) return "";
        String ajuste = "cubrir".equals(el.path("ajuste").asText("contener")) ? "cover" : "contain";
        return "<div style=\"" + caja + "\">"
                + "<img src=\"" + Html.escapar(url)
                + "\" style=\"width:100%;height:100%;object-fit:" + ajuste + ";\"/></div>";
    }

    private String figura(JsonNode el, String caja) {
        String forma = el.path("forma").asText("rectangulo");
        double grosor = el.path("grosorBordeMm").asDouble(0);
        String colorBorde = color(el, "colorBorde", "#333333");

        if ("linea".equals(forma)) {
            return "<div style=\"" + caja + "\">"
                    + "<div style=\"width:100%;height:" + (grosor > 0 ? grosor : 0.3) + "mm;"
                    + "background:" + colorBorde + ";\"></div></div>";
        }

        String estilo = caja
                + "background:" + color(el, "relleno", "transparent") + ";"
                + (grosor > 0 ? "border:" + grosor + "mm solid " + colorBorde + ";" : "")
                + ("elipse".equals(forma)
                    ? "border-radius:50%;"
                    : "border-radius:" + el.path("radioMm").asDouble(0) + "mm;");
        return "<div style=\"" + estilo + "\"></div>";
    }

    /**
     * Un bloque de datos. La clave dice de qué estudio sale, así que una misma hoja
     * puede llevar la tabla del DEXA y las evidencias de otro estudio distinto.
     *
     * <p>Una clave que ya no se sabe dibujar se omite en silencio: es una plantilla
     * hecha con una versión anterior, y dejar un recuadro con la clave impresa en
     * medio del documento sería peor.</p>
     */
    private String datos(JsonNode el, String caja, ContextoReporte ctx) {
        String clave = el.path("clave").asText("");
        Persona.Sexo sexo = ctx.persona() != null ? ctx.persona().getSexo() : null;

        // «Crecer» deja que la caja se estire: se pone el alto como mínimo y no como
        // tope, o una tabla larga quedaría recortada.
        String cajaBloque = "crecer".equals(el.path("desbordamiento").asText("crecer"))
                ? caja.replace("height:", "min-height:")
                : caja + "overflow:hidden;";

        if (ClaveCampo.BLOQUE_LISTADO_ESTUDIOS.equals(clave)) {
            return "<div style=\"" + cajaBloque + "\">"
                    + bloqueEstudios.html(ctx, BloqueResultados.Estilo.de(el)) + "</div>";
        }

        ClaveCampo.BloqueEstudio bloque = ClaveCampo.comoBloqueEstudio(clave);
        if (bloque == null) return "";

        EstudioMedico estudio = ctx.estudioDeTipo(bloque.idTipo()).orElse(null);

        if ("resultados".equals(bloque.bloque())) {
            return "<div style=\"" + cajaBloque + "\">"
                    + bloqueResultados.html(estudio, sexo, seleccion(el), BloqueResultados.Estilo.de(el))
                    + "</div>";
        }
        if ("evidencias".equals(bloque.bloque())) {
            return "<div style=\"" + cajaBloque + "\">"
                    + evidencias.html(estudio) + "</div>";
        }
        return "";
    }

    /** Qué parámetros mostrar. Vacío o ausente significa todos. */
    private List<Long> seleccion(JsonNode el) {
        JsonNode nodo = el.path("seleccion");
        if (!nodo.isArray() || nodo.isEmpty()) return List.of();
        List<Long> ids = new ArrayList<>(nodo.size());
        nodo.forEach(n -> ids.add(n.asLong()));
        return ids;
    }

    // ── Marcadores ───────────────────────────────────────────────────────────

    /**
     * Reemplaza {{clave}} por su valor.
     *
     * <p>El valor se escapa antes de entrar al HTML, y se hace aquí sobre el valor y
     * no sobre el texto completo: si se escapara todo después, las llaves ya no se
     * distinguirían.</p>
     */
    private String sustituirMarcadores(String texto, ContextoReporte ctx) {
        if (texto == null || texto.isEmpty()) return "";
        Matcher m = MARCADOR.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(resolvedor.valorDe(m.group(1), ctx)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private JsonNode leer(String json) {
        try {
            JsonNode nodo = objectMapper.readTree(json);
            if (!nodo.path("paginas").isArray() || nodo.path("paginas").isEmpty()) {
                throw new ValidationException("El diseño de la plantilla no tiene ninguna página.");
            }
            return nodo;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("El diseño de la plantilla no se pudo leer: " + e.getMessage());
        }
    }

    private double medida(JsonNode diseno, boolean ancho) {
        double a, b;
        switch (diseno.path("tamano").asText("CARTA")) {
            case "A4"     -> { a = 210;   b = 297; }
            case "OFICIO" -> { a = 215.9; b = 355.6; }
            default       -> { a = 215.9; b = 279.4; }
        }
        boolean horizontal = "horizontal".equals(diseno.path("orientacion").asText("vertical"));
        return ancho ? (horizontal ? b : a) : (horizontal ? a : b);
    }

    private List<JsonNode> elementosRepetidos(JsonNode paginas) {
        List<JsonNode> repetidos = new ArrayList<>();
        if (paginas.isEmpty()) return repetidos;
        for (JsonNode el : ordenados(paginas.get(0).path("elementos"))) {
            if (el.path("repiteEnTodas").asBoolean(false)) repetidos.add(el);
        }
        return repetidos;
    }

    /** Por capa: lo de z menor se dibuja primero y queda debajo. */
    private List<JsonNode> ordenados(JsonNode elementos) {
        List<JsonNode> lista = new ArrayList<>();
        elementos.forEach(lista::add);
        lista.sort((a, b) -> Integer.compare(a.path("z").asInt(0), b.path("z").asInt(0)));
        return lista;
    }

    private String mm(JsonNode el, String campo) {
        return el.path(campo).asDouble(0) + "mm";
    }

    /** Solo colores con la forma que produce el editor; lo demás iría al style tal cual. */
    private String color(JsonNode el, String campo, String porDefecto) {
        String v = el.path(campo).asText("");
        return v.matches("#[0-9a-fA-F]{3,8}") ? v : porDefecto;
    }

    private String estilos(double anchoMm, double altoMm) {
        return "@page { size: " + anchoMm + "mm " + altoMm + "mm; margin: 0; }\n"
             + "body { margin:0; font-family: sans-serif; color:#111; }\n"
             + ".hoja { position:relative; width:" + anchoMm + "mm; height:" + altoMm + "mm;"
             + " page-break-after: always; overflow:hidden; }\n"
             + ".hoja:last-child { page-break-after: auto; }\n"
             + ".evid { display:inline-block; vertical-align:top; margin:0 3mm 3mm 0; }\n"
             + ".evid-img { max-width:80mm; max-height:80mm; border:0.2mm solid #dde5e9; }\n"
             + ".evid-pie { font-size:7.5pt; color:#5a6b78; margin-top:1mm; }\n"
             + ".evid-nota { font-style:italic; }\n";
    }
}
