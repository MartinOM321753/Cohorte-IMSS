package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.JsonNode;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.persona.Persona;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * La lista de resultados del reporte que se entrega al participante.
 *
 * <p>Es la misma información que la tabla de resultados, con otro trato. La tabla
 * está pensada para el expediente: columnas parejas, densa, para leerla de un vistazo
 * profesional. Esto está pensado para quien se lleva el papel a casa: cada medición
 * ocupa una fila con su nombre en palabras, su valor grande, una barra que enseña
 * dónde cae dentro de lo habitual, y una frase que dice qué significa.</p>
 *
 * <p>Se emite como una tabla HTML de verdad y no como cajas colocadas a mano, porque
 * es lo único que sabe partirse entre páginas conservando el encabezado. Dentro de la
 * celda de la barra sí hay cajas absolutas: ahí no hay nada que partir.</p>
 *
 * <h3>Una fila por resultado capturado</h3>
 *
 * <p>Se recorre lo que se midió, no el catálogo. Un parámetro retirado conserva sus
 * resultados y sigue apareciendo; uno dado de alta después no sale como hueco en un
 * estudio que nunca lo midió.</p>
 */
@Service
@AllArgsConstructor
public class BloqueLista {

    private final ResolvedorCampos resolvedor;

    /** Una fila ya resuelta, venga de un examen o de un parámetro de estudio. */
    public record Fila(String nombre, String valor, String unidad,
                       RangoReferencia.Rango rango, Double numero,
                       EstadoResultado estado) {

        /** El texto de la referencia, en las palabras del reporte. */
        public String referencia() {
            String base = RangoReferencia.texto(rango);
            if (base.isEmpty()) return "";
            String sentido = RangoReferencia.sentido(numero, rango);
            return sentido.isEmpty() ? base : sentido + " · " + base;
        }

        /**
         * Cómo se rotula el estado en la fila.
         *
         * <p>Una diferencia menor se nombra por su lado —«Por arriba», «Por abajo»—
         * porque es lo que el participante necesita saber, y lo que hay que atender se
         * nombra como tal. Los tres caben en una columna sin partirse, que es la
         * condición para que se lean de un vistazo.</p>
         *
         * <p>No basta con el color: un documento que sólo distinga por color deja
         * fuera a quien no lo percibe y no sobrevive a una fotocopia.</p>
         */
        public String etiquetaEstado() {
            if (!estado.medido()) return "";
            if (estado == EstadoResultado.REVISAR) return estado.etiquetaCorta();
            String sentido = RangoReferencia.sentido(numero, rango);
            return sentido.isEmpty() ? EstadoResultado.EN_RANGO.etiquetaCorta() : sentido;
        }
    }

    /** Cuántas mediciones hay en cada situación, para el resumen de la portada. */
    public record Resumen(int total, int enRango, int ligeramenteFuera, int revisar,
                          int sinDato) {

        public static Resumen de(List<Fila> filas) {
            int en = 0, lig = 0, rev = 0, sin = 0;
            for (Fila f : filas) {
                switch (f.estado()) {
                    case EN_RANGO -> en++;
                    case LIGERAMENTE_FUERA -> lig++;
                    case REVISAR -> rev++;
                    case SIN_DATO -> sin++;
                }
            }
            return new Resumen(filas.size(), en, lig, rev, sin);
        }
    }

    // ── Recolección ──────────────────────────────────────────────────────────

    /** Las filas de la tabla de laboratorio del participante. */
    public List<Fila> deExamenes(ContextoReporte ctx, List<Long> seleccion) {
        Persona.Sexo sexo = ctx.persona() != null ? ctx.persona().getSexo() : null;
        Set<Long> visibles = seleccion == null ? Set.of() : Set.copyOf(seleccion);

        List<Fila> filas = new ArrayList<>();
        for (ResultadoExamen r : ctx.examenesOrdenados()) {
            if (r.getExamen() == null) continue;
            if (!visibles.isEmpty() && !visibles.contains(r.getExamen().getId())) continue;

            filas.add(new Fila(
                    r.getExamen().getParametro(),
                    resolvedor.numero(r.getValorObtenido()),
                    r.getExamen().getUnidad(),
                    BloqueExamenes.rangoDe(r, sexo),
                    r.getValorObtenido(),
                    BloqueExamenes.estadoDe(r, sexo)));
        }
        return filas;
    }

    /** Las filas de un estudio concreto. */
    public List<Fila> deEstudio(EstudioMedico estudio, Persona.Sexo sexo, List<Long> seleccion) {
        if (estudio == null || estudio.getResultadoEstudio() == null) return List.of();
        Set<Long> visibles = seleccion == null ? Set.of() : Set.copyOf(seleccion);

        List<Fila> filas = new ArrayList<>();
        for (ResultadoEstudio r : estudio.getResultadoEstudio()) {
            ParametroEstudio p = r.getParametro();
            if (p == null) continue;
            if (!visibles.isEmpty() && !visibles.contains(p.getId())) continue;

            filas.add(new Fila(
                    p.getNombre(),
                    resolvedor.textoDelValor(r),
                    p.getUnidad(),
                    RangoReferencia.de(p, sexo),
                    r.getValorNumerico(),
                    BloqueResultados.estadoDe(r, sexo)));
        }
        return filas;
    }

    // ── Dibujo ───────────────────────────────────────────────────────────────

    /** Qué columnas puede llevar una lista. */
    public static final List<String> COLUMNAS = List.of("nombre", "valor", "barra", "referencia", "estado");

