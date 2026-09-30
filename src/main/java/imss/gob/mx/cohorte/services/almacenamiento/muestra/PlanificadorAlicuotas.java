package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Decide cuántas alícuotas caben en el volumen que de verdad se extrajo y qué
 * hacer con el sobrante.
 *
 * <p>Antes de esto el sistema creaba siempre las {@code numeroAlicuotas} del
 * tubo con su volumen nominal, sin mirar cuánto se había extraído: un tubo de
 * 5 × 50 mL sobre una extracción de 200 mL inventaba 50 mL que no existían y
 * dejaba además una etiqueta impresa para un vial que nunca se llenó.</p>
 *
 * <p>Es una clase pura —sin Spring, sin repositorios, sin entidades— a
 * propósito: el mismo cálculo lo consumen la previsualización del formulario y
 * la creación del lote. Si viviera en dos sitios, lo que la pantalla promete y
 * lo que el servidor crea acabarían divergiendo sin que nadie lo note.</p>
 *
 * <p>Toda la aritmética interna va en {@link BigDecimal}. Con {@code double},
 * {@code 0.1 + 0.2} da {@code 0.30000000000000004} y el sistema rechazaría un
 * plan correcto alegando que no alcanza por una diezmilmillonésima de
 * mililitro.</p>
 */
public final class PlanificadorAlicuotas {

    /** Decimales con los que se redondea cualquier volumen. */
    public static final int ESCALA = 4;

    /** Por debajo de esto, dos volúmenes son el mismo volumen. */
    private static final BigDecimal EPSILON = new BigDecimal("0.000001");

    private PlanificadorAlicuotas() {
    }

    // ── Planificación ────────────────────────────────────────────────────────

    /**
     * Calcula el plan por omisión para una extracción concreta.
     *
     * @param receta          configuración del tubo elegido
     * @param valorDisponible volumen que la muestra padre puede comprometer (su
     *                        valor menos lo ya comprometido), nunca su valor bruto
     */
    public static PlanAlicuotas planificar(RecetaTubo receta, Double valorDisponible) {
        return planificar(receta, valorDisponible, 0);
    }

    /**
     * Calcula el plan para llenar los huecos que le quedan a un lote.
     *
     * <p>Un lote no se cierra al crearse: el tubo define un número de huecos y
     * pueden llenarse en varias tandas. Es lo normal cuando la extracción sale
     * corta —se hacen las que alcanzan— y más tarde se completa. Por eso el tope
     * no es lo que el tubo define, sino lo que le queda libre.</p>
     *
     * <p>Esta sobrecarga solo conoce <em>cuántos</em> huecos hay ocupados, no
     * cuáles. Da por libres los últimos —el orden natural en que se llenan— y es
     * la vía correcta mientras el lote crece en secuencia. Cuando un hueco
     * intermedio se reabre (se elimina una alícuota) y las capacidades por slot
     * difieren, hay que pasar los slots reales con
     * {@link #planificar(RecetaTubo, Double, List)}.</p>
     *
     * @param slotsOcupados alícuotas del lote que ya existen
     */
    public static PlanAlicuotas planificar(RecetaTubo receta, Double valorDisponible, int slotsOcupados) {
        int configuradas = exigirNumeroAlicuotas(receta);
        int ocupados = Math.max(0, Math.min(slotsOcupados, configuradas));
        return planificar(receta, valorDisponible, slotsPorDefecto(configuradas, ocupados));
    }

