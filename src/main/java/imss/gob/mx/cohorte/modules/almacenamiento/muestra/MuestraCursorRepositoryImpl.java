package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Armado de las consultas del listado de muestras.
 *
 * <p>El texto JPQL se construye por partes, pero ningún valor se interpola: los
 * fragmentos son constantes del propio código y todo dato del usuario entra por
 * {@code setParameter}. Lo que varía es qué cláusulas existen, no qué dicen.</p>
 */
public class MuestraCursorRepositoryImpl implements MuestraCursorRepository {

    @PersistenceContext
    private EntityManager em;

    // ── Vocabulario de la consulta ───────────────────────────────────────────

    private static final String TRASLADO = "imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra";
    private static final String CANCELADO = "imss.gob.mx.cohorte.modules.almacenamiento.traslado.EstadoTraslado.CANCELADO";

    /**
     * Uniones de la fila principal.
     *
     * <p>Las del padre son externas a propósito: la mayoría de las muestras no
     * tienen padre, y una unión interna las eliminaría antes de que la condición
     * «no tiene padre» llegara a evaluarse.</p>
     */
    private static final String UNIONES = """
             FROM Muestra m
             JOIN m.paciente pac
             JOIN pac.persona per
             JOIN m.institucion inst
             JOIN m.institucionActual instAct
             LEFT JOIN m.tipoMuestra tip
             LEFT JOIN m.tuboMuestra tub
             LEFT JOIN m.muestraPadre pad
             LEFT JOIN pad.institucion padInst
             LEFT JOIN pad.institucionActual padInstAct
            """;

    /** Todo lo que se puede teclear en la caja de búsqueda, concatenado. */
    private static String pajar(String m, String pac, String per, String tip, String tub) {
        return "LOWER(CONCAT("
                + m + ".etiqueta, ' ', COALESCE(" + m + ".unidad, ''), ' ', "
                + pac + ".folio, ' ', "
                + per + ".nombre, ' ', COALESCE(" + per + ".segundoNombre, ''), ' ', "
                + per + ".apellidoPaterno, ' ', COALESCE(" + per + ".apellidoMaterno, ''), ' ', "
                + "COALESCE(" + tip + ".nombre, ''), ' ', COALESCE(" + tub + ".nombre, '')))";
    }

    // ── Cláusulas ────────────────────────────────────────────────────────────

    /**
     * Qué ve esta institución: lo propio, lo que tiene en su poder y —solo si se
     * pide el histórico— lo que alguna vez recibió prestado.
     */
    private static String visible(String alias, boolean incluirHistorico) {
        String base = "(" + alias + ".institucion.id = :idInst OR " + alias + ".institucionActual.id = :idInst";
        if (!incluirHistorico) {
            return base + ")";
        }
        return base + " OR EXISTS (SELECT 1 FROM " + TRASLADO + " t WHERE t.muestra.id = " + alias + ".id"
                + " AND t.institucionDestino.id = :idInst AND t.estado <> " + CANCELADO + "))";
    }

    /** Igual que {@link #visible}, pero sobre el padre ya unido con alias propios. */
    private static String padreVisible(boolean incluirHistorico) {
        String base = "(padInst.id = :idInst OR padInstAct.id = :idInst";
        if (!incluirHistorico) {
            return base + ")";
        }
        return base + " OR EXISTS (SELECT 1 FROM " + TRASLADO + " t2 WHERE t2.muestra.id = pad.id"
                + " AND t2.institucionDestino.id = :idInst AND t2.estado <> " + CANCELADO + "))";
    }

