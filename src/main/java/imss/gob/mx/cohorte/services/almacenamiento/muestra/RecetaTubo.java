package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import java.util.ArrayList;
import java.util.List;

/**
 * La receta de alicuotado de un tubo: cuántas alícuotas salen de él, de qué
 * tamaño y en qué unidad.
 *
 * <p>Es un calco deliberado de los campos de {@code TuboMuestra} y no la entidad
 * misma. El planificador tiene que poder ejercitarse sin base de datos ni JPA, y
 * si recibiera la entidad acabaría arrastrando su grafo —tipo de muestra,
 * institución— hasta las pruebas.</p>
 *
 * <p>Cada alícuota puede tener su propio volumen. {@link #volumenesPorSlot} lo
 * recoge en orden; cuando llega vacía —un tubo uniforme, o uno heredado sin
 * configuración por slot— cada slot cae a {@link #volumenAlicuota} y la receta
 * se comporta igual que cuando solo existía un volumen para todas.</p>
 *
 * @param nombreTubo       solo para redactar mensajes que el usuario entienda
 * @param numeroAlicuotas  cuántas alícuotas produce el tubo; 0 = tubo directo
 * @param volumenAlicuota  volumen general; semilla y respaldo de cada slot
 * @param unidad           unidad del volumen; manda sobre la de la muestra padre
 * @param permiteParcial   si se admite cerrar el lote con una alícuota incompleta
 * @param volumenesPorSlot capacidad de cada alícuota en orden; vacía = uniforme
 */
public record RecetaTubo(
        String nombreTubo,
        Integer numeroAlicuotas,
        Double volumenAlicuota,
        String unidad,
        boolean permiteParcial,
        List<Double> volumenesPorSlot
) {
    /** Normaliza la lista por slot a una copia inmutable que nunca es null. */
    public RecetaTubo {
        volumenesPorSlot = volumenesPorSlot == null ? List.of() : List.copyOf(volumenesPorSlot);
    }

    /**
     * Receta uniforme: todas las alícuotas comparten el mismo volumen.
     *
     * <p>Es el caso histórico —un tubo con una sola capacidad— y el que usan las
     * pruebas. Deja {@link #volumenesPorSlot} vacía a propósito: no hay nada por
     * slot que distinga a una alícuota de otra, y {@link #capacidadDeSlot} las
     * resuelve todas contra el volumen general.</p>
     */
    public RecetaTubo(String nombreTubo, Integer numeroAlicuotas, Double volumenAlicuota,
                      String unidad, boolean permiteParcial) {
        this(nombreTubo, numeroAlicuotas, volumenAlicuota, unidad, permiteParcial, List.of());
    }

    /**
     * Capacidad configurada del slot indicado (1-based).
     *
     * <p>Cae al volumen general cuando el tubo no tiene configuración por slot, o
     * cuando el índice se sale de ella: así un tubo uniforme resuelve cualquier
     * slot al mismo número y nada cambia respecto al comportamiento anterior.</p>
     */
    public Double capacidadDeSlot(int slot) {
        if (slot >= 1 && slot <= volumenesPorSlot.size()) {
            Double v = volumenesPorSlot.get(slot - 1);
            if (v != null) {
                return v;
            }
        }
        return volumenAlicuota;
    }

    /** Capacidades, en orden, de los slots indicados (1-based). */
    public List<Double> capacidadesDeSlots(List<Integer> slots) {
        List<Double> caps = new ArrayList<>(slots.size());
        for (Integer slot : slots) {
            caps.add(capacidadDeSlot(slot));
        }
        return caps;
    }
}
