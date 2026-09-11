package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporte;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.services.examenes.ExamenService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Qué se puede insertar en un reporte.
 *
 * <p>El catálogo se arma <b>desde el catálogo real de la institución</b>: sus tipos
 * de estudio y los parámetros de cada uno. No es una lista fija, porque lo que se
 * puede imprimir depende de lo que esa institución mide.</p>
 *
 * <p>Vive en el servidor y se expone por API en lugar de estar escrito en el editor.
 * Con dos listas separadas —la que se ofrece y la que se sabe resolver— acabarían
 * divergiendo: se ofrecería un campo que nadie resuelve, o quedaría uno resoluble
 * que no se puede elegir. Las claves las construye {@link ClaveCampo}, la misma
 * clase que las interpreta, para que no se separen.</p>
 */
@Service
@AllArgsConstructor
public class CatalogoCamposReporte {

    /** Un campo suelto se mete dentro de un texto; un bloque ocupa su propia caja. */
    public enum Clase { CAMPO, BLOQUE }

    /**
     * @param clave       lo que se guarda en la plantilla
     * @param rotulo      lo que ve quien diseña
     * @param grupo       familia: Participante, Estudios, Exámenes, Generales…
     * @param subgrupo    dentro de la familia: el estudio concreto. Null si no aplica
     * @param clase       campo suelto o bloque
     * @param ayuda       qué es, cuando el rótulo no basta
     * @param idTipoEstudio de qué estudio viene, si viene de alguno
     * @param seleccionable si el bloque permite elegir qué filas muestra
     * @param columnas    las que ese bloque sabe imprimir, clave → rótulo
     */
    public record Campo(String clave, String rotulo, String grupo, String subgrupo, Clase clase,
                        String ayuda, Long idTipoEstudio, boolean seleccionable,
                        Map<String, String> columnas) {}

    private static final String G_PARTICIPANTE = "Participante";
    private static final String G_GENERAL      = "Generales";
    private static final String G_TOTALES      = "Totales";
    private static final String G_ESTUDIOS     = "Estudios";
    private static final String G_EXAMENES     = "Exámenes de laboratorio";
    private static final String G_FORMULAS     = "Fórmulas";
    private static final String G_RESUMEN      = "Resumen de laboratorio";

    private final TipoService tipoService;
    private final ExamenService examenService;
    private final FormulaReporteService formulaService;

    /**
     * Todo lo insertable: los datos del participante y, por cada tipo de estudio del
     * catálogo, sus campos, sus parámetros uno a uno, su tabla y sus evidencias.
     *
     * <p>Que cada parámetro aparezca por separado es lo que permite escribir «la
     * densidad fue {{…}} y la masa magra {{…}}» dentro de un párrafo, en vez de
     * verse obligado a meter la tabla entera.</p>
     */
    @Transactional(readOnly = true)
    public List<Campo> todos() {
        List<Campo> campos = new ArrayList<>(fijos());
        campos.addAll(deEstudios());
        campos.addAll(deExamenes());
        campos.addAll(deFormulas());
        return campos;
    }

    /**
     * Las fórmulas del catálogo, como un dato más que se puede insertar.
     *
     * <p>Aparecen aquí y no en una lista aparte a propósito: quien diseña no distingue
     * entre «un dato medido» y «un dato calculado», y no tiene por qué. Al entrar por
     * el mismo catálogo, una fórmula se arrastra a una celda de una tabla, se mete
     * dentro de un párrafo o se coloca como campo suelto exactamente igual que la
     * estatura.</p>
     *
     * <p>Solo se ofrecen las que están en uso. Una retirada sigue calculando en los
     * reportes que ya la mencionan —para eso se retira en vez de borrarse— pero no se
     * propone para diseños nuevos.</p>
     */
    private List<Campo> deFormulas() {
        if (formulaService == null) return List.of();

        List<Campo> campos = new ArrayList<>();
        for (FormulaReporte f : formulaService.getActivas()) {
            if (f.getId() == null) continue;
            String rotulo = f.getUnidadSalida() != null && !f.getUnidadSalida().isBlank()
                    ? f.getNombre() + " (" + f.getUnidadSalida() + ")"
                    : f.getNombre();
            campos.add(new Campo(ClaveCampo.deFormula(f.getId()), rotulo, G_FORMULAS, f.getNombre(),
                    Clase.CAMPO, f.getDescripcion(), null, false, Map.of()));

            // Una fórmula con límites trae además su referencia y su clasificación, cada
            // una como campo suelto: el documento no impone una forma de tabla, así que
            // quien diseña decide si pone las tres, dos o solo el valor.
            boolean tieneReferencia =
                    (f.getExpresionMinimo() != null && !f.getExpresionMinimo().isBlank())
                            || (f.getExpresionMaximo() != null && !f.getExpresionMaximo().isBlank());
            if (!tieneReferencia) continue;

            campos.add(new Campo(ClaveCampo.deParteDeFormula(f.getId(), "referencia"),
                    f.getNombre() + " — referencia", G_FORMULAS, f.getNombre(), Clase.CAMPO,
                    "El rango que aplica a este participante, ya calculado.",
                    null, false, Map.of()));
            campos.add(new Campo(ClaveCampo.deParteDeFormula(f.getId(), "estado"),
                    f.getNombre() + " — dentro o fuera", G_FORMULAS, f.getNombre(), Clase.CAMPO,
                    "Dice si el valor cae dentro del rango, por arriba o por abajo. "
                            + "Queda en blanco si falta el dato: «no se sabe» no es «está mal».",
                    null, false, Map.of()));
            campos.add(new Campo(ClaveCampo.deParteDeFormula(f.getId(), "minimo"),
                    f.getNombre() + " — límite mínimo", G_FORMULAS, f.getNombre(), Clase.CAMPO,
                    null, null, false, Map.of()));
            campos.add(new Campo(ClaveCampo.deParteDeFormula(f.getId(), "maximo"),
                    f.getNombre() + " — límite máximo", G_FORMULAS, f.getNombre(), Clase.CAMPO,
                    null, null, false, Map.of()));
        }
        return campos;
    }

