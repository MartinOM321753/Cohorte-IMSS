package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporte;
import imss.gob.mx.cohorte.services.formulas.Unidad;
import imss.gob.mx.cohorte.services.formulas.VariableFormula;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Una fórmula puesta en un reporte, hasta el documento.
 *
 * <p>Es la prueba de que la decisión de tratar la fórmula como <b>una clave más</b> —y
 * no como un tipo de elemento nuevo— era la correcta: sin tocar el lienzo ni el
 * maquetador, el resultado aparece en los tres sitios donde se puede meter un dato, que
 * son un párrafo, una celda de tabla hecha a mano y un campo suelto.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FormulaEnElReporteTest {

    private static final long TANITA = 7L;
    private static final long PESO = 92L;
    private static final long ESTATURA = 85L;

    @Mock private FormulaReporteService formulaService;

    private ResolvedorCampos resolvedor;
    private MaquetadorReporte maquetador;

    @BeforeEach
    void preparar() {
        resolvedor = new ResolvedorCampos();
        resolvedor.conFormulas(formulaService, new CalculadoraFormulas(formulaService));

        maquetador = new MaquetadorReporte(
                new ObjectMapper(), resolvedor, new BloqueResultados(resolvedor),
                new BloqueEstudios(), new BloqueExamenes(resolvedor),
                new BloqueLista(resolvedor),
                new EvidenciasReporte(null, null), new ImagenesReporte(null));

        when(formulaService.buscar(1L)).thenReturn(Optional.of(imc()));
        when(formulaService.variablesDe(any())).thenReturn(List.of(
                new VariableFormula("peso", ClaveCampo.deParametro(TANITA, PESO), Unidad.de("kg")),
                // La estatura está guardada en centímetros y la fórmula la quiere en
                // metros: es justo la conversión que decide quien arma la fórmula.
                new VariableFormula("estatura", ClaveCampo.deParametro(TANITA, ESTATURA), Unidad.de("m"))));
    }

    private FormulaReporte imc() {
        FormulaReporte f = new FormulaReporte();
        f.setId(1L);
        f.setNombre("Índice de masa corporal");
        f.setExpresion("peso / estatura²");
        f.setUnidadSalida("kg/m²");
        f.setDecimales(2);
        return f;
    }

    /** Un participante de 68 kg y 165 cm, medido en la TANITA. */
    private ContextoReporte contexto() {
        Persona persona = new Persona();
        persona.setNombre("Ana");
        persona.setApellidoPaterno("López");
        persona.setSexo(Persona.Sexo.F);
        Paciente paciente = new Paciente();
        paciente.setPersona(persona);
        paciente.setFolio("F-001");

        TipoEstudio tipo = new TipoEstudio();
        tipo.setId(TANITA);
        tipo.setNombre("Analizador de composición corporal TANITA");

        EstudioMedico estudio = new EstudioMedico();
        estudio.setId(500L);
        estudio.setTipoEstudio(tipo);
        estudio.setPaciente(paciente);
        estudio.setFechaEstudio(LocalDateTime.now());
        estudio.setResultadoEstudio(new ArrayList<>());
        estudio.getResultadoEstudio().add(resultado(estudio, PESO, "Peso", "kg", 68d));
        estudio.getResultadoEstudio().add(resultado(estudio, ESTATURA, "Estatura", "cm", 165d));

        return new ContextoReporte(paciente, List.of(estudio), List.of(), null,
                ContextoReporte.Totales.sinCalcular());
    }

    private ResultadoEstudio resultado(EstudioMedico estudio, long idParametro,
                                       String nombre, String unidad, Double valor) {
        ParametroEstudio p = new ParametroEstudio();
        p.setId(idParametro);
        p.setNombre(nombre);
        p.setUnidad(unidad);
        p.setTipo(TipoParametro.NUMERICO);

        ResultadoEstudio r = new ResultadoEstudio();
        r.setEstudio(estudio);
        r.setParametro(p);
        r.setValorNumerico(valor);
        r.setGrupoCodigo("g");
        r.setOrdenResultado(1);
        return r;
    }

    private String diseno(String elementos) {
        return """
            {"version":1,"tamano":"CARTA","orientacion":"vertical",
             "margenes":{"superiorMm":18,"derechoMm":15,"inferiorMm":16,"izquierdoMm":15},
             "paginas":[{"id":"p1","elementos":[""" + elementos + "]}]}";
    }

    @Test
    @DisplayName("la fórmula se calcula dentro de un párrafo")
    void dentroDeUnTexto() {
        String texto = """
            {"id":"t1","tipo":"texto","xMm":20,"yMm":30,"anchoMm":150,"altoMm":20,"z":1,
             "contenido":"Su índice es {{formula.1}}.","tamanoPt":11,
             "color":"#111111","alineacion":"left"}
            """;

        assertThat(maquetador.maquetar(diseno(texto), contexto()))
                .as("la estatura tuvo que pasar de 165 cm a 1.65 m para dar esto")
                .contains("Su índice es 24.98.");
    }

    @Test
    @DisplayName("la fórmula se calcula en una celda de una tabla hecha a mano")
    void dentroDeUnaTabla() {
        String tabla = """
            {"id":"tb1","tipo":"tabla","xMm":15,"yMm":40,"anchoMm":180,"altoMm":30,"z":1,
             "columnas":[{"anchoPct":60},{"anchoPct":40}],
             "filas":[[{"texto":"Medición"},{"texto":"Resultado"}],
                      [{"texto":"Índice de masa corporal"},{"texto":"{{formula.1}}"}]],
             "conEncabezado":true}
            """;

        String html = maquetador.maquetar(diseno(tabla), contexto());

        assertThat(html).contains("<table");
        assertThat(html).contains("24.98");
    }

    @Test
    @DisplayName("una fórmula es un campo, no un bloque: va donde va un dato suelto")
    void esUnCampoYNoUnBloque() {
        // El elemento «datos» dibuja bloques —tablas de resultados, evidencias—, y una
        // fórmula no es eso: da un valor. Por eso se inserta como marcador dentro de un
        // texto o de una celda, igual que la estatura o el folio. Aquí se deja fijado
        // para que nadie la registre como bloque y se pregunte por qué sale la hoja en
        // blanco.
        String comoBloque = """
            {"id":"d1","tipo":"datos","xMm":20,"yMm":60,"anchoMm":80,"altoMm":10,"z":1,
             "clave":"formula.1","tamanoPt":11,"color":"#111111","alineacion":"left"}
            """;

        assertThat(maquetador.maquetar(diseno(comoBloque), contexto()))
                .as("puesta como bloque no dibuja nada, igual que cualquier otro campo")
                .doesNotContain("24.98");

        // Y en el catálogo se ofrece como campo, que es lo que hace que el diseñador la
        // inserte donde corresponde.
        assertThat(ClaveCampo.comoFormula("formula.1")).isEqualTo(1L);
        assertThat(ClaveCampo.comoBloqueEstudio("formula.1")).isNull();
    }

    @Test
    @DisplayName("al participante sin los datos le queda el hueco, no un cero")
    void participanteSinLosDatos() {
        Persona persona = new Persona();
        persona.setNombre("Luis");
        Paciente paciente = new Paciente();
        paciente.setPersona(persona);
        ContextoReporte sinEstudios = new ContextoReporte(paciente, List.of(), List.of(), null,
                ContextoReporte.Totales.sinCalcular());

        String texto = """
            {"id":"t1","tipo":"texto","xMm":20,"yMm":30,"anchoMm":150,"altoMm":20,"z":1,
             "contenido":"Índice: [{{formula.1}}]","tamanoPt":11,
             "color":"#111111","alineacion":"left"}
            """;

        assertThat(maquetador.maquetar(diseno(texto), sinEstudios))
                .as("un cero impreso se leería como una medición")
                .contains("Índice: []");
    }

    @Test
    @DisplayName("una fórmula borrada deja hueco en vez de tumbar la emisión")
    void formulaQueYaNoExiste() {
        when(formulaService.buscar(99L)).thenReturn(Optional.empty());

        String texto = """
            {"id":"t1","tipo":"texto","xMm":20,"yMm":30,"anchoMm":150,"altoMm":20,"z":1,
             "contenido":"Valor: [{{formula.99}}]","tamanoPt":11,
             "color":"#111111","alineacion":"left"}
            """;

        assertThat(maquetador.maquetar(diseno(texto), contexto())).contains("Valor: []");
    }

    @Test
    @DisplayName("sin catálogo de fórmulas detrás, la clave simplemente no resuelve")
    void sinCatalogoDetras() {
        // Es como queda el resolvedor cuando se construye a mano, sin base de datos.
        ResolvedorCampos suelto = new ResolvedorCampos();
        assertThat(suelto.valorDe("formula.1", contexto())).isEmpty();
    }
}
