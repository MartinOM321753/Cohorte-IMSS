package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Arma el HTML del reporte de un estudio.
 *
 * <p>Es la plantilla fija de la primera fase: el diseño está escrito aquí, no lo elige
 * nadie todavía. Sirve para tener reportes reales desde el primer día y, sobre todo,
 * para dejar resuelto lo que el diseñador va a necesitar igual —cómo se leen los
 * resultados, qué rango aplica, cómo se agrupan— antes de construir la parte visual.</p>
 *
 * <p>El HTML se genera pensando en un motor de CSS 2.1: posiciones absolutas para el
 * marco y tablas para el cuerpo. Nada de flexbox ni grid.</p>
 *
 * <p>Se recorren <b>los resultados del estudio</b>, no el catálogo de parámetros
 * vigente. Es deliberado y es la misma regla que sigue el expediente: un parámetro
 * retirado conserva sus resultados históricos, y un parámetro añadido después de esta
 * captura no debe aparecer como un hueco en un estudio que nunca lo midió.</p>
 */
@Service
public class ReporteEstudioHtmlService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /** Código del grupo que usan los estudios sin agrupación. */
    private static final String GRUPO_PLANO = "ROOT";

    public String generar(EstudioMedico estudio) {
        Paciente paciente = estudio.getPaciente();
        Persona persona = paciente != null ? paciente.getPersona() : null;

        StringBuilder html = new StringBuilder(4096);
        html.append("<html><head><meta charset=\"utf-8\"/><style>")
            .append(estilos())
            .append("</style></head><body>");

        html.append(encabezado(estudio));
        html.append(datosParticipante(paciente, persona));
        html.append(datosEstudio(estudio));
        html.append(tablaResultados(estudio, persona));
        html.append(observaciones(estudio));
        html.append(pie(estudio));

        html.append("</body></html>");
        return html.toString();
    }

    // ── Secciones ────────────────────────────────────────────────────────────

    private String encabezado(EstudioMedico estudio) {
        String institucion = estudio.getInstitucion() != null ? estudio.getInstitucion().getNombre() : "";
        String tipo = estudio.getTipoEstudio() != null ? estudio.getTipoEstudio().getNombre() : "Estudio";
        return "<div class=\"membrete\">"
                + "<div class=\"inst\">" + esc(institucion) + "</div>"
                + "<div class=\"titulo\">" + esc(tipo) + "</div>"
                + "</div>";
    }

    private String datosParticipante(Paciente paciente, Persona persona) {
        if (paciente == null) return "";
        String edad = edadDe(persona);
        return "<table class=\"ficha\"><tbody>"
                + fila2("Participante", nombreCompleto(persona), "Folio", esc(paciente.getFolio()))
                + fila2("Sexo", sexoLegible(persona), "Edad", edad)
                + "</tbody></table>";
    }

    private String datosEstudio(EstudioMedico estudio) {
        String realiza = estudio.getUsuarioRealiza() != null
                ? nombreCompleto(estudio.getUsuarioRealiza().getPersona()) : "";
        return "<table class=\"ficha\"><tbody>"
                + fila2("Fecha del estudio", fechaHora(estudio.getFechaEstudio()),
                        "Realizó", realiza)
                + "</tbody></table>";
    }

    /**
     * La tabla de resultados, agrupada si el estudio usa grupos.
     *
     * <p>Un mismo parámetro puede repetirse dentro de un estudio si cambia el grupo o
     * el orden —son mediciones distintas—, así que agrupar es imprescindible: en una
     * lista plana esas repeticiones parecerían un error de captura.</p>
     */
    private String tablaResultados(EstudioMedico estudio, Persona persona) {
        List<ResultadoEstudio> resultados = estudio.getResultadoEstudio();
        if (resultados == null || resultados.isEmpty()) {
            return "<p class=\"vacio\">Este estudio no tiene resultados capturados.</p>";
        }

        Persona.Sexo sexo = persona != null ? persona.getSexo() : null;

        // LinkedHashMap: el orden de los grupos es el que trae la consulta, que ya
        // viene ordenada por grupo y posición.
        Map<String, List<ResultadoEstudio>> porGrupo = new LinkedHashMap<>();
        for (ResultadoEstudio r : resultados) {
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
                sb.append("<div class=\"grupo\">").append(esc(etiqueta)).append("</div>");
            }
            sb.append(tablaDe(grupo.getValue(), sexo));
        }
        return sb.toString();
    }

    private String tablaDe(List<ResultadoEstudio> resultados, Persona.Sexo sexo) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class=\"resultados\">")
          .append("<thead><tr>")
          .append("<th class=\"c-param\">Parámetro</th>")
          .append("<th class=\"c-valor\">Resultado</th>")
          .append("<th class=\"c-unidad\">Unidad</th>")
          .append("<th class=\"c-ref\">Referencia</th>")
          .append("</tr></thead><tbody>");

        for (ResultadoEstudio r : resultados) {
            ParametroEstudio p = r.getParametro();
            RangoReferencia.Rango rango = RangoReferencia.de(p, sexo);
            boolean fuera = p != null && p.getTipo() == TipoParametro.NUMERICO
                    && RangoReferencia.fueraDeRango(r.getValorNumerico(), rango);

            sb.append("<tr>")
              .append("<td>").append(esc(p != null ? p.getNombre() : "")).append("</td>")
              .append("<td class=\"valor").append(fuera ? " fuera" : "").append("\">")
              .append(esc(valorDe(r))).append(fuera ? " <span class=\"marca\">*</span>" : "")
              .append("</td>")
              .append("<td>").append(esc(p != null ? p.getUnidad() : "")).append("</td>")
              .append("<td>").append(esc(RangoReferencia.texto(rango))).append("</td>")
              .append("</tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    private String observaciones(EstudioMedico estudio) {
        String obs = estudio.getObservaciones();
        if (obs == null || obs.isBlank()) return "";
        return "<div class=\"obs\"><div class=\"obs-tit\">Observaciones</div>"
                + "<div>" + esc(obs) + "</div></div>";
    }

    private String pie(EstudioMedico estudio) {
        return "<div class=\"pie\">"
                + "<span class=\"nota\">* Fuera del rango de referencia</span>"
                + "<span class=\"emision\">Emitido el " + fechaHora(LocalDateTime.now()) + "</span>"
                + "</div>";
    }

    // ── Presentación de valores ──────────────────────────────────────────────

    /** El valor guardado, según el tipo del parámetro. */
    private String valorDe(ResultadoEstudio r) {
        if (r.getValorNumerico() != null) {
            double v = r.getValorNumerico();
            return v == Math.floor(v) && !Double.isInfinite(v)
                    ? String.valueOf((long) v) : String.valueOf(v);
        }
        if (r.getValorBooleano() != null) return r.getValorBooleano() ? "Sí" : "No";
        return r.getValorTexto() != null ? r.getValorTexto() : "";
    }

    private String nombreCompleto(Persona p) {
        if (p == null) return "";
        StringBuilder sb = new StringBuilder();
        anexar(sb, p.getNombre());
        anexar(sb, p.getSegundoNombre());
        anexar(sb, p.getApellidoPaterno());
        anexar(sb, p.getApellidoMaterno());
        return esc(sb.toString());
    }

    private void anexar(StringBuilder sb, String parte) {
        if (parte == null || parte.isBlank()) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(parte.trim());
    }

    private String sexoLegible(Persona p) {
        if (p == null || p.getSexo() == null) return "";
        return p.getSexo() == Persona.Sexo.F ? "Mujer" : "Hombre";
    }

    /**
     * La edad no se guarda en ninguna parte: se calcula desde la fecha de nacimiento.
     * Hasta ahora solo la calculaba el navegador, así que en el servidor no existía.
     */
    private String edadDe(Persona p) {
        if (p == null || p.getFechaNacimiento() == null) return "";
        int anios = Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears();
        return anios + " años";
    }

    private String fechaHora(LocalDateTime f) {
        return f == null ? "" : f.format(FECHA_HORA);
    }

    // ── Utilidades de HTML ───────────────────────────────────────────────────

    private String fila2(String r1, String v1, String r2, String v2) {
        return "<tr>"
                + "<td class=\"rot\">" + r1 + "</td><td class=\"val\">" + v1 + "</td>"
                + "<td class=\"rot\">" + r2 + "</td><td class=\"val\">" + v2 + "</td>"
                + "</tr>";
    }

    /**
     * Escapa el texto que va al HTML.
     *
     * <p>No es una precaución teórica: los nombres, las observaciones y los valores de
     * texto los escribe un usuario, y un «&amp;» o un «&lt;» sueltos romperían el
     * documento —o algo peor— sin que nadie entendiera por qué.</p>
     */
    private String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String estilos() {
        return """
            @page { size: 215.9mm 279.4mm; margin: 18mm 15mm 16mm 15mm; }
            body { margin: 0; font-family: sans-serif; font-size: 9.5pt; color: #111; }

            .membrete { border-bottom: 0.6mm solid #1f4e5f; padding-bottom: 3mm; margin-bottom: 5mm; }
            .membrete .inst { font-size: 8pt; letter-spacing: 0.3mm; color: #1f4e5f; text-transform: uppercase; }
            .membrete .titulo { font-size: 15pt; margin-top: 1.5mm; }

            table.ficha { width: 100%; border-collapse: collapse; margin-bottom: 3mm; }
            table.ficha td { padding: 1.2mm 2mm; font-size: 9pt; }
            table.ficha td.rot { color: #5a6b78; width: 22mm; }
            table.ficha td.val { font-weight: bold; }

            .grupo { margin-top: 4mm; margin-bottom: 1.5mm; font-size: 9pt;
                     color: #1f4e5f; border-left: 1mm solid #1f4e5f; padding-left: 2mm; }

            table.resultados { width: 100%; border-collapse: collapse; margin-top: 2mm; }
            /* Que el encabezado se repita al partir la hoja: sin esto, la segunda
               pagina de una tabla larga sale sin titulos de columna. */
            table.resultados thead { display: table-header-group; }
            table.resultados th { background: #eef3f5; border-bottom: 0.3mm solid #9fb4bd;
                                  text-align: left; padding: 1.8mm 2mm; font-size: 8.5pt;
                                  color: #33505c; }
            table.resultados td { border-bottom: 0.2mm solid #dde5e9; padding: 1.6mm 2mm; }
            table.resultados .c-valor, table.resultados .c-unidad { width: 25mm; }
            table.resultados .c-ref { width: 32mm; }
            table.resultados td.valor { font-weight: bold; }
            table.resultados td.fuera { color: #a8352c; }
            .marca { font-weight: bold; }

            .obs { margin-top: 5mm; border: 0.2mm solid #dde5e9; padding: 2.5mm; }
            .obs-tit { font-size: 8pt; color: #5a6b78; text-transform: uppercase;
                       letter-spacing: 0.2mm; margin-bottom: 1mm; }

            .vacio { color: #5a6b78; font-style: italic; }

            .pie { margin-top: 6mm; border-top: 0.2mm solid #dde5e9; padding-top: 2mm;
                   font-size: 7.5pt; color: #5a6b78; }
            .pie .emision { float: right; }
            """;
    }
}
