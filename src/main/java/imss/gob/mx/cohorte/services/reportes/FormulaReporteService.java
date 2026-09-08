package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporte;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporteHistorial;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporteHistorialRepository;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporteRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.formulas.Unidad;
import imss.gob.mx.cohorte.services.formulas.ValidadorFormula;
import imss.gob.mx.cohorte.services.formulas.VariableFormula;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * El catálogo de fórmulas de una institución.
 *
 * <p>Dos cosas distinguen este servicio de los demás catálogos del sistema.</p>
 *
 * <p>La primera es que <b>no se guarda una fórmula que no se pueda leer</b>. La
 * revisión corre aquí, en el servidor, con el mismo validador que usa el editor
 * mientras se escribe; que sea el mismo es lo que evita que la pantalla acepte algo que
 * el servidor rechaza. Solo detiene lo que no tiene arreglo: las advertencias —mezclar
 * unidades, por ejemplo— se devuelven pero no impiden guardar, porque el criterio de
 * qué tiene sentido es de quien arma el reporte.</p>
 *
 * <p>La segunda es la <b>versión</b>. Cada vez que cambia el cálculo se archiva lo que
 * decía antes y el número sube. Un reporte emitido guarda con qué versión salió, y así
 * la pregunta de dentro de cinco años —«¿con qué ecuación se calculó esto?»— tiene
 * respuesta.</p>
 */
@Service
@AllArgsConstructor
public class FormulaReporteService {

    /** Lo que aguanta la columna; igual que el @Size del DTO. */
    private static final int MAX_NOMBRE = 120;

    private final FormulaReporteRepository repository;
    private final FormulaReporteHistorialRepository historialRepository;
    private final InstitucionContextService institucionContextService;
    private final ObjectMapper objectMapper;

    private Long miInstitucion() {
        return institucionContextService.getIdInstitucionActual();
    }

