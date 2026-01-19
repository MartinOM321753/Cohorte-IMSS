package imss.gob.mx.cohorte.services.Escalonpruebas;

import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalonRepository;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapaRepository;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class EtapaService {
    private final PruebaEscalonEtapaRepository pruebaEscalonEtapaRepository;
    private final PruebaEscalonRepository pruebaEscalonRepository;


    @Transactional(readOnly = true)
    public List<PruebaEscalonEtapa> getAll(){return pruebaEscalonEtapaRepository.findAll();}

    @Transactional(readOnly = true)
    public PruebaEscalonEtapa getOne(Long id){return pruebaEscalonEtapaRepository.findById(id).orElseThrow(()-> new ObjNotFoundException("No se encontro la etapa buscada")) ;}

    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalonEtapa create(PruebaEscalonEtapa etapa){
        Optional<PruebaEscalon> findPruebaEscalon = pruebaEscalonRepository.findById(etapa.getPruebaEscalon().getId());
        if(findPruebaEscalon.isEmpty()){throw new ObjNotFoundException("No se encontro la prueba escalon");}

        etapa.setPruebaEscalon(findPruebaEscalon.get());
        etapa.setFechaRegistro(LocalDateTime.now());
        etapa.setFechaActualizacion(LocalDateTime.now());
        return pruebaEscalonEtapaRepository.save(etapa);
    }

    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalonEtapa update(PruebaEscalonEtapa etapa){

        PruebaEscalonEtapa etapaBD = pruebaEscalonEtapaRepository.findById(etapa.getId()).orElseThrow(()-> new ObjNotFoundException("No se encontro la etapa buscada"));

            etapaBD.setEtapa(etapa.getEtapa());
            etapaBD.setObservaciones(etapa.getObservaciones());
            etapaBD.setFechaActualizacion(LocalDateTime.now());

        return pruebaEscalonEtapaRepository.save(etapaBD);

    }

    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalonEtapa delete(Long id){
        PruebaEscalonEtapa etapaBD = pruebaEscalonEtapaRepository.findById(id).orElseThrow(()-> new ObjNotFoundException("No se encontro la etapa buscada"));
       if (etapaBD.getMedicion() != null) {
            throw new ObjConflictException("No se puede eliminar la etapa porque tiene mediciones asociadas.");
       }
        pruebaEscalonEtapaRepository.deleteById(etapaBD.getId());
        return etapaBD;
    }


}
