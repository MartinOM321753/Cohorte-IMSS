package imss.gob.mx.cohorte.services.Escalonpruebas;

import imss.gob.mx.cohorte.modules.escalonPrueba.DTO.PruebaEscalonDTO;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class PruebaEscalonServices {
    private final PruebaService pruebaService;
    private final EtapaService pruebaEtapaService;
    private final MedicionService pruebaMedicionService;

    @Transactional(rollbackFor = Exception.class)

    public Boolean CrearPruebaEscalonCompleta(PruebaEscalonDTO pruebaEscalonDTO) {
        try {
            PruebaEscalon prueba = pruebaService.create(pruebaEscalonDTO.getPrueba().toEntity());
            if (prueba == null) {
                throw new Exception("Error al crear la prueba");
            }

            PruebaEscalonEtapa etapa = pruebaEtapaService.create(pruebaEscalonDTO.getEtapa().toEntity());
            if (etapa == null) {
                throw new Exception("Error al crear la etapa");
            }

            PruebaEscalonMedicion medicionCreada = pruebaMedicionService.create(pruebaEscalonDTO.getValor().toEntity());
            if (medicionCreada == null) {
                throw new Exception("Error al crear la medición");
            }

            return true;
        } catch (Exception e) {

            return false;
        }
    }



}