    /**
     * Calcula el plan para unos huecos libres concretos, con su capacidad
     * individual.
     *
     * <p>Es la vía completa: cada hueco puede tener su propio volumen y el plan
     * llena los que quepan <em>en orden</em>, no por división entera contra una
     * capacidad única. Un tubo uniforme resuelve todos los huecos al mismo número
     * y el resultado coincide, número a número, con el cálculo de antes.</p>
     *
     * @param slotsLibres números de alícuota (1-based) que el lote tiene libres
     */
    public static PlanAlicuotas planificar(RecetaTubo receta, Double valorDisponible, List<Integer> slotsLibres) {
        int configuradas = exigirNumeroAlicuotas(receta);
        BigDecimal nominal = exigirVolumenAlicuota(receta);
        BigDecimal disponible = normalizar(valorDisponible);

        List<Integer> libres = slotsLibres == null ? List.of() : slotsLibres;
        int nLibres = libres.size();
        int ocupados = Math.max(0, configuradas - nLibres);

        List<BigDecimal> capacidades = capacidadesLibres(receta, libres);

        // Lo que haría falta para llenar lo que queda, no para el tubo entero:
        // en una continuación, el total del tubo ya no es el número accionable.
        BigDecimal totalRequerido = BigDecimal.ZERO;
        for (BigDecimal c : capacidades) {
            totalRequerido = totalRequerido.add(c);
        }
        totalRequerido = totalRequerido.setScale(ESCALA, RoundingMode.HALF_UP);

        // Se llenan los huecos en orden mientras el disponible alcance para el
        // volumen de cada uno. El primero que no cabe entero marca la frontera de
        // la parcial. Con capacidades iguales esto es exactamente la división
        // entera de antes: 200 mL en un tubo de 5 × 50 llena 4 y se detiene.
        List<Double> sugeridos = new ArrayList<>();
        BigDecimal consumido = BigDecimal.ZERO;
        for (BigDecimal capacidad : capacidades) {
            BigDecimal restante = disponible.subtract(consumido);
            if (restante.subtract(capacidad).compareTo(EPSILON.negate()) >= 0) {
                consumido = consumido.add(capacidad);
                sugeridos.add(capacidad.doubleValue());
            } else {
                break;
            }
        }
        int completas = sugeridos.size();

        BigDecimal remanente = disponible.subtract(consumido).setScale(ESCALA, RoundingMode.HALF_UP);
        if (remanente.signum() < 0) {
            remanente = BigDecimal.ZERO.setScale(ESCALA);
        }

        int lugaresRestantes = nLibres - completas;
        boolean puedeAlojarParcial = receta.permiteParcial()
                && lugaresRestantes > 0
                && remanente.compareTo(EPSILON) > 0;

        boolean alcanzaCompleto = nLibres > 0 && completas == nLibres;

        List<Double> capsLibres = new ArrayList<>(capacidades.size());
        for (BigDecimal c : capacidades) {
            capsLibres.add(c.doubleValue());
        }

        return new PlanAlicuotas(
                configuradas,
                nominal.doubleValue(),
                receta.unidad(),
                totalRequerido.doubleValue(),
                disponible.doubleValue(),
                completas,
                remanente.doubleValue(),
                lugaresRestantes,
                List.copyOf(sugeridos),
                puedeAlojarParcial,
                alcanzaCompleto,
                ocupados,
                nLibres,
                List.copyOf(capsLibres),
                redactarMensaje(receta, nominal, disponible, totalRequerido, completas,
                        remanente, alcanzaCompleto, ocupados, nLibres, configuradas)
        );
    }

    /**
     * Reparte el remanente entre {@code entreCuantas} alícuotas adicionales,
     * encima de las completas del plan.
     *
     * <p>Es lo que responde a «los 20 mL que sobran, ¿en una sola o repartidos
     * entre las dos que faltan?». El sobrante del redondeo se le da a la
     * primera, de modo que la suma sea exactamente el remanente: pasarse aunque
     * fuera por redondeo haría fallar la validación del plan que acabamos de
     * generar nosotros mismos.</p>
     *
     * @param entreCuantas cuántas alícuotas adicionales, entre 1 y los lugares restantes
     */
    public static List<Double> distribuirRemanente(PlanAlicuotas plan, int entreCuantas) {
        if (!plan.puedeAlojarParcial()) {
            throw new ValidationException(
                    "El lote no admite alícuotas incompletas: no queda remanente, no quedan lugares en el tubo "
                    + "o el tubo está configurado para no aceptarlas.");
        }
        if (entreCuantas < 1 || entreCuantas > plan.lugaresRestantes()) {
            throw new ValidationException(
                    "El remanente solo puede repartirse entre 1 y " + plan.lugaresRestantes()
                    + " alícuota(s); se pidieron " + entreCuantas + ".");
        }

        BigDecimal remanente = BigDecimal.valueOf(plan.remanente()).setScale(ESCALA, RoundingMode.HALF_UP);
        BigDecimal divisor = BigDecimal.valueOf(entreCuantas);
        BigDecimal base = remanente.divide(divisor, ESCALA, RoundingMode.DOWN);
        BigDecimal residuo = remanente.subtract(base.multiply(divisor));

        List<Double> volumenes = new ArrayList<>(plan.volumenesSugeridos());
        for (int i = 0; i < entreCuantas; i++) {
            BigDecimal parte = i == 0 ? base.add(residuo) : base;
            volumenes.add(parte.setScale(ESCALA, RoundingMode.HALF_UP).doubleValue());
        }
        return List.copyOf(volumenes);
    }

