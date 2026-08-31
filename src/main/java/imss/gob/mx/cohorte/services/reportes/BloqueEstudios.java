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

        if (estilo.mostrarEncabezado()) {
            sb.append("<thead style=\"display:table-header-group;\"><tr>");
            for (String rotulo : List.of("Estudio", "Fecha", "Resultados")) {
                sb.append("<th style=\"background:").append(estilo.fondoEncabezado())
                  .append(";color:").append(estilo.colorEncabezado())
                  .append(";border-bottom:0.3mm solid ").append(estilo.colorBorde())
                  .append(";text-align:left;padding:1.6mm 2mm;\">").append(rotulo).append("</th>");
            }
            sb.append("</tr></thead>");
        }

        sb.append("<tbody>");
        for (EstudioMedico e : estudios) {
            String nombre = e.getTipoEstudio() != null ? e.getTipoEstudio().getNombre() : "";
            int cuantos = e.getResultadoEstudio() == null ? 0 : e.getResultadoEstudio().size();
            sb.append("<tr>")
              .append(celda(Html.escapar(nombre), estilo))
              .append(celda(ctx.fechaHora(e.getFechaEstudio()), estilo))
              .append(celda(String.valueOf(cuantos), estilo))
              .append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String celda(String contenido, BloqueResultados.Estilo estilo) {
        return "<td style=\"border-bottom:0.2mm solid " + estilo.colorBorde()
                + ";padding:1.4mm 2mm;\">" + contenido + "</td>";
    }
}
