package imss.gob.mx.cohorte.services.Escalonpruebas;

import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapaRepository;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicionRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class MedicionService {

    private final PruebaEscalonMedicionRepository medicionRepository;
    private final PruebaEscalonEtapaRepository etapaRepository;

    @Transactional(readOnly = true)
    public PruebaEscalonMedicion getOne(Long id) {
        return medicionRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el valor solicitado"));
    }

    @Transactional(readOnly = true)
    public List<PruebaEscalonMedicion> getAll() {
        return medicionRepository.findAll();
    }

    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalonMedicion create(PruebaEscalonMedicion valor) {
        PruebaEscalonEtapa etapa = etapaRepository.findById(valor.getEtapa().getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la etapa asociada"));
        valor.setEtapa(etapa);
        return medicionRepository.save(valor);
    }

    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalonMedicion update(PruebaEscalonMedicion valor) {
        PruebaEscalonMedicion medicion = medicionRepository.findById(valor.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el valor asociado"));

        medicion.setParametro(valor.getParametro());
        medicion.setValor(valor.getValor());
        medicion.setUnidad(valor.getUnidad());

        return medicionRepository.save(medicion);
    }

    @Transactional(rollbackFor = Exception.class)
    public PruebaEscalonMedicion delete(Long id) {
        PruebaEscalonMedicion medicion = medicionRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el valor asociado"));
        medicionRepository.deleteById(id);
        return medicion;
    }
}
