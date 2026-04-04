package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;

import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class EstudioService {
    private final EstudioMedicoRepository estudioMedicoRepository;


    @Transactional(readOnly = true)
    public EstudioMedico getOne(Long id){
        return estudioMedicoRepository.findById(id).orElseThrow(()-> new RuntimeException("No se encontro el valor solicitado"));
    }

    @Transactional(readOnly = true)
    public List<EstudioMedico> getAll(){
        return estudioMedicoRepository.findAllByOrderByFechaEstudioDesc();
    }

    @Transactional(rollbackFor = Exception.class)
    public EstudioMedico create(EstudioMedico estudioMedico){

        estudioMedico.setFechaRegistro(LocalDateTime.now());
        return estudioMedicoRepository.save(estudioMedico);
    }
    @Transactional(rollbackFor = Exception.class)
    public EstudioMedico update(EstudioMedico estudioMedico){

        EstudioMedico estudioMedicoBD = estudioMedicoRepository.findById(estudioMedico.getId()).orElseThrow(() -> new ObjNotFoundException("No se encontro el estudio medico"));


        estudioMedicoBD.setPaciente(estudioMedico.getPaciente());
        estudioMedicoBD.setUsuarioRealiza(estudioMedico.getUsuarioRealiza());
        estudioMedicoBD.setTipoEstudio(estudioMedico.getTipoEstudio());

        estudioMedicoBD.setResultadoEstudio(estudioMedico.getResultadoEstudio());
        estudioMedicoBD.setFechaEstudio(estudioMedico.getFechaEstudio());
        estudioMedicoBD.setObservaciones(estudioMedico.getObservaciones());

        return estudioMedicoRepository.save(estudioMedicoBD);
    }




}
