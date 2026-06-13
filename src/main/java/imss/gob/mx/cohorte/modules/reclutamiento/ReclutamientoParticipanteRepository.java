package imss.gob.mx.cohorte.modules.reclutamiento;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReclutamientoParticipanteRepository extends JpaRepository<ReclutamientoParticipante, Long> {
    Optional<ReclutamientoParticipante> findByPaciente_Id(Long idPaciente);
    Optional<ReclutamientoParticipante> findByPaciente_Uuid(String uuidPaciente);
    boolean existsByPaciente_Id(Long idPaciente);

    Optional<ReclutamientoParticipante> findByPaciente_IdAndPaciente_Institucion_Id(Long idPaciente, Long idInstitucion);
    Optional<ReclutamientoParticipante> findByPaciente_UuidAndPaciente_Institucion_Id(String uuidPaciente, Long idInstitucion);
}
