package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.services.examenes.ExamenService;
import imss.gob.mx.cohorte.services.formulas.Unidad;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Lo que se le puede ofrecer a una fórmula como variable.
 *
 * <p>Es un catálogo aparte del de campos del diseñador, y no un filtro sobre aquel,
 * porque responde a otra pregunta. El diseñador pregunta «¿qué se puede imprimir?» y
 * ahí cabe todo: nombres, fechas, tablas. Una fórmula pregunta «¿con qué se puede
 * calcular?», y la respuesta es mucho más corta y necesita un dato que el otro no
 * lleva: a qué otras unidades se puede pasar cada variable.</p>
 *
 * <h2>La regla que hace segura la herramienta</h2>
 * <p>Hay dos usos distintos y por eso hay dos reglas.</p>
 *
 * <p><b>Para calcular</b> —sumar, dividir, elevar— solo sirven los parámetros
 * <b>numéricos y con unidad declarada</b>. Sin unidad no hay conversión posible, y la
 * conversión es lo que evita que la estatura entre en centímetros a una fórmula escrita
 * para metros.</p>
 *
 * <p><b>Para comparar</b> entran además los de <b>opciones</b> y los de <b>sí/no</b>,
 * que es donde viven las preguntas que de verdad ramifican una regla: de qué mano es la
 * dinamometría, si hubo fatiga visible, si la persona es mujer u hombre. Estos traen su
 * lista de valores y el editor los ofrece únicamente dentro de una condición: no se
 * pueden sumar, y el evaluador lo impide.</p>
 *
 * <p>Los de texto libre siguen fuera: sin un conjunto cerrado de valores, comparar
 * contra ellos es adivinar cómo lo escribió quien capturó.</p>
 */
@Service
@AllArgsConstructor
public class CatalogoVariablesFormula {

    private static final String G_PARTICIPANTE = "Participante";
    private static final String G_ESTUDIOS     = "Estudios";
    private static final String G_EXAMENES     = "Exámenes de laboratorio";

    private final TipoService tipoService;
    private final ExamenService examenService;

    /**
     * @param clave          de dónde sale el dato; es lo que se guarda en la fórmula
     * @param rotulo         lo que ve quien la arma
     * @param grupo          familia: Participante, Estudios, Exámenes
     * @param subgrupo       el estudio concreto, cuando viene de uno
     * @param unidad         en la que está guardado
     * @param unidadesPosibles a cuáles se puede pasar; siempre incluye la suya
     * @param ayuda          lo que haya que aclarar, como qué significa el 1 en el sexo
     */
    public record Variable(String clave, String rotulo, String grupo, String subgrupo,
                           String unidad, List<String> unidadesPosibles, String ayuda,
                           List<OpcionVariable> opciones) {

        /** Tiene un conjunto cerrado de valores: solo se puede preguntar si es uno u otro. */
        public boolean esDeOpciones() {
            return opciones != null && !opciones.isEmpty();
        }
    }

    /**
     * Un valor posible de una variable, con lo que se ve y lo que se escribe.
     *
     * <p>Las dos cosas no siempre coinciden, y separarlas es lo que evita que alguien
     * tenga que memorizar convenciones. El sexo se compara contra 1 o 0 —así estaba
     * modelado desde el principio y así siguen funcionando las fórmulas ya escritas—
     * pero en la pantalla se elige «Mujer» u «Hombre». En un parámetro de opciones, en
     * cambio, lo que se ve es lo que se escribe, solo que entre comillas.</p>
     *
     * @param etiqueta       lo que lee quien arma la condición
     * @param valorEnFormula lo que se inserta en la expresión, ya listo para comparar
     */
    public record OpcionVariable(String etiqueta, String valorEnFormula) {

        /** Un texto: se escribe entrecomillado. */
        static OpcionVariable texto(String valor) {
            return new OpcionVariable(valor, "'" + valor + "'");
        }

        /** Un número con nombre, como el sexo. */
        static OpcionVariable numero(String etiqueta, String valor) {
            return new OpcionVariable(etiqueta, valor);
        }
    }

    @Transactional(readOnly = true)
    public List<Variable> todas() {
        List<Variable> variables = new ArrayList<>(delParticipante());
        variables.addAll(deEstudios());
        variables.addAll(deExamenes());
        return variables;
    }