    /**
     * @param columnas cuáles de {@link #COLUMNAS} salen, en ese orden
     */
    public String html(List<Fila> filas, List<String> columnas, Estilo estilo) {
        if (filas.isEmpty()) {
            return "<p style=\"font-size:" + estilo.tamanoPt() + "pt;color:#5a6b78;font-style:italic;\">"
                    + "No hay mediciones que mostrar.</p>";
        }
        List<String> cols = columnas == null || columnas.isEmpty() ? COLUMNAS : columnas;

        StringBuilder sb = new StringBuilder(2048);
        // table-layout:fixed y un <colgroup> con los anchos: sin ellos el motor
        // reparte a su criterio y la última columna se sale del papel — se imprimía
        // el documento con la columna de estado fuera del borde.
        sb.append("<table style=\"border-collapse:collapse;width:100%;table-layout:fixed;font-size:")
          .append(estilo.tamanoPt()).append("pt;color:").append(estilo.colorTexto()).append(";\">");
        sb.append("<colgroup>");
        for (String col : cols) {
            sb.append("<col style=\"width:").append(ancho(col, cols)).append("%;\"/>");
        }
        sb.append("</colgroup>");

        for (Fila f : filas) {
            sb.append("<tr>");
            for (String col : cols) {
                sb.append(celda(col, f, estilo));
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    /**
     * Qué parte del ancho se lleva cada columna.
     *
     * <p>Los pesos son relativos y se reparten sobre las columnas que de verdad
     * salgan: quitar la barra tiene que devolverle su sitio a las demás, no dejar un
     * hueco ni empujar la última fuera del papel.</p>
     */
    private static double ancho(String col, List<String> presentes) {
        double total = presentes.stream().mapToDouble(BloqueLista::peso).sum();
        return total <= 0 ? 100.0 / presentes.size() : peso(col) / total * 100;
    }

    private static double peso(String col) {
        return switch (col) {
            case "nombre" -> 32;
            case "valor" -> 16;
            case "barra" -> 22;
            case "referencia" -> 20;
            case "estado" -> 14;
            default -> 10;
        };
    }

    private String celda(String col, Fila f, Estilo estilo) {
        String borde = "border-bottom:0.2mm solid " + estilo.colorBorde() + ";";
        String base = "padding:1.8mm 2mm;vertical-align:middle;" + borde;

        return switch (col) {
            case "nombre" -> "<td style=\"" + base + "\">"
                    + Html.escapar(f.nombre()) + "</td>";

            case "valor" -> "<td style=\"" + base + "text-align:right;font-weight:bold;color:"
                    + (f.estado().fuera() ? f.estado().color() : estilo.colorTexto()) + ";\">"
                    + Html.escapar(f.valor())
                    + (f.unidad() == null || f.unidad().isBlank() ? ""
                       : " <span style=\"font-weight:normal;font-size:" + (estilo.tamanoPt() - 1.5)
                         + "pt;color:#5a6b78;\">" + Html.escapar(f.unidad()) + "</span>")
                    + "</td>";

            case "barra" -> "<td style=\"" + base + "\">" + barra(f, estilo) + "</td>";

            case "referencia" -> "<td style=\"" + base + "font-size:" + (estilo.tamanoPt() - 1)
                    + "pt;color:#5a6b78;\">" + Html.escapar(f.referencia()) + "</td>";

            // nowrap: una etiqueta de estado partida en dos renglones deja de leerse
            // de un vistazo, que es lo único que tiene que hacer.
            case "estado" -> "<td style=\"" + base + "text-align:right;font-size:"
                    + (estilo.tamanoPt() - 1) + "pt;color:"
                    + (f.estado().medido() ? f.estado().color() : "#5a6b78") + ";\">"
                    + Html.escapar(f.etiquetaEstado())
                    + "</td>";

            default -> "";
        };
    }

    /**
     * La barra: una pista, la franja del rango encima y la marca del resultado.
     *
     * <p>Con cajas absolutas dentro de una relativa, que es lo que el motor de PDF
     * entiende. Sin rango no se dibuja nada — ni la pista— porque una pista vacía
     * parecería una medición que no llegó, y lo que pasa es que ese parámetro no
     * tiene referencia.</p>
     */
    private String barra(Fila f, Estilo estilo) {
        BarraRango b = BarraRango.de(f.numero(), f.rango());
        if (b == null) return "";

        double altoMm = 2.2;
        String pista = "position:relative;height:" + altoMm + "mm;background:#eff2f0;";

        StringBuilder sb = new StringBuilder(320);
        sb.append("<div style=\"").append(pista).append("\">");
        sb.append("<div style=\"position:absolute;top:0;height:").append(altoMm)
          .append("mm;left:").append(b.franjaIzquierdaCss()).append("%;width:")
          .append(b.franjaAnchoCss()).append("%;background:")
          .append(EstadoResultado.EN_RANGO.fondo()).append(";\"></div>");

        if (b.marcaVisible()) {
            sb.append("<div style=\"position:absolute;top:-0.8mm;height:").append(altoMm + 1.6)
              .append("mm;left:").append(b.marcaCss()).append("%;width:0.8mm;background:")
              .append(f.estado().medido() ? f.estado().color() : "#5a6b78")
              .append(";\"></div>");
        }
        sb.append("</div>");
        return sb.toString();
    }

    /** El aspecto de la lista. Se reaprovecha el de las tablas para no tener dos. */
    public record Estilo(double tamanoPt, String colorTexto, String colorBorde) {

        public static Estilo de(JsonNode el) {
            BloqueResultados.Estilo t = BloqueResultados.Estilo.de(el);
            return new Estilo(t.tamanoPt(), t.colorTexto(), t.colorBorde());
        }
    }
}