    // ── Validación ───────────────────────────────────────────────────────────

    /**
     * Comprueba un plan propuesto desde el cliente y devuelve sus volúmenes
     * redondeados a la escala de persistencia.
     *
     * <p>El cliente propone y el servidor dispone: la pantalla puede ofrecer los
     * repartos que quiera, pero ninguno entra si viola el tope del tubo, la
     * capacidad del vial o el volumen realmente disponible en la muestra padre.</p>
     */
    public static List<Double> validarPlan(List<Double> volumenes, RecetaTubo receta, Double valorDisponible) {
        return validarPlan(volumenes, receta, valorDisponible, 0);
    }

    /** @param slotsOcupados alícuotas del lote que ya existen y no se pueden repetir */
    public static List<Double> validarPlan(List<Double> volumenes, RecetaTubo receta,
                                           Double valorDisponible, int slotsOcupados) {
        int configuradas = exigirNumeroAlicuotas(receta);
        int ocupados = Math.max(0, Math.min(slotsOcupados, configuradas));
        return validarPlan(volumenes, receta, valorDisponible, slotsPorDefecto(configuradas, ocupados));
    }

    /**
     * Comprueba el plan contra los huecos concretos que va a llenar.
     *
     * <p>El {@code i}-ésimo volumen propuesto va al {@code i}-ésimo hueco libre,
     * así que cada uno se mide contra la capacidad <em>de su slot</em>, no contra
     * una capacidad única del tubo. Un tubo uniforme resuelve todos los slots al
     * mismo número y la comprobación es la misma de siempre.</p>
     *
     * @param slotsLibres números de alícuota (1-based) que el lote tiene libres
     */
    public static List<Double> validarPlan(List<Double> volumenes, RecetaTubo receta,
                                           Double valorDisponible, List<Integer> slotsLibres) {
        int configuradas = exigirNumeroAlicuotas(receta);
        exigirVolumenAlicuota(receta);
        BigDecimal disponible = normalizar(valorDisponible);

        List<Integer> libres = slotsLibres == null ? List.of() : slotsLibres;
        int nLibres = libres.size();
        int ocupados = Math.max(0, configuradas - nLibres);

        if (volumenes == null || volumenes.isEmpty()) {
            throw new ValidationException("El plan de alícuotas no puede venir vacío.");
        }
        if (nLibres <= 0) {
            throw new ValidationException(
                    "El lote ya está completo: el tubo define " + configuradas
                    + " alícuota(s) y todas existen.");
        }
        if (volumenes.size() > nLibres) {
            throw new ValidationException(
                    ocupados > 0
                        ? "Al lote le quedan " + nLibres + " hueco(s) de los " + configuradas
                          + " que define el tubo; no se pueden generar " + volumenes.size() + "."
                        : "El tubo seleccionado define " + configuradas
                          + " alícuota(s); no se pueden generar " + volumenes.size() + " en un mismo lote.");
        }

        List<Double> normalizados = new ArrayList<>(volumenes.size());
        BigDecimal suma = BigDecimal.ZERO;
        boolean hayParcial = false;

        for (int i = 0; i < volumenes.size(); i++) {
            Double crudo = volumenes.get(i);
            if (crudo == null || crudo.isNaN() || crudo.isInfinite()) {
                throw new ValidationException("El volumen de la alícuota " + (i + 1) + " no es un número válido.");
            }
            BigDecimal v = BigDecimal.valueOf(crudo).setScale(ESCALA, RoundingMode.HALF_UP);
            BigDecimal capacidad = capacidadDeSlot(receta, libres.get(i));

            if (v.compareTo(EPSILON) <= 0) {
                throw new ValidationException("El volumen de la alícuota " + (i + 1) + " debe ser mayor a 0.");
            }
            // Que el vial lleve menos que su volumen configurado es una decisión
            // del usuario; que lleve más es físicamente imposible.
            if (v.subtract(capacidad).compareTo(EPSILON) > 0) {
                throw new ValidationException(
                        "La alícuota " + (i + 1) + " (" + fmt(v) + unidadSufijo(receta)
                        + ") excede la capacidad configurada para ese vial ("
                        + fmt(capacidad) + unidadSufijo(receta) + ").");
            }
            if (capacidad.subtract(v).compareTo(EPSILON) > 0) {
                hayParcial = true;
            }

            suma = suma.add(v);
            normalizados.add(v.doubleValue());
        }

        if (hayParcial && !receta.permiteParcial()) {
            throw new ValidationException(
                    "El tubo seleccionado no admite alícuotas incompletas: cada alícuota debe "
                    + "llenarse a su volumen configurado.");
        }

        if (suma.subtract(disponible).compareTo(EPSILON) > 0) {
            throw new ValidationException(
                    "El lote solicitado suma " + fmt(suma) + unidadSufijo(receta)
                    + " y la muestra padre solo tiene " + fmt(disponible) + unidadSufijo(receta)
                    + " disponibles.");
        }

        return List.copyOf(normalizados);
    }

