package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.almacen.Almacen;
import imss.gob.mx.cohorte.services.almacenamiento.almacen.AlmacenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlmacenApplicationService {

    private final AlmacenService almacenService;

    @Transactional(readOnly = true)
    public List<Almacen> getAllAlmacenes() {
        return almacenService.getAll();
    }

    @Transactional(readOnly = true)
    public Almacen getAlmacen(Long id) {
        return almacenService.getById(id);
    }

    @Transactional(readOnly = true)
    public Almacen getAlmacenByEncargadoUuid(String uuid) {
        return almacenService.getByEncargadoUuid(uuid);
    }

    @Transactional(readOnly = true)
    public List<Almacen> findAllByEncargadoUuid(String uuid) {
        return almacenService.getAllByEncargadoUuid(uuid);
    }

    @Transactional
    public Almacen createAlmacen(Almacen almacen, String uuidEncargado) {
        return almacenService.create(almacen, uuidEncargado);
    }

    @Transactional
    public Almacen updateAlmacen(Long id, Almacen almacen, String uuidEncargado) {
        return almacenService.update(id, almacen, uuidEncargado);
    }

    @Transactional
    public void deleteAlmacen(Long id) {
        almacenService.delete(id);
    }

    @Transactional
    public Almacen activateAlmacen(Long id) {
        return almacenService.activate(id);
    }
}
