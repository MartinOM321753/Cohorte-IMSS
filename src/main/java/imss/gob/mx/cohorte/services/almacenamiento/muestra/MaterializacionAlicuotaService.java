package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La contabilidad de volumen entre una muestra padre y sus alícuotas.
 *
 * <p>Vive en la capa de servicio y no dentro de un ApplicationService porque
 * tiene dos clientes: el alta y ubicación de muestras, y la recepción de
 * traslados. Duplicar el descuento en ambos sitios es justamente la forma de
 * que acabe aplicándose dos veces.</p>
 *
 * <h3>Por qué el descuento va al ubicar y no al crear</h3>
 * <p>Crear una alícuota es una intención; ubicarla es la prueba de que el vial
 * existe de verdad. Entre una cosa y otra el volumen queda <em>reservado</em> en
 * la padre —ni disponible para otro uso ni todavía descontado—, que es lo que
 * refleja {@code valorComprometido}.</p>
 *
 * <h3>Por qué la marca es una fecha y no la posición</h3>
 * <p>Hay cuatro caminos que asignan posición: el endpoint de posición, la
 * edición de la muestra, la confirmación de un traslado y la cancelación de
 * uno. Si el descuento colgara de «tiene posición», confirmar la recepción de
 * una alícuota ya ubicada volvería a descontar, y liberar su hueco sugeriría
 * devolver líquido a la padre. No se puede despipetear: la materialización es
 * un hecho irreversible y {@code fechaMaterializacion} lo sella una sola vez.</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MaterializacionAlicuotaService {

    private final MuestraRepository muestraRepository;
    private final HistorialCambioMuestraService historialService;

    /**
     * Reserva volumen en la muestra padre al crear un lote.
     *
     * @return el comprometido resultante
     */
    @Transactional
    public double reservar(Muestra padre, double sumaLote, BeanUser usuario, String detalle) {
        double anterior = valorSeguro(padre.getValorComprometido());
        double nuevo = PlanificadorAlicuotas.sumar(anterior, sumaLote);
        padre.setValorComprometido(nuevo);
        muestraRepository.save(padre);

        historialService.registrarEvento(padre, usuario,
                TipoEventoMuestra.ALICUOTAS_COMPROMETIDAS,
                PlanificadorAlicuotas.fmt(anterior),
                PlanificadorAlicuotas.fmt(nuevo),
                detalle, null);

        return nuevo;
    }

    /**
     * Libera una reserva sin descontar nada, cuando un lote se deshace antes de
     * ubicarse (por ejemplo al eliminar una alícuota que nunca se llenó).
     */
    @Transactional
    public void liberarReserva(Muestra padre, double suma, BeanUser usuario, String detalle) {
        double anterior = valorSeguro(padre.getValorComprometido());
        double nuevo = Math.max(0.0, PlanificadorAlicuotas.restar(anterior, suma));
        padre.setValorComprometido(nuevo);
        muestraRepository.save(padre);

        historialService.registrarEvento(padre, usuario,
                TipoEventoMuestra.ALICUOTAS_COMPROMETIDAS,
                PlanificadorAlicuotas.fmt(anterior),
                PlanificadorAlicuotas.fmt(nuevo),
                detalle, null);
    }

    /** Materializa una sola alícuota. Idempotente: si ya lo estaba, no hace nada. */
    @Transactional
    public void materializar(Muestra alicuota, BeanUser usuario, String detalleUbicacion) {
        materializarLote(List.of(alicuota), usuario, detalleUbicacion);
    }

    /**
     * Materializa varias alícuotas descontando de cada padre una sola vez.
     *
     * <p>Agrupa por padre y la guarda una vez por grupo. Hacerlo alícuota a
     * alícuota generaría tantas escrituras sobre la misma fila como alícuotas
     * —con su correspondiente choque contra {@code @Version}— y llenaría la
     * línea de tiempo de la padre con un renglón por vial, sepultando el evento
     * que de verdad importa.</p>
     */
    @Transactional
    public void materializarLote(List<Muestra> alicuotas, BeanUser usuario, String detalleUbicacion) {
        if (alicuotas == null || alicuotas.isEmpty()) {
            return;
        }

        Map<Long, List<Muestra>> porPadre = new LinkedHashMap<>();
        Map<Long, Muestra> padres = new LinkedHashMap<>();

        for (Muestra a : alicuotas) {
            if (a == null || a.isMaterializada() || a.getMuestraPadre() == null) {
                continue; // ya materializada, o es una padre: nada que descontar
            }
            Muestra padre = a.getMuestraPadre();
            padres.putIfAbsent(padre.getId(), padre);
            porPadre.computeIfAbsent(padre.getId(), k -> new ArrayList<>()).add(a);
        }

        for (Map.Entry<Long, List<Muestra>> grupo : porPadre.entrySet()) {
            descontar(padres.get(grupo.getKey()), grupo.getValue(), usuario, detalleUbicacion);
        }
    }

    /**
     * Sella el agotamiento si la muestra se quedó sin volumen.
     *
     * <p>No es una baja: la baja es una decisión —contaminación, pérdida,
     * retiro de consentimiento— y el agotamiento es un hecho consumado. Si se
     * mezclaran, la pregunta «cuántas muestras se echaron a perder» quedaría
     * sepultada bajo decenas de miles de tubos consumidos con normalidad.</p>
     *
     * <p>Tampoco libera la posición si la tenía: puede haber un tubo vacío
     * todavía dentro de la caja hasta que alguien pase a retirarlo, y vaciar el
     * hueco solo le mentiría al visor 3D. Eso se ofrece, no se hace.</p>
     */
    @Transactional
    public void sellarAgotamientoSiProcede(Muestra muestra, BeanUser usuario) {
        if (muestra.getFechaAgotamiento() != null) {
            return;
        }
        if (!PlanificadorAlicuotas.agotado(muestra.getValor())) {
            return;
        }
        muestra.setFechaAgotamiento(Timestamp.valueOf(LocalDateTime.now()));
        muestraRepository.save(muestra);

        historialService.registrarEvento(muestra, usuario,
                TipoEventoMuestra.MUESTRA_AGOTADA,
                null, "0" + unidad(muestra),
                "Consumida por completo. No es una baja: conserva su historial y "
                + "sigue siendo el origen de sus alícuotas.", null);
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private void descontar(Muestra padre, List<Muestra> alicuotas, BeanUser usuario, String detalleUbicacion) {
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());

        double valorPadre = valorSeguro(padre.getValor());
        double comprometido = valorSeguro(padre.getValorComprometido());
        double valorInicial = valorPadre;
        double descontadoTotal = 0.0;
        List<String> inconsistencias = new ArrayList<>();

        for (Muestra a : alicuotas) {
            double pedido = valorSeguro(a.getValor());
            // Truncar en lugar de fallar: guardar un tubo en una caja es un acto
            // físico y no puede quedar bloqueado por un descuadre contable
            // heredado. Queda constancia para que se pueda investigar después.
            double descuento = PlanificadorAlicuotas.minimo(pedido, Math.max(valorPadre, 0.0));

            valorPadre = PlanificadorAlicuotas.restar(valorPadre, descuento);
            comprometido = Math.max(0.0, PlanificadorAlicuotas.restar(comprometido, pedido));
            descontadoTotal = PlanificadorAlicuotas.sumar(descontadoTotal, descuento);

            a.setFechaMaterializacion(ahora);
            a.setCantidadDescontadaPadre(descuento);
            muestraRepository.save(a);

            historialService.registrarEvento(a, usuario,
                    TipoEventoMuestra.ALICUOTA_MATERIALIZADA,
                    null, PlanificadorAlicuotas.fmt(descuento) + unidad(a),
                    (detalleUbicacion != null && !detalleUbicacion.isBlank()
                            ? "Ubicada en " + detalleUbicacion + ". "
                            : "")
                    + "Descontado de " + padre.getEtiqueta(), null);

            if (PlanificadorAlicuotas.mayorQue(pedido, descuento)) {
                inconsistencias.add(a.getEtiqueta() + " (pedía "
                        + PlanificadorAlicuotas.fmt(pedido) + unidad(a) + ", se descontó "
                        + PlanificadorAlicuotas.fmt(descuento) + unidad(a) + ")");
                log.warn("Materialización con descuadre: alícuota {} pedía {} y la padre {} solo tenía {}",
                        a.getEtiqueta(), pedido, padre.getEtiqueta(), descuento);
            }
        }

        padre.setValor(valorPadre);
        padre.setValorComprometido(comprometido);
        padre.setFechaActualizacion(ahora);
        muestraRepository.save(padre);

        // Corto a proposito: la columna `motivo` son 200 caracteres y el detalle
        // por vial ya vive en el historial de cada alicuota y en el log. Aqui
        // solo va lo que le importa a la padre: cuanto perdio y por que.
        StringBuilder motivo = new StringBuilder(alicuotas.size() == 1
                ? "Ubicada " + alicuotas.get(0).getEtiqueta()
                : "Lote de " + alicuotas.size() + " alícuotas ubicado");
        if (detalleUbicacion != null && !detalleUbicacion.isBlank()) {
            motivo.append(" en ").append(detalleUbicacion);
        }
        motivo.append(". −").append(PlanificadorAlicuotas.fmt(descontadoTotal)).append(unidad(padre));
        if (!inconsistencias.isEmpty()) {
            motivo.append(". AVISO: volumen insuficiente en ").append(inconsistencias.size())
                  .append(" alícuota").append(inconsistencias.size() == 1 ? "" : "s")
                  .append(" (ver su historial)");
        }

        historialService.registrarEvento(padre, usuario,
                TipoEventoMuestra.ALICUOTA_MATERIALIZADA,
                PlanificadorAlicuotas.fmt(valorInicial) + unidad(padre),
                PlanificadorAlicuotas.fmt(valorPadre) + unidad(padre),
                motivo.toString(), null);

        sellarAgotamientoSiProcede(padre, usuario);
    }

    private static double valorSeguro(Double valor) {
        return valor == null || valor.isNaN() || valor.isInfinite() ? 0.0 : valor;
    }

    private static String unidad(Muestra m) {
        return m.getUnidad() == null || m.getUnidad().isBlank() ? "" : " " + m.getUnidad();
    }
}
