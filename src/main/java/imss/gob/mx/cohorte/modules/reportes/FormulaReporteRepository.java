package imss.gob.mx.cohorte.modules.reportes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Toda consulta lleva la institución, igual que en el resto del sistema: una fórmula
 * pertenece al catálogo de quien la creó, y buscar por id a secas es lo que permite
 * leer lo de otra institución probando números.
 */
@Repository
public interface FormulaReporteRepository extends JpaRepository<FormulaReporte, Long> {

    List<FormulaReporte> findAllByInstitucion_IdOrderByNombreAsc(Long idInstitucion);

    List<FormulaReporte> findAllByInstitucion_IdAndActivoTrueOrderByNombreAsc(Long idInstitucion);

    Optional<FormulaReporte> findByNombreIgnoreCaseAndInstitucion_Id(String nombre, Long idInstitucion);
}
