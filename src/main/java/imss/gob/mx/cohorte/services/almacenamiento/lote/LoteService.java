package imss.gob.mx.cohorte.services.almacenamiento.lote;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Lecturas de muestras agrupadas por lote. No duplica la custodia ni la
 * contabilidad; solo recupera, con alcance de institución, las muestras de un
 * participante para armar su carta.
 */
@Service
@RequiredArgsConstructor
public class LoteService {

    private final MuestraRepository muestraRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public List<Muestra> getMuestrasDeFolio(String uuid) {
        Long idInst = institucionContextService.getIdInstitucionActual();
        return muestraRepository.findAllByPaciente_UuidAndInstitucion_Id(uuid, idInst);
    }

    /** Todas las muestras del flujo de protocolo (padres y alícuotas) de mi institución. */
    @Transactional(readOnly = true)
    public List<Muestra> getMuestrasProtocolo() {
        Long idInst = institucionContextService.getIdInstitucionActual();
        return muestraRepository.findProtocoloMuestras(idInst);
    }
}
