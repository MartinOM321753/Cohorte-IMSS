package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucionRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoMuestraService {

    private final TipoMuestraRepository tipoMuestraRepository;
    private final TuboMuestraRepository tuboMuestraRepository;
    private final MuestraRepository muestraRepository;
    private final MuestraTipoInstitucionRepository muestraTipoInstitucionRepository;
    private final InstitucionContextService institucionContextService;

    private Long myInstId() {
        return institucionContextService.getIdInstitucionActual();
    }

    // ── TipoMuestra ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TipoMuestra> getAll() {
        return tipoMuestraRepository.findAllByInstitucion_IdOrderByNombreAsc(myInstId());
    }

    @Transactional(readOnly = true)
    public List<TipoMuestra> getAllActivos() {
        return tipoMuestraRepository.findAllByInstitucion_IdAndActivoTrueOrderByNombreAsc(myInstId());
    }

    @Transactional(readOnly = true)
    public TipoMuestra getById(Long id) {
        TipoMuestra tipo = tipoMuestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tipo de muestra con id: " + id));
        // El listado ya filtra por institución; esta consulta también tiene que
        // hacerlo, o el catálogo ajeno queda a la vista —y editable— con solo
        // pasar su id.
        institucionContextService.verificarPertenece(tipo.getInstitucion());
        return tipo;
    }

    @Transactional
    public TipoMuestra create(TipoMuestra tipoMuestra) {
        Long idInst = myInstId();
        tipoMuestraRepository.findByNombreIgnoreCaseAndInstitucion_Id(tipoMuestra.getNombre(), idInst).ifPresent(t -> {
            throw new ObjConflictException("Ya existe un tipo de muestra con ese nombre");
        });
        Institucion inst = institucionContextService.getInstitucionActual();
        tipoMuestra.setInstitucion(inst);
        return tipoMuestraRepository.save(tipoMuestra);
    }

    @Transactional
    public TipoMuestra update(Long id, TipoMuestra datos) {
        TipoMuestra tipo = getById(id);
        Long idInst = myInstId();
        if (!tipo.getNombre().equalsIgnoreCase(datos.getNombre())) {
            tipoMuestraRepository.findByNombreIgnoreCaseAndInstitucion_Id(datos.getNombre(), idInst).ifPresent(t -> {
                throw new ObjConflictException("Ya existe un tipo de muestra con ese nombre");
            });
            tipo.setNombre(datos.getNombre());
        }
        if (datos.getDescripcion() != null) tipo.setDescripcion(datos.getDescripcion());
        if (datos.getTemperaturaAlmacenamiento() != null) tipo.setTemperaturaAlmacenamiento(datos.getTemperaturaAlmacenamiento());
        return tipoMuestraRepository.save(tipo);
    }

    @Transactional
    public TipoMuestra toggleActivo(Long id) {
        TipoMuestra tipo = getById(id);
        tipo.setActivo(!tipo.getActivo());
        return tipoMuestraRepository.save(tipo);
    }

    // ── TuboMuestra ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TuboMuestra getTuboById(Long id) {
        TuboMuestra tubo = tuboMuestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tubo con id: " + id));
        // Un tubo no guarda institución: la hereda del tipo de muestra del que cuelga.
        institucionContextService.verificarPertenece(tubo.getTipoMuestra().getInstitucion());
        return tubo;
    }

    @Transactional
    public TuboMuestra addTubo(Long idTipoMuestra, TuboMuestra tubo) {
        TipoMuestra tipo = getById(idTipoMuestra);
        tubo.setTipoMuestra(tipo);
        if (tubo.getOrden() == null || tubo.getOrden() == 0) {
            int maxOrden = tipo.getTubos().stream()
                    .mapToInt(TuboMuestra::getOrden)
                    .max().orElse(0);
            tubo.setOrden(maxOrden + 1);
        }
        validarRecetaAlicuotado(tubo);
        return tuboMuestraRepository.save(tubo);
    }

    @Transactional
    public TuboMuestra updateTubo(Long idTubo, TuboMuestra datos) {
        TuboMuestra tubo = getTuboById(idTubo);
        if (datos.getNombre() != null) tubo.setNombre(datos.getNombre());
        if (datos.getPrefijoCodigo() != null) tubo.setPrefijoCodigo(datos.getPrefijoCodigo());
        if (datos.getNumeroAlicuotas() != null) tubo.setNumeroAlicuotas(datos.getNumeroAlicuotas());
        if (datos.getVolumenAlicuota() != null) tubo.setVolumenAlicuota(datos.getVolumenAlicuota());
        if (datos.getUnidadVolumen() != null) tubo.setUnidadVolumen(datos.getUnidadVolumen());
        if (datos.getDestinoSugerido() != null) tubo.setDestinoSugerido(datos.getDestinoSugerido());
        if (datos.getOrden() != null) tubo.setOrden(datos.getOrden());
        if (datos.getActivo() != null) tubo.setActivo(datos.getActivo());
        if (datos.getGeneracionAutomatica() != null) tubo.setGeneracionAutomatica(datos.getGeneracionAutomatica());
        if (datos.getPermiteAlicuotaParcial() != null) tubo.setPermiteAlicuotaParcial(datos.getPermiteAlicuotaParcial());
        validarRecetaAlicuotado(tubo);
        return tuboMuestraRepository.save(tubo);
    }

    /**
     * Un tubo que alicuota tiene que decir de cuánto y en qué unidad.
     *
     * <p>Antes ambos datos eran opcionales y el sistema los suplía por su
     * cuenta: el volumen se daba por bueno aunque fuera nulo y la unidad se
     * heredaba de la muestra padre. Con la unidad imponiéndose ahora desde el
     * tubo hacia la muestra, un tubo sin unidad no tendría nada que imponer, y
     * uno sin volumen no permitiría calcular cuántas alícuotas alcanzan.</p>
     */
    private void validarRecetaAlicuotado(TuboMuestra tubo) {
        int alicuotas = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        if (alicuotas <= 0) {
            return; // tubo directo: no alicuota, no necesita receta
        }
        if (tubo.getVolumenAlicuota() == null || tubo.getVolumenAlicuota() <= 0) {
            throw new ValidationException(
                    "El tubo \"" + tubo.getNombre() + "\" genera " + alicuotas
                    + " alícuota(s): indique el volumen de cada una.");
        }
        if (tubo.getUnidadVolumen() == null || tubo.getUnidadVolumen().isBlank()) {
            throw new ValidationException(
                    "El tubo \"" + tubo.getNombre() + "\" genera " + alicuotas
                    + " alícuota(s): indique la unidad del volumen. La muestra padre se "
                    + "registrará en esa misma unidad.");
        }
    }

    /**
     * Elimina un tubo de su tipo de muestra.
     *
     * El borrado se hace quitando el tubo de la colección del padre, no llamando
     * a delete() sobre el hijo: TipoMuestra.tubos es cascade = ALL con
     * orphanRemoval, y la relación inversa (TuboMuestra.tipoMuestra) es EAGER por
     * omisión. Al cargar el tubo se carga también el tipo con toda su colección,
     * que seguiría conteniendo el tubo borrado; al hacer flush, la cascada lo
     * volvía a persistir y el DELETE quedaba sin efecto.
     */
    @Transactional
    public void deleteTubo(Long idTubo) {
        TuboMuestra tubo = getTuboById(idTubo);

        // Un tubo solo puede eliminarse mientras no tenga registros asociados:
        // la eliminación existe para corregir un alta equivocada, no para retirar
        // un tubo en uso. Sin esta comprobación, el DELETE choca contra las claves
        // foráneas y aflora como error 500 sin explicación.
        long muestras = muestraRepository.countByTuboMuestra_Id(idTubo);
        if (muestras > 0) {
            throw new ObjConflictException("No se puede eliminar el tubo '" + tubo.getNombre()
                    + "': tiene " + muestras + " muestra(s) o alícuota(s) asociadas.");
        }
        long asignaciones = muestraTipoInstitucionRepository.countByTuboMuestra_Id(idTubo);
        if (asignaciones > 0) {
            throw new ObjConflictException("No se puede eliminar el tubo '" + tubo.getNombre()
                    + "': tiene " + asignaciones + " asignación(es) a instituciones.");
        }

        TipoMuestra tipo = tubo.getTipoMuestra();
        tipo.getTubos().removeIf(t -> t.getId().equals(idTubo));
        tipoMuestraRepository.save(tipo);
    }
}
