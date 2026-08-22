package imss.gob.mx.cohorte.modules.estudios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EstudioMedicoRepository extends JpaRepository<EstudioMedico, Long> {

    /** Sin filtro de institucion: se usa para saber si un participante ya quedo vinculado a alguna. */
    long countByPaciente_Uuid(String uuid);

    /*
     * Las colecciones resultadoEstudio y adjuntos son LAZY + @BatchSize(30).
     * Hibernate las carga en queries secundarias con IN(...) agrupando hasta 30 IDs
     * por lote, lo que evita tanto el N+1 como el MultipleBagFetchException que
     * causaba el @EntityGraph con dos bags simultáneos.
     */

    List<EstudioMedico> findAllByOrderByFechaEstudioDesc();

    List<EstudioMedico> findAllByInstitucion_IdOrderByFechaEstudioDesc(Long idInstitucion);

    List<EstudioMedico> findAllByPaciente_UuidOrderByFechaEstudioDesc(String uuid);

    List<EstudioMedico> findAllByPaciente_UuidAndInstitucion_IdOrderByFechaEstudioDesc(String uuid, Long idInstitucion);

    /** Variante por conjunto de instituciones: al atender entre sedes, el historial es la union de lo que hizo el grupo. */
    List<EstudioMedico> findAllByPaciente_UuidAndInstitucion_IdInOrderByFechaEstudioDesc(String uuid, java.util.List<Long> idsInstituciones);

    org.springframework.data.domain.Page<EstudioMedico> findAllByPaciente_UuidAndInstitucion_IdInOrderByFechaEstudioDesc(String uuid, java.util.List<Long> idsInstituciones, org.springframework.data.domain.Pageable pageable);

    /**
     * Variantes paginadas: sin fetch de colecciones para no forzar paginación en memoria.
     * El @BatchSize de la entidad acota el N+1 al tamaño de página.
     */
    org.springframework.data.domain.Page<EstudioMedico> findAllByOrderByFechaEstudioDesc(org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<EstudioMedico> findAllByInstitucion_IdOrderByFechaEstudioDesc(Long idInstitucion, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<EstudioMedico> findAllByPaciente_UuidOrderByFechaEstudioDesc(String uuid, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<EstudioMedico> findAllByPaciente_UuidAndInstitucion_IdOrderByFechaEstudioDesc(String uuid, Long idInstitucion, org.springframework.data.domain.Pageable pageable);

    /** Cuenta estudios que tienen al menos un resultado registrado. */
    @Query("SELECT COUNT(DISTINCT e.Id) FROM EstudioMedico e WHERE SIZE(e.resultadoEstudio) > 0")
    long countEstudiosConResultados();

    /** Cuenta estudios con al menos un resultado cuya fechaEstudio cae en el rango [inicio, fin]. */
    @Query("SELECT COUNT(DISTINCT e.Id) FROM EstudioMedico e " +
           "WHERE SIZE(e.resultadoEstudio) > 0 " +
           "AND e.fechaEstudio >= :inicio AND e.fechaEstudio <= :fin")
    long countEstudiosConResultadosEnMes(@Param("inicio") LocalDateTime inicio,
                                         @Param("fin")    LocalDateTime fin);

    /** Variante de countEstudiosConResultadosEnMes acotada a la institución dada (aislamiento de datos). */
    @Query("SELECT COUNT(DISTINCT e.Id) FROM EstudioMedico e " +
           "WHERE SIZE(e.resultadoEstudio) > 0 " +
           "AND e.fechaEstudio >= :inicio AND e.fechaEstudio <= :fin " +
           "AND e.paciente.institucion.id = :idInstitucion")
    long countEstudiosConResultadosEnMes(@Param("inicio") LocalDateTime inicio,
                                         @Param("fin")    LocalDateTime fin,
                                         @Param("idInstitucion") Long idInstitucion);

    // ── Cobertura ────────────────────────────────────────────────────────────

    /** Cuenta pacientes distintos (de la institución dada) con ≥ 1 estudio médico del tipo dado. */
    @Query("SELECT COUNT(DISTINCT e.paciente.Id) FROM EstudioMedico e " +
           "WHERE e.tipoEstudio.Id = :tipoId AND e.paciente.institucion.id = :idInstitucion")
    long countDistinctPacienteByTipoEstudioId(@Param("tipoId") Long tipoId, @Param("idInstitucion") Long idInstitucion);

    /**
     * Por cada paciente activo (de la institución dada) con al menos un estudio, devuelve
     * cuántos tipos de estudio distintos tiene. Devuelve pares [pacienteId, count].
     */
    @Query("SELECT e.paciente.Id, COUNT(DISTINCT e.tipoEstudio.Id) " +
           "FROM EstudioMedico e " +
           "WHERE e.paciente.activo = true AND e.paciente.institucion.id = :idInstitucion " +
           "GROUP BY e.paciente.Id")
    List<Object[]> countDistinctTipoByPacienteActivo(@Param("idInstitucion") Long idInstitucion);

    /**
     * Pacientes activos de la institución dada que NO tienen EstudioMedico con tipoEstudio.id = tipoId.
     */
    @Query("SELECT p.Id FROM Paciente p WHERE p.activo = true AND p.institucion.id = :idInstitucion " +
           "AND p.Id NOT IN (" +
           "  SELECT DISTINCT e.paciente.Id FROM EstudioMedico e WHERE e.tipoEstudio.Id = :tipoId)")
    List<Long> findPacientesActivosSinTipoEstudio(@Param("tipoId") Long tipoId, @Param("idInstitucion") Long idInstitucion);

    /**
     * Pacientes activos de la institución dada cuyo conteo de tipos de estudio distintos = k.
     */
    @Query("SELECT e.paciente.Id FROM EstudioMedico e " +
           "WHERE e.paciente.activo = true AND e.paciente.institucion.id = :idInstitucion " +
           "GROUP BY e.paciente.Id " +
           "HAVING COUNT(DISTINCT e.tipoEstudio.Id) = :k")
    List<Long> findPacientesConExactamenteKEstudios(@Param("k") long k, @Param("idInstitucion") Long idInstitucion);

    /**
     * Tipos de estudio cubiertos (ids) para un paciente.
     */
    @Query("SELECT DISTINCT e.tipoEstudio.Id FROM EstudioMedico e WHERE e.paciente.Id = :pacienteId")
    List<Long> findTiposEstudioCubiertosIdsForPaciente(@Param("pacienteId") Long pacienteId);

    /**
     * Los estudios ya registrados de un tipo para un grupo de participantes.
     *
     * <p>La usa la carga masiva para avisar de lo que ya existe antes de
     * escribir. Se consulta en bloque y no fila a fila porque un archivo trae
     * cientos de filas, y tambien porque el duplicado hay que detectarlo ANTES
     * de empezar a guardar: descubrirlo a mitad dejaria media carga hecha.</p>
     *
     * @return tuplas (idPaciente, fechaEstudio, idEstudio)
     */
    @Query("SELECT e.paciente.id, e.fechaEstudio, e.id FROM EstudioMedico e "
         + "WHERE e.tipoEstudio.id = :idTipo AND e.paciente.id IN :idsPacientes")
    List<Object[]> buscarDeTipoParaPacientes(@Param("idTipo") Long idTipo,
                                             @Param("idsPacientes") List<Long> idsPacientes);

    boolean existsByTipoEstudio_Id(Long id);

    /**
     * ¿Hay algún estudio de este tipo que el consultante tenga derecho a leer?
     *
     * <p>El catálogo de tipos es por institución, así que consultar un estudio ajeno
     * obliga a leer su definición para pintar los parámetros. Filtrar el tipo por el
     * conjunto alcanzable no basta: con la colaboración entre sedes se puede leer un
     * estudio de una institución que no está en ese conjunto —porque el permiso lo
     * da el participante, no la sede— y entonces la consulta rebotaba al pedir los
     * parámetros.</p>
     *
     * <p>Es la misma regla de unión que gobierna la lectura del estudio, aplicada al
     * tipo: se abre la definición solo si existe al menos un estudio de ese tipo que
     * ya se podía abrir.</p>
     */
    @Query("""
        SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END
        FROM EstudioMedico e
        WHERE e.tipoEstudio.Id = :idTipo
          AND (e.institucion.id IN :alcanzables OR e.paciente.institucion.id IN :alcanzables)
    """)
    boolean existeEstudioLegibleDeTipo(@Param("idTipo") Long idTipo,
                                       @Param("alcanzables") List<Long> alcanzables);
}
