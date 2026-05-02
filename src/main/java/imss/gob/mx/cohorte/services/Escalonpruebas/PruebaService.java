package imss.gob.mx.cohorte.services.Escalonpruebas;

import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalonRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PruebaService {

    private final PruebaEscalonRepository pruebaEscalonRepository;
    private final PacienteRepository pacienteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PruebaEscalon> getAll() {
        return pruebaEscalonRepository.findAll();
    }

    @Transactional(readOnly = true)
    public PruebaEscalon getOne(Long id) {
        return pruebaEscalonRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la prueba escalon"));
    }

    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalon create(PruebaEscalon pruebaEscalon) {
        Optional<Paciente> findPatient = pacienteRepository.findByUuid(pruebaEscalon.getPaciente().getUuid());
        Optional<BeanUser> findUser = userRepository.findByUUID(pruebaEscalon.getUsuarioRealiza().getUUID());

        if (findPatient.isEmpty()) {
            throw new ObjNotFoundException("No se encontro el paciente");
        }
        if (findUser.isEmpty()) {
            throw new ObjNotFoundException("No se encontro el usuario que realiza la prueba");
        }
        if (!findUser.get().getActivo()) {
            throw new ObjConflictException("El usuario no puede realizar esta accion");
        }

        pruebaEscalon.setPaciente(findPatient.get());
        pruebaEscalon.setUsuarioRealiza(findUser.get());
        pruebaEscalon.setFechaRegistro(LocalDateTime.now());
        pruebaEscalon.setFechaActualizacion(LocalDateTime.now());
        return pruebaEscalonRepository.save(pruebaEscalon);
    }

    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalon update(PruebaEscalon pruebaEscalon) {
        PruebaEscalon pruebaEscalonBD = pruebaEscalonRepository.findById(pruebaEscalon.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la prueba escalon"));
        Paciente pacienteBD = pacienteRepository.findByUuid(pruebaEscalon.getPaciente().getUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        BeanUser usuarioRealizaBD = userRepository.findByUUID(pruebaEscalon.getUsuarioRealiza().getUUID())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el usuario que realiza la prueba"));

        pruebaEscalonBD.setFechaEstudio(pruebaEscalon.getFechaEstudio());
        pruebaEscalonBD.setPaciente(pacienteBD);
        pruebaEscalonBD.setUsuarioRealiza(usuarioRealizaBD);
        pruebaEscalonBD.setFechaActualizacion(LocalDateTime.now());

        return pruebaEscalonRepository.save(pruebaEscalonBD);
    }

    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalon delete(Long id) {
        PruebaEscalon pruebaEscalonBD = pruebaEscalonRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la prueba escalon"));
        if (!pruebaEscalonBD.getEtapas().isEmpty()) {
            throw new ObjConflictException("No se puede eliminar la prueba escalon porque tiene etapas asociadas");
        }

        pruebaEscalonRepository.delete(pruebaEscalonBD);
        return pruebaEscalonBD;
    }
}
