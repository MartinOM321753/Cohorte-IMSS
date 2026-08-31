package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.reportes.TipoReporte;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Qué se puede insertar en un reporte.
 *
 * <p>El catálogo vive aquí y se expone por API en lugar de estar escrito en el
 * editor. Si estuviera en el frontend habría dos listas que mantener —la que se
 * ofrece y la que se sabe resolver—, y acabarían separándose: se ofrecería un
 * campo que el servidor no reconoce, o quedaría uno resoluble que nadie puede
 * elegir.</p>
 *
 * <p>Las claves son el contrato: se guardan dentro del diseño de cada plantilla,
 * así que renombrar una rompe las plantillas ya hechas. Al retirar un campo, más
 * vale dejar de ofrecerlo y seguir resolviéndolo.</p>
 */
@Service
public class CatalogoCamposReporte {

    /** Un campo suelto se mete dentro de un texto; un bloque ocupa su propia caja. */
    public enum Clase { CAMPO, BLOQUE }

    /**
     * @param clave      lo que se guarda en la plantilla
     * @param rotulo     lo que ve quien diseña
     * @param grupo      para agrupar el panel del editor
     * @param clase      campo suelto o bloque
     * @param ayuda      qué es, cuando el rótulo no basta
     * @param seleccionable si el bloque permite elegir qué filas muestra
     */
    public record Campo(String clave, String rotulo, String grupo, Clase clase,
                        String ayuda, boolean seleccionable) {

        static Campo de(String clave, String rotulo, String grupo) {
            return new Campo(clave, rotulo, grupo, Clase.CAMPO, null, false);
        }

        static Campo de(String clave, String rotulo, String grupo, String ayuda) {
            return new Campo(clave, rotulo, grupo, Clase.CAMPO, ayuda, false);
        }

        static Campo bloque(String clave, String rotulo, String grupo, String ayuda, boolean seleccionable) {
            return new Campo(clave, rotulo, grupo, Clase.BLOQUE, ayuda, seleccionable);
        }
    }

    // ── Claves ───────────────────────────────────────────────────────────────
    // Constantes y no cadenas sueltas: el resolvedor y el catálogo tienen que
    // hablar de lo mismo, y una errata en una de las dos partes es un campo que
    // se ofrece y no se resuelve.

    public static final String PARTICIPANTE_NOMBRE   = "participante.nombreCompleto";
    public static final String PARTICIPANTE_FOLIO    = "participante.folio";
    public static final String PARTICIPANTE_EDAD     = "participante.edad";
    public static final String PARTICIPANTE_SEXO     = "participante.sexo";
    public static final String PARTICIPANTE_CURP     = "participante.curp";
    public static final String PARTICIPANTE_NACIMIENTO = "participante.fechaNacimiento";

    public static final String ESTUDIO_TIPO          = "estudio.tipo";
    public static final String ESTUDIO_FECHA         = "estudio.fecha";
    public static final String ESTUDIO_REALIZO       = "estudio.realizo";
    public static final String ESTUDIO_OBSERVACIONES = "estudio.observaciones";

    public static final String INSTITUCION_NOMBRE    = "institucion.nombre";
    public static final String EMISION_FECHA         = "emision.fecha";

    public static final String TOTAL_ESTUDIOS        = "totales.estudios";
    public static final String TOTAL_EXAMENES        = "totales.examenes";
    public static final String TOTAL_MUESTRAS        = "totales.muestras";

    public static final String BLOQUE_RESULTADOS     = "bloque.estudio.resultados";
    public static final String BLOQUE_EVIDENCIAS     = "bloque.estudio.evidencias";

    // ── Catálogo ─────────────────────────────────────────────────────────────

    private static final String G_PARTICIPANTE = "Participante";
    private static final String G_ESTUDIO      = "Estudio";
    private static final String G_GENERAL      = "Generales";
    private static final String G_TOTALES      = "Totales";
    private static final String G_BLOQUES      = "Bloques";

    private static final List<Campo> DEL_PARTICIPANTE = List.of(
            Campo.de(PARTICIPANTE_NOMBRE,     "Nombre completo", G_PARTICIPANTE),
            Campo.de(PARTICIPANTE_FOLIO,      "Folio", G_PARTICIPANTE),
            Campo.de(PARTICIPANTE_EDAD,       "Edad", G_PARTICIPANTE,
                     "Se calcula a la fecha de emisión, no se guarda en el expediente"),
            Campo.de(PARTICIPANTE_SEXO,       "Sexo", G_PARTICIPANTE),
            Campo.de(PARTICIPANTE_CURP,       "CURP", G_PARTICIPANTE),
            Campo.de(PARTICIPANTE_NACIMIENTO, "Fecha de nacimiento", G_PARTICIPANTE));

    private static final List<Campo> GENERALES = List.of(
            Campo.de(INSTITUCION_NOMBRE, "Institución", G_GENERAL),
            Campo.de(EMISION_FECHA,      "Fecha de emisión", G_GENERAL,
                     "Cuándo se generó el documento, no cuándo se hizo el estudio"));

    private static final List<Campo> TOTALES = List.of(
            Campo.de(TOTAL_ESTUDIOS, "Total de estudios",  G_TOTALES),
            Campo.de(TOTAL_EXAMENES, "Total de exámenes",  G_TOTALES),
            Campo.de(TOTAL_MUESTRAS, "Total de muestras",  G_TOTALES));

    private static final List<Campo> DEL_ESTUDIO = List.of(
            Campo.de(ESTUDIO_TIPO,          "Tipo de estudio", G_ESTUDIO),
            Campo.de(ESTUDIO_FECHA,         "Fecha del estudio", G_ESTUDIO),
            Campo.de(ESTUDIO_REALIZO,       "Quién lo realizó", G_ESTUDIO),
            Campo.de(ESTUDIO_OBSERVACIONES, "Observaciones", G_ESTUDIO),
            Campo.bloque(BLOQUE_RESULTADOS, "Tabla de resultados", G_BLOQUES,
                    "Los parámetros capturados, con unidad y rango de referencia. "
                  + "Puedes elegir cuáles se muestran.", true),
            Campo.bloque(BLOQUE_EVIDENCIAS, "Evidencias adjuntas", G_BLOQUES,
                    "Los archivos adjuntos del estudio.", false));

    /** Lo que se puede insertar en una plantilla de ese tipo. */
    public List<Campo> paraTipo(TipoReporte tipo) {
        return switch (tipo) {
            case ESTUDIO -> concat(DEL_PARTICIPANTE, DEL_ESTUDIO, GENERALES, TOTALES);
            // Los demás tipos se irán llenando conforme se implementen sus
            // resolvedores. Ofrecer campos que nadie sabe resolver produciría
            // plantillas que fallan al emitir.
            case EXAMENES, SOMATOMETRIA, EXPEDIENTE -> concat(DEL_PARTICIPANTE, GENERALES, TOTALES);
            case AGREGADO -> concat(GENERALES, TOTALES);
        };
    }

    @SafeVarargs
    private static List<Campo> concat(List<Campo>... listas) {
        return java.util.Arrays.stream(listas).flatMap(List::stream).toList();
    }
}
