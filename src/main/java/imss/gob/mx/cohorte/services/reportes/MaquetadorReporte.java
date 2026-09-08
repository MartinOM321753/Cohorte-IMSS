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
import java.util.Map;
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

    /** Un milímetro en puntos: 1 pt = 1/72", 1 mm = 1/25.4". */
    private static final double PT_POR_MM = 72.0 / 25.4;

    /** Marcador de campo dentro de un texto: {{clave}}, con espacios tolerados. */
    private static final Pattern MARCADOR = Pattern.compile("\\{\\{\\s*([\\w.]+)\\s*}}");

    private final ObjectMapper objectMapper;
    private final ResolvedorCampos resolvedor;
    private final BloqueResultados bloqueResultados;
    private final BloqueEstudios bloqueEstudios;
    private final BloqueExamenes bloqueExamenes;
    private final EvidenciasReporte evidencias;
    private final ImagenesReporte imagenes;

    public String maquetar(String disenoJson, ContextoReporte contexto) {
        JsonNode diseno = leer(disenoJson);

        double anchoMm = medida(diseno, true);
        double altoMm = medida(diseno, false);

        // Una memoria por documento: un membrete marcado «en todas las páginas» se
        // descarga y se codifica una vez, no una por hoja.
        Map<String, ImagenesReporte.Resuelta> cacheImagenes = ImagenesReporte.nuevaCache();

        StringBuilder html = new StringBuilder(8192);
        html.append("<html><head><meta charset=\"utf-8\"/><style>")
            .append(estilos(anchoMm, altoMm))
            .append("</style></head><body>");

        JsonNode paginas = diseno.path("paginas");
        List<JsonNode> repetidos = elementosRepetidos(paginas);

        JsonNode encabezado = bandaActiva(diseno, "encabezado");
        JsonNode pie = bandaActiva(diseno, "pie");
        double origenPie = pie == null ? 0 : altoMm - pie.path("altoMm").asDouble(0);

        for (int i = 0; i < paginas.size(); i++) {
            html.append("<div class=\"hoja\">");

            // Las bandas van primero para quedar debajo: un membrete no debe taparle
            // nada al contenido de la página.
            html.append(banda(encabezado, 0, contexto, cacheImagenes));
            html.append(banda(pie, origenPie, contexto, cacheImagenes));

            if (i > 0) for (JsonNode el : repetidos) html.append(elemento(el, contexto, cacheImagenes));
            for (JsonNode el : ordenados(paginas.get(i).path("elementos"))) {
                html.append(elemento(el, contexto, cacheImagenes));
            }
            html.append("</div>");
        }

        html.append("</body></html>");
        return html.toString();
    }

    // ── Bandas ───────────────────────────────────────────────────────────────

    /**
     * El encabezado o el pie, si existe y está encendido.
     *
     * <p>Ambos son opcionales en los dos sentidos: un diseño anterior no los trae y
     * sale exactamente igual que antes, y apagar uno no borra su contenido — se deja
     * de dibujar y ya, para poder volver a encenderlo tal como estaba.</p>
     */
    private JsonNode bandaActiva(JsonNode diseno, String nombre) {
        JsonNode banda = diseno.path(nombre);
        if (banda.isMissingNode() || !banda.path("activo").asBoolean(false)) return null;
        return banda.path("elementos").isArray() && !banda.path("elementos").isEmpty() ? banda : null;
    }

    /**
     * Dibuja una banda en todas las páginas.
     *
     * <p>Sus elementos llevan coordenadas relativas a la banda, así que se envuelven
     * en una caja colocada a su altura. Guardar coordenadas absolutas habría hecho que
     * subir el pie de 20 a 30 mm dejara su contenido flotando donde estaba.</p>
     */
    private String banda(JsonNode banda, double origenMm, ContextoReporte ctx,
                         Map<String, ImagenesReporte.Resuelta> cacheImagenes) {
        if (banda == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<div style=\"position:absolute;left:0;right:0;top:").append(origenMm)
          .append("mm;height:").append(banda.path("altoMm").asDouble(0)).append("mm;\">");
        for (JsonNode el : ordenados(banda.path("elementos"))) {
            sb.append(elemento(el, ctx, cacheImagenes));
        }
        sb.append("</div>");
        return sb.toString();
    }

    // ── Elementos ────────────────────────────────────────────────────────────

    private String elemento(JsonNode el, ContextoReporte ctx, Map<String, ImagenesReporte.Resuelta> cacheImagenes) {
        // Lo oculto tampoco se imprime. Si solo desapareciera del editor, saldría en
        // el papel algo que quien lo diseñó creía haber quitado.
        if (el.path("oculto").asBoolean(false)) return "";

        // El giro se aplica a la caja entera. Es la misma propiedad que usa el
        // editor, y este motor la reconoce: aparece en su tabla de propiedades.
        double giro = el.path("rotacionGrados").asDouble(0);

        String caja = "position:absolute;"
                + "left:" + mm(el, "xMm") + ";top:" + mm(el, "yMm") + ";"
                + "width:" + mm(el, "anchoMm") + ";height:" + mm(el, "altoMm") + ";"
                + "z-index:" + el.path("z").asInt(0) + ";"
                + (giro != 0 ? "transform:rotate(" + giro + "deg);" : "");

        return switch (el.path("tipo").asText("")) {
            case "texto"  -> texto(el, caja, ctx);
            case "imagen" -> imagen(el, caja, cacheImagenes);
            case "figura" -> figura(el, caja);
            case "datos"  -> datos(el, caja, ctx);
            case "icono"  -> icono(el, caja);
            case "tabla"  -> tabla(el, caja, ctx);
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

    /**
     * Una imagen de la galería de la institución.
     *
     * <p>Solo se dibujan referencias {@code imagen:{id}}. Cualquier otra cosa —una URL
     * escrita a mano— se omite: quien la descargaría es el servidor, no el navegador
     * de quien mira, así que una dirección guardada en un diseño se convertiría en una
     * petición saliendo de dentro de la red hacia donde diga esa plantilla.</p>
     *
     * <p>Una imagen borrada tampoco rompe nada: el documento sale sin ella.</p>
     */
    private String imagen(JsonNode el, String caja, Map<String, ImagenesReporte.Resuelta> cacheImagenes) {
        String clave = el.path("url").asText("");
        if (clave.isBlank()) return "";

        ImagenesReporte.Resuelta img = imagenes.resuelta(clave, cacheImagenes);
        if (img == null) return "";

        // El tamaño y la posición se calculan aquí y no con object-fit, que este
        // motor no conoce: con él, «entera» y «recortada» salían estiradas en el
        // papel aunque en el editor se vieran bien.
        EncuadreImagen enc = EncuadreImagen.de(
                el.path("anchoMm").asDouble(0), el.path("altoMm").asDouble(0),
                img.anchoPx(), img.altoPx(),
                el.path("ajuste").asText("contener"),
                el.path("zoom").asDouble(1),
                el.path("desplazamientoXMm").asDouble(0),
                el.path("desplazamientoYMm").asDouble(0));

        String alt = el.path("descripcion").asText("");
        return "<div style=\"" + caja + "overflow:hidden;\">"
                + "<img src=\"" + img.dataUri() + "\" alt=\"" + Html.escapar(alt)
                + "\" style=\"position:absolute;"
                + "left:" + enc.izquierdaMm() + "mm;top:" + enc.arribaMm() + "mm;"
                + "width:" + enc.anchoMm() + "mm;height:" + enc.altoMm() + "mm;\"/></div>";
    }

    /**
     * Un icono del catálogo.
     *
     * <p>Es un glifo, no un dibujo: se emite el carácter y se pide la familia de
     * iconos, que el generador incrusta recortada a lo que se use. Si la tipografía
     * faltara, el documento saldría igual y con un hueco justo aquí — por eso hay
     * una prueba que abre el PDF y comprueba que el glifo llegó.</p>
     *
     * <p>El tamaño sale del lado corto de la caja: estos glifos se dibujan dentro de
     * un cuadrado, y tomar el lado largo los desbordaría por el otro.</p>
     */
    private String icono(JsonNode el, String caja) {
        int codigo = codigoDeIcono(el.path("codigo").asText(""));
        if (codigo <= 0) return "";

        double anchoMm = el.path("anchoMm").asDouble(0);
        double altoMm = el.path("altoMm").asDouble(0);
        double ladoMm = Math.max(1, Math.min(anchoMm, altoMm));

        return "<div style=\"" + caja
                + "font-family:'" + ReportePdfService.FAMILIA_ICONOS + "';"
                + "font-size:" + (ladoMm * PT_POR_MM) + "pt;"
                + "line-height:" + altoMm + "mm;"
                + "color:" + color(el, "color", "#111111") + ";"
                + "text-align:center;overflow:hidden;\">"
                + "&#x" + Integer.toHexString(codigo).toUpperCase() + ";"
                + "</div>";
    }

    /** El código del glifo, o 0 si eso no es un hexadecimal aceptable. */
    private int codigoDeIcono(String hexa) {
        // Se valida antes de escribirlo: el valor viene del diseño, y una cadena
        // cualquiera acabaría dentro de una entidad HTML del documento.
        if (!hexa.matches("[0-9a-fA-F]{2,6}")) return 0;
        return Integer.parseInt(hexa, 16);
    }

    private String figura(JsonNode el, String caja) {
        return "<div style=\"" + caja + "\">" + FiguraReporte.html(el) + "</div>";
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
        if (ClaveCampo.BLOQUE_LISTADO_EXAMENES.equals(clave)) {
            return "<div style=\"" + cajaBloque + "\">"
                    + bloqueExamenes.html(ctx, BloqueResultados.Estilo.de(el), seleccion(el))
                    + "</div>";
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

    // ── Tablas hechas a mano ──────────────────────────────────

    /**
     * Con qué aspecto se dibuja lo que la tabla no traiga puesto.
     *
     * <p>Son los mismos valores que {@code TABLA_POR_DEFECTO} del editor. Están
     * repetidos aquí porque el diseño solo guarda lo que se tocó: una tabla recién
     * insertada no lleva ni un color, y sin estos números saldría en el papel con un
     * aspecto distinto al que enseñó el editor.</p>
     */
    private static final double TABLA_TAMANO_PT        = 9;
    private static final String TABLA_COLOR_TEXTO      = "#111111";
    private static final String TABLA_COLOR_ENCABEZADO = "#33505c";
    private static final String TABLA_FONDO_ENCABEZADO = "#eef3f5";
    private static final String TABLA_COLOR_BORDE      = "#dde5e9";
    private static final double TABLA_GROSOR_BORDE_MM  = 0.2;
    private static final double TABLA_RELLENO_MM       = 1.5;

    /** Las alineaciones que el editor sabe producir. Lo demás iría al style tal cual. */
    private static final List<String> ALINEACIONES = List.of("left", "center", "right", "justify");

    /**
     * Una tabla dibujada a mano, con sus propios encabezados y su propio contenido.
     *
     * <p>Se emite con una tabla HTML de verdad, no con cajas colocadas a mano: es lo
     * que reparte el ancho entre las columnas y lo que hace que una celda de dos
     * renglones estire su fila entera. El lienzo del editor emite esta misma
     * estructura, y por eso lo que se ve ahí es lo que sale en el papel.</p>
     *
     * <p>El texto de cada celda pasa por los marcadores <code>{{clave}}</code> antes de
     * escaparse: una celda puede traer el nombre del participante sin dejar de ser
     * texto.</p>
     */
    private String tabla(JsonNode el, String caja, ContextoReporte ctx) {
        JsonNode filas = el.path("filas");
        if (!filas.isArray() || filas.isEmpty()) return "";

        int columnas = cuantasColumnas(el, filas);
        double[] anchos = anchosDe(el, columnas);
        boolean conEncabezado = el.path("conEncabezado").asBoolean(true);

        double tamanoPt  = el.path("tamanoPt").asDouble(TABLA_TAMANO_PT);
        double grosorMm  = el.path("grosorBordeMm").asDouble(TABLA_GROSOR_BORDE_MM);
        double rellenoMm = el.path("rellenoMm").asDouble(TABLA_RELLENO_MM);

        String colorTexto      = color(el, "colorTexto", TABLA_COLOR_TEXTO);
        String colorBorde      = color(el, "colorBorde", TABLA_COLOR_BORDE);
        String colorEncabezado = color(el, "colorEncabezado", TABLA_COLOR_ENCABEZADO);
        String fondoEncabezado = color(el, "fondoEncabezado", TABLA_FONDO_ENCABEZADO);
        // Sin fondo alterno todas las filas del cuerpo van iguales, así que aquí no
        // sirve un valor por defecto: hay que poder distinguir «ausente» de «un color».
        String fondoAlterno    = color(el, "fondoAlterno", "");

        String borde   = grosorMm + "mm solid " + colorBorde;
        String relleno = rellenoMm + "mm " + (rellenoMm * 1.3) + "mm";

        // «Crecer» estira la caja igual que en los bloques de datos: el alto pasa a ser
        // un mínimo y no un tope, o una tabla larga saldría cortada por abajo.
        String cajaTabla = "recortar".equals(el.path("desbordamiento").asText("crecer"))
                ? caja + "overflow:hidden;"
                : caja.replace("height:", "min-height:");

        StringBuilder sb = new StringBuilder(1024);
        sb.append("<div style=\"").append(cajaTabla).append("\">")
          .append("<table style=\"border-collapse:collapse;width:100%;table-layout:fixed;")
          .append("font-size:").append(tamanoPt).append("pt;")
          .append("color:").append(colorTexto).append(";\"><colgroup>");
        for (double pct : anchos) sb.append("<col style=\"width:").append(pct).append("%;\"/>");
        sb.append("</colgroup><tbody>");

        for (int i = 0; i < filas.size(); i++) {
            boolean esEncabezado = conEncabezado && i == 0;
            // Las alternas se cuentan desde la primera fila del cuerpo, no desde la del
            // encabezado: si no, con encabezado la franja empezaba invertida.
            int indiceCuerpo = conEncabezado ? i - 1 : i;
            boolean alterna = !esEncabezado && !fondoAlterno.isEmpty() && indiceCuerpo % 2 == 1;

            sb.append("<tr>");
            for (int j = 0; j < columnas; j++) {
                // Una fila guardada antes de añadir una columna se queda corta; el nodo
                // ausente se comporta como celda vacía y la tabla sale cuadrada igual.
                JsonNode celda = filas.get(i).path(j);

                String fondoCelda = color(celda, "fondo",
                        esEncabezado ? fondoEncabezado : (alterna ? fondoAlterno : ""));
                String colorCelda = color(celda, "colorTexto",
                        esEncabezado ? colorEncabezado : colorTexto);
                String alineacion = celda.path("alineacion").asText("left");
                if (!ALINEACIONES.contains(alineacion)) alineacion = "left";

                sb.append("<td style=\"border:").append(borde)
                  .append(";padding:").append(relleno)
                  .append(";vertical-align:top")
                  .append(";text-align:").append(alineacion)
                  .append(";font-weight:")
                  .append(celda.path("negrita").asBoolean(false) || esEncabezado ? "bold" : "normal")
                  .append(";font-style:")
                  .append(celda.path("cursiva").asBoolean(false) ? "italic" : "normal")
                  .append(";color:").append(colorCelda)
                  .append(";word-wrap:break-word");
                if (!fondoCelda.isEmpty()) sb.append(";background:").append(fondoCelda);
                sb.append(";\">")
                  .append(Html.escaparConSaltos(sustituirMarcadores(celda.path("texto").asText(""), ctx)))
                  .append("</td>");
            }
            sb.append("</tr>");
        }

        sb.append("</tbody></table></div>");
        return sb.toString();
    }

    /** Cuántas columnas tiene de verdad: manda la fila más ancha. */
    private int cuantasColumnas(JsonNode el, JsonNode filas) {
        int cuantas = el.path("columnas").isArray() ? el.path("columnas").size() : 0;
        for (JsonNode fila : filas) cuantas = Math.max(cuantas, fila.size());
        return Math.max(cuantas, 1);
    }

    /**
     * Los anchos de las columnas, en porcentaje del ancho de la caja.
     *
     * <p>Se normalizan al vuelo: un diseño guardado a medias puede no sumar cien, y
     * una tabla que ocupara el 92 % de su caja se vería descuadrada sin decir por qué.
     * Cuando los anchos guardados no corresponden a las columnas que hay, se reparten
     * a partes iguales, que es lo mismo que hace el editor.</p>
     */
    private double[] anchosDe(JsonNode el, int columnas) {
        JsonNode cols = el.path("columnas");
        double suma = 0;
        if (cols.isArray()) for (JsonNode c : cols) suma += c.path("anchoPct").asDouble(0);

        double[] anchos = new double[columnas];
        if (!cols.isArray() || cols.size() != columnas || suma <= 0) {
            java.util.Arrays.fill(anchos, 100.0 / columnas);
            return anchos;
        }
        for (int i = 0; i < columnas; i++) {
            anchos[i] = cols.get(i).path("anchoPct").asDouble(0) / suma * 100;
        }
        return anchos;
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
