package imss.gob.mx.cohorte.modules.institucion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermisoRegistroParticipantesRepository extends JpaRepository<PermisoRegistroParticipantes, Long> {

    List<PermisoRegistroParticipantes> findAllByInstitucionRecibe_IdAndHabilitadoTrue(Long idInstitucionRecibe);

    List<PermisoRegistroParticipantes> findAllByInstitucionOtorga_Id(Long idInstitucionOtorga);

    Optional<PermisoRegistroParticipantes> findByInstitucionOtorga_IdAndInstitucionRecibe_Id(
            Long idInstitucionOtorga, Long idInstitucionRecibe);
}
