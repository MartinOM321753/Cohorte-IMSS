package imss.gob.mx.cohorte.modules.cuestionarios.variables;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VariableCuestionarioRepository extends JpaRepository<VariableCuestionario, Long> {

    /**
     * Todas las variables del instrumento, con sus opciones, de dos consultas.
     *
     * <p>Es lo que carga el importador antes de recorrer el archivo. Sin el
     * {@code JOIN FETCH} serían dos mil consultas adicionales —una por variable al
     * tocar sus opciones—, y con ellas la importación de un archivo pasa de
     * segundos a minutos. El {@code DISTINCT} hace falta porque la unión multiplica
     * la variable por cada opción.</p>
     *
     * <h3>El bloque también se trae, y no es un adorno</h3>
     *
     * <p>El motor de validaciones identifica un bloque por su nombre
     * ({@code v.getSeccion().getNombre()}), así que necesita la sección de verdad y no
     * un proxy. Dentro de una petición eso funcionaba igual con o sin el fetch —la
     * sesión sigue abierta hasta que se contesta— pero la carga de un volcado corre en
     * un hilo sin sesión abierta: ahí el proxy ya no se puede inicializar y cada
     * participante se caía con un «no session» que no tenía nada que ver con su hoja.
     * Pasó, y se llevó siete de ocho participantes de la primera carga buena.</p>
     */
    @Query("""
           SELECT DISTINCT v FROM VariableCuestionario v
           LEFT JOIN FETCH v.opciones
           LEFT JOIN FETCH v.seccion
           WHERE v.tipoCuestionario.id = :idTipo
           ORDER BY v.orden ASC, v.id ASC
           """)
    List<VariableCuestionario> findTodasConOpciones(@Param("idTipo") Long idTipo);

    /** Las variables de una sección, con opciones: lo que pinta una página del formulario. */
    @Query("""
           SELECT DISTINCT v FROM VariableCuestionario v
           LEFT JOIN FETCH v.opciones
           WHERE v.seccion.id = :idSeccion AND v.activa = true
           ORDER BY v.orden ASC, v.id ASC
           """)
    List<VariableCuestionario> findDeSeccionConOpciones(@Param("idSeccion") Long idSeccion);

    /**
     * El último orden ocupado en un bloque, o -1 si está vacío.
     *
     * <p>Cuenta también las retiradas de uso: siguen teniendo su sitio en la hoja, y
     * reutilizar su número dejaría dos preguntas peleándose por la misma posición.</p>
     */
    @Query("SELECT COALESCE(MAX(v.orden), -1) FROM VariableCuestionario v "
            + "WHERE v.seccion.id = :idSeccion")
    int ultimoOrdenDe(@Param("idSeccion") Long idSeccion);

    List<VariableCuestionario> findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(Long idTipo);

    Page<VariableCuestionario> findAllByTipoCuestionario_Id(Long idTipo, Pageable pageable);

    Optional<VariableCuestionario> findByTipoCuestionario_IdAndCodigoIgnoreCase(
            Long idTipo, String codigo);

    Optional<VariableCuestionario> findByTipoCuestionario_IdAndColumnaNormalizada(
            Long idTipo, String columnaNormalizada);

    List<VariableCuestionario> findAllByTipoCuestionario_IdAndGrupoMultiple(
            Long idTipo, String grupoMultiple);

    /** Las que el volcado produce y ningún catálogo documenta. Es una bandeja de trabajo. */
    List<VariableCuestionario> findAllByTipoCuestionario_IdAndDocumentadaFalseOrderByOrdenAsc(Long idTipo);

    /**
     * Las preguntas que cuelgan de alguna de estas llaves.
     *
     * <p>Lo usa el autoguardado para borrar lo contestado en preguntas que acaban de dejar
     * de aplicar. Recibe un conjunto y no un codigo porque un solo guardado puede tocar
     * varias llaves a la vez, y una consulta por cada una multiplicaria los viajes a la
     * base en el camino mas sensible al tiempo de todo el modulo.</p>
     */
    /**
     * Las preguntas que dependen de alguna llave.
     *
     * <p>Es lo único que hace falta para seguir las cadenas de saltos —la 34 depende
     * de la 33, que depende de la 30—: una llave que no aparezca aquí es que no
     * depende de nadie, y ahí se acaba la cadena. Son unas seiscientas de dos mil, así
     * que sale mucho más barato que traer el catálogo entero en cada pantalla.</p>
     */
    List<VariableCuestionario> findAllByTipoCuestionario_IdAndDependeDeIsNotNull(Long idTipo);

    List<VariableCuestionario> findAllByTipoCuestionario_IdAndDependeDeIn(
            Long idTipo, java.util.Collection<String> codigosLlave);

    long countByTipoCuestionario_Id(Long idTipo);

    long countByTipoCuestionario_IdAndActivaTrue(Long idTipo);

    @Query("SELECT COUNT(v) FROM VariableCuestionario v WHERE v.tipoCuestionario.id = :idTipo AND v.documentada = false")
    long contarSinDocumentar(@Param("idTipo") Long idTipo);

    /**
     * Búsqueda en el catálogo por código o por enunciado.
     *
     * <p>Paginada porque sin paginar devolvería el instrumento completo, y es la
     * pantalla que el investigador va a usar para encontrar «la pregunta del
     * tabaco» sin saberse el código de memoria.</p>
     */
    @Query("""
           SELECT v FROM VariableCuestionario v
           WHERE v.tipoCuestionario.id = :idTipo
             AND (LOWER(v.codigo) LIKE LOWER(CONCAT('%', :buscar, '%'))
               OR LOWER(v.pregunta) LIKE LOWER(CONCAT('%', :buscar, '%')))
           ORDER BY v.orden ASC, v.id ASC
           """)
    Page<VariableCuestionario> buscar(@Param("idTipo") Long idTipo,
                                      @Param("buscar") String buscar,
                                      Pageable pageable);

    void deleteAllByTipoCuestionario_Id(Long idTipo);
}
