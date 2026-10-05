package imss.gob.mx.cohorte.modules.almacenamiento.protocolo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProtocoloRepository extends JpaRepository<Protocolo, Long> {

    List<Protocolo> findAllByInstitucion_IdOrderByNombreAsc(Long idInstitucion);

    List<Protocolo> findAllByInstitucion_IdAndActivoTrueOrderByNombreAsc(Long idInstitucion);

    Optional<Protocolo> findByNombreIgnoreCaseAndInstitucion_Id(String nombre, Long idInstitucion);

    long countByTipoOrigen_Id(Long idTipoMuestra);

    long countByTubos_TipoResultante_Id(Long idTipoMuestra);
}
