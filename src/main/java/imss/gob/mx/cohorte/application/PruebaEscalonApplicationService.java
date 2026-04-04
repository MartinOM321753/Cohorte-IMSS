package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import imss.gob.mx.cohorte.services.Escalonpruebas.EtapaService;
import imss.gob.mx.cohorte.services.Escalonpruebas.MedicionService;
import imss.gob.mx.cohorte.services.Escalonpruebas.PruebaService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class PruebaEscalonApplicationService {

    private final PruebaService pruebaService;
    private final EtapaService etapaService;
    private final MedicionService medicionService;

    @Transactional(readOnly = true)
    public List<PruebaEscalon> getAll() {
        return pruebaService.getAll();
    }

    @Transactional(readOnly = true)
    public PruebaEscalon getOne(Long id) {
        return pruebaService.getOne(id);
    }

    @Transactional
    public PruebaEscalon create(PruebaEscalon prueba) {
        return pruebaService.create(prueba);
    }

    @Transactional
    public PruebaEscalon update(Long id, PruebaEscalon prueba) {
        prueba.setId(id);
        return pruebaService.update(prueba);
    }

    @Transactional
    public void delete(Long id) {
        pruebaService.delete(id);
    }

    @Transactional
    public PruebaEscalonEtapa createEtapa(PruebaEscalonEtapa etapa) {
        return etapaService.create(etapa);
    }

    @Transactional
    public PruebaEscalonEtapa updateEtapa(Long id, PruebaEscalonEtapa etapa) {
        etapa.setId(id);
        return etapaService.update(etapa);
    }

    @Transactional
    public void deleteEtapa(Long id) {
        etapaService.delete(id);
    }

    @Transactional
    public PruebaEscalonMedicion createMedicion(PruebaEscalonMedicion medicion) {
        return medicionService.create(medicion);
    }

    @Transactional
    public PruebaEscalonMedicion updateMedicion(Long id, PruebaEscalonMedicion medicion) {
        medicion.setId(id);
        return medicionService.update(medicion);
    }

    @Transactional
    public void deleteMedicion(Long id) {
        medicionService.delete(id);
    }
}
