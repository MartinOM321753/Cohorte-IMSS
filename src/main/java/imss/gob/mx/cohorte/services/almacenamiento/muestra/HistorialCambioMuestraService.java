package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestraRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@AllArgsConstructor
public class HistorialCambioMuestraService {

    private final HistorialCambioMuestraRepository repository;

    @Transactional(readOnly = true)
    public List<HistorialCambioMuestra> getByMuestra(Long idMuestra) {
        return repository.findAllByMuestra_IdOrderByFechaCambioDesc(idMuestra);
    }

    /**
     * Registra un cambio individual en el historial.
     */
    @Transactional(rollbackFor = Exception.class)
    public HistorialCambioMuestra registrar(Muestra muestra, BeanUser usuario,
                                            String campo, String valorAnterior, String valorNuevo,
                                            String motivo) {
        HistorialCambioMuestra h = new HistorialCambioMuestra();
        h.setMuestra(muestra);
        h.setUsuario(usuario);
        h.setCampo(campo);
        h.setValorAnterior(valorAnterior);
        h.setValorNuevo(valorNuevo);
        h.setFechaCambio(LocalDateTime.now());
        h.setMotivo(motivo);
        return repository.save(h);
    }

    /**
     * Compara dos estados de Muestra y registra historial por cada campo que cambió.
     * Solo campos de valor/unidad/observaciones/posicionCaja/tipoMuestra/tuboMuestra.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<HistorialCambioMuestra> registrarCambios(Muestra muestraAnterior,
                                                          Muestra muestraNueva,
                                                          BeanUser usuario) {
        List<HistorialCambioMuestra> registros = new ArrayList<>();

        // valor
        if (!Objects.equals(muestraAnterior.getValor(), muestraNueva.getValor())) {
            registros.add(registrar(muestraNueva, usuario, "valor",
                    str(muestraAnterior.getValor()), str(muestraNueva.getValor()), null));
        }
        // unidad
        if (!Objects.equals(muestraAnterior.getUnidad(), muestraNueva.getUnidad())) {
            registros.add(registrar(muestraNueva, usuario, "unidad",
                    muestraAnterior.getUnidad(), muestraNueva.getUnidad(), null));
        }
        // observaciones
        if (!Objects.equals(muestraAnterior.getObservaciones(), muestraNueva.getObservaciones())) {
            registros.add(registrar(muestraNueva, usuario, "observaciones",
                    muestraAnterior.getObservaciones(), muestraNueva.getObservaciones(), null));
        }
        // posicionCaja
        String posAnterior = muestraAnterior.getPosicionCaja() != null
                ? "PosicionCaja#" + muestraAnterior.getPosicionCaja().getId() : null;
        String posNueva = muestraNueva.getPosicionCaja() != null
                ? "PosicionCaja#" + muestraNueva.getPosicionCaja().getId() : null;
        if (!Objects.equals(posAnterior, posNueva)) {
            registros.add(registrar(muestraNueva, usuario, "posicionCaja",
                    posAnterior, posNueva, null));
        }
        // tipoMuestra
        String tipoAnterior = muestraAnterior.getTipoMuestra() != null
                ? muestraAnterior.getTipoMuestra().getNombre() : null;
        String tipoNuevo = muestraNueva.getTipoMuestra() != null
                ? muestraNueva.getTipoMuestra().getNombre() : null;
        if (!Objects.equals(tipoAnterior, tipoNuevo)) {
            registros.add(registrar(muestraNueva, usuario, "tipoMuestra",
                    tipoAnterior, tipoNuevo, null));
        }
        // tuboMuestra
        String tuboAnterior = muestraAnterior.getTuboMuestra() != null
                ? muestraAnterior.getTuboMuestra().getNombre() : null;
        String tuboNuevo = muestraNueva.getTuboMuestra() != null
                ? muestraNueva.getTuboMuestra().getNombre() : null;
        if (!Objects.equals(tuboAnterior, tuboNuevo)) {
            registros.add(registrar(muestraNueva, usuario, "tuboMuestra",
                    tuboAnterior, tuboNuevo, null));
        }

        return registros;
    }

    private String str(Object val) {
        return val == null ? null : String.valueOf(val);
    }
}
