package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.JsonNode;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.persona.Persona;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La tabla de resultados de un estudio, con qué parámetros salen y con qué aspecto.
 *
 * <p>Dos reglas del dominio que esta tabla respeta, y que son la razón de que no sea
 * un simple bucle:</p>
 *
 * <ul>
 *   <li>Se recorren <b>los resultados capturados</b>, no el catálogo vigente. Un
 *       parámetro retirado conserva sus resultados y sigue saliendo; uno añadido
 *       después de la captura no aparece como hueco en un estudio que nunca lo midió.</li>
 *   <li>Un mismo parámetro puede repetirse en <b>grupos</b> distintos —son mediciones
 *       separadas—, y en una lista plana esas repeticiones parecerían un error.</li>
 * </ul>
 */
@Service
@AllArgsConstructor
public class BloqueResultados {

    private static final String GRUPO_PLANO = "ROOT";

    private final ResolvedorCampos resolvedor;

    /** Cómo se ve la tabla. Todo opcional: sin nada, sale con el aspecto de siempre. */
    public record Estilo(double tamanoPt, String colorTexto, String colorEncabezado,
                         String fondoEncabezado, String colorBorde,
                         boolean mostrarEncabezado, List<String> columnas) {

        /**
         * Lee el estilo del elemento, cayendo a valores sensatos donde falte.
         *
         * <p>Las columnas por defecto salen de lo que ese bloque sabe imprimir, no de
         * una lista fija: el listado de estudios imprime «Estudio / Fecha /
         * Resultados» y la tabla de un estudio imprime «Parámetro / Resultado /
         * Unidad / Referencia». Con una lista fija, el panel ofrecía unas y el
         * documento sacaba otras.</p>
         *
         * <p>Se descarta además cualquier columna guardada que ese bloque no admita:
         * pasa al cambiar la clave de un elemento ya diseñado.</p>
         */
        public static Estilo de(JsonNode el) {
            JsonNode e = el.path("estilo");
            List<String> admitidas = ColumnasBloque.clavesDe(el.path("clave").asText(""));

            List<String> columnas = new ArrayList<>();
            for (JsonNode c : e.path("columnas")) {
                String col = c.asText();
                if (admitidas.contains(col)) columnas.add(col);
            }
            if (columnas.isEmpty()) columnas = admitidas;

            return new Estilo(
                    e.path("tamanoPt").asDouble(9),
                    color(e, "colorTexto", "#111111"),
                    color(e, "colorEncabezado", "#33505c"),
                    color(e, "fondoEncabezado", "#eef3f5"),
                    color(e, "colorBorde", "#dde5e9"),
                    e.path("mostrarEncabezado").asBoolean(true),
                    columnas);
        }

        /**
         * Solo se aceptan colores con la forma que produce el editor. Cualquier otra
         * cosa entraría tal cual en el atributo style del documento.
         */
        private static String color(JsonNode e, String campo, String porDefecto) {
            String v = e.path(campo).asText("");
            return v.matches("#[0-9a-fA-F]{3,8}") ? v : porDefecto;
        }
    }

