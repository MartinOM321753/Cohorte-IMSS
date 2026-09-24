package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.persona.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

/**
 * Lo que la pantalla de muestras pide, ya normalizado y listo para consultar.
 *
 * <p>Todos los criterios son opcionales salvo la institución. Un campo nulo o
 * vacío significa «sin restricción» y su cláusula ni siquiera se agrega a la
 * consulta, de modo que la vista sin filtros no paga por los filtros que nadie
 * activó.</p>
 *
 * <p>La normalización vive aquí y no en el repositorio para que el texto que se
 * compara contra la base ya venga en la forma correcta: minúsculas para las
 * comparaciones insensibles a caja, folios rellenos de ceros, fechas convertidas
 * al instante que les corresponde. Repartirla entre el controlador y la consulta
 * es como acaban divergiendo el filtro de la pantalla y el del servidor.</p>
 */
public record CriteriosMuestra(
        Long idInstitucion,
        boolean incluirHistorico,
        boolean ocultarDevueltasHuerfanas,
        String busqueda,
        LocalDateTime desde,
        LocalDateTime hasta,
        List<String> tipos,
        Persona.Sexo sexo,
        String folioDesde,
        String folioHasta) {

    /**
     * Ancho con el que {@code FolioGeneratorService} rellena los folios numéricos.
     *
     * <p>La consulta lo exige de forma explícita, así que tiene que poder leerlo:
     * el rango solo es comparable como texto entre folios de este mismo ancho.</p>
     */
    public static final int ANCHO_FOLIO = 6;

    public static CriteriosMuestra de(
            Long idInstitucion,
            boolean incluirHistorico,
            boolean ocultarDevueltasHuerfanas,
            String busqueda,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            List<String> tipos,
            String sexo,
            String folioDesde,
            String folioHasta) {

        return new CriteriosMuestra(
                idInstitucion,
                incluirHistorico,
                ocultarDevueltasHuerfanas,
                normalizarTexto(busqueda),
                fechaDesde != null ? fechaDesde.atStartOfDay() : null,
                // Fin de día inclusivo: la fecha de recolección lleva hora, y un
                // `<= 2026-09-20` a secas dejaría fuera todo lo recolectado ese
                // mismo día después de la medianoche.
                fechaHasta != null ? fechaHasta.atTime(23, 59, 59, 999_999_999) : null,
                normalizarTipos(tipos),
                normalizarSexo(sexo),
                extremo(folioDesde, folioHasta, true),
                extremo(folioDesde, folioHasta, false));
    }

    private static String normalizarTexto(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().toLowerCase(Locale.ROOT);
        return limpio.isEmpty() ? null : limpio;
    }

    private static List<String> normalizarTipos(List<String> tipos) {
        if (tipos == null) {
            return List.of();
        }
        return tipos.stream()
                .map(CriteriosMuestra::normalizarTexto)
                .filter(t -> t != null)
                .distinct()
                .toList();
    }

    private static Persona.Sexo normalizarSexo(String sexo) {
        String limpio = sexo == null ? "" : sexo.trim().toUpperCase(Locale.ROOT);
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            return Persona.Sexo.valueOf(limpio);
        } catch (IllegalArgumentException e) {
            return null;   // valor desconocido: se ignora el filtro, no se vacía la lista
        }
    }

    /** El folio más alto que cabe en el ancho con el que se generan. */
    private static final long FOLIO_MAXIMO = 999_999L;

    /**
     * Convierte el rango tecleado en dos extremos de seis dígitos.
     *
     * <p>Los folios numéricos se guardan rellenos de ceros a seis dígitos. Entre
     * cadenas de ese mismo ancho, el orden alfabético y el numérico coinciden, y
     * eso permite acotar el rango en la base sin convertir la columna a número
     * en cada fila —lo que anularía cualquier índice—. La igualdad se rompe en
     * cuanto las longitudes difieren: como texto, {@code "1000000"} es MENOR que
     * {@code "999999"}, porque la comparación termina en el primer carácter. Por
     * eso la consulta exige además que el folio mida justo seis, y por eso los
     * dos extremos se fijan siempre juntos aunque solo se teclee uno: con un
     * extremo abierto, un folio manual con letras ({@code ABC123}) se colaría en
     * un rango que se pidió en números.</p>
     *
     * <p>Un folio pedido por encima del máximo no puede existir, así que el
     * rango se deja deliberadamente invertido: no devuelve nada, que es la
     * respuesta correcta.</p>
     *
     * @param inicio true para el extremo inferior, false para el superior
     */
    private static String extremo(String folioDesde, String folioHasta, boolean inicio) {
        Long pedidoDesde = aNumero(folioDesde);
        Long pedidoHasta = aNumero(folioHasta);
        if (pedidoDesde == null && pedidoHasta == null) {
            return null;
        }

        long desde = pedidoDesde != null ? pedidoDesde : 0L;
        long hasta = pedidoHasta != null ? Math.min(pedidoHasta, FOLIO_MAXIMO) : FOLIO_MAXIMO;
        if (desde > FOLIO_MAXIMO) {
            desde = 1L;
            hasta = 0L;
        }

        return rellenar(inicio ? desde : hasta);
    }

    /**
     * El número que se tecleó, ignorando lo que no sea dígito.
     *
     * <p>Devuelve nulo cuando no hay ninguno: un extremo en blanco no acota, en
     * vez de vaciar la lista.</p>
     */
    private static Long aNumero(String folio) {
        if (folio == null) {
            return null;
        }
        String digitos = folio.trim().replaceAll("\\D", "").replaceFirst("^0+(?=.)", "");
        if (digitos.isEmpty()) {
            return null;
        }
        // Más dígitos de los que caben ya está por encima del máximo; parsearlo
        // solo arriesgaría un desbordamiento.
        return digitos.length() > ANCHO_FOLIO ? FOLIO_MAXIMO + 1 : Long.parseLong(digitos);
    }

    private static String rellenar(long numero) {
        String texto = Long.toString(numero);
        return texto.length() >= ANCHO_FOLIO
                ? texto
                : "0".repeat(ANCHO_FOLIO - texto.length()) + texto;
    }

    public boolean hayBusqueda() {
        return busqueda != null;
    }

    public boolean hayTipos() {
        return tipos != null && !tipos.isEmpty();
    }

    /** Si algún criterio distinto de la institución está activo. */
    public boolean hayFiltros() {
        return hayBusqueda() || hayTipos() || desde != null || hasta != null
                || sexo != null || folioDesde != null || folioHasta != null;
    }
}