    /**
     * Exige que la unidad del tubo y la de la muestra padre sean la misma.
     *
     * <p>No hay tabla de conversiones en el sistema —{@code Unidad_Medida} es
     * solo un nombre por institución—, así que la única comparación posible es
     * la igualdad. Sin esto, un tubo configurado en µL contra una padre en mL
     * restaría microlitros a mililitros y la contabilidad quedaría mil veces
     * desviada sin un solo error visible.</p>
     *
     * <p>Al registrar la muestra esto no debería dispararse nunca, porque ahí la
     * unidad se impone desde el tubo. Hace falta para el otro camino: el lote
     * que genera más tarde otra unidad sobre una padre que ya existe con la
     * suya, donde imponer ya no es posible.</p>
     */
    /**
     * Si dos unidades son la misma, escrita de dos formas.
     *
     * <p><b>No es una conversión.</b> Lo que el sistema no hace —y sigue sin
     * hacer— es convertir magnitudes: de mL a L, o de mg a g. Esto es otra cosa:
     * {@code µL} y {@code uL} son el mismo microlitro, y una hoja de cálculo
     * escribe el micro de las dos maneras según quién la teclee y con qué
     * teclado. Distinguirlos solo conseguía rechazar archivos correctos.</p>
     *
     * <p>El signo micro {@code µ} (U+00B5) y la mu griega {@code μ} (U+03BC) son
     * dos puntos de código distintos para el mismo prefijo, y ninguno de los dos
     * se pliega a una {@code u} por las reglas normales de mayúsculas o acentos:
     * hay que decirlo explícitamente.</p>
     */
    public static boolean mismaUnidad(String a, String b) {
        return canonizarUnidad(a).equals(canonizarUnidad(b));
    }

    private static String canonizarUnidad(String unidad) {
        if (unidad == null) {
            return "";
        }
        return unidad.trim()
                .replace('µ', 'u')   // signo micro
                .replace('μ', 'u')   // mu griega
                // Locale.ROOT a propósito: con el del sistema, una JVM en turco
                // convierte la i en ı y dos unidades iguales dejan de coincidir.
                .toUpperCase(java.util.Locale.ROOT);
    }