    /**
     * Cada tipo de estudio cuelga de «Estudios» como su propio apartado, con sus
     * parámetros uno a uno, su tabla y sus evidencias.
     *
     * <p>Que cada parámetro aparezca por separado es lo que permite escribir «la
     * densidad fue {{…}} y la masa magra {{…}}» dentro de un párrafo, en vez de
     * verse obligado a meter la tabla entera. Y que el estudio quede como subgrupo
     * es lo que hace que, al buscar por el nombre de un parámetro, se vea a qué
     * estudio pertenece — que es justo lo que necesita quien recibe una lista de
     * nombres sueltos y tiene que armar el formato.</p>
     */
    private List<Campo> deEstudios() {
        List<Campo> campos = new ArrayList<>();
        campos.add(new Campo(ClaveCampo.BLOQUE_LISTADO_ESTUDIOS, "Listado de estudios",
                G_ESTUDIOS, null, Clase.BLOQUE,
                "Una tabla con los estudios del participante y su fecha.",
                null, false, ColumnasBloque.LISTADO_ESTUDIOS));

        // Activos e inactivos: una plantilla puede necesitar un estudio retirado del
        // catálogo si los participantes antiguos lo tienen hecho.
        for (TipoEstudio tipo : tipoService.getAllByInstitucion()) {
            if (tipo.getId() == null) continue;
            long id = tipo.getId();
            String sub = tipo.getNombre();

            campos.add(campo(ClaveCampo.deCampoEstudio(id, "fecha"), "Fecha del estudio", G_ESTUDIOS, sub, id));
            campos.add(campo(ClaveCampo.deCampoEstudio(id, "realizo"), "Quién lo realizó", G_ESTUDIOS, sub, id));
            campos.add(campo(ClaveCampo.deCampoEstudio(id, "observaciones"), "Observaciones", G_ESTUDIOS, sub, id));

            for (ParametroEstudio p : parametrosDe(tipo)) {
                if (p.getId() == null) continue;
                String rotulo = p.getUnidad() != null && !p.getUnidad().isBlank()
                        ? p.getNombre() + " (" + p.getUnidad() + ")"
                        : p.getNombre();
                String ayuda = Boolean.FALSE.equals(p.getActivo())
                        ? "Fuera de uso — solo saldrá en estudios que ya lo midieron"
                        : null;
                campos.add(new Campo(ClaveCampo.deParametro(id, p.getId()), rotulo,
                        G_ESTUDIOS, sub, Clase.CAMPO, ayuda, id, false, Map.of()));
            }

            campos.add(new Campo(ClaveCampo.deBloqueResultados(id), "Tabla de resultados",
                    G_ESTUDIOS, sub, Clase.BLOQUE,
                    "Los parámetros de este estudio, con unidad y referencia. Se puede elegir cuáles.",
                    id, true, ColumnasBloque.RESULTADOS));
            campos.add(new Campo(ClaveCampo.deBloqueEvidencias(id), "Evidencias adjuntas",
                    G_ESTUDIOS, sub, Clase.BLOQUE,
                    "Los archivos adjuntos de este estudio.", id, false, Map.of()));
        }
        return campos;
    }

