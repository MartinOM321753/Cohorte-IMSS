package imss.gob.mx.cohorte.modules.almacenamiento.colaborador;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColaboradorRepository extends JpaRepository<Colaborador, Long> {

    List<Colaborador> findAllByInstitucion_IdOrderByFechaRegistroDesc(Long idInstitucion);

    List<Colaborador> findAllByInstitucion_IdAndActivoOrderByPersona_ApellidoPaternoAsc(Long idInstitucion, Boolean activo);

    Optional<Colaborador> findByIdAndInstitucion_Id(Long id, Long idInstitucion);

    /**
     * Búsqueda paginada por nombre/apellidos/CURP/cargo dentro de una institución.
     * Un colaborador puede no tener segundo nombre ni apellido materno (columnas
     * nullable en Persona), por eso el LIKE usa COALESCE para no perder filas.
     */
    @Query("""
            SELECT c FROM Colaborador c
            WHERE c.institucion.id = :idInstitucion
              AND (:buscar IS NULL OR :buscar = ''
                   OR LOWER(CONCAT(c.persona.nombre, ' ',
                                   COALESCE(c.persona.segundoNombre, ''), ' ',
                                   c.persona.apellidoPaterno, ' ',
                                   COALESCE(c.persona.apellidoMaterno, ''), ' ',
                                   COALESCE(c.persona.curp, ''), ' ',
                                   COALESCE(c.cargo, ''))) LIKE LOWER(CONCAT('%', :buscar, '%')))
            ORDER BY c.fechaRegistro DESC
            """)
    Page<Colaborador> buscarPaginado(@Param("idInstitucion") Long idInstitucion,
                                     @Param("buscar") String buscar,
                                     Pageable pageable);
}