    // ── Lectura ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<FormulaReporte> getAll() {
        return repository.findAllByInstitucion_IdOrderByNombreAsc(miInstitucion());
    }

    @Transactional(readOnly = true)
    public List<FormulaReporte> getActivas() {
        return repository.findAllByInstitucion_IdAndActivoTrueOrderByNombreAsc(miInstitucion());
    }

    @Transactional(readOnly = true)
    public FormulaReporte getById(Long id) {
        FormulaReporte formula = repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la fórmula con id: " + id));
        institucionContextService.verificarPertenece(formula.getInstitucion());
        return formula;
    }

    /**
     * La fórmula, o vacío si ya no está.
     *
     * <p>Es la versión indulgente de {@link #getById(Long)}, para cuando se está
     * emitiendo un reporte: una plantilla puede seguir mencionando una fórmula que
     * alguien borró, y eso tiene que dejar la celda en blanco, no tumbar el documento
     * entero. La comprobación de institución se mantiene igual.</p>
     *
     * <p><b>Sin {@code @Transactional} a propósito.</b> Se llama desde el resolvedor de
     * campos, en mitad de la emisión, que ya corre dentro de una transacción. Abrir aquí
     * otra que se une a esa tenía una consecuencia que costó encontrar: si algo fallaba
     * dentro, Spring marcaba la transacción <i>compartida</i> como «solo reversión», la
     * calculadora se comía la excepción para dejar la celda en blanco, y el reporte
     * reventaba al final con un <i>UnexpectedRollbackException</i> que no decía nada de
     * la fórmula. Sin la anotación, esta consulta usa la transacción de quien la llama y
     * un fallo aquí ya no envenena todo el documento.</p>
     */
    public Optional<FormulaReporte> buscar(Long id) {
        if (id == null) return Optional.empty();
        return repository.findById(id)
                .filter(f -> f.getInstitucion() != null
                        && f.getInstitucion().getId().equals(miInstitucion()));
    }

    @Transactional(readOnly = true)
    public List<FormulaReporteHistorial> getHistorial(Long id) {
        getById(id);   // comprueba que la fórmula sea de esta institución
        return historialRepository.findAllByIdFormulaOrderByVersionDesc(id);
    }

    // ── Escritura ────────────────────────────────────────────────────────────

    @Transactional
    public FormulaReporte create(FormulaReporte formula) {
        Institucion institucion = institucionContextService.getInstitucionActual();
        normalizar(formula);
        validarNombreLibre(formula.getNombre(), institucion.getId(), null);
        exigirQueSePuedaGuardar(formula);

        formula.setInstitucion(institucion);
        formula.setVersion(1);
        return repository.save(formula);
    }

    /**
     * Actualiza una fórmula, archivando la versión anterior si el cálculo cambió.
     *
     * <p>Corregir la descripción o el nombre no sube la versión: no cambia ningún
     * resultado, y una versión nueva por cada retoque de redacción llenaría el historial
     * de ruido y haría inútil justamente lo que se quiere poder consultar.</p>
     */
    @Transactional
    public FormulaReporte update(Long id, FormulaReporte cambios) {
        FormulaReporte actual = getById(id);
        normalizar(cambios);
        validarNombreLibre(cambios.getNombre(), actual.getInstitucion().getId(), id);
        exigirQueSePuedaGuardar(cambios);

        if (elCalculoCambia(actual, cambios)) {
            historialRepository.save(FormulaReporteHistorial.de(actual));
            actual.setVersion(actual.getVersion() + 1);
        }

        actual.setNombre(cambios.getNombre());
        actual.setDescripcion(cambios.getDescripcion());
        actual.setExpresion(cambios.getExpresion());
        actual.setVariables(cambios.getVariables());
        actual.setUnidadSalida(cambios.getUnidadSalida());
        actual.setDecimales(cambios.getDecimales());
        return repository.save(actual);
    }

    @Transactional
    public boolean toggleActivo(Long id) {
        FormulaReporte formula = getById(id);
        formula.setActivo(!Boolean.TRUE.equals(formula.getActivo()));
        repository.save(formula);
        return formula.getActivo();
    }

    /**
     * Borra la fórmula del catálogo.
     *
     * <p>El historial no se toca: los reportes emitidos con ella siguen necesitando que
     * se pueda saber qué calculaba. Para dejar de ofrecerla sin perderla de vista está
     * {@link #toggleActivo(Long)}, que es lo que conviene casi siempre.</p>
     */
    @Transactional
    public void delete(Long id) {
        repository.delete(getById(id));
    }

    // ── Revisión ─────────────────────────────────────────────────────────────

    /**
     * Revisa sin guardar. Es lo que consulta el editor mientras se escribe.
     */
    public ValidadorFormula.Revision revisar(FormulaReporte formula) {
        return ValidadorFormula.revisar(
                formula.getExpresion(),
                variablesDe(formula),
                formula.getUnidadSalida() == null ? null : Unidad.de(formula.getUnidadSalida()));
    }

    /** Las variables declaradas, ya leídas del JSON. */
    public List<VariableFormula> variablesDe(FormulaReporte formula) {
        String json = formula.getVariables();
        if (json == null || json.isBlank()) return List.of();

        List<Map<String, String>> crudas;
        try {
            crudas = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception malFormado) {
            throw new ValidationException("La lista de variables de la fórmula no es válida.");
        }

        return crudas.stream()
                .map(m -> new VariableFormula(
                        m.get("nombre"),
                        m.get("clave"),
                        m.get("unidad") == null ? null : Unidad.de(m.get("unidad"))))
                .toList();
    }

    private void exigirQueSePuedaGuardar(FormulaReporte formula) {
        exigir(revisar(formula), "");
        // Los límites de la referencia son expresiones con derecho propio: si uno no se
        // puede leer, la columna de referencia saldría en blanco sin que nadie supiera
        // por qué. Se revisan con el mismo validador y las mismas variables.
        exigir(revisarLimite(formula, formula.getExpresionMinimo()), "En el límite mínimo: ");
        exigir(revisarLimite(formula, formula.getExpresionMaximo()), "En el límite máximo: ");
    }

    private void exigir(ValidadorFormula.Revision revision, String prefijo) {
        if (revision == null || revision.sePuedeGuardar()) return;

        String problemas = revision.deNivel(ValidadorFormula.Nivel.IMPIDE).stream()
                .map(ValidadorFormula.Aviso::mensaje)
                .reduce((a, b) -> a + " " + b)
                .orElse("La fórmula no es válida.");
        throw new ValidationException(prefijo + problemas);
    }

    /** Revisa un límite, o devuelve null si esa fórmula no define ese lado. */
    private ValidadorFormula.Revision revisarLimite(FormulaReporte formula, String expresion) {
        if (expresion == null || expresion.isBlank()) return null;
        return ValidadorFormula.revisar(expresion, variablesDe(formula), null);
    }

    /** Solo lo que altera el resultado; el nombre y la descripción no cuentan. */
    private boolean elCalculoCambia(FormulaReporte actual, FormulaReporte cambios) {
        return !Objects.equals(actual.getExpresion(), cambios.getExpresion())
                || !Objects.equals(actual.getVariables(), cambios.getVariables())
                || !Objects.equals(actual.getUnidadSalida(), cambios.getUnidadSalida())
                || !Objects.equals(actual.getDecimales(), cambios.getDecimales())
                // Cambiar un límite cambia qué se imprime en la columna de referencia y
                // puede mover a un participante de «dentro» a «por arriba».
                || !Objects.equals(actual.getExpresionMinimo(), cambios.getExpresionMinimo())
                || !Objects.equals(actual.getExpresionMaximo(), cambios.getExpresionMaximo());
    }

    private void normalizar(FormulaReporte formula) {
        if (formula.getNombre() != null) formula.setNombre(formula.getNombre().trim());
        if (formula.getExpresion() != null) formula.setExpresion(formula.getExpresion().trim());
        if (formula.getUnidadSalida() != null && formula.getUnidadSalida().isBlank()) {
            formula.setUnidadSalida(null);
        }
    }

    private void validarNombreLibre(String nombre, Long idInstitucion, Long idQueSeEdita) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidationException("La fórmula necesita un nombre.");
        }
        if (nombre.length() > MAX_NOMBRE) {
            throw new ValidationException(
                    "El nombre no puede pasar de " + MAX_NOMBRE + " caracteres.");
        }
        repository.findByNombreIgnoreCaseAndInstitucion_Id(nombre, idInstitucion)
                .filter(existente -> !existente.getId().equals(idQueSeEdita))
                .ifPresent(existente -> {
                    throw new ObjConflictException("Ya hay una fórmula que se llama «" + nombre + "».");
                });
    }
}
