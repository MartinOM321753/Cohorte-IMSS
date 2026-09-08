package imss.gob.mx.cohorte.modules.reportes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FormulaReporteHistorialRepository extends JpaRepository<FormulaReporteHistorial, Long> {

    List<FormulaReporteHistorial> findAllByIdFormulaOrderByVersionDesc(Long idFormula);

    /** Qué decía esa fórmula en esa versión, que es la pregunta que se hace años después. */
    Optional<FormulaReporteHistorial> findByIdFormulaAndVersion(Long idFormula, Integer version);
}
