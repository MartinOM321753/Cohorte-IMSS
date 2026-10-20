package imss.gob.mx.cohorte.modules.cuestionarios.discrepancias;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiscrepanciaLecturaRepository extends JpaRepository<DiscrepanciaLectura, Long> {

    List<DiscrepanciaLectura> findAllByCuestionario_IdOrderByIdAsc(Long idCuestionario);

    List<DiscrepanciaLectura> findAllByCuestionario_IdAndResueltaFalseOrderByIdAsc(Long idCuestionario);

    long countByCuestionario_IdAndResueltaFalse(Long idCuestionario);

    Page<DiscrepanciaLectura> findAllByResueltaFalseOrderByIdDesc(Pageable pageable);

    /**
     * Qué variables acumulan más desacuerdos de lectura.
     *
     * <p>No es una curiosidad: es el diagnóstico del instrumento en papel. Una
     * variable que concentra discrepancias suele tener un problema de diseño —las
     * opciones repartidas en dos columnas, las burbujas muy juntas— y eso se
     * arregla en la hoja, no en la base. En los volcados recibidos la más afectada
     * es {@code P6}, que es también la que tenía el orden de opciones equivocado en
     * el catálogo.</p>
     */
    @Query("""
           SELECT d.columnaVolcado, COUNT(d) FROM DiscrepanciaLectura d
           WHERE d.cuestionario.tipoCuestionario.id = :idTipo
           GROUP BY d.columnaVolcado
           ORDER BY COUNT(d) DESC
           """)
    List<Object[]> variablesMasAfectadas(@Param("idTipo") Long idTipo);

    void deleteAllByCuestionario_Id(Long idCuestionario);
}
