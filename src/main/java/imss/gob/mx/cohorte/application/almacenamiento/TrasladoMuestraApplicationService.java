package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.services.almacenamiento.traslado.TrasladoMuestraService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TrasladoMuestraApplicationService {

    private final TrasladoMuestraService trasladoService;

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

    @Transactional(readOnly = true)
    public List<TrasladoMuestra> getTrasladosByAlmacen(Long idAlmacen) {
        return trasladoService.getTrasladosByAlmacen(idAlmacen);
    }

    @Transactional(readOnly = true)
    public Page<TrasladoMuestra> getTrasladosByAlmacenPaginated(Long idAlmacen, int page, int size) {
        return trasladoService.getTrasladosByAlmacenPaginated(idAlmacen, page, size);
    }

    @Transactional
    public TrasladoMuestra registrarTraslado(Long idMuestra, Long idAlmacen, String uuidAutoriza,
                                              String motivo, String observaciones) {
        return trasladoService.registrarTraslado(idMuestra, idAlmacen, uuidAutoriza, motivo, observaciones);
    }

    @Transactional
    public TrasladoMuestra confirmarRecepcion(Long idTraslado, String uuidEncargado) {
        return trasladoService.confirmarRecepcion(idTraslado, uuidEncargado);
    }

    @Transactional
    public TrasladoMuestra iniciarDevolucion(Long idTraslado, String uuidEncargado, String observaciones) {
        return trasladoService.iniciarDevolucion(idTraslado, uuidEncargado, observaciones);
    }

    @Transactional
    public TrasladoMuestra confirmarDevolucion(Long idTraslado, String observaciones) {
        return trasladoService.confirmarDevolucion(idTraslado, observaciones);
    }
}