    /**
     * @param estudio   el estudio del que sale la tabla; null si el participante no lo tiene
     * @param seleccion ids de parámetros a mostrar; vacío significa todos
     */
    public String html(EstudioMedico estudio, Persona.Sexo sexo, List<Long> seleccion, Estilo estilo) {
        if (estudio == null) {
            return vacio(estilo, "El participante no tiene este estudio registrado.");
        }
        List<ResultadoEstudio> resultados = estudio.getResultadoEstudio();
        if (resultados == null || resultados.isEmpty()) {
            return vacio(estilo, "Este estudio no tiene resultados capturados.");
        }

        Set<Long> visibles = seleccion == null ? Set.of() : Set.copyOf(seleccion);
        List<ResultadoEstudio> mostrables = resultados.stream()
                .filter(r -> visibles.isEmpty()
                        || (r.getParametro() != null && visibles.contains(r.getParametro().getId())))
                .toList();

        if (mostrables.isEmpty()) {
            // La plantilla eligió parámetros que este estudio no midió. No es un fallo:
            // la misma plantilla sirve para participantes distintos.
            return vacio(estilo, "Sin resultados para los parámetros seleccionados.");
        }

        Map<String, List<ResultadoEstudio>> porGrupo = new LinkedHashMap<>();
        for (ResultadoEstudio r : mostrables) {
            String clave = r.getGrupoCodigo() == null ? GRUPO_PLANO : r.getGrupoCodigo();
            porGrupo.computeIfAbsent(clave, k -> new ArrayList<>()).add(r);
        }
        boolean agrupado = porGrupo.size() > 1 || !porGrupo.containsKey(GRUPO_PLANO);

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, List<ResultadoEstudio>> grupo : porGrupo.entrySet()) {
            if (agrupado) {
                String etiqueta = grupo.getValue().stream()
                        .map(ResultadoEstudio::getGrupoEtiqueta)
                        .filter(e -> e != null && !e.isBlank())
                        .findFirst().orElse(grupo.getKey());
                sb.append("<div style=\"font-size:").append(estilo.tamanoPt()).append("pt;color:")
                  .append(estilo.colorEncabezado()).append(";margin:2mm 0 1mm;\">")
                  .append(Html.escapar(etiqueta)).append("</div>");
            }
            sb.append(tabla(grupo.getValue(), sexo, estilo));
        }
        return sb.toString();
    }

    private String tabla(List<ResultadoEstudio> resultados, Persona.Sexo sexo, Estilo estilo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"border-collapse:collapse;width:100%;font-size:")
          .append(estilo.tamanoPt()).append("pt;color:").append(estilo.colorTexto()).append(";\">");

        if (estilo.mostrarEncabezado()) {
            // display:table-header-group hace que el encabezado se repita cuando la
            // tabla parte página; sin eso, la segunda hoja sale sin títulos.
            sb.append("<thead style=\"display:table-header-group;\"><tr>");
            for (String col : estilo.columnas()) {
                sb.append("<th style=\"background:").append(estilo.fondoEncabezado())
                  .append(";color:").append(estilo.colorEncabezado())
                  .append(";border-bottom:0.3mm solid ").append(estilo.colorBorde())
                  .append(";text-align:left;padding:1.6mm 2mm;\">")
                  .append(Html.escapar(rotuloColumna(col))).append("</th>");
            }
            sb.append("</tr></thead>");
        }

        sb.append("<tbody>");
        for (ResultadoEstudio r : resultados) {
            ParametroEstudio p = r.getParametro();
            RangoReferencia.Rango rango = RangoReferencia.de(p, sexo);
            boolean fuera = p != null && p.getTipo() == TipoParametro.NUMERICO
                    && RangoReferencia.fueraDeRango(r.getValorNumerico(), rango);

            sb.append("<tr>");
            for (String col : estilo.columnas()) {
                String contenido = switch (col) {
                    case "parametro"  -> p != null ? p.getNombre() : "";
                    case "valor"      -> resolvedor.textoDelValor(r) + (fuera ? " *" : "");
                    case "unidad"     -> p != null ? p.getUnidad() : "";
                    case "referencia" -> RangoReferencia.texto(rango);
                    default -> "";
                };
                String colorCelda = "valor".equals(col) && fuera ? "#a8352c" : estilo.colorTexto();
                sb.append("<td style=\"border-bottom:0.2mm solid ").append(estilo.colorBorde())
                  .append(";padding:1.4mm 2mm;color:").append(colorCelda)
                  .append("valor".equals(col) ? ";font-weight:bold;" : ";")
                  .append("\">").append(Html.escapar(contenido)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String rotuloColumna(String col) {
        return switch (col) {
            case "parametro"  -> "Parámetro";
            case "valor"      -> "Resultado";
            case "unidad"     -> "Unidad";
            case "referencia" -> "Referencia";
            default -> col;
        };
    }

    private String vacio(Estilo estilo, String mensaje) {
        return "<p style=\"font-size:" + estilo.tamanoPt() + "pt;color:#5a6b78;font-style:italic;\">"
                + Html.escapar(mensaje) + "</p>";
    }
}