    private List<Variable> delParticipante() {
        return List.of(
                variable(ResolvedorCampos.PARTICIPANTE_EDAD, "Edad", G_PARTICIPANTE, null,
                        "años", "Años cumplidos a la fecha en que se emite el reporte."),
                variable(ResolvedorCampos.PARTICIPANTE_SEXO, "Sexo", G_PARTICIPANTE, null,
                        null,
                        "Se elige «Mujer» u «Hombre» de la lista. Por dentro vale 1 y 0 "
                                + "respectivamente, que es lo que se ve en la fórmula. Sin sexo "
                                + "registrado, la regla no elige ninguna rama.",
                        List.of(OpcionVariable.numero("Mujer", "1"),
                                OpcionVariable.numero("Hombre", "0"))));
    }

    private List<Variable> deEstudios() {
        List<Variable> variables = new ArrayList<>();

        for (TipoEstudio tipo : tipoService.getAllByInstitucion()) {
            if (tipo.getId() == null) continue;
            long id = tipo.getId();

            for (ParametroEstudio p : tipo.getParametros() == null ? List.<ParametroEstudio>of()
                    : tipo.getParametros()) {
                if (p.getId() == null) continue;

                String ayuda = Boolean.FALSE.equals(p.getActivo())
                        ? "Fuera de uso — solo tendrá valor en estudios que ya lo midieron"
                        : null;
                String clave = ClaveCampo.deParametro(id, p.getId());

                if (sirveParaCalcular(p)) {
                    variables.add(variable(clave, p.getNombre(),
                            G_ESTUDIOS, tipo.getNombre(), p.getUnidad(), ayuda, List.of()));
                    continue;
                }

                // Los de opciones y los de sí/no entran solo para poder compararlos. Se
                // distinguen por traer su lista de valores, y el editor los ofrece
                // únicamente dentro de una condición.
                List<OpcionVariable> opciones = opcionesDe(p);
                if (!opciones.isEmpty()) {
                    variables.add(variable(clave, p.getNombre(), G_ESTUDIOS, tipo.getNombre(),
                            null, ayuda != null ? ayuda
                                    : "No es un número, así que no se puede sumar ni dividir. "
                                            + "Sirve para comparar dentro de una condición.",
                            opciones));
                }
            }
        }
        return variables;
    }

    private List<Variable> deExamenes() {
        List<Variable> variables = new ArrayList<>();
        for (Examen examen : examenService.getAllExamenes()) {
            if (examen.getId() == null) continue;
            variables.add(variable(ClaveCampo.deExamen(examen.getId(), "valor"),
                    examen.getParametro(), G_EXAMENES, null, examen.getUnidad(), null));
        }
        return variables;
    }

    /**
     * Un parámetro sirve para calcular si guarda un número y dice en qué se mide.
     *
     * <p>Sin unidad no se puede convertir, y la conversión es justamente lo que evita
     * que la estatura entre en centímetros a una fórmula escrita para metros. Un
     * parámetro numérico sin unidad se deja fuera en lugar de ofrecerlo a medias.</p>
     */
    private boolean sirveParaCalcular(ParametroEstudio p) {
        return p.getTipo() == TipoParametro.NUMERICO
                && p.getUnidad() != null && !p.getUnidad().isBlank();
    }

    private Variable variable(String clave, String rotulo, String grupo, String subgrupo,
                              String textoUnidad, String ayuda) {
        return variable(clave, rotulo, grupo, subgrupo, textoUnidad, ayuda, List.of());
    }

    private Variable variable(String clave, String rotulo, String grupo, String subgrupo,
                              String textoUnidad, String ayuda, List<OpcionVariable> opciones) {
        Unidad unidad = Unidad.de(textoUnidad);
        List<String> posibles = unidad.intercambiables().stream().map(Unidad::nombre).toList();
        return new Variable(clave, rotulo, grupo, subgrupo, unidad.nombre(), posibles,
                ayuda, opciones);
    }

    /**
     * Los valores que ese parámetro admite.
     *
     * <p>Los de opciones traen los suyos, en el orden del catálogo. Los de sí/no traen
     * los dos que hay: se guardan como booleano y el resolvedor los entrega ya escritos
     * en palabras, que es como se comparan.</p>
     */
    private List<OpcionVariable> opcionesDe(ParametroEstudio p) {
        if (p.getTipo() == TipoParametro.BOOLEANO) {
            return List.of(OpcionVariable.texto("Sí"), OpcionVariable.texto("No"));
        }
        if (p.getTipo() != TipoParametro.TEXTO_OPCIONES || p.getOpciones() == null) return List.of();

        return p.getOpciones().stream()
                .sorted(java.util.Comparator.comparing(
                        o -> o.getOrden() == null ? 0 : o.getOrden()))
                .map(OpcionParametro::getValor)
                .filter(v -> v != null && !v.isBlank())
                .map(OpcionVariable::texto)
                .toList();
    }
}
