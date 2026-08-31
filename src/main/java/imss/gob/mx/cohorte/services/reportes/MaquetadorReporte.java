package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Convierte el diseño de una plantilla, con los datos ya resueltos, en el HTML que
 * se imprime.
 *
 * <p>Es el gemelo del lienzo del editor: los dos dibujan lo mismo a partir del
 * mismo JSON, uno en pantalla y otro para el PDF. Por eso ambos se limitan a
 * posición absoluta, colores planos y tablas — el motor de PDF entiende CSS 2.1, y
 * usar aquí algo que allá no existe haría que la vista previa mintiera.</p>
 *
 * <p>Las medidas del diseño están en milímetros y aquí se emiten en milímetros:
 * no hay conversión que pueda desajustarse.</p>
 */
@Service
@AllArgsConstructor
public class MaquetadorReporte {

    /** Marcador de campo dentro de un texto: {{clave}}, con espacios tolerados. */
    private static final Pattern MARCADOR = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    private final ObjectMapper objectMapper;
    private final BloqueResultados bloqueResultados;
    private final EvidenciasReporte evidencias;

    public String maquetar(String disenoJson, ContextoEstudio contexto) {
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

            // Los que se repiten van primero para quedar debajo: un membrete no
            // debe taparle nada al contenido de la página.
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

    private String elemento(JsonNode el, ContextoEstudio contexto) {
        String tipo = el.path("tipo").asText("");
        String caja = "position:absolute;"
                + "left:" + mm(el, "xMm") + ";top:" + mm(el, "yMm") + ";"
                + "width:" + mm(el, "anchoMm") + ";height:" + mm(el, "altoMm") + ";"
                + "z-index:" + el.path("z").asInt(0) + ";";

        return switch (tipo) {
            case "texto"  -> texto(el, caja, contexto);
            case "imagen" -> imagen(el, caja);
            case "figura" -> figura(el, caja);
            case "datos"  -> datos(el, caja, contexto);
            default -> "";
        };
    }

    private String texto(JsonNode el, String caja, ContextoEstudio contexto) {
        String contenido = sustituirMarcadores(el.path("contenido").asText(""), contexto);
        String estilo = caja
                + "font-size:" + el.path("tamanoPt").asDouble(10) + "pt;"
                + "font-weight:" + (el.path("negrita").asBoolean(false) ? "bold" : "normal") + ";"
                + "font-style:" + (el.path("cursiva").asBoolean(false) ? "italic" : "normal") + ";"
                + "color:" + color(el, "color", "#111111") + ";"
                + "text-align:" + el.path("alineacion").asText("left") + ";"
                + "line-height:" + el.path("interlineado").asDouble(1.35) + ";"
                + "overflow:hidden;";
        return "<div style=\"" + estilo + "\">" + escaparConSaltos(contenido) + "</div>";
    }

    private String imagen(JsonNode el, String caja) {
        String url = el.path("url").asText("");
        if (url.isBlank()) return "";
        String ajuste = "cubrir".equals(el.path("ajuste").asText("contener")) ? "cover" : "contain";
        return "<div style=\"" + caja + "\">"
                + "<img src=\"" + escapar(url) + "\" style=\"width:100%;height:100%;object-fit:" + ajuste + ";\"/>"
                + "</div>";
    }

    private String figura(JsonNode el, String caja) {
        String forma = el.path("forma").asText("rectangulo");
        double grosor = el.path("grosorBordeMm").asDouble(0);
        String colorBorde = color(el, "colorBorde", "#333333");

        if ("linea".equals(forma)) {
            // La línea se dibuja como una barra del grosor pedido, centrada en su
            // caja: así se arrastra y redimensiona como cualquier otro elemento.
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
     * Un bloque de datos. Solo la tabla de resultados por ahora; lo que no se sepa
     * dibujar se omite en silencio en vez de dejar un recuadro con una clave
     * impresa en medio del documento.
     */
    private String datos(JsonNode el, String caja, ContextoEstudio contexto) {
        String clave = el.path("clave").asText("");
        if (CatalogoCamposReporte.BLOQUE_RESULTADOS.equals(clave)) {
            String desbordamiento = el.path("desbordamiento").asText("crecer");
            // «Crecer» deja que la caja se estire: se fija el alto como mínimo y no
            // como tope, o una tabla larga quedaría recortada.
            String cajaBloque = "crecer".equals(desbordamiento)
                    ? caja.replace("height:", "min-height:")
                    : caja + "overflow:hidden;";
            return "<div style=\"" + cajaBloque + "\">"
                    + bloqueResultados.html(contexto, seleccion(el))
                    + "</div>";
        }
        if (CatalogoCamposReporte.BLOQUE_EVIDENCIAS.equals(clave)) {
            return "<div style=\"" + caja.replace("height:", "min-height:") + "\">"
                    + evidenciasHtml(contexto)
                    + "</div>";
        }
        return "";
    }

    /**
     * Las evidencias adjuntas del estudio.
     *
     * <p>Las imágenes se dibujan; de un PDF adjunto solo se deja constancia de que
     * existe. Meter un PDF dentro de otro no es dibujarlo, es concatenarlo, y eso
     * ocurre al ensamblar el documento, no aquí.</p>
     */
    private String evidenciasHtml(ContextoEstudio contexto) {
        Long idEstudio = contexto.estudio().getId();
        if (idEstudio == null) return "";

        List<EvidenciasReporte.Evidencia> lista = evidencias.deEstudio(idEstudio);
        if (lista.isEmpty()) {
            return "<p class=\"vacio\">Este estudio no tiene archivos adjuntos.</p>";
        }

        StringBuilder sb = new StringBuilder();
        for (EvidenciasReporte.Evidencia ev : lista) {
            sb.append("<div class=\"evid\">");
            if (ev.incrustable() && ev.dataUri() != null) {
                sb.append("<img src=\"").append(ev.dataUri()).append("\" class=\"evid-img\"/>");
            }
            sb.append("<div class=\"evid-pie\">").append(escapar(ev.nombre()));
            if (!ev.incrustable() && ev.motivo() != null) {
                sb.append(" <span class=\"evid-nota\">— ").append(escapar(ev.motivo())).append("</span>");
            }
            sb.append("</div></div>");
        }
        return sb.toString();
    }

    /** Qué parámetros mostrar. Vacío o ausente significa todos. */
    private List<Long> seleccion(JsonNode el) {
        JsonNode nodo = el.path("seleccion");
        if (!nodo.isArray() || nodo.isEmpty()) return List.of();
        List<Long> ids = new ArrayList<>(nodo.size());
        nodo.forEach(n -> ids.add(n.asLong()));
        return ids;
    }

    // ── Sustitución de marcadores ────────────────────────────────────────────

    /**
     * Reemplaza {{clave}} por su valor.
     *
     * <p>El valor se escapa antes de entrar al HTML porque puede venir de algo que
     * escribió una persona —un nombre, una observación—, y un «&» suelto rompería
     * el documento. Se hace aquí, sobre el valor, y no sobre el texto completo: si
     * se escapara todo después, las llaves ya no se distinguirían.</p>
     */
    private String sustituirMarcadores(String texto, ContextoEstudio contexto) {
        if (texto == null || texto.isEmpty()) return "";
        Matcher m = MARCADOR.matcher(texto);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String valor = contexto.valorDe(m.group(1));
            m.appendReplacement(sb, Matcher.quoteReplacement(valor));
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

    /** Medidas de la hoja, ya considerando la orientación. */
    private double medida(JsonNode diseno, boolean ancho) {
        String tamano = diseno.path("tamano").asText("CARTA");
        double a, b;
        switch (tamano) {
            case "A4"     -> { a = 210;   b = 297; }
            case "OFICIO" -> { a = 215.9; b = 355.6; }
            default       -> { a = 215.9; b = 279.4; }
        }
        boolean horizontal = "horizontal".equals(diseno.path("orientacion").asText("vertical"));
        double anchoMm = horizontal ? b : a;
        double altoMm = horizontal ? a : b;
        return ancho ? anchoMm : altoMm;
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

    private String color(JsonNode el, String campo, String porDefecto) {
        String v = el.path(campo).asText("");
        // Solo se aceptan colores con la forma que produce el editor. Cualquier
        // otra cosa entraría tal cual en el atributo style.
        return v.matches("#[0-9a-fA-F]{3,8}") ? v : porDefecto;
    }

    private String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }

    /** Escapa y convierte los saltos de línea en saltos visibles. */
    private String escaparConSaltos(String s) {
        return escapar(s).replace("\n", "<br/>");
    }

    private String estilos(double anchoMm, double altoMm) {
        return "@page { size: " + anchoMm + "mm " + altoMm + "mm; margin: 0; }\n"
             + "body { margin:0; font-family: sans-serif; color:#111; }\n"
             + ".hoja { position:relative; width:" + anchoMm + "mm; height:" + altoMm + "mm;"
             + " page-break-after: always; overflow:hidden; }\n"
             + ".hoja:last-child { page-break-after: auto; }\n"
             + "table.res { border-collapse:collapse; width:100%; font-size:9pt; }\n"
             + "table.res thead { display: table-header-group; }\n"
             + "table.res th { background:#eef3f5; border-bottom:0.3mm solid #9fb4bd;"
             + " text-align:left; padding:1.6mm 2mm; font-size:8.5pt; color:#33505c; }\n"
             + "table.res td { border-bottom:0.2mm solid #dde5e9; padding:1.4mm 2mm; }\n"
             + "table.res td.v { font-weight:bold; }\n"
             + "table.res td.fuera { color:#a8352c; }\n"
             + ".grupo { margin:2mm 0 1mm; font-size:9pt; color:#1f4e5f;"
             + " border-left:1mm solid #1f4e5f; padding-left:2mm; }\n"
             + ".vacio { color:#5a6b78; font-style:italic; font-size:9pt; }\n";
    }
}
