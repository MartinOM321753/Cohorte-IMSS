package imss.gob.mx.cohorte.modules.reportes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Todas las consultas llevan la institución. Es la misma regla que en el resto del
 * sistema: una plantilla es del catálogo de quien la creó, y buscar por id a secas es
 * lo que abre la puerta a leer lo de otra institución pasando un número.
 */
@Repository
public interface PlantillaReporteRepository extends JpaRepository<PlantillaReporte, Long> {

    List<PlantillaReporte> findAllByInstitucion_IdOrderByNombreAsc(Long idInstitucion);

    List<PlantillaReporte> findAllByInstitucion_IdAndActivoTrueOrderByNombreAsc(Long idInstitucion);

    List<PlantillaReporte> findAllByInstitucion_IdAndTipoReporteAndActivoTrueOrderByNombreAsc(
            Long idInstitucion, TipoReporte tipoReporte);

    Optional<PlantillaReporte> findByNombreIgnoreCaseAndInstitucion_Id(String nombre, Long idInstitucion);

    /** La predeterminada de un tipo. Solo debe haber una; de eso responde el servicio. */
    Optional<PlantillaReporte> findByInstitucion_IdAndTipoReporteAndPredeterminadaTrue(
            Long idInstitucion, TipoReporte tipoReporte);

    List<PlantillaReporte> findAllByInstitucion_IdAndTipoReporteAndPredeterminadaTrue(
            Long idInstitucion, TipoReporte tipoReporte);
}