    public static void validarUnidad(String unidadTubo, String unidadPadre) {
        if (unidadTubo == null || unidadTubo.isBlank()) {
            throw new ValidationException(
                    "El tubo seleccionado no tiene unidad de volumen configurada. "
                    + "Defínala en Tipos de muestra antes de generar alícuotas.");
        }
        if (unidadPadre == null || unidadPadre.isBlank()) {
            return; // muestra heredada sin unidad: adopta la del tubo
        }
        if (!mismaUnidad(unidadTubo, unidadPadre)) {
            throw new ValidationException(
                    "El tubo seleccionado está configurado en " + unidadTubo
                    + " y la muestra padre está en " + unidadPadre
                    + ". El sistema no convierte unidades: configure el tubo en " + unidadPadre
                    + " para alicuotar esta muestra.");
        }
    }

    // ── Aritmética de volúmenes ──────────────────────────────────────────────

    /** Suma un plan completo, redondeando a la escala de persistencia. */
    public static double sumar(List<Double> volumenes) {
        BigDecimal total = BigDecimal.ZERO;
        if (volumenes != null) {
            for (Double v : volumenes) {
                if (v != null && !v.isNaN() && !v.isInfinite()) {
                    total = total.add(BigDecimal.valueOf(v));
                }
            }
        }
        return total.setScale(ESCALA, RoundingMode.HALF_UP).doubleValue();
    }

    /** Suma dos volúmenes sin arrastrar el error del coma flotante. */
    public static double sumar(Double a, Double b) {
        return normalizar(a).add(normalizar(b)).setScale(ESCALA, RoundingMode.HALF_UP).doubleValue();
    }

    /** Resta dos volúmenes sin arrastrar el error del coma flotante. */
    public static double restar(Double minuendo, Double sustraendo) {
        return normalizar(minuendo).subtract(normalizar(sustraendo))
                .setScale(ESCALA, RoundingMode.HALF_UP).doubleValue();
    }

    /** El menor de dos volúmenes, ya normalizado. */
    public static double minimo(Double a, Double b) {
        return normalizar(a).min(normalizar(b)).doubleValue();
    }

    /** Si un volumen es cero dentro del margen de tolerancia (o negativo). */
    public static boolean agotado(Double valor) {
        return normalizar(valor).compareTo(EPSILON) <= 0;
    }

    /** Si {@code a} es mayor que {@code b} de forma significativa. */
    public static boolean mayorQue(Double a, Double b) {
        return normalizar(a).subtract(normalizar(b)).compareTo(EPSILON) > 0;
    }

    /** Texto legible de un volumen: 250 en lugar de 250.0, 2.5 en lugar de 2.5000. */
    public static String fmt(double valor) {
        return fmt(BigDecimal.valueOf(valor).setScale(ESCALA, RoundingMode.HALF_UP));
    }

    // ── Privados ─────────────────────────────────────────────────────────────

    private static String fmt(BigDecimal valor) {
        BigDecimal limpio = valor.stripTrailingZeros();
        return limpio.scale() < 0 ? limpio.setScale(0).toPlainString() : limpio.toPlainString();
    }

    private static String unidadSufijo(RecetaTubo receta) {
        return receta.unidad() == null || receta.unidad().isBlank() ? "" : " " + receta.unidad();
    }

