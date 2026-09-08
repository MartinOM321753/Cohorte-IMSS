package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporte;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporteHistorial;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporteHistorialRepository;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporteRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.formulas.ValidadorFormula;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Lo que el catálogo de fórmulas tiene que garantizar.
 *
 * <p>Dos comportamientos concentran el valor de estas pruebas. Que <b>no se guarde una
 * fórmula que no se puede leer</b>, porque el momento de enterarse es al escribirla y
 * no cuando ya se está emitiendo el reporte de un participante. Y que la <b>versión
 * suba solo cuando cambia el cálculo</b>: si subiera con cada retoque de redacción, el
 * historial se llenaría de ruido y dejaría de servir para lo que existe.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FormulaReporteServiceTest {

    @Mock private FormulaReporteRepository repository;
    @Mock private FormulaReporteHistorialRepository historialRepository;
    @Mock private InstitucionContextService institucionContext;

    private FormulaReporteService service;
    private Institucion institucion;

    @BeforeEach
    void preparar() {
        institucion = new Institucion();
        institucion.setId(1L);
        institucion.setNombre("Unidad de prueba");

        service = new FormulaReporteService(
                repository, historialRepository, institucionContext, new ObjectMapper());

        when(institucionContext.getIdInstitucionActual()).thenReturn(1L);
        when(institucionContext.getInstitucionActual()).thenReturn(institucion);
        when(repository.findByNombreIgnoreCaseAndInstitucion_Id(any(), any()))
                .thenReturn(Optional.empty());
        when(repository.save(any(FormulaReporte.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    private FormulaReporte imc() {
        FormulaReporte f = new FormulaReporte();
        f.setNombre("Índice de masa corporal");
        f.setExpresion("peso / estatura²");
        f.setVariables("""
                [{"nombre":"peso","clave":"estudio.7.param.92","unidad":"kg"},
                 {"nombre":"estatura","clave":"estudio.7.param.85","unidad":"m"}]""");
        f.setUnidadSalida("kg/m²");
        f.setDecimales(2);
        f.setVersion(1);
        f.setInstitucion(institucion);
        return f;
    }

    // ── Al guardar ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("una fórmula correcta se guarda en la versión uno")
    void seGuardaLaPrimera() {
        FormulaReporte guardada = service.create(imc());

        assertThat(guardada.getVersion()).isEqualTo(1);
        assertThat(guardada.getInstitucion()).isEqualTo(institucion);
    }

    @Test
    @DisplayName("una fórmula que no se puede leer no llega a la base")
    void noSeGuardaLoIlegible() {
        FormulaReporte rota = imc();
        rota.setExpresion("peso / (estatura");

        assertThatThrownBy(() -> service.create(rota))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Falta cerrar un paréntesis");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("una variable sin declarar tampoco se guarda")
    void noSeGuardaConVariablesSueltas() {
        FormulaReporte f = imc();
        f.setExpresion("peso / talla²");

        assertThatThrownBy(() -> service.create(f))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("«talla»");
    }

    @Test
    @DisplayName("mezclar unidades sí se guarda: solo es una advertencia")
    void lasAdvertenciasNoDetienen() {
        FormulaReporte f = imc();
        f.setExpresion("peso + estatura");
        f.setUnidadSalida(null);

        FormulaReporte guardada = service.create(f);

        assertThat(guardada).isNotNull();
        assertThat(service.revisar(guardada).deNivel(ValidadorFormula.Nivel.ADVIERTE))
                .as("se guarda, pero el editor tiene que poder mostrar el aviso")
                .isNotEmpty();
    }

    // ── Versionado ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("cambiar el cálculo archiva lo anterior y sube la versión")
    void elCalculoQueCambiaSubeLaVersion() {
        FormulaReporte existente = imc();
        existente.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existente));

        FormulaReporte cambios = imc();
        cambios.setExpresion("peso / (estatura * estatura)");

        FormulaReporte actualizada = service.update(10L, cambios);

        assertThat(actualizada.getVersion()).isEqualTo(2);

        ArgumentCaptor<FormulaReporteHistorial> archivada =
                ArgumentCaptor.forClass(FormulaReporteHistorial.class);
        verify(historialRepository).save(archivada.capture());

        // Lo archivado es lo que decía ANTES, que es lo que necesita quien pregunte
        // dentro de cinco años cómo salió un reporte de hoy.
        assertThat(archivada.getValue().getVersion()).isEqualTo(1);
        assertThat(archivada.getValue().getExpresion()).isEqualTo("peso / estatura²");
        assertThat(archivada.getValue().getIdFormula()).isEqualTo(10L);
    }

    @Test
    @DisplayName("corregir la redacción no sube la versión ni archiva nada")
    void laRedaccionNoEsUnCambioDeCalculo() {
        FormulaReporte existente = imc();
        existente.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existente));

        FormulaReporte cambios = imc();
        cambios.setDescripcion("Peso entre la estatura al cuadrado");

        FormulaReporte actualizada = service.update(10L, cambios);

        assertThat(actualizada.getVersion())
                .as("una versión por cada retoque de redacción haría inútil el historial")
                .isEqualTo(1);
        verify(historialRepository, never()).save(any());
    }

    @Test
    @DisplayName("cambiar los decimales sí cambia el resultado impreso")
    void losDecimalesCuentanComoCalculo() {
        FormulaReporte existente = imc();
        existente.setId(10L);
        when(repository.findById(10L)).thenReturn(Optional.of(existente));

        FormulaReporte cambios = imc();
        cambios.setDecimales(1);

        assertThat(service.update(10L, cambios).getVersion()).isEqualTo(2);
    }

    // ── Variables ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("las variables se leen del JSON con su clave y su unidad")
    void variablesDeclaradas() {
        assertThat(service.variablesDe(imc()))
                .extracting(v -> v.nombre() + "→" + v.clave() + " en " + v.unidad().nombre())
                .containsExactly(
                        "peso→estudio.7.param.92 en kg",
                        "estatura→estudio.7.param.85 en m");
    }

    @Test
    @DisplayName("una lista de variables corrupta se avisa, no se ignora")
    void variablesCorruptas() {
        FormulaReporte f = imc();
        f.setVariables("{esto no es json}");

        assertThatThrownBy(() -> service.variablesDe(f))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("variables");
    }

    @Test
    @DisplayName("una fórmula sin variables es válida: puede ser solo números")
    void sinVariables() {
        FormulaReporte f = imc();
        f.setExpresion("18.5 * 2");
        f.setVariables("[]");
        f.setUnidadSalida(null);

        assertThat(service.variablesDe(f)).isEmpty();
        assertThat(service.create(f)).isNotNull();
    }
}
