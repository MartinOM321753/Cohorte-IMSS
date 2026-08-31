package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.persona.Persona;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * La tabla de resultados de un estudio, con la selección de qué parámetros salen.
 *
 * <p>Dos reglas del dominio que esta tabla tiene que respetar, y que son la razón
 * de que el bloque no sea un simple bucle:</p>
 *
 * <ul>
 *   <li>Se recorren <b>los resultados capturados</b>, no el catálogo vigente. Un
 *       parámetro retirado conserva sus resultados y tiene que seguir saliendo; uno
 *       añadido después de la captura no debe aparecer como un hueco en un estudio
 *       que nunca lo midió.</li>
 *   <li>Un mismo parámetro puede repetirse en <b>grupos</b> distintos —son
 *       mediciones separadas—, así que en una lista plana esas repeticiones
 *       parecerían un error de captura.</li>
 * </ul>
 */
@Service
public class BloqueResultados {

    private static final String GRUPO_PLANO = "ROOT";

    /**
     * @param seleccion ids de los parámetros a mostrar; vacío significa todos
     */
    public String html(ContextoEstudio contexto, List<Long> seleccion) {
        List<ResultadoEstudio> resultados = contexto.estudio().getResultadoEstudio();
        if (resultados == null || resultados.isEmpty()) {
            return "<p class=\"vacio\">Este estudio no tiene resultados capturados.</p>";
        }

        Set<Long> visibles = seleccion == null ? Set.of() : Set.copyOf(seleccion);
        List<ResultadoEstudio> mostrables = resultados.stream()
                .filter(r -> visibles.isEmpty()
                        || (r.getParametro() != null && visibles.contains(r.getParametro().getId())))
                .toList();

        if (mostrables.isEmpty()) {
            // La plantilla eligió parámetros que este estudio no midió. No es un
            // fallo: la misma plantilla sirve para estudios distintos del mismo tipo.
            return "<p class=\"vacio\">Sin resultados para los parámetros seleccionados.</p>";
        }

        Persona.Sexo sexo = contexto.persona() != null ? contexto.persona().getSexo() : null;

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
                sb.append("<div class=\"grupo\">").append(escapar(etiqueta)).append("</div>");
            }
            sb.append(tabla(grupo.getValue(), sexo));
        }
        return sb.toString();
    }

    private String tabla(List<ResultadoEstudio> resultados, Persona.Sexo sexo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"res\"><thead><tr>")
          .append("<th>Parámetro</th><th>Resultado</th><th>Unidad</th><th>Referencia</th>")
          .append("</tr></thead><tbody>");

        for (ResultadoEstudio r : resultados) {
            ParametroEstudio p = r.getParametro();
            RangoReferencia.Rango rango = RangoReferencia.de(p, sexo);
            boolean fuera = p != null && p.getTipo() == TipoParametro.NUMERICO
                    && RangoReferencia.fueraDeRango(r.getValorNumerico(), rango);

            sb.append("<tr>")
              .append("<td>").append(escapar(p != null ? p.getNombre() : "")).append("</td>")
              .append("<td class=\"v").append(fuera ? " fuera" : "").append("\">")
              .append(escapar(valorDe(r))).append(fuera ? " *" : "").append("</td>")
              .append("<td>").append(escapar(p != null ? p.getUnidad() : "")).append("</td>")
              .append("<td>").append(escapar(RangoReferencia.texto(rango))).append("</td>")
              .append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String valorDe(ResultadoEstudio r) {
        if (r.getValorNumerico() != null) {
            double v = r.getValorNumerico();
            return v == Math.floor(v) && !Double.isInfinite(v)
                    ? String.valueOf((long) v) : String.valueOf(v);
        }
        if (r.getValorBooleano() != null) return r.getValorBooleano() ? "Sí" : "No";
        return r.getValorTexto() != null ? r.getValorTexto() : "";
    }

    private String escapar(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;");
    }
}
