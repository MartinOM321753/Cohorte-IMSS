package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Una tabla con los estudios del participante: cuáles tiene y cuándo se los hicieron.
 *
 * <p>Es el bloque para cuando lo que se quiere no son los valores sino el panorama —
 * «qué se le ha hecho a esta persona»— y por eso no depende de ningún tipo concreto.</p>
 */
@Service
public class BloqueEstudios {

    public String html(ContextoReporte ctx, BloqueResultados.Estilo estilo) {
        List<EstudioMedico> estudios = ctx.estudiosOrdenados();
        if (estudios.isEmpty()) {
            return "<p style=\"font-size:" + estilo.tamanoPt()
                    + "pt;color:#5a6b78;font-style:italic;\">Sin estudios registrados.</p>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<table style=\"border-collapse:collapse;width:100%;font-size:")
          .append(estilo.tamanoPt()).append("pt;color:").append(estilo.colorTexto()).append(";\">");

        List<String> columnas = estilo.columnas();

        if (estilo.mostrarEncabezado()) {
            sb.append("<thead style=\"display:table-header-group;\"><tr>");
            for (String col : columnas) {
                sb.append("<th style=\"background:").append(estilo.fondoEncabezado())
                  .append(";color:").append(estilo.colorEncabezado())
                  .append(";border-bottom:0.3mm solid ").append(estilo.colorBorde())
                  .append(";text-align:left;padding:1.6mm 2mm;\">")
                  .append(Html.escapar(ColumnasBloque.LISTADO_ESTUDIOS.getOrDefault(col, col)))
                  .append("</th>");
            }
            sb.append("</tr></thead>");
        }

        sb.append("<tbody>");
        for (EstudioMedico e : estudios) {
            sb.append("<tr>");
            for (String col : columnas) {
                String contenido = switch (col) {
                    case "estudio" -> e.getTipoEstudio() != null ? e.getTipoEstudio().getNombre() : "";
                    case "fecha" -> ctx.fechaHora(e.getFechaEstudio());
                    case "resultados" -> String.valueOf(
                            e.getResultadoEstudio() == null ? 0 : e.getResultadoEstudio().size());
                    default -> "";
                };
                sb.append(celda(Html.escapar(contenido), estilo));
            }
            sb.append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String celda(String contenido, BloqueResultados.Estilo estilo) {
        return "<td style=\"border-bottom:0.2mm solid " + estilo.colorBorde()
                + ";padding:1.4mm 2mm;\">" + contenido + "</td>";
    }
}
