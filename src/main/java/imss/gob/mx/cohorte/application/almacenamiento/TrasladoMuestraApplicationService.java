package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.traslado.TrasladoMuestraService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class TrasladoMuestraApplicationService {

    private final TrasladoMuestraService trasladoService;
    private final MuestraRepository muestraRepository;
    private final MuestraService muestraService;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public TrasladoMuestra getTraslado(Long id) {
        return trasladoService.getById(id);
    }

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getHistorialByMuestra(Long idMuestra) {
        // Validar que el usuario tenga acceso a la muestra (dueña, tenedor actual,
        // o participó en la cadena de custodia). Sin este chequeo, cualquiera
        // con el módulo BIOBANCO podría enumerar el historial de cualquier muestra por ID.
        muestraService.getByIdConAccesoHistorico(idMuestra);
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
                                                  String observaciones,
                                                  LocalDateTime fechaLimite) {
        return trasladoService.iniciarPrestamo(
                idsMuestras,
                institucionContextService.getIdInstitucionActual(),
                idInstitucionDestino,
                uuidAutoriza, motivo, observaciones, fechaLimite);
    }

    @Transactional
    public TrasladoMuestra confirmarRecepcion(Long idTraslado, String uuidConfirma, Long idPosicionCaja) {
        requireInstitucion(idTraslado, true);
        return trasladoService.confirmarRecepcion(idTraslado, uuidConfirma, idPosicionCaja);
    }

    @Transactional
    public List<TrasladoMuestra> iniciarDevolucion(Long idTraslado, String uuidInicia,
                                                    String observaciones, List<Long> idsAlicuotasDevolver,
                                                    Long idInstitucionDestinoDevolucion) {
        requireInstitucion(idTraslado, true);
        return trasladoService.iniciarDevolucion(idTraslado, uuidInicia, observaciones,
                idsAlicuotasDevolver, idInstitucionDestinoDevolucion);
    }

    @Transactional(readOnly = true)
    public List<Muestra> getAlicuotasEnDestino(Long idTraslado) {
        TrasladoMuestra traslado = trasladoService.getById(idTraslado);
        Long idMuestraPadre = traslado.getMuestra().getId();
        Long idInstDestino = traslado.getInstitucionDestino().getId();
        return muestraRepository.findAllByMuestraPadre_IdAndInstitucionActual_Id(idMuestraPadre, idInstDestino);
    }

    @Transactional
    public List<TrasladoMuestra> confirmarDevolucion(Long idTraslado, String uuidConfirma, String observaciones) {
        requireInstitucionParaConfirmarDevolucion(idTraslado);
        return trasladoService.confirmarDevolucion(idTraslado, uuidConfirma, observaciones);
    }

    @Transactional
    public List<TrasladoMuestra> cancelarPrestamo(Long idTraslado, String uuidUsuario, String motivo) {
        requireInstitucion(idTraslado, false);
        return trasladoService.cancelarPrestamo(idTraslado, uuidUsuario, motivo);
    }

    /**
     * Verifica que la institución del usuario autenticado coincide con la institución
     * esperada para la operación sobre el traslado.
     *
     * @param requireDestino true → debe ser institucionDestino (confirmarRecepcion, iniciarDevolucion);
     *                       false → debe ser institucionOrigen (confirmarDevolucion, cancelarPrestamo).
     */
    private void requireInstitucion(Long idTraslado, boolean requireDestino) {
        TrasladoMuestra traslado = trasladoService.getById(idTraslado);
        Long myInstId = institucionContextService.getIdInstitucionActual();
        Long expectedId = requireDestino
                ? traslado.getInstitucionDestino().getId()
                : traslado.getInstitucionOrigen().getId();
        if (!myInstId.equals(expectedId)) {
            throw new AccessDeniedException(
                    "Su institución no tiene autorización para esta operación sobre este préstamo");
        }
    }

    /**
     * Al confirmar la devolución, la institución esperada es la que recibirá la muestra:
     * {@code idInstitucionDestinoDevolucion} si el traslado usó un atajo en la cadena,
     * o {@code institucionOrigen} si el flujo es estándar.
     */
    private void requireInstitucionParaConfirmarDevolucion(Long idTraslado) {
        TrasladoMuestra traslado = trasladoService.getById(idTraslado);
        Long myInstId = institucionContextService.getIdInstitucionActual();
        Long expectedId = traslado.getIdInstitucionDestinoDevolucion() != null
                ? traslado.getIdInstitucionDestinoDevolucion()
                : traslado.getInstitucionOrigen().getId();
        if (!myInstId.equals(expectedId)) {
            throw new AccessDeniedException(
                    "Su institución no tiene autorización para confirmar esta devolución");
        }
    }
}
