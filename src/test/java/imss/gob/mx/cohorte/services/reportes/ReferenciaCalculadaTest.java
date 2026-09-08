package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporte;
import imss.gob.mx.cohorte.services.formulas.Magnitud;
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
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * La referencia que depende del participante, y si el valor cae dentro.
 *
 * <p>El reporte de salud imprime el peso deseable como «18.5 × talla² — 24.9 × talla²»:
 * no es un rango fijo, son dos cuentas que dan un número distinto para cada persona.
 * Aquí se comprueba que esos dos números salen bien y que la clasificación usa el valor
 * <b>sin redondear</b>, que es donde se juega que un participante quede dentro o
 * fuera.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReferenciaCalculadaTest {

    private static final long TANITA = 7L;
    private static final long PESO = 92L;
    private static final long ESTATURA = 85L;

    @Mock private FormulaReporteService formulaService;

    private CalculadoraFormulas calculadora;
    private ResolvedorCampos resolvedor;

    @BeforeEach
    void preparar() {
        calculadora = new CalculadoraFormulas(formulaService);
        resolvedor = new ResolvedorCampos();
        resolvedor.conFormulas(formulaService, calculadora);

        when(formulaService.variablesDe(any())).thenReturn(List.of(
                new VariableFormula("peso", ClaveCampo.deParametro(TANITA, PESO), Unidad.de("kg")),
                new VariableFormula("talla", ClaveCampo.deParametro(TANITA, ESTATURA), Unidad.de("m"))));
    }

    /** «Peso deseable»: el valor es el peso medido y el rango sale de la estatura. */
    private FormulaReporte pesoDeseable() {
        FormulaReporte f = new FormulaReporte();
        f.setId(1L);
        f.setNombre("Peso");
        f.setExpresion("peso");
        f.setExpresionMinimo("18.5 * talla²");
        f.setExpresionMaximo("24.9 * talla²");
        f.setUnidadSalida("kg");
        f.setDecimales(2);
        return f;
    }

    /** Un participante con el peso que se le pase, y 1.65 m de estatura. */
    private ContextoReporte contextoConPeso(double kg) {
        Persona persona = new Persona();
        persona.setNombre("Ana");
        persona.setSexo(Persona.Sexo.F);
        Paciente paciente = new Paciente();
        paciente.setPersona(persona);

        TipoEstudio tipo = new TipoEstudio();
        tipo.setId(TANITA);
        tipo.setNombre("TANITA");

        EstudioMedico estudio = new EstudioMedico();
        estudio.setId(1L);
        estudio.setTipoEstudio(tipo);
        estudio.setPaciente(paciente);
        estudio.setFechaEstudio(LocalDateTime.now());
        estudio.setResultadoEstudio(new ArrayList<>());
        estudio.getResultadoEstudio().add(resultado(estudio, PESO, "Peso", "kg", kg));
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

    private Function<String, Magnitud> datosDe(ContextoReporte ctx) {
        return clave -> resolvedor.magnitudDe(clave, ctx);
    }

    // ── Los límites ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("los dos límites del peso deseable salen de la estatura")
    void losLimitesSeCalculan() {
        ContextoReporte ctx = contextoConPeso(60);
        FormulaReporte f = pesoDeseable();

        // Con 1.65 m: 18.5 × 2.7225 y 24.9 × 2.7225.
        assertThat(calculadora.minimo(f, ctx, datosDe(ctx)).texto(2)).isEqualTo("50.37");
        assertThat(calculadora.maximo(f, ctx, datosDe(ctx)).texto(2)).isEqualTo("67.79");
    }

    @Test
    @DisplayName("la referencia se escribe como se lee en la columna")
    void laReferenciaEscrita() {
        ContextoReporte ctx = contextoConPeso(60);
        assertThat(calculadora.textoReferencia(pesoDeseable(), ctx, datosDe(ctx)))
                .isEqualTo("50.37 – 67.79");
    }

    @Test
    @DisplayName("con un solo límite se escribe con su símbolo")
    void unSoloLimite() {
        ContextoReporte ctx = contextoConPeso(60);

        FormulaReporte soloTecho = pesoDeseable();
        soloTecho.setExpresionMinimo(null);
        assertThat(calculadora.textoReferencia(soloTecho, ctx, datosDe(ctx))).isEqualTo("≤ 67.79");

        FormulaReporte soloPiso = pesoDeseable();
        soloPiso.setExpresionMaximo(null);
        assertThat(calculadora.textoReferencia(soloPiso, ctx, datosDe(ctx))).isEqualTo("≥ 50.37");
    }

    // ── La clasificación ─────────────────────────────────────────────────────

    @Test
    @DisplayName("dice si el valor cae dentro, por arriba o por abajo")
    void laClasificacion() {
        FormulaReporte f = pesoDeseable();

        ContextoReporte dentro = contextoConPeso(60);
        assertThat(calculadora.estado(f, dentro, datosDe(dentro))).isEqualTo("Dentro del rango");

        ContextoReporte arriba = contextoConPeso(80);
        assertThat(calculadora.estado(f, arriba, datosDe(arriba))).isEqualTo("Por arriba");

        ContextoReporte abajo = contextoConPeso(45);
        assertThat(calculadora.estado(f, abajo, datosDe(abajo))).isEqualTo("Por abajo");
    }

    @Test
    @DisplayName("el extremo del rango cuenta como dentro")
    void losExtremosEntran() {
        // El documento escribe «70–99» sin más, y quien lo lee entiende que el 99 está
        // dentro. Cuando el límite excluye, el documento lo dice con «<».
        ContextoReporte justoEnElTecho = contextoConPeso(67.79025);
        assertThat(calculadora.estado(pesoDeseable(), justoEnElTecho, datosDe(justoEnElTecho)))
                .isEqualTo("Dentro del rango");
    }

    @Test
    @DisplayName("la clasificación compara sin redondear, no lo que se imprime")
    void seComparaElValorCompleto() {
        // 67.7903 se imprime como «67.79», igual que el techo, pero está por encima. Si
        // se comparara el texto redondeado saldría «dentro» y sería falso.
        ContextoReporte apenasArriba = contextoConPeso(67.7903);

        assertThat(calculadora.calcular(pesoDeseable(), apenasArriba, datosDe(apenasArriba)).texto(2))
                .isEqualTo("67.79");
        assertThat(calculadora.estado(pesoDeseable(), apenasArriba, datosDe(apenasArriba)))
                .as("redondear antes de comparar es lo que convierte un fuera en un dentro")
                .isEqualTo("Por arriba");
    }

    @Test
    @DisplayName("sin dato no se marca nada: «no se sabe» no es «está mal»")
    void sinDatoNoSeClasifica() {
        Persona persona = new Persona();
        persona.setNombre("Luis");
        Paciente paciente = new Paciente();
        paciente.setPersona(persona);
        ContextoReporte vacio = new ContextoReporte(paciente, List.of(), List.of(), null,
                ContextoReporte.Totales.sinCalcular());

        assertThat(calculadora.estado(pesoDeseable(), vacio, datosDe(vacio))).isEmpty();
        assertThat(calculadora.textoReferencia(pesoDeseable(), vacio, datosDe(vacio))).isEmpty();
    }

    @Test
    @DisplayName("una fórmula sin límites no clasifica nada")
    void sinReferenciaNoHayEstado() {
        FormulaReporte sinLimites = pesoDeseable();
        sinLimites.setExpresionMinimo(null);
        sinLimites.setExpresionMaximo(null);

        ContextoReporte ctx = contextoConPeso(60);
        assertThat(calculadora.estado(sinLimites, ctx, datosDe(ctx))).isEmpty();
    }

    // ── Desde la plantilla ───────────────────────────────────────────────────

    @Test
    @DisplayName("las claves de referencia resuelven desde el diseño")
    void lasClavesDesdeLaPlantilla() {
        when(formulaService.buscar(1L)).thenReturn(Optional.of(pesoDeseable()));
        ContextoReporte ctx = contextoConPeso(60);

        // Los dos decimales que declara la fórmula valen igual para el valor y para su
        // referencia: en la hoja se leen en la misma fila.
        assertThat(resolvedor.valorDe("formula.1", ctx)).isEqualTo("60.00");
        assertThat(resolvedor.valorDe("formula.1.referencia", ctx)).isEqualTo("50.37 – 67.79");
        assertThat(resolvedor.valorDe("formula.1.estado", ctx)).isEqualTo("Dentro del rango");
        assertThat(resolvedor.valorDe("formula.1.minimo", ctx)).isEqualTo("50.37");
        assertThat(resolvedor.valorDe("formula.1.maximo", ctx)).isEqualTo("67.79");
    }
}
