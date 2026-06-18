package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.almacenamiento.traslado.TrasladoMuestraService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class TrasladoMuestraApplicationService {

    private final TrasladoMuestraService trasladoService;
    private final MuestraRepository muestraRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getAllTraslados() {
        return trasladoService.getAll();
    }

    @Transactional(readOnly = true)
    public TrasladoMuestra getTraslado(Long id) {
        return trasladoService.getById(id);
    }

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getHistorialByMuestra(Long idMuestra) {
        return trasladoService.getHistorialByMuestra(idMuestra);
    }

    /** Préstamos activos (no DEVUELTA) donde mi institución es origen O destino. */
    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getActivosByMiInstitucion() {
        return trasladoService.getActivosByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    /** Todos los préstamos (histórico) de mi institución. */
    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getAllByMiInstitucion() {
        return trasladoService.getAllByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public Page<TrasladoMuestra> getAllByMiInstitucionPaginado(int page, int size) {
        return trasladoService.getAllByInstitucionPaginado(
                institucionContextService.getIdInstitucionActual(), page, size);
    }

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getByGrupo(String grupoTraslado) {
        return trasladoService.getByGrupo(grupoTraslado);
    }

    /**
     * Inicia un préstamo de una o varias muestras hacia otra institución.
     * La institución origen es la del usuario logueado (tenedor actual).
     */
    @Transactional
    public List<TrasladoMuestra> iniciarPrestamo(List<Long> idsMuestras,
                                                  Long idInstitucionDestino,
                                                  String uuidAutoriza,
                                                  String motivo,
                                                  String observaciones) {
        return trasladoService.iniciarPrestamo(
                idsMuestras,
                institucionContextService.getIdInstitucionActual(),
                idInstitucionDestino,
                uuidAutoriza, motivo, observaciones);
    }

    @Transactional
    public TrasladoMuestra confirmarRecepcion(Long idTraslado, String uuidConfirma, Long idPosicionCaja) {
        return trasladoService.confirmarRecepcion(idTraslado, uuidConfirma, idPosicionCaja);
    }

    @Transactional
    public List<TrasladoMuestra> iniciarDevolucion(Long idTraslado, String uuidInicia,
                                                    String observaciones, List<Long> idsAlicuotasDevolver) {
        return trasladoService.iniciarDevolucion(idTraslado, uuidInicia, observaciones, idsAlicuotasDevolver);
    }

    @Transactional(readOnly = true)
    public List<Muestra> getAlicuotasEnDestino(Long idTraslado) {
        TrasladoMuestra traslado = trasladoService.getById(idTraslado);
        Long idMuestraPadre = traslado.getMuestra().getId();
        Long idInstDestino = traslado.getInstitucionDestino().getId();
        return muestraRepository.findAllByMuestraPadre_IdAndInstitucionActual_Id(idMuestraPadre, idInstDestino);
    }

    @Transactional
    public TrasladoMuestra confirmarDevolucion(Long idTraslado, String uuidConfirma, String observaciones) {
        return trasladoService.confirmarDevolucion(idTraslado, uuidConfirma, observaciones);
    }

    @Transactional
    public TrasladoMuestra cancelarPrestamo(Long idTraslado, String uuidUsuario, String motivo) {
        return trasladoService.cancelarPrestamo(idTraslado, uuidUsuario, motivo);
    }
}
