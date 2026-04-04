package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.almacenamiento.caja.PosicionCajaService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class MuestraApplicationService {

    private final MuestraService muestraService;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final PosicionCajaService posicionCajaService;

    @Transactional(readOnly = true)
    public List<Muestra> getAllMuestras() {
        return muestraService.getAll();
    }

    @Transactional(readOnly = true)
    public Muestra getMuestra(Long id) {
        return muestraService.getById(id);
    }

    @Transactional(readOnly = true)
    public List<Muestra> getMuestrasByPacienteUUID(String uuid) {
        return muestraService.getAll().stream()
            .filter(m -> m.getPaciente().getUuid().equals(uuid))
            .toList();
    }

    @Transactional
    public Muestra createMuestra(Muestra muestra) {
        Paciente paciente = pacienteService.getByUUID(muestra.getPaciente().getUuid());
        muestra.setPaciente(paciente);

        BeanUser usuario = userService.getByUUID(muestra.getUsuarioRecolecta().getUUID());
        muestra.setUsuarioRecolecta(usuario);

        if (muestra.getPosicionCaja() != null && muestra.getPosicionCaja().getId() != null) {
            PosicionCaja posicion = posicionCajaService.getById(muestra.getPosicionCaja().getId());
            if (posicion.getOcupada()) {
                throw new ObjConflictException("La posición de caja ya está ocupada");
            }
            muestra.setPosicionCaja(posicion);
        }

        Muestra saved = muestraService.create(muestra);

        if (saved.getPosicionCaja() != null) {
            marcarPosicionCajaOcupada(saved.getPosicionCaja().getId(), true);
        }

        return saved;
    }

    @Transactional
    public Muestra updateMuestra(Long id, Muestra muestra) {
        Muestra muestraBD = muestraService.getById(id);

        Long idPosActual = muestraBD.getPosicionCaja() != null ? muestraBD.getPosicionCaja().getId() : null;
        Long idPosNueva = muestra.getPosicionCaja() != null ? muestra.getPosicionCaja().getId() : null;

        if (idPosNueva != null && !idPosNueva.equals(idPosActual)) {
            PosicionCaja nuevaPos = posicionCajaService.getById(idPosNueva);
            if (nuevaPos.getOcupada()) {
                throw new ObjConflictException("La posición de caja destino ya está ocupada");
            }
            if (idPosActual != null) {
                marcarPosicionCajaOcupada(idPosActual, false);
            }
            marcarPosicionCajaOcupada(idPosNueva, true);
        } else if (idPosNueva == null && idPosActual != null) {
            marcarPosicionCajaOcupada(idPosActual, false);
        }

        muestra.setId(id);
        return muestraService.update(muestra);
    }

    private void marcarPosicionCajaOcupada(Long idPosicion, Boolean ocupada) {
        PosicionCaja pos = posicionCajaService.getById(idPosicion);
        pos.setOcupada(ocupada);
        posicionCajaService.update(pos);
    }
}
