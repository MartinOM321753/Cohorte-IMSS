package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucion;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucionRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MuestraTipoInstitucionService {

    private final MuestraTipoInstitucionRepository muestraTipoInstitucionRepository;
    private final MuestraRepository muestraRepository;
    private final TipoMuestraService tipoMuestraService;
    private final InstitucionContextService institucionContextService;

    @Transactional
    public MuestraTipoInstitucion asignarTipoTubo(Long idMuestra, Long idTipoMuestra, Long idTuboMuestra) {
        Muestra muestra = muestraRepository.findById(idMuestra)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la muestra"));

        Long idInst = institucionContextService.getIdInstitucionActual();
        if (!muestra.getInstitucionActual().getId().equals(idInst)) {
            throw new ObjConflictException("La muestra no se encuentra actualmente en su institución.");
        }
        if (muestra.getMuestraPadre() != null) {
            throw new ValidationException("Solo se puede asignar tipo/tubo a muestras padre, no a alícuotas.");
        }

        TipoMuestra tipo = tipoMuestraService.getById(idTipoMuestra);
        TuboMuestra tubo = tipoMuestraService.getTuboById(idTuboMuestra);

        if (!tubo.getTipoMuestra().getId().equals(tipo.getId())) {
            throw new ValidationException("El tubo seleccionado no pertenece al tipo de muestra indicado.");
        }

        Institucion institucion = institucionContextService.getInstitucionActual();

        Optional<MuestraTipoInstitucion> existente =
                muestraTipoInstitucionRepository.findByMuestra_IdAndInstitucion_Id(idMuestra, idInst);

        MuestraTipoInstitucion mapping;
        if (existente.isPresent()) {
            mapping = existente.get();
            mapping.setTipoMuestra(tipo);
            mapping.setTuboMuestra(tubo);
        } else {
            mapping = new MuestraTipoInstitucion();
            mapping.setMuestra(muestra);
            mapping.setInstitucion(institucion);
            mapping.setTipoMuestra(tipo);
            mapping.setTuboMuestra(tubo);
            mapping.setFechaRegistro(Timestamp.valueOf(LocalDateTime.now()));
        }

        return muestraTipoInstitucionRepository.save(mapping);
    }

    @Transactional(readOnly = true)
    public Optional<MuestraTipoInstitucion> getByMuestraYMiInstitucion(Long idMuestra) {
        Long idInst = institucionContextService.getIdInstitucionActual();
        return muestraTipoInstitucionRepository.findByMuestra_IdAndInstitucion_Id(idMuestra, idInst);
    }

    @Transactional(readOnly = true)
    public List<MuestraTipoInstitucion> getAllByMuestra(Long idMuestra) {
        return muestraTipoInstitucionRepository.findAllByMuestra_Id(idMuestra);
    }
}
