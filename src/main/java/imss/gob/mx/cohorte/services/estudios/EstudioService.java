package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import imss.gob.mx.cohorte.modules.usuarios.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class EstudioService {
    private final EstudioMedicoRepository estudioMedicoRepository;
    private final TipoEstudioRepository tipoEstudioRepository;
    private final PacienteRepository pacienteRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public EstudioMedico getOne(Long id){
        return estudioMedicoRepository.findById(id).orElseThrow(()-> new RuntimeException("No se encontro el valor solicitado"));
    }

    @Transactional(readOnly = true)
    public List<EstudioMedico> getAll(Long id){
        return estudioMedicoRepository.findAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public EstudioMedico create(EstudioMedico estudioMedico){

        Paciente paciente = pacienteRepository.findByUUID(estudioMedico.getPaciente().getUUID()).orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        BeanUser usuarioRealiza = userRepository.findByUUID(estudioMedico.getUsuarioRealiza().getUUID()).orElseThrow(() -> new ObjNotFoundException("No se encontro el usuario que realiza la prueba"));
        if (usuarioRealiza.getActivo()){throw new ObjNotFoundException("El Usuario no puede realizar esta accion");}
        TipoEstudio estudio = tipoEstudioRepository.findById(estudioMedico.getTipoEstudio().getId()).orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio"));
        if (estudio.getActivo()){throw new ObjNotFoundException("El tipo de estudio no se encuentra disponible por el momento");}

        estudioMedico.setFechaRegistro(LocalDateTime.now());
        estudioMedico.setPaciente(paciente);
        estudioMedico.setUsuarioRealiza(usuarioRealiza);
        estudioMedico.setTipoEstudio(estudio);

        return estudioMedicoRepository.save(estudioMedico);
    }
    @Transactional(rollbackFor = Exception.class)
    public EstudioMedico update(EstudioMedico estudioMedico){

        EstudioMedico estudioMedicoBD = estudioMedicoRepository.findById(estudioMedico.getId()).orElseThrow(() -> new ObjNotFoundException("No se encontro el estudio medico"));
        Paciente paciente = pacienteRepository.findByUUID(estudioMedico.getPaciente().getUUID()).orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        BeanUser usuarioRealiza = userRepository.findByUUID(estudioMedico.getUsuarioRealiza().getUUID()).orElseThrow(() -> new ObjNotFoundException("No se encontro el usuario que realiza la prueba"));
        if (usuarioRealiza.getActivo()){throw new ObjNotFoundException("El Usuario no puede realizar esta accion");}

        TipoEstudio estudio = tipoEstudioRepository.findById(estudioMedico.getTipoEstudio().getId()).orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio"));
        if (estudio.getActivo()){throw new ObjNotFoundException("El tipo de estudio no se encuentra disponible por el momento");}

        estudioMedicoBD.setPaciente(paciente);
        estudioMedicoBD.setUsuarioRealiza(usuarioRealiza);
        estudioMedicoBD.setTipoEstudio(estudio);
        estudioMedicoBD.setFechaEstudio(estudioMedico.getFechaEstudio());
        estudioMedicoBD.setObservaciones(estudioMedico.getObservaciones());

        return estudioMedicoRepository.save(estudioMedicoBD);
    }




}