    /**
     * Los analitos de laboratorio.
     *
     * <p>Un examen no es un panel con parámetros dentro como un estudio: cada uno es
     * un analito suelto con su propio resultado. Por eso cuelgan todos del mismo
     * apartado en lugar de abrir uno por examen.</p>
     */
    private List<Campo> deExamenes() {
        List<Campo> campos = new ArrayList<>();
        campos.add(new Campo(ClaveCampo.BLOQUE_LISTADO_EXAMENES, "Tabla de laboratorio",
                G_EXAMENES, null, Clase.BLOQUE,
                "Los resultados de laboratorio del participante. Se puede elegir cuáles.",
                null, true, ColumnasBloque.LISTADO_EXAMENES));

        for (Examen examen : examenService.getAllExamenes()) {
            if (examen.getId() == null) continue;
            long id = examen.getId();
            String nombre = examen.getParametro();
            String rotulo = examen.getUnidad() != null && !examen.getUnidad().isBlank()
                    ? nombre + " (" + examen.getUnidad() + ")" : nombre;
            String ayuda = Boolean.FALSE.equals(examen.getActivo())
                    ? "Fuera de uso — solo saldrá si ya se le tomó al participante" : null;

            campos.add(new Campo(ClaveCampo.deExamen(id, "valor"), rotulo,
                    G_EXAMENES, "Analitos", Clase.CAMPO, ayuda, null, false, Map.of()));
            campos.add(new Campo(ClaveCampo.deExamen(id, "fecha"), nombre + " — fecha",
                    G_EXAMENES, "Analitos", Clase.CAMPO, null, null, false, Map.of()));
            campos.add(new Campo(ClaveCampo.deExamen(id, "referencia"), nombre + " — referencia",
                    G_EXAMENES, "Analitos", Clase.CAMPO, null, null, false, Map.of()));
        }
        return campos;
    }

    /** Los que no dependen de ningún estudio ni examen. */
    private List<Campo> fijos() {
        return List.of(
                campo(ResolvedorCampos.PARTICIPANTE_NOMBRE, "Nombre completo", G_PARTICIPANTE, null, null),
                campo(ResolvedorCampos.PARTICIPANTE_FOLIO, "Folio", G_PARTICIPANTE, null, null),
                new Campo(ResolvedorCampos.PARTICIPANTE_EDAD, "Edad", G_PARTICIPANTE, null, Clase.CAMPO,
                        "Se calcula a la fecha de emisión; no se guarda en el expediente",
                        null, false, Map.of()),
                campo(ResolvedorCampos.PARTICIPANTE_SEXO, "Sexo", G_PARTICIPANTE, null, null),
                campo(ResolvedorCampos.PARTICIPANTE_CURP, "CURP", G_PARTICIPANTE, null, null),
                campo(ResolvedorCampos.PARTICIPANTE_NACIMIENTO, "Fecha de nacimiento", G_PARTICIPANTE, null, null),

                campo(ResolvedorCampos.INSTITUCION_NOMBRE, "Institución", G_GENERAL, null, null),
                new Campo(ResolvedorCampos.EMISION_FECHA, "Fecha de emisión", G_GENERAL, null, Clase.CAMPO,
                        "Cuándo se generó el documento, no cuándo se hizo el estudio",
                        null, false, Map.of()),

                campo(ResolvedorCampos.TOTAL_ESTUDIOS, "Total de estudios", G_TOTALES, null, null),
                campo(ResolvedorCampos.TOTAL_EXAMENES, "Total de exámenes", G_TOTALES, null, null),
                campo(ResolvedorCampos.TOTAL_MUESTRAS, "Total de muestras", G_TOTALES, null, null),

                // El encabezado del reporte del participante: cuántas mediciones hay
                // en cada situación. Cuentan los laboratorios, que son los que la hoja
                // lista; un total que incluyera lo que no se enseña no cuadraría con
                // lo que se lee debajo.
                new Campo(ClaveCampo.deResumen("total"), "Mediciones realizadas", G_RESUMEN, null,
                        Clase.CAMPO, "Cuántos laboratorios tiene registrados el participante",
                        null, false, Map.of()),
                new Campo(ClaveCampo.deResumen("enRango"), "En rango habitual", G_RESUMEN, null,
                        Clase.CAMPO, "Dentro del rango que aplica a su sexo",
                        null, false, Map.of()),
                new Campo(ClaveCampo.deResumen("ligeramenteFuera"), "Ligeramente fuera", G_RESUMEN, null,
                        Clase.CAMPO, "Fuera, pero dentro del margen configurado en el analito",
                        null, false, Map.of()),
                new Campo(ClaveCampo.deResumen("revisar"), "A revisar", G_RESUMEN, null,
                        Clase.CAMPO, "Fuera y más allá de ese margen",
                        null, false, Map.of()),
                new Campo(ClaveCampo.deResumen("sinDato"), "Sin dato", G_RESUMEN, null,
                        Clase.CAMPO, "Sin valor o sin rango con el que comparar; no cuentan como normales",
                        null, false, Map.of()));
    }

    /**
     * Los parámetros del tipo. Se piden a la entidad, que los trae consigo; si la
     * colección viniera vacía por no estar cargada, el estudio aparecería sin
     * parámetros y nadie entendería por qué.
     */
    private List<ParametroEstudio> parametrosDe(TipoEstudio tipo) {
        return tipo.getParametros() == null ? List.of() : tipo.getParametros();
    }

    private Campo campo(String clave, String rotulo, String grupo, String subgrupo, Long idTipo) {
        return new Campo(clave, rotulo, grupo, subgrupo, Clase.CAMPO, null, idTipo, false, Map.of());
    }
}
