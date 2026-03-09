package imss.gob.mx.cohorte.services.Escalonpruebas;

import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalonRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class PruebaService {

    private PruebaEscalonRepository pruebaEscalonRepository;
    private PacienteRepository pacienteRepository;
    private UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<PruebaEscalon> getAll(){return  pruebaEscalonRepository.findAll();}

    @Transactional(readOnly = true)
    public PruebaEscalon getOne(Long id){
        return pruebaEscalonRepository.findById(id).orElseThrow(()-> new ObjNotFoundException("No se encontro el prueba escalon"));
    }
    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalon create(PruebaEscalon pruebaEscalon){
        Optional <Paciente> findPatient = pacienteRepository.findByUUID(pruebaEscalon.getPaciente().getUUID());
        Optional <BeanUser> findUser = userRepository.findByUUID(pruebaEscalon.getUsuarioRealiza().getUUID());

        if(findPatient.isEmpty()){throw new ObjNotFoundException("No se encontro el paciente");}
        if(findUser.get().getActivo()){throw new ObjNotFoundException("El Usuario no puede realizar esta accion");}

        pruebaEscalon.setFechaRegistro(LocalDateTime.now());
        pruebaEscalon.setFechaActualizacion(LocalDateTime.now());
        return pruebaEscalonRepository.save(pruebaEscalon);

    }
    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalon update(PruebaEscalon pruebaEscalon){
        Optional<PruebaEscalon> pruebaEscalonBD = pruebaEscalonRepository.findById(pruebaEscalon.getId());
        if (pruebaEscalonBD.isEmpty()){throw new ObjNotFoundException("No se encontro el prueba escalon");}
        Paciente pacienteBD = pacienteRepository.findByUUID(pruebaEscalon.getPaciente().getUUID())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        BeanUser usuarioRealizaBD = userRepository.findByUUID(pruebaEscalon.getUsuarioRealiza().getUUID())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el usuario que realiza la prueba"));

        pruebaEscalonBD.get().setFechaEstudio(pruebaEscalon.getFechaEstudio());
        pruebaEscalonBD.get().setPaciente(pacienteBD);
        pruebaEscalonBD.get().setUsuarioRealiza(usuarioRealizaBD);
        pruebaEscalonBD.get().setFechaActualizacion(LocalDateTime.now());

        return pruebaEscalonRepository.save(pruebaEscalonBD.get());
    }


    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalon delete(Long id){
        PruebaEscalon pruebaEscalonBD = pruebaEscalonRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el prueba escalon"));
        if (!pruebaEscalonBD.getEtapas().isEmpty()){throw new ObjNotFoundException("No se puede eliminar el prueba escalon porque tiene etapas asociadas");}

        pruebaEscalonRepository.delete(pruebaEscalonBD);
        return pruebaEscalonBD;
    }





}
