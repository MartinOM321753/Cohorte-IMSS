package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
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

    // ── Registro de ciclo de vida ────────────────────────────────────────────

    /** Registra un evento de ciclo de vida (sin campo específico, ej. PRESTAMO_ENVIADO). */
    @Transactional(rollbackFor = Exception.class)
    public HistorialCambioMuestra registrarEvento(Muestra muestra, BeanUser usuario,
                                                  TipoEventoMuestra tipoEvento,
                                                  String valorAnterior, String valorNuevo,
                                                  String motivo, TrasladoMuestra traslado) {
        HistorialCambioMuestra h = new HistorialCambioMuestra();
        h.setMuestra(muestra);
        h.setUsuario(usuario);
        h.setTipoEvento(tipoEvento);
        h.setCampo(null);
        h.setValorAnterior(valorAnterior);
        h.setValorNuevo(valorNuevo);
        h.setFechaCambio(LocalDateTime.now());
        h.setMotivo(motivo);
        h.setTraslado(traslado);
        return repository.save(h);
    }

    /** Registra un cambio de campo individual (ACTUALIZACION_CAMPO). */
    @Transactional(rollbackFor = Exception.class)
    public HistorialCambioMuestra registrar(Muestra muestra, BeanUser usuario,
                                            String campo, String valorAnterior, String valorNuevo,
                                            String motivo) {
        HistorialCambioMuestra h = new HistorialCambioMuestra();
        h.setMuestra(muestra);
        h.setUsuario(usuario);
        h.setTipoEvento(TipoEventoMuestra.ACTUALIZACION_CAMPO);
        h.setCampo(campo);
        h.setValorAnterior(valorAnterior);
        h.setValorNuevo(valorNuevo);
        h.setFechaCambio(LocalDateTime.now());
        h.setMotivo(motivo);
        return repository.save(h);
    }

    /**
     * Compara dos estados de Muestra y registra historial por cada campo editable que cambió.
     * Campos editables: valor, unidad, fechaRecoleccion, observaciones, posicionCaja.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<HistorialCambioMuestra> registrarCambios(Muestra muestraAnterior,
                                                          Muestra muestraNueva,
                                                          BeanUser usuario) {
        List<HistorialCambioMuestra> registros = new ArrayList<>();

        if (!Objects.equals(muestraAnterior.getValor(), muestraNueva.getValor())) {
            registros.add(registrar(muestraNueva, usuario, "valor",
                    str(muestraAnterior.getValor()), str(muestraNueva.getValor()), null));
        }
        if (!Objects.equals(muestraAnterior.getUnidad(), muestraNueva.getUnidad())) {
            registros.add(registrar(muestraNueva, usuario, "unidad",
                    muestraAnterior.getUnidad(), muestraNueva.getUnidad(), null));
        }
        if (!Objects.equals(muestraAnterior.getFechaRecoleccion(), muestraNueva.getFechaRecoleccion())) {
            registros.add(registrar(muestraNueva, usuario, "fechaRecoleccion",
                    str(muestraAnterior.getFechaRecoleccion()), str(muestraNueva.getFechaRecoleccion()), null));
        }
        if (!Objects.equals(muestraAnterior.getObservaciones(), muestraNueva.getObservaciones())) {
            registros.add(registrar(muestraNueva, usuario, "observaciones",
                    muestraAnterior.getObservaciones(), muestraNueva.getObservaciones(), null));
        }
        String posAnterior = muestraAnterior.getPosicionCaja() != null
                ? "PosicionCaja#" + muestraAnterior.getPosicionCaja().getId() : null;
        String posNueva = muestraNueva.getPosicionCaja() != null
                ? "PosicionCaja#" + muestraNueva.getPosicionCaja().getId() : null;
        if (!Objects.equals(posAnterior, posNueva)) {
            registros.add(registrar(muestraNueva, usuario, "posicionCaja",
                    posAnterior, posNueva, null));
        }

        return registros;
    }

    private String str(Object val) {
        return val == null ? null : String.valueOf(val);
    }
}