    /**
     * Las condiciones comunes a la página y a los conteos.
     *
     * <p>Se comparte entre ambos para que el número del pie y las tarjetas de la
     * lista no puedan describir conjuntos distintos.</p>
     */
    private static StringBuilder condiciones(CriteriosMuestra c) {
        StringBuilder w = new StringBuilder(" WHERE ").append(visible("m", c.incluirHistorico()));

        // Solo filas de primer nivel: sin padre, o con el padre fuera de la vista
        // (alícuota recibida en préstamo cuyo tubo original está en otra sede).
        w.append(" AND (pad IS NULL OR NOT ").append(padreVisible(c.incluirHistorico())).append(")");

        if (c.ocultarDevueltasHuerfanas()) {
            // Alícuota ajena que además ya no está en mi biobanco: solo asoma con
            // el histórico encendido y es ruido en la vista del día a día.
            w.append(" AND NOT (pad IS NOT NULL AND inst.id <> :idInst AND instAct.id <> :idInst)");
        }

        if (c.hayBusqueda()) {
            w.append(" AND (").append(pajar("m", "pac", "per", "tip", "tub")).append(" LIKE :busqueda")
             .append(" OR EXISTS (SELECT 1 FROM Muestra a JOIN a.paciente apac JOIN apac.persona aper")
             .append(" LEFT JOIN a.tipoMuestra atip LEFT JOIN a.tuboMuestra atub")
             .append(" WHERE a.muestraPadre.id = m.id AND ")
             .append(pajar("a", "apac", "aper", "atip", "atub")).append(" LIKE :busqueda))");
        }

        /*
         * Fecha de recolección y tipo se evalúan contra la tarjeta O contra
         * alguna de sus alícuotas: cada institución alicuota con su propia
         * receta, así que el tipo del lote puede no ser el de la padre, y
         * juzgar solo por la padre escondería alícuotas que sí coinciden.
         *
         * Sexo y folio no entran en esa disyunción porque salen del
         * participante, que es el mismo en toda la descendencia.
         */
        boolean porFecha = c.desde() != null || c.hasta() != null;
        if (porFecha || c.hayTipos()) {
            w.append(" AND (");
            w.append("(").append(recoleccionYTipo("m", "tip", c)).append(")");
            w.append(" OR EXISTS (SELECT 1 FROM Muestra b LEFT JOIN b.tipoMuestra btip")
             .append(" WHERE b.muestraPadre.id = m.id AND ")
             .append(recoleccionYTipo("b", "btip", c)).append(")");
            w.append(")");
        }

        if (c.sexo() != null) {
            w.append(" AND per.sexo = :sexo");
        }
        if (c.folioDesde() != null) {
            /*
             * Los dos extremos van siempre juntos y el ancho se exige aparte.
             * El rango se compara como texto para no convertir la columna a
             * número en cada fila, y eso solo equivale al orden numérico entre
             * folios de la misma longitud: como texto, «1000000» es menor que
             * «999999». Sin el ancho, además, un folio manual con letras se
             * colaría en un rango que se pidió en números.
             */
            w.append(" AND LENGTH(pac.folio) = ").append(CriteriosMuestra.ANCHO_FOLIO)
             .append(" AND pac.folio >= :folioDesde AND pac.folio <= :folioHasta");
        }

        return w;
    }

    private static String recoleccionYTipo(String alias, String aliasTipo, CriteriosMuestra c) {
        List<String> partes = new ArrayList<>();
        if (c.desde() != null) {
            partes.add(alias + ".fechaRecoleccion >= :desde");
        }
        if (c.hasta() != null) {
            partes.add(alias + ".fechaRecoleccion <= :hasta");
        }
        if (c.hayTipos()) {
            partes.add("LOWER(" + aliasTipo + ".nombre) IN :tipos");
        }
        return String.join(" AND ", partes);
    }

    /**
     * Enlaza exactamente los parámetros que las cláusulas construidas mencionan.
     *
     * <p>Visible para las pruebas: que este método y {@link #condiciones} dejen
     * de coincidir es el fallo más fácil de introducir aquí, y no se nota hasta
     * que alguien abre la pantalla.</p>
     */
    static void enlazar(Query q, CriteriosMuestra c) {
        q.setParameter("idInst", c.idInstitucion());
        if (c.hayBusqueda()) {
            q.setParameter("busqueda", "%" + c.busqueda() + "%");
        }
        if (c.desde() != null) {
            q.setParameter("desde", c.desde());
        }
        if (c.hasta() != null) {
            q.setParameter("hasta", c.hasta());
        }
        if (c.hayTipos()) {
            q.setParameter("tipos", c.tipos());
        }
        if (c.sexo() != null) {
            q.setParameter("sexo", c.sexo());
        }
        if (c.folioDesde() != null) {
            q.setParameter("folioDesde", c.folioDesde());
            q.setParameter("folioHasta", c.folioHasta());
        }
    }

    // ── Operaciones ──────────────────────────────────────────────────────────

