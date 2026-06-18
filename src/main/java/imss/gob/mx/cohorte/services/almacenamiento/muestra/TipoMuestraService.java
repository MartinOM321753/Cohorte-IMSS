package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestraRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TipoMuestraService {

    private final TipoMuestraRepository tipoMuestraRepository;
    private final TuboMuestraRepository tuboMuestraRepository;
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
        return tipoMuestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tipo de muestra con id: " + id));
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
        return tuboMuestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tubo con id: " + id));
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
        return tuboMuestraRepository.save(tubo);
    }

    @Transactional
    public void deleteTubo(Long idTubo) {
        TuboMuestra tubo = getTuboById(idTubo);
        tuboMuestraRepository.delete(tubo);
    }
}
