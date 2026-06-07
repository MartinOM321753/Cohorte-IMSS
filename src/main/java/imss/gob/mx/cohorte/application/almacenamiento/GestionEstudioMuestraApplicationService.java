package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.OpcionParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.TipoEstudioMuestra;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.OpcionParametroEstudioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.ParametroEstudioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.TipoEstudioMuestraService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class GestionEstudioMuestraApplicationService {

    private final TipoEstudioMuestraService tipoService;
    private final ParametroEstudioMuestraService parametroService;
    private final OpcionParametroEstudioMuestraService opcionService;

    // ─── Tipos ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TipoEstudioMuestra> getAllActivos() {
        return tipoService.getAllActivos();
    }

    @Transactional(readOnly = true)
    public List<TipoEstudioMuestra> getAll() {
        return tipoService.getAll();
    }

    @Transactional(readOnly = true)
    public TipoEstudioMuestra getById(Long id) {
        return tipoService.getById(id);
    }

    @Transactional
    public TipoEstudioMuestra createTipo(TipoEstudioMuestra tipo) {
        return tipoService.create(tipo);
    }

    @Transactional
    public TipoEstudioMuestra updateTipo(Long id, TipoEstudioMuestra datos) {
        return tipoService.update(id, datos);
    }

    @Transactional
    public boolean toggleTipo(Long id) {
        return tipoService.toggleActivo(id);
    }

    // ─── Parámetros ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ParametroEstudioMuestra> getParametrosByTipo(Long idTipo) {
        tipoService.getById(idTipo); // valida que exista
        return parametroService.getByTipo(idTipo);
    }

    @Transactional
    public ParametroEstudioMuestra createParametro(ParametroEstudioMuestra parametro, List<String> opciones) {
        TipoEstudioMuestra tipo = tipoService.getByIdActivo(parametro.getTipoEstudioMuestra().getId());
        parametro.setTipoEstudioMuestra(tipo);
        ParametroEstudioMuestra creado = parametroService.create(parametro);
        if (creado.getTipo() == TipoParametro.TEXTO_OPCIONES && opciones != null && !opciones.isEmpty()) {
            opcionService.replaceAll(creado, opciones);
            creado = parametroService.getById(creado.getId());
        }
        return creado;
    }

    @Transactional
    public ParametroEstudioMuestra updateParametro(Long id, ParametroEstudioMuestra datos, List<String> opciones) {
        ParametroEstudioMuestra paramBD = parametroService.getById(id);
        // No cambiamos el tipo de estudio al actualizar
        datos.setTipoEstudioMuestra(paramBD.getTipoEstudioMuestra());
        ParametroEstudioMuestra actualizado = parametroService.update(id, datos);
        if (actualizado.getTipo() == TipoParametro.TEXTO_OPCIONES) {
            opcionService.replaceAll(actualizado, opciones != null ? opciones : List.of());
            actualizado = parametroService.getById(actualizado.getId());
        }
        return actualizado;
    }

    @Transactional
    public void deleteParametro(Long id) {
        parametroService.delete(id);
    }

    // ─── Opciones ────────────────────────────────────────────────────────────

    @Transactional
    public OpcionParametroEstudioMuestra addOpcion(Long parametroId, String valor) {
        ParametroEstudioMuestra parametro = parametroService.getById(parametroId);
        if (parametro.getTipo() != TipoParametro.TEXTO_OPCIONES) {
            throw new IllegalArgumentException("Solo se pueden agregar opciones a parámetros TEXTO_OPCIONES");
        }
        return opcionService.addOpcion(parametro, valor);
    }

    @Transactional
    public void deleteOpcion(Long opcionId) {
        opcionService.deleteOpcion(opcionId);
    }
}