    /**
     * El texto de la consulta de una página.
     *
     * <p>Separado de su ejecución para poder comprobarlo sin base de datos: el
     * JPQL que se arma en tiempo de ejecución no lo revisa nadie al compilar, y
     * el error típico —una cláusula que menciona un parámetro que luego no se
     * enlaza— solo saldría a la luz al abrir la pantalla.</p>
     */
    static String jpqlPagina(CriteriosMuestra criterios, boolean conCursor, boolean haciaAtras) {
        StringBuilder jpql = new StringBuilder("SELECT m").append(UNIONES).append(condiciones(criterios));

        if (conCursor) {
            // Comparación estricta sobre el par (fecha, id): la fila del cursor ya
            // se entregó, y el id desempata cuando dos muestras comparten el
            // instante de registro —lo normal al crear un lote de alícuotas—.
            String comparador = haciaAtras ? ">" : "<";
            jpql.append(" AND (m.fechaRegistro ").append(comparador).append(" :curFecha")
                .append(" OR (m.fechaRegistro = :curFecha AND m.id ").append(comparador).append(" :curId))");
        }

        // Hacia atrás se consulta en orden inverso para que el LIMIT recorte por
        // el lado correcto; la lista se voltea antes de devolverla, de modo que
        // quien la recibe siempre la ve de la más reciente a la más antigua.
        jpql.append(haciaAtras
                ? " ORDER BY m.fechaRegistro ASC, m.id ASC"
                : " ORDER BY m.fechaRegistro DESC, m.id DESC");

        return jpql.toString();
    }

    static String jpqlConteo(CriteriosMuestra criterios) {
        return "SELECT COUNT(m)" + UNIONES + condiciones(criterios);
    }

    static String jpqlHuerfanas(CriteriosMuestra criterios) {
        return "SELECT COUNT(m)" + UNIONES + condiciones(sinOcultamiento(criterios))
                + " AND pad IS NOT NULL AND inst.id <> :idInst AND instAct.id <> :idInst";
    }

    static String jpqlAlicuotas(CriteriosMuestra criterios) {
        return "SELECT a FROM Muestra a WHERE a.muestraPadre.id IN :idsPadre AND "
                + visible("a", criterios.incluirHistorico())
                + " ORDER BY a.muestraPadre.id ASC, a.numeroAlicuota ASC, a.id ASC";
    }

    /**
     * Los mismos criterios con el ocultamiento apagado.
     *
     * <p>El botón que revela las alícuotas huérfanas devueltas debe decir
     * cuántas aparecerían si se pulsa; contarlas con el filtro puesto daría
     * siempre cero.</p>
     */
    private static CriteriosMuestra sinOcultamiento(CriteriosMuestra c) {
        return new CriteriosMuestra(
                c.idInstitucion(), c.incluirHistorico(), false,
                c.busqueda(), c.desde(), c.hasta(), c.tipos(), c.sexo(),
                c.folioDesde(), c.folioHasta());
    }

    @Override
    public List<Muestra> buscarPagina(CriteriosMuestra criterios, CursorMuestra cursor,
                                      boolean haciaAtras, int limite) {
        TypedQuery<Muestra> q = em.createQuery(
                jpqlPagina(criterios, cursor != null, haciaAtras), Muestra.class);
        enlazar(q, criterios);
        if (cursor != null) {
            q.setParameter("curFecha", new Timestamp(cursor.milisegundos()));
            q.setParameter("curId", cursor.id());
        }
        q.setMaxResults(limite);

        List<Muestra> filas = new ArrayList<>(q.getResultList());
        if (haciaAtras) {
            Collections.reverse(filas);
        }
        return filas;
    }

    @Override
    public long contarPagina(CriteriosMuestra criterios) {
        Query q = em.createQuery(jpqlConteo(criterios));
        enlazar(q, criterios);
        return (Long) q.getSingleResult();
    }

    @Override
    public long contarHuerfanasDevueltas(CriteriosMuestra criterios) {
        Query q = em.createQuery(jpqlHuerfanas(criterios));
        enlazar(q, sinOcultamiento(criterios));
        return (Long) q.getSingleResult();
    }

    @Override
    public List<Muestra> buscarAlicuotasDe(List<Long> idsPadre, CriteriosMuestra criterios) {
        if (idsPadre == null || idsPadre.isEmpty()) {
            return List.of();
        }
        /*
         * Sin los filtros de la pantalla: una vez que la tarjeta padre entra en
         * la página, se despliega completa. Esconder parte del lote porque una
         * alícuota no cumple el filtro haría que el contador de la tarjeta y lo
         * que se ve al desplegarla dijeran cosas distintas.
         */
        TypedQuery<Muestra> q = em.createQuery(jpqlAlicuotas(criterios), Muestra.class);
        q.setParameter("idsPadre", idsPadre);
        q.setParameter("idInst", criterios.idInstitucion());
        return q.getResultList();
    }
}
