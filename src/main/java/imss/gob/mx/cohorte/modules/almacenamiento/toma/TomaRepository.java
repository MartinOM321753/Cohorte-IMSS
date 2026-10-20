package imss.gob.mx.cohorte.modules.almacenamiento.toma;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TomaRepository extends JpaRepository<Toma, Long> {

    Optional<Toma> findByPaciente_IdAndTipoMuestra_IdAndFechaRecoleccionAndInstitucionOrigen_Id(
            Long idPaciente, Long idTipoMuestra, LocalDateTime fechaRecoleccion, Long idInstitucion);

    Optional<Toma> findFirstByPaciente_IdAndTipoMuestra_IdAndFechaRecoleccion(
            Long idPaciente, Long idTipoMuestra, LocalDateTime fechaRecoleccion);

    List<Toma> findAllByPaciente_IdOrderByFechaRecoleccionDesc(Long idPaciente);

    List<Toma> findAllByPaciente_IdAndInstitucionOrigen_IdOrderByFechaRecoleccionDesc(
            Long idPaciente, Long idInstitucion);
}
