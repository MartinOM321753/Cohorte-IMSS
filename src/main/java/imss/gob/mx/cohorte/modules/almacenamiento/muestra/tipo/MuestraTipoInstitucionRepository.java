package imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MuestraTipoInstitucionRepository extends JpaRepository<MuestraTipoInstitucion, Long> {

    Optional<MuestraTipoInstitucion> findByMuestra_IdAndInstitucion_Id(Long idMuestra, Long idInstitucion);

    List<MuestraTipoInstitucion> findAllByMuestra_Id(Long idMuestra);

    void deleteAllByMuestra_Id(Long idMuestra);

    /** Registros que referencian un tubo. Bloquean su eliminación. */
    long countByTuboMuestra_Id(Long idTuboMuestra);
}
