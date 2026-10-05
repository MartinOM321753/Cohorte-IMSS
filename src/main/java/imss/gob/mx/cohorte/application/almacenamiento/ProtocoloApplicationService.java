package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.Protocolo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.TuboProtocolo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.almacenamiento.protocolo.ProtocoloService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class ProtocoloApplicationService {

    private final ProtocoloService protocoloService;

    // ── Protocolo ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Protocolo> getAll() {
        return protocoloService.getAll();
    }

    @Transactional(readOnly = true)
    public List<Protocolo> getAllActivos() {
        return protocoloService.getAllActivos();
    }

    @Transactional(readOnly = true)
    public Protocolo getById(Long id) {
        return protocoloService.getById(id);
    }

    @Transactional
    public Protocolo create(Protocolo protocolo, Long idTipoOrigen) {
        return protocoloService.create(protocolo, idTipoOrigen);
    }

    @Transactional
    public Protocolo update(Long id, Protocolo datos, Long idTipoOrigen) {
        return protocoloService.update(id, datos, idTipoOrigen);
    }

    @Transactional
    public Protocolo toggleActivo(Long id) {
        return protocoloService.toggleActivo(id);
    }

    @Transactional
    public void deleteProtocolo(Long id) {
        protocoloService.deleteProtocolo(id);
    }

    // ── TuboProtocolo ────────────────────────────────────────────────────────────

    @Transactional
    public TuboProtocolo addTubo(Long idProtocolo, TuboProtocolo tubo, Long idTipoResultante) {
        return protocoloService.addTubo(idProtocolo, tubo, idTipoResultante);
    }

    @Transactional
    public TuboProtocolo updateTubo(Long idTubo, TuboProtocolo datos, Long idTipoResultante) {
        return protocoloService.updateTubo(idTubo, datos, idTipoResultante, true);
    }

    @Transactional
    public void deleteTubo(Long idTubo) {
        protocoloService.deleteTubo(idTubo);
    }
}
