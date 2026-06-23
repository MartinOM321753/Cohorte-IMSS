package imss.gob.mx.cohorte.modules.institucion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermisoAccesoPacientesRepository extends JpaRepository<PermisoAccesoPacientes, Long> {

    List<PermisoAccesoPacientes> findAllByInstitucionRecibe_IdAndHabilitadoTrue(Long idInstitucionRecibe);

    List<PermisoAccesoPacientes> findAllByInstitucionOtorga_Id(Long idInstitucionOtorga);

    Optional<PermisoAccesoPacientes> findByInstitucionOtorga_IdAndInstitucionRecibe_Id(
            Long idInstitucionOtorga, Long idInstitucionRecibe);
}
