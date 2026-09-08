package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.services.examenes.ExamenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * La regla que hace segura la herramienta, comprobada.
 *
 * <p>Se acordó que el sistema no le pida a nadie que cambie el tipo de un parámetro:
 * simplemente no ofrece lo que no se puede calcular. Estas pruebas fijan esa regla,
 * porque es la única barrera que separa una fórmula de multiplicar un texto.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogoVariablesFormulaTest {

    @Mock private TipoService tipoService;
    @Mock private ExamenService examenService;

    private ParametroEstudio parametro(long id, String nombre, TipoParametro tipo, String unidad) {
        ParametroEstudio p = new ParametroEstudio();
        p.setId(id);
        p.setNombre(nombre);
        p.setTipo(tipo);
        p.setUnidad(unidad);
        p.setActivo(true);
        return p;
    }

    /**
     * Un parámetro de opciones con sus valores.
     *
     * <p>Sin valores no hay nada contra qué comparar, y el catálogo lo deja fuera con
     * razón: por eso aquí se le ponen.</p>
     */
    private ParametroEstudio conOpciones(long id, String nombre, String... valores) {
        ParametroEstudio p = parametro(id, nombre, TipoParametro.TEXTO_OPCIONES, null);
        List<OpcionParametro> opciones = new ArrayList<>();
        int orden = 0;
        for (String v : valores) {
            OpcionParametro o = new OpcionParametro();
            o.setId((long) (id * 100 + orden));
            o.setParametro(p);
            o.setValor(v);
            o.setOrden(orden++);
            opciones.add(o);
        }
        p.setOpciones(opciones);
        return p;
    }

    private List<CatalogoVariablesFormula.Variable> catalogo() {
        TipoEstudio tanita = new TipoEstudio();
        tanita.setId(7L);
        tanita.setNombre("Analizador de composición corporal TANITA");
        tanita.setParametros(List.of(
                parametro(85, "Estatura", TipoParametro.NUMERICO, "cm"),
                parametro(92, "Peso", TipoParametro.NUMERICO, "kg"),
                parametro(82, "Tipo de cuerpo", TipoParametro.TEXTO, null),
                conOpciones(83, "Sexo", "Mujer", "Hombre"),
                parametro(61, "Uso accesorio de manos", TipoParametro.BOOLEANO, null),
                parametro(999, "Medición sin unidad", TipoParametro.NUMERICO, null)));

        when(tipoService.getAllByInstitucion()).thenReturn(List.of(tanita));
        when(examenService.getAllExamenes()).thenReturn(List.of());

        return new CatalogoVariablesFormula(tipoService, examenService).todas();
    }

    @Test
    @DisplayName("para calcular solo entran los numéricos con unidad")
    void loQueSePuedeCalcular() {
        // Se comprueba por clave y no por rótulo: el catálogo real tiene un parámetro
        // «Sexo» en el TANITA además del sexo del participante, y por el nombre no se
        // distinguen. La clave sí es única.
        assertThat(catalogo())
                .filteredOn(v -> !v.esDeOpciones())
                .extracting(CatalogoVariablesFormula.Variable::clave)
                .contains("estudio.7.param.85", "estudio.7.param.92")
                .as("un texto libre no se puede multiplicar ni comparar con nada fiable")
                .doesNotContain("estudio.7.param.82");
    }

    @Test
    @DisplayName("para comparar entran también los de opciones y los de sí/no")
    void loQueSePuedeComparar() {
        // Son las preguntas que de verdad ramifican una regla: de qué mano, si hubo
        // fatiga, si es mujer u hombre.
        assertThat(catalogo())
                .filteredOn(CatalogoVariablesFormula.Variable::esDeOpciones)
                .extracting(CatalogoVariablesFormula.Variable::clave)
                .contains(
                        "estudio.7.param.83",   // Sexo del TANITA, opciones
                        "estudio.7.param.61");  // Uso accesorio de manos, booleano
    }

    @Test
    @DisplayName("un sí/no ofrece sus dos valores escritos en palabras")
    void losBooleanosTraenSusValores() {
        var booleano = catalogo().stream()
                .filter(v -> "estudio.7.param.61".equals(v.clave())).findFirst().orElseThrow();

        // Así se comparan: el resolvedor los entrega ya escritos, no como true/false.
        assertThat(booleano.opciones())
                .extracting(CatalogoVariablesFormula.OpcionVariable::valorEnFormula)
                .containsExactly("'Sí'", "'No'");
    }

    @Test
    @DisplayName("un numérico sin unidad se queda fuera en vez de ofrecerse a medias")
    void sinUnidadNoEntra() {
        // Sin unidad no hay conversión posible, y la conversión es justo lo que evita
        // que la estatura entre en centímetros a una fórmula escrita para metros.
        assertThat(catalogo())
                .extracting(CatalogoVariablesFormula.Variable::rotulo)
                .doesNotContain("Medición sin unidad");
    }

    @Test
    @DisplayName("cada variable trae a qué unidades se puede pasar")
    void lasUnidadesQueSeOfrecen() {
        var estatura = catalogo().stream()
                .filter(v -> "Estatura".equals(v.rotulo())).findFirst().orElseThrow();

        assertThat(estatura.unidad()).isEqualTo("cm");
        assertThat(estatura.unidadesPosibles())
                .as("es lo que el editor ofrece cambiar al insertar la variable")
                .containsExactly("mm", "cm", "m");
    }

    @Test
    @DisplayName("el sexo se elige por su nombre, aunque por dentro sea 1 y 0")
    void elSexoComoVariable() {
        var sexo = catalogo().stream()
                .filter(v -> ResolvedorCampos.PARTICIPANTE_SEXO.equals(v.clave()))
                .findFirst().orElseThrow();

        // Nadie tiene que memorizar la convención: se elige «Mujer» y se escribe el 1.
        // Que el valor siga siendo numérico es lo que mantiene vivas las fórmulas ya
        // escritas como si(sexo = 1, …, …).
        assertThat(sexo.opciones())
                .extracting(CatalogoVariablesFormula.OpcionVariable::etiqueta)
                .containsExactly("Mujer", "Hombre");
        assertThat(sexo.opciones())
                .extracting(CatalogoVariablesFormula.OpcionVariable::valorEnFormula)
                .containsExactly("1", "0");
    }

    @Test
    @DisplayName("la edad también, porque varias ecuaciones la llevan dentro")
    void laEdadComoVariable() {
        assertThat(catalogo())
                .filteredOn(v -> "Edad".equals(v.rotulo()))
                .singleElement()
                .extracting(CatalogoVariablesFormula.Variable::unidad)
                .isEqualTo("años");
    }
}