    private static BigDecimal normalizar(Double valor) {
        if (valor == null || valor.isNaN() || valor.isInfinite()) {
            return BigDecimal.ZERO.setScale(ESCALA);
        }
        return BigDecimal.valueOf(valor).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    /** Los últimos huecos del tubo: el orden natural en que un lote se llena. */
    private static List<Integer> slotsPorDefecto(int configuradas, int ocupados) {
        List<Integer> libres = new ArrayList<>(Math.max(0, configuradas - ocupados));
        for (int slot = ocupados + 1; slot <= configuradas; slot++) {
            libres.add(slot);
        }
        return libres;
    }

    /** Capacidades, en orden, de los huecos libres; cada una exigida mayor a 0. */
    private static List<BigDecimal> capacidadesLibres(RecetaTubo receta, List<Integer> slotsLibres) {
        List<BigDecimal> caps = new ArrayList<>(slotsLibres.size());
        for (Integer slot : slotsLibres) {
            caps.add(capacidadDeSlot(receta, slot));
        }
        return caps;
    }

    /** Capacidad del slot como {@link BigDecimal}, cayendo al volumen general. */
    private static BigDecimal capacidadDeSlot(RecetaTubo receta, int slot) {
        Double capacidad = receta.capacidadDeSlot(slot);
        if (capacidad == null || capacidad.isNaN() || capacidad.isInfinite() || capacidad <= 0) {
            // El respaldo es el volumen general, que ya se exige configurado; que
            // un slot concreto quede sin capacidad válida solo puede ser un dato
            // corrupto, y alicuotar contra él inventaría o perdería volumen.
            throw new ValidationException(
                    "La alícuota " + slot + " del tubo seleccionado no tiene un volumen válido configurado. "
                    + "Revíselo en Tipos de muestra antes de generar alícuotas.");
        }
        return BigDecimal.valueOf(capacidad).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private static int exigirNumeroAlicuotas(RecetaTubo receta) {
        int configuradas = receta.numeroAlicuotas() == null ? 0 : receta.numeroAlicuotas();
        if (configuradas <= 0) {
            throw new ValidationException("El tubo seleccionado no genera alícuotas (tubo directo).");
        }
        return configuradas;
    }

    private static BigDecimal exigirVolumenAlicuota(RecetaTubo receta) {
        Double capacidad = receta.volumenAlicuota();
        if (capacidad == null || capacidad.isNaN() || capacidad.isInfinite() || capacidad <= 0) {
            throw new ValidationException(
                    "El tubo seleccionado no tiene volumen por alícuota configurado. "
                    + "Defínalo en Tipos de muestra antes de generar alícuotas.");
        }
        return BigDecimal.valueOf(capacidad).setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private static String redactarMensaje(RecetaTubo receta, BigDecimal capacidad, BigDecimal disponible,
                                          BigDecimal totalRequerido, int completas, BigDecimal remanente,
                                          boolean alcanzaCompleto, int ocupados, int slotsLibres,
                                          int configuradas) {
        String u = unidadSufijo(receta);
        StringBuilder sb = new StringBuilder();

        // En una continuación el número accionable es lo que falta, no el total
        // del tubo: decirle «se requieren 36 dl» a quien ya tiene una alícuota
        // hecha le pide de nuevo volumen que ya gastó.
        String queLote = ocupados > 0
                ? "completar el lote (faltan " + slotsLibres + " de " + configuradas + ")"
                : "generar el lote completo";

        if (slotsLibres <= 0) {
            return "El lote ya está completo: el tubo define " + configuradas
                 + " alícuota(s) y todas existen.";
        }

        if (completas == 0) {
            sb.append("Con ").append(fmt(disponible)).append(u)
              .append(" no alcanza para ninguna alícuota completa de ").append(fmt(capacidad)).append(u)
              .append(". Para ").append(queLote).append(" se requieren ")
              .append(fmt(totalRequerido)).append(u).append(".");
            return sb.toString();
        }

        if (alcanzaCompleto) {
            sb.append("Se generarán ").append(completas).append(" alícuota")
              .append(completas == 1 ? "" : "s").append(" de ").append(fmt(capacidad)).append(u)
              .append(" (").append(fmt(totalRequerido)).append(u).append(" en total)");
            if (ocupados > 0) {
                sb.append(", con lo que el lote queda completo en ").append(configuradas);
            }
            sb.append(".");
        } else {
            sb.append("Para ").append(queLote).append(" se requieren ").append(fmt(totalRequerido)).append(u)
              .append(". Con ").append(fmt(disponible)).append(u).append(" alcanzan ")
              .append(completas).append(" alícuota").append(completas == 1 ? "" : "s")
              .append(" de ").append(fmt(capacidad)).append(u).append(".");
        }

        if (remanente.compareTo(EPSILON) > 0) {
            sb.append(" Restan ").append(fmt(remanente)).append(u).append(" en la muestra padre.");
        }
        return sb.toString();
    }
}
