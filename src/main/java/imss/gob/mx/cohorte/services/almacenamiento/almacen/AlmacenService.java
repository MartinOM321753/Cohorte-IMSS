package imss.gob.mx.cohorte.services.almacenamiento.almacen;

import imss.gob.mx.cohorte.modules.almacenamiento.almacen.Almacen;
import imss.gob.mx.cohorte.modules.almacenamiento.almacen.AlmacenRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestraRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.*;

@Service
@RequiredArgsConstructor
public class AlmacenService {

    private final AlmacenRepository almacenRepository;
    private final TrasladoMuestraRepository trasladoRepository;
    private final UserRepository userRepository;

    private static final List<EstadoTraslado> ESTADOS_ACTIVOS =
            List.of(TRASLADADA, RECIBIDA, EN_DEVOLUCION);

    @Transactional(readOnly = true)
    public List<Almacen> getAll() {
        return almacenRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Almacen getById(Long id) {
        return almacenRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el almacén con id: " + id));
    }

    @Transactional(readOnly = true)
    public Almacen getByEncargadoUuid(String uuid) {
        return almacenRepository.findFirstByEncargado_UUIDAndActivoTrue(uuid)
                .orElseThrow(() -> new ObjNotFoundException(
                        "No se encontró un almacén asignado al usuario con UUID: " + uuid));
    }

    @Transactional(readOnly = true)
    public List<Almacen> getAllByEncargadoUuid(String uuid) {
        return almacenRepository.findAllByEncargado_UUID(uuid);
    }

    @Transactional
    public Almacen create(Almacen almacen, String uuidEncargado) {
        almacenRepository.findByNombreIgnoreCase(almacen.getNombre()).ifPresent(a -> {
            throw new ObjConflictException("Ya existe un almacén con el nombre: " + almacen.getNombre());
        });

        if (uuidEncargado != null && !uuidEncargado.isBlank()) {
            BeanUser encargado = resolverEncargado(uuidEncargado);
            almacen.setEncargado(encargado);
        }

        almacen.setActivo(true);
        almacen.setFechaRegistro(Timestamp.from(Instant.now()));
        return almacenRepository.save(almacen);
    }

    @Transactional
    public Almacen update(Long id, Almacen almacen, String uuidEncargado) {
        Almacen almacenBD = getById(id);

        if (!almacenBD.getNombre().equalsIgnoreCase(almacen.getNombre())) {
            almacenRepository.findByNombreIgnoreCase(almacen.getNombre()).ifPresent(a -> {
                throw new ObjConflictException("Ya existe un almacén con el nombre: " + almacen.getNombre());
            });
        }

        almacenBD.setNombre(almacen.getNombre());
        almacenBD.setEstado(almacen.getEstado());
        almacenBD.setCiudad(almacen.getCiudad());
        almacenBD.setDireccion(almacen.getDireccion());
        almacenBD.setResponsable(almacen.getResponsable());
        almacenBD.setTelefono(almacen.getTelefono());
        almacenBD.setActivo(almacen.getActivo() != null ? almacen.getActivo() : almacenBD.getActivo());

        if (uuidEncargado != null && !uuidEncargado.isBlank()) {
            BeanUser encargado = resolverEncargado(uuidEncargado);
            almacenBD.setEncargado(encargado);
        } else if (uuidEncargado != null && uuidEncargado.isBlank()) {
            almacenBD.setEncargado(null);
        }

        return almacenRepository.save(almacenBD);
    }

    @Transactional
    public void delete(Long id) {
        Almacen almacen = getById(id);
        if (trasladoRepository.existsByAlmacen_IdAndEstadoIn(id, ESTADOS_ACTIVOS)) {
            throw new ObjConflictException(
                    "No se puede desactivar el almacén '" + almacen.getNombre() +
                    "' porque tiene muestras que aún no han sido devueltas. " +
                    "Espera a que todas las muestras sean devueltas antes de desactivarlo.");
        }
        almacen.setActivo(false);
        almacenRepository.save(almacen);
    }

    @Transactional
    public Almacen activate(Long id) {
        Almacen almacen = getById(id);
        if (Boolean.TRUE.equals(almacen.getActivo())) {
            throw new ObjConflictException("El almacén '" + almacen.getNombre() + "' ya se encuentra activo.");
        }
        almacen.setActivo(true);
        return almacenRepository.save(almacen);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────────

    private BeanUser resolverEncargado(String uuid) {
        BeanUser usuario = userRepository.findByUUID(uuid)
                .orElseThrow(() -> new ObjNotFoundException(
                        "No se encontró el usuario encargado con UUID: " + uuid));

        if (!usuario.getActivo()) {
            throw new ValidationException("El usuario seleccionado como encargado está inactivo.");
        }

        String rol = usuario.getRol().getRole();
        if (!"ENCARGADO".equals(rol)) {
            throw new ValidationException(
                    "El usuario '" + usuario.getUsername() + "' no tiene el rol ENCARGADO (tiene: " + rol + ").");
        }

        return usuario;
    }
}
