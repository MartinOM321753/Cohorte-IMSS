package imss.gob.mx.cohorte.application.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.controllers.reportes.dto.FormulaReporteMapper;
import imss.gob.mx.cohorte.controllers.reportes.dto.FormulaReporteRequestDTO;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporte;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporteHistorial;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.application.EstudiosApplicationService;
import imss.gob.mx.cohorte.application.PacienteApplicationService;
import imss.gob.mx.cohorte.services.examenes.ResultadoExamenService;
import imss.gob.mx.cohorte.services.formulas.Magnitud;
import imss.gob.mx.cohorte.services.formulas.ValidadorFormula;
import imss.gob.mx.cohorte.services.formulas.VariableFormula;
import imss.gob.mx.cohorte.services.reportes.CalculadoraFormulas;
import imss.gob.mx.cohorte.services.reportes.ContextoReporte;
import imss.gob.mx.cohorte.services.reportes.FormulaReporteService;
import imss.gob.mx.cohorte.services.reportes.ResolvedorCampos;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Casos de uso del catálogo de fórmulas.
 *
 * <p>El aislamiento por institución y la revisión de la fórmula los resuelve
 * {@link FormulaReporteService}. Aquí queda lo de la capa de entrada: pasar del DTO a
 * la entidad y devolver, junto a lo guardado, las advertencias que no impidieron
 * guardarlo — que el editor tiene que poder mostrar.</p>
 */
@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.REPORTES)
public class FormulaReporteApplicationService {

    private final FormulaReporteService service;
    private final CalculadoraFormulas calculadora;
    private final ResolvedorCampos resolvedorCampos;
    private final EstudiosApplicationService estudiosApplicationService;
    private final PacienteApplicationService pacienteApplicationService;
    private final ResultadoExamenService resultadoExamenService;
    private final ObjectMapper objectMapper;

    /** Lo guardado, con lo que conviene mirar aunque se haya podido guardar. */
    public record Guardada(FormulaReporte formula, List<ValidadorFormula.Aviso> advertencias) {}

    /**
     * Lo que la fórmula da para un participante concreto.
     *
     * @param resultado   ya con sus decimales; vacío si no se pudo calcular
     * @param conUnidad   el mismo, con la unidad detrás
     * @param calculable  false cuando falta algún dato del participante
     * @param variables   qué valió cada variable, para ver de dónde salió el número
     */
    public record Prueba(String resultado, String conUnidad, boolean calculable,
                         Map<String, String> variables) {}

    /**
     * Calcula la fórmula contra un participante real, sin guardarla.
     *
     * <p>Es la única forma que tiene alguien que no programa de comprobar que su
     * fórmula hace lo que cree. Devuelve también cuánto valió cada variable, porque
     * cuando el resultado sorprende lo que hay que mirar casi siempre es de dónde salió
     * cada dato — y muy a menudo, en qué unidad entró.</p>
     */
    @Transactional(readOnly = true)
    public Prueba probar(FormulaReporteRequestDTO dto, String uuidParticipante) {
        FormulaReporte formula = FormulaReporteMapper.toEntity(dto, objectMapper);
        ContextoReporte ctx = contextoDe(uuidParticipante);

        Map<String, String> valores = new LinkedHashMap<>();
        for (VariableFormula v : service.variablesDe(formula)) {
            Magnitud dato = resolvedorCampos.magnitudDe(v.clave(), ctx);
            Magnitud enSuUnidad = v.unidad() == null ? dato : dato.en(v.unidad());
            valores.put(v.nombre(), enSuUnidad.ausente() ? "(sin dato)" : enSuUnidad.textoConUnidad(null));
        }

        Magnitud resultado = calculadora.calcular(formula, ctx,
                clave -> resolvedorCampos.magnitudDe(clave, ctx));
        return new Prueba(
                resultado.texto(formula.getDecimales()),
                resultado.textoConUnidad(formula.getDecimales()),
                resultado.presente(),
                valores);
    }

    /**
     * Los datos del participante, por el mismo camino que usa la emisión.
     *
     * <p>Importa que sea el mismo: así la vista previa pasa por las mismas
     * comprobaciones de institución y de acceso que el reporte de verdad, y lo que se
     * ve al probar es lo que va a salir impreso.</p>
     */
    private ContextoReporte contextoDe(String uuid) {
        var paciente = pacienteApplicationService.findByUUID(uuid);
        var estudios = estudiosApplicationService.getEstudiosByPaciente(uuid);
        var examenes = resultadoExamenService.findAllByUUID(uuid);
        return new ContextoReporte(paciente, estudios, examenes, null,
                ContextoReporte.Totales.sinCalcular());
    }

    @Transactional(readOnly = true)
    public List<FormulaReporte> listar() {
        return service.getAll();
    }

    @Transactional(readOnly = true)
    public List<FormulaReporte> listarActivas() {
        return service.getActivas();
    }

    @Transactional(readOnly = true)
    public FormulaReporte obtener(Long id) {
        return service.getById(id);
    }

    @Transactional(readOnly = true)
    public List<FormulaReporteHistorial> historial(Long id) {
        return service.getHistorial(id);
    }

    @Transactional
    public Guardada crear(FormulaReporteRequestDTO dto) {
        FormulaReporte creada = service.create(FormulaReporteMapper.toEntity(dto, objectMapper));
        return new Guardada(creada, advertenciasDe(creada));
    }

    @Transactional
    public Guardada actualizar(Long id, FormulaReporteRequestDTO dto) {
        FormulaReporte actualizada = service.update(id, FormulaReporteMapper.toEntity(dto, objectMapper));
        return new Guardada(actualizada, advertenciasDe(actualizada));
    }

    @Transactional
    public boolean toggleActivo(Long id) {
        return service.toggleActivo(id);
    }

    @Transactional
    public void eliminar(Long id) {
        service.delete(id);
    }

    /**
     * Revisa sin guardar, para el editor mientras se escribe.
     *
     * <p>Es el mismo validador que corre al guardar. Que sea el mismo es lo que evita
     * que la pantalla acepte algo que el servidor después rechaza.</p>
     */
    public ValidadorFormula.Revision revisar(FormulaReporteRequestDTO dto) {
        return service.revisar(FormulaReporteMapper.toEntity(dto, objectMapper));
    }

    private List<ValidadorFormula.Aviso> advertenciasDe(FormulaReporte formula) {
        return service.revisar(formula).deNivel(ValidadorFormula.Nivel.ADVIERTE);
    }
}
