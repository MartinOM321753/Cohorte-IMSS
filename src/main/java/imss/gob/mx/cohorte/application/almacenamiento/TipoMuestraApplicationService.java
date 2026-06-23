package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.TipoMuestraService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class TipoMuestraApplicationService {

    private final TipoMuestraService tipoMuestraService;

    // ── TipoMuestra ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TipoMuestra> getAll() {
        return tipoMuestraService.getAll();
    }

    @Transactional(readOnly = true)
    public List<TipoMuestra> getAllActivos() {
        return tipoMuestraService.getAllActivos();
    }

    @Transactional(readOnly = true)
    public TipoMuestra getById(Long id) {
        return tipoMuestraService.getById(id);
    }

    @Transactional
    public TipoMuestra create(TipoMuestra tipoMuestra) {
        return tipoMuestraService.create(tipoMuestra);
    }

    @Transactional
    public TipoMuestra update(Long id, TipoMuestra datos) {
        return tipoMuestraService.update(id, datos);
    }

    @Transactional
    public TipoMuestra toggleActivo(Long id) {
        return tipoMuestraService.toggleActivo(id);
    }

    // ── TuboMuestra ──────────────────────────────────────────────────────────

    @Transactional
    public TuboMuestra addTubo(Long idTipoMuestra, TuboMuestra tubo) {
        return tipoMuestraService.addTubo(idTipoMuestra, tubo);
    }

    @Transactional
    public TuboMuestra updateTubo(Long idTubo, TuboMuestra datos) {
        return tipoMuestraService.updateTubo(idTubo, datos);
    }

    @Transactional
    public void deleteTubo(Long idTubo) {
        tipoMuestraService.deleteTubo(idTubo);
    }
}
