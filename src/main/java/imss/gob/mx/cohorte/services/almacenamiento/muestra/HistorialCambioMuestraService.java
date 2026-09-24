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

    // ── Límites de las columnas ──────────────────────────────────────────────
    //
    // Tienen que coincidir con los @Column de HistorialCambioMuestra. Ver
    // recortar() para por qué se recorta en lugar de dejar que falle.

    static final int MAX_CAMPO = 50;
    static final int MAX_VALOR = 500;
    static final int MAX_MOTIVO = 200;

    /**
     * Recorta un texto al ancho de su columna.
     *
     * <p>El historial es el registro de algo que <em>ya ocurrió</em>: si el texto
     * no cabe, lo que debe perderse es la cola, no la operación que se estaba
     * registrando. Sin esto, un motivo de 220 caracteres hacía fallar el alta
     * completa de una muestra con «Data too long for column 'motivo'» —la
     * excepción salta dentro de la transacción de negocio y se lleva por delante
     * la muestra, sus alícuotas y la reserva de volumen—.</p>
     *
     * <p>Los llamadores redactan mensajes que caben de sobra; esto existe para
     * que ninguno pueda volver a tumbar una operación por pasarse de largo, por
     * ejemplo cuando una etiqueta o un nombre de tubo son inusualmente largos.</p>
     */
    static String recortar(String texto, int maximo) {
        if (texto == null || texto.length() <= maximo) {
            return texto;
        }
        return texto.substring(0, maximo - 1) + "…";
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
        h.setValorAnterior(recortar(valorAnterior, MAX_VALOR));
        h.setValorNuevo(recortar(valorNuevo, MAX_VALOR));
        h.setFechaCambio(LocalDateTime.now());
        h.setMotivo(recortar(motivo, MAX_MOTIVO));
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
        h.setCampo(recortar(campo, MAX_CAMPO));
        h.setValorAnterior(recortar(valorAnterior, MAX_VALOR));
        h.setValorNuevo(recortar(valorNuevo, MAX_VALOR));
        h.setFechaCambio(LocalDateTime.now());
        h.setMotivo(recortar(motivo, MAX_MOTIVO));
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
