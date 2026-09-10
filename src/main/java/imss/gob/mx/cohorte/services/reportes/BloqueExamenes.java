package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.persona.Persona;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * La tabla de resultados de laboratorio del participante.
 *
 * <p>Los exámenes no son paneles: cada uno es un analito suelto con su propio
 * resultado y su propia fecha. Por eso esta tabla lista resultados, no un examen
 * con sus parámetros como pasa en los estudios.</p>
 *
 * <p>El "fuera de rango" se calcula igual que en los estudios, contra el rango del
 * sexo del participante, para que los dos documentos marquen con el mismo criterio.</p>
 */
@Service
@AllArgsConstructor
public class BloqueExamenes {

    private final ResolvedorCampos resolvedor;

    public String html(ContextoReporte ctx, BloqueResultados.Estilo estilo, List<Long> seleccion) {
        List<ResultadoExamen> resultados = ctx.examenesOrdenados().stream()
                .filter(r -> seleccion == null || seleccion.isEmpty()
                        || (r.getExamen() != null && seleccion.contains(r.getExamen().getId())))
                .toList();

        if (resultados.isEmpty()) {
            return "<p style=\"font-size:" + estilo.tamanoPt()
                    + "pt;color:#5a6b78;font-style:italic;\">Sin resultados de laboratorio.</p>";
        }

        Persona.Sexo sexo = ctx.persona() != null ? ctx.persona().getSexo() : null;
        List<String> columnas = estilo.columnas();

        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"border-collapse:collapse;width:100%;font-size:")
          .append(estilo.tamanoPt()).append("pt;color:").append(estilo.colorTexto()).append(";\">");

        if (estilo.mostrarEncabezado()) {
            sb.append("<thead style=\"display:table-header-group;\"><tr>");
            for (String col : columnas) {
                sb.append("<th style=\"background:").append(estilo.fondoEncabezado())
                  .append(";color:").append(estilo.colorEncabezado())
                  .append(";border-bottom:0.3mm solid ").append(estilo.colorBorde())
                  .append(";text-align:left;padding:1.6mm 2mm;\">")
                  .append(Html.escapar(ColumnasBloque.LISTADO_EXAMENES.getOrDefault(col, col)))
                  .append("</th>");
            }
            sb.append("</tr></thead>");
        }

        sb.append("<tbody>");
        for (ResultadoExamen r : resultados) {
            EstadoResultado estado = estadoDe(r, sexo);
            sb.append("<tr>");
            for (String col : columnas) {
                String contenido = switch (col) {
                    case "examen" -> r.getExamen() != null ? r.getExamen().getParametro() : "";
                    case "valor"  -> resolvedor.numero(r.getValorObtenido());
                    case "unidad" -> r.getExamen() != null ? r.getExamen().getUnidad() : "";
                    case "referencia" -> referencia(r, sexo);
                    case "estado" -> estado.medido() ? estado.etiquetaCorta() : "";
                    case "fecha"  -> ctx.fechaHora(r.getFechaResultado());
                    default -> "";
                };
                // El color lo pone el estado, no un rojo único: el reporte distingue
                // una diferencia menor de algo que hay que atender, y pintarlas igual
                // borraba justo esa distinción.
                String color = ("valor".equals(col) || "estado".equals(col)) && estado.fuera()
                        ? estado.color() : estilo.colorTexto();
                sb.append("<td style=\"border-bottom:0.2mm solid ").append(estilo.colorBorde())
                  .append(";padding:1.4mm 2mm;color:").append(color)
                  .append("valor".equals(col) ? ";font-weight:bold;" : ";")
                  .append("\">").append(Html.escapar(contenido)).append("</td>");
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    /**
     * El rango de ese analito para ese sexo.
     *
     * <p>Es público y estático porque la fila repetible del reporte necesita el mismo
     * rango que esta tabla, y calcularlo dos veces es como acaban divergiendo: el
     * documento diría una referencia en la tabla y otra en la barra.</p>
     */
    public static RangoReferencia.Rango rangoDe(ResultadoExamen r, Persona.Sexo sexo) {
        if (r == null || r.getExamen() == null || sexo == null) return null;
        boolean mujer = sexo == Persona.Sexo.F;
        Double min = mujer ? r.getExamen().getValorMinMujeres() : r.getExamen().getValorMinHombres();
        Double max = mujer ? r.getExamen().getValorMaxMujeres() : r.getExamen().getValorMaxHombres();
        if (min == null && max == null) return null;
        return new RangoReferencia.Rango(min, max, r.getExamen().getMargenRevision());
    }

    /** En qué situación queda ese resultado. */
    public static EstadoResultado estadoDe(ResultadoExamen r, Persona.Sexo sexo) {
        return r == null
                ? EstadoResultado.SIN_DATO
                : RangoReferencia.estado(r.getValorObtenido(), rangoDe(r, sexo));
    }

    private String referencia(ResultadoExamen r, Persona.Sexo sexo) {
        return RangoReferencia.texto(rangoDe(r, sexo));
    }
}
