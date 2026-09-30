package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.services.almacenamiento.muestra.PlanAlicuotas;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PlanificadorAlicuotas;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Traduce el plan calculado a lo que la pantalla necesita, incluidos los
 * repartos posibles del remanente.
 *
 * <p>Las alternativas se calculan aquí y no en el cliente para que la
 * aritmética de volúmenes ocurra una sola vez y en {@code BigDecimal}: un
 * reparto redondeado en JavaScript puede sumar una diezmilésima de más y ser
 * rechazado por la validación del servidor, dejando al usuario ante una opción
 * que la propia pantalla le ofreció.</p>
 */
public class PlanAlicuotasMapper {

    /** Cuántos repartos distintos se ofrecen antes de mandar al modo personalizado. */
    private static final int MAX_OPCIONES_REPARTO = 3;

    private PlanAlicuotasMapper() {
    }

    public static PlanAlicuotasResponseDTO toResponseDTO(PlanAlicuotas plan) {
        return PlanAlicuotasResponseDTO.builder()
                .numeroAlicuotasConfiguradas(plan.numeroAlicuotasConfiguradas())
                .volumenAlicuota(plan.volumenAlicuota())
                .unidad(plan.unidad())
                .totalRequerido(plan.totalRequerido())
                .valorDisponible(plan.valorDisponible())
                .alicuotasCompletas(plan.alicuotasCompletas())
                .remanente(plan.remanente())
                .lugaresRestantes(plan.lugaresRestantes())
                .alcanzaLoteCompleto(plan.alcanzaLoteCompleto())
                .puedeAlojarParcial(plan.puedeAlojarParcial())
                .slotsOcupados(plan.slotsOcupados())
                .slotsLibres(plan.slotsLibres())
                .capacidadesSlots(plan.capacidadesSlots())
                .mensaje(plan.mensaje())
                .opciones(construirOpciones(plan))
                .build();
    }

    private static List<PlanAlicuotasResponseDTO.OpcionDistribucionDTO> construirOpciones(PlanAlicuotas plan) {
        List<PlanAlicuotasResponseDTO.OpcionDistribucionDTO> opciones = new ArrayList<>();
        String u = plan.unidad() == null || plan.unidad().isBlank() ? "" : " " + plan.unidad();

        // Siempre la primera y por omisión: solo las completas, el resto se queda
        // en la padre. Nunca se alicuota el remanente sin que lo pidan.
        String descripcionBase = plan.alicuotasCompletas() == 0
                ? "No generar alícuotas por ahora"
                : PlanificadorAlicuotas.fmt(plan.remanente()) .equals("0")
                        ? "Generar " + plan.alicuotasCompletas() + " alícuota"
                          + (plan.alicuotasCompletas() == 1 ? "" : "s") + " completa"
                          + (plan.alicuotasCompletas() == 1 ? "" : "s")
                        : "Dejar " + PlanificadorAlicuotas.fmt(plan.remanente()) + u + " en la muestra padre";

        opciones.add(PlanAlicuotasResponseDTO.OpcionDistribucionDTO.builder()
                .clave("SOLO_COMPLETAS")
                .descripcion(descripcionBase)
                .volumenes(plan.volumenesSugeridos())
                .totalAlicuotas(plan.volumenesSugeridos().size())
                .remanenteEnPadre(plan.remanente())
                .build());

        if (!plan.puedeAlojarParcial()) {
            return opciones;
        }

        int tope = Math.min(plan.lugaresRestantes(), MAX_OPCIONES_REPARTO);
        for (int n = 1; n <= tope; n++) {
            List<Double> volumenes = PlanificadorAlicuotas.distribuirRemanente(plan, n);
            List<Double> extras = volumenes.subList(plan.alicuotasCompletas(), volumenes.size());

            // Con volúmenes distintos por slot, un reparto en partes iguales puede
            // superar el tope de algún vial pequeño. No se ofrece un reparto que
            // la validación rechazaría después: el usuario tiene «personalizar».
            if (!extrasCabenEnSusSlots(extras, plan)) {
                continue;
            }

            opciones.add(PlanAlicuotasResponseDTO.OpcionDistribucionDTO.builder()
                    .clave("PARCIALES_" + n)
                    .descripcion(describirReparto(n, extras, u))
                    .volumenes(volumenes)
                    .totalAlicuotas(volumenes.size())
                    .remanenteEnPadre(0.0)
                    .build());
        }
        return opciones;
    }

    /** Si cada parcial del reparto cabe en el hueco que le tocaría, en orden. */
    private static boolean extrasCabenEnSusSlots(List<Double> extras, PlanAlicuotas plan) {
        List<Double> caps = plan.capacidadesSlots();
        if (caps == null || caps.isEmpty()) {
            return true; // tubo uniforme sin capacidades por slot: nada que topar
        }
        int desde = plan.alicuotasCompletas();
        for (int j = 0; j < extras.size(); j++) {
            int idx = desde + j;
            if (idx < caps.size() && extras.get(j) - caps.get(idx) > 1e-6) {
                return false;
            }
        }
        return true;
    }

    private static String describirReparto(int cuantas, List<Double> extras, String unidad) {
        if (cuantas == 1) {
            return "Agregar una alícuota de " + PlanificadorAlicuotas.fmt(extras.get(0)) + unidad;
        }
        Set<String> distintos = new LinkedHashSet<>();
        for (Double v : extras) {
            distintos.add(PlanificadorAlicuotas.fmt(v));
        }
        String detalle = distintos.size() == 1
                ? distintos.iterator().next() + unidad + " c/u"
                : String.join(unidad + " y ", distintos) + unidad;
        return "Repartir entre " + cuantas + " alícuotas (" + detalle + ")";
    }
}
