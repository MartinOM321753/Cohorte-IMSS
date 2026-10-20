package imss.gob.mx.cohorte.services.almacenamiento.caja;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reserva temporal de posiciones de caja, estilo «butacas de cine».
 *
 * Mientras un usuario tiene abierta la rejilla de ubicación, las celdas que
 * seleccionó quedan bloqueadas para los demás por un máximo de 5 minutos.
 * No toca la base de datos: las reservas viven en memoria y se limpian solas.
 */
@Service
public class ReservaPosicionService {

    private static final long TTL_MS = 5 * 60 * 1000L;

    private record Reserva(String sesion, Instant expira) {}

    /** posicionId → reserva vigente. */
    private final ConcurrentHashMap<Long, Reserva> reservas = new ConcurrentHashMap<>();

    public boolean reservar(Long idPosicion, String sesion) {
        Instant limite = Instant.now().plusMillis(TTL_MS);
        Reserva nueva = new Reserva(sesion, limite);
        Reserva existente = reservas.compute(idPosicion, (k, vieja) -> {
            if (vieja == null || vieja.expira.isBefore(Instant.now())) return nueva;
            if (vieja.sesion.equals(sesion)) return nueva;
            return vieja;
        });
        return existente.sesion.equals(sesion);
    }

    public void reservarVarias(List<Long> ids, String sesion) {
        for (Long id : ids) reservar(id, sesion);
    }

    public void liberar(Long idPosicion, String sesion) {
        reservas.computeIfPresent(idPosicion, (k, r) ->
                r.sesion.equals(sesion) ? null : r);
    }

    public void liberarSesion(String sesion) {
        reservas.entrySet().removeIf(e -> e.getValue().sesion.equals(sesion));
    }

    public Set<Long> reservadasEnCaja(Collection<Long> idsPosiciones, String sesionPropia) {
        Instant ahora = Instant.now();
        Set<Long> resultado = new HashSet<>();
        for (Long id : idsPosiciones) {
            Reserva r = reservas.get(id);
            if (r != null && r.expira.isAfter(ahora) && !r.sesion.equals(sesionPropia)) {
                resultado.add(id);
            }
        }
        return resultado;
    }

    @Scheduled(fixedRate = 60_000)
    public void limpiarExpiradas() {
        Instant ahora = Instant.now();
        reservas.entrySet().removeIf(e -> e.getValue().expira.isBefore(ahora));
    }
}
