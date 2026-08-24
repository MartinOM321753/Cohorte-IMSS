package imss.gob.mx.cohorte.modules.examenes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AliasExamenRepository extends JpaRepository<AliasExamen, Long> {

    List<AliasExamen> findAllByExamen_IdOrderByOrdenAsc(Long idExamen);

    /** Todos los alias de la institución, para emparejar las columnas de un archivo. */
    List<AliasExamen> findAllByIdInstitucion(Long idInstitucion);

    Optional<AliasExamen> findByIdInstitucionAndAliasNormalizado(
            Long idInstitucion, String aliasNormalizado);

    void deleteAllByExamen_Id(Long idExamen);
}
