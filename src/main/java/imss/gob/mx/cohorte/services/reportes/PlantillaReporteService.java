package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.reportes.PlantillaReporte;
import imss.gob.mx.cohorte.modules.reportes.PlantillaReporteRepository;
import imss.gob.mx.cohorte.modules.reportes.TipoReporte;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Predicate;

/**
 * El catálogo de plantillas de una institución.
 *
 * <p>Toda consulta por id pasa por {@link #getById(Long)}, que comprueba la institución.
 * Es a propósito: un identificador secuencial no protege nada, y ya hubo que cerrar esa
 * puerta en varios módulos del sistema.</p>
 */
@Service
@AllArgsConstructor
public class PlantillaReporteService {

    /** Lo que aguanta la columna. Igual que el @Size del DTO. */
    private static final int MAX_NOMBRE = 120;

    private final PlantillaReporteRepository repository;
    private final InstitucionContextService institucionContextService;
    private final TipoService tipoService;

    private Long miInstitucion() {
        return institucionContextService.getIdInstitucionActual();
    }

    // ── Lectura ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PlantillaReporte> getAll() {
        return repository.findAllByInstitucion_IdOrderByNombreAsc(miInstitucion());
    }

    @Transactional(readOnly = true)
    public List<PlantillaReporte> getActivasPorTipo(TipoReporte tipo) {
        return repository.findAllByInstitucion_IdAndTipoReporteAndActivoTrueOrderByNombreAsc(
                miInstitucion(), tipo);
    }

    @Transactional(readOnly = true)
    public PlantillaReporte getById(Long id) {
        PlantillaReporte plantilla = repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la plantilla de reporte con id: " + id));
        institucionContextService.verificarPertenece(plantilla.getInstitucion());
        return plantilla;
    }

    /** La predeterminada del tipo, o null si esa institución no ha marcado ninguna. */
    @Transactional(readOnly = true)
    public PlantillaReporte getPredeterminada(TipoReporte tipo) {
        return repository.findByInstitucion_IdAndTipoReporteAndPredeterminadaTrue(miInstitucion(), tipo)
                .orElse(null);
    }

    // ── Escritura ────────────────────────────────────────────────────────────

    @Transactional
    public PlantillaReporte create(PlantillaReporte plantilla, Long idTipoEstudio) {
        Institucion institucion = institucionContextService.getInstitucionActual();
        validarNombreLibre(plantilla.getNombre(), institucion.getId(), null);
        plantilla.setTipoEstudio(resolverTipoEstudio(idTipoEstudio));

        plantilla.setInstitucion(institucion);
        if (plantilla.getActivo() == null) plantilla.setActivo(true);
        if (plantilla.getPredeterminada() == null) plantilla.setPredeterminada(false);

        PlantillaReporte guardada = repository.save(plantilla);
        if (Boolean.TRUE.equals(guardada.getPredeterminada())) {
            dejarSoloEstaComoPredeterminada(guardada);
        }
        return guardada;
    }

    @Transactional
    public PlantillaReporte update(Long id, PlantillaReporte datos, Long idTipoEstudio) {
        PlantillaReporte actual = getById(id);
        validarNombreLibre(datos.getNombre(), actual.getInstitucion().getId(), id);

        actual.setNombre(datos.getNombre());
        actual.setDescripcion(datos.getDescripcion());
        actual.setTipoReporte(datos.getTipoReporte());
        actual.setDiseno(datos.getDiseno());
        actual.setTipoEstudio(resolverTipoEstudio(idTipoEstudio));
        return repository.save(actual);
    }

    /**
     * El tipo de estudio al que se ata la plantilla, comprobando que sea de esta
     * institución. Sin esa comprobación bastaría pasar un id ajeno para enterarse
     * de los parámetros del catálogo de otra al abrir el selector.
     */
    private TipoEstudio resolverTipoEstudio(Long idTipoEstudio) {
        return idTipoEstudio == null ? null : tipoService.getOne(idTipoEstudio);
    }

    @Transactional
    public boolean toggleActivo(Long id) {
        PlantillaReporte plantilla = getById(id);
        boolean nuevoEstado = !Boolean.TRUE.equals(plantilla.getActivo());

        // Una plantilla retirada no puede seguir siendo la que se ofrece por defecto:
        // quedaría un formato fuera de uso como primera opción al emitir.
        if (!nuevoEstado) plantilla.setPredeterminada(false);

        plantilla.setActivo(nuevoEstado);
        repository.save(plantilla);
        return nuevoEstado;
    }

    @Transactional
    public void establecerPredeterminada(Long id) {
        PlantillaReporte plantilla = getById(id);
        if (!Boolean.TRUE.equals(plantilla.getActivo())) {
            throw new ObjConflictException(
                    "Una plantilla retirada de uso no puede ser la predeterminada. Actívala primero.");
        }
        dejarSoloEstaComoPredeterminada(plantilla);
    }

    /**
     * Cambia solo el nombre y la descripción.
     *
     * <p>Va aparte del actualizar general porque ese exige el diseño completo: para
     * renombrar desde el listado habría que traérselo y devolverlo, y un diseño
     * viejo en ese viaje —otra pestaña abierta, una lista sin refrescar— pisaría el
     * guardado bueno sin que nada avisara. Renombrar no toca el diseño.</p>
     */
    @Transactional
    public PlantillaReporte renombrar(Long id, String nombre, String descripcion) {
        PlantillaReporte plantilla = getById(id);
        validarNombreLibre(nombre, plantilla.getInstitucion().getId(), id);

        plantilla.setNombre(nombre);
        plantilla.setDescripcion(descripcion);
        return repository.save(plantilla);
    }

    /**
     * Una copia de la plantilla, con su diseño, para partir de algo ya hecho.
     *
     * <p>La copia nace <b>sin</b> la marca de predeterminada aunque el original la
     * tenga: duplicar un formato para probar cambios no puede robarle el sitio al que
     * la institución ya usa para emitir.</p>
     *
     * @param nombre el que quiera quien copia; si viene vacío se propone uno libre
     */
    @Transactional
    public PlantillaReporte duplicar(Long id, String nombre) {
        PlantillaReporte original = getById(id);
        Long idInstitucion = original.getInstitucion().getId();

        String nombreFinal = nombre != null && !nombre.isBlank()
                ? nombre.trim()
                : nombreLibreParaCopia(original.getNombre(), idInstitucion);
        validarNombreLibre(nombreFinal, idInstitucion, null);

        PlantillaReporte copia = new PlantillaReporte();
        copia.setNombre(nombreFinal);
        copia.setDescripcion(original.getDescripcion());
        copia.setTipoReporte(original.getTipoReporte());
        copia.setDiseno(original.getDiseno());
        copia.setTipoEstudio(original.getTipoEstudio());
        copia.setInstitucion(original.getInstitucion());
        copia.setActivo(true);
        copia.setPredeterminada(false);

        return repository.save(copia);
    }

    private String nombreLibreParaCopia(String base, Long idInstitucion) {
        return nombreLibreParaCopia(base, nombre ->
                repository.findByNombreIgnoreCaseAndInstitucion_Id(nombre, idInstitucion).isPresent());
    }

    /**
     * «X (copia)», y si ya existe, «X (copia 2)», «X (copia 3)»…
     *
     * <p>El nombre no puede pasar de 120 caracteres, así que la base se recorta antes
     * de añadirle el sufijo: recortar el resultado dejaría el «(copia)» partido a la
     * mitad y volvería a chocar con el nombre de al lado.</p>
     *
     * <p>Recibe «¿está ocupado?» en vez de consultar la base para poder probarse: la
     * regla es la numeración y el recorte, no de dónde salen los nombres ocupados.</p>
     */
    static String nombreLibreParaCopia(String base, Predicate<String> ocupado) {
        for (int i = 1; i <= 50; i++) {
            String sufijo = i == 1 ? " (copia)" : " (copia " + i + ")";
            String recortada = base.length() + sufijo.length() > MAX_NOMBRE
                    ? base.substring(0, MAX_NOMBRE - sufijo.length())
                    : base;
            String candidato = recortada + sufijo;
            if (!ocupado.test(candidato)) return candidato;
        }
        throw new ObjConflictException(
                "Ya hay demasiadas copias de \"" + base + "\". Renombra alguna antes de volver a duplicar.");
    }

    @Transactional
    public void delete(Long id) {
        PlantillaReporte plantilla = getById(id);
        repository.delete(plantilla);
    }

    // ── Reglas ───────────────────────────────────────────────────────────────

    /**
     * Deja esta como única predeterminada de su tipo.
     *
     * <p>Se quitan todas las demás en vez de solo «la anterior»: si por cualquier razón
     * hubiera quedado más de una marcada, esta operación también lo arregla en lugar de
     * arrastrar el problema.</p>
     */
    private void dejarSoloEstaComoPredeterminada(PlantillaReporte plantilla) {
        List<PlantillaReporte> marcadas = repository
                .findAllByInstitucion_IdAndTipoReporteAndPredeterminadaTrue(
                        plantilla.getInstitucion().getId(), plantilla.getTipoReporte());

        for (PlantillaReporte otra : marcadas) {
            if (!otra.getId().equals(plantilla.getId())) {
                otra.setPredeterminada(false);
                repository.save(otra);
            }
        }
        plantilla.setPredeterminada(true);
        repository.save(plantilla);
    }

    private void validarNombreLibre(String nombre, Long idInstitucion, Long idQueSeEdita) {
        repository.findByNombreIgnoreCaseAndInstitucion_Id(nombre, idInstitucion)
                .filter(existente -> !existente.getId().equals(idQueSeEdita))
                .ifPresent(existente -> {
                    throw new ObjConflictException("Ya existe una plantilla con el nombre \"" + nombre + "\".");
                });
    }
}
