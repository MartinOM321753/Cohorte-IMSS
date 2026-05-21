package imss.gob.mx.cohorte.modules.cita;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByUuid(String uuid);

    Optional<Cita> findByPaciente_Folio(String pacienteFolio);

    Optional<Cita> findByPaciente_Uuid(String pacienteUUID);

    @Query("SELECT c FROM Cita c WHERE c.usuarioAgenda.UUID = :usuarioUuid " +
           "AND c.estadoCita <> 'Cancelada' " +
           "AND (:citaUuid IS NULL OR c.uuid <> :citaUuid) " +
           "AND c.startAtUtc < :endAt " +
           "AND (c.endAtUtc IS NULL OR c.endAtUtc > :startAt)")
    List<Cita> findOverlappingCitas(@Param("usuarioUuid") String usuarioUuid,
                                    @Param("startAt") Instant startAt,
                                    @Param("endAt") Instant endAt,
                                    @Param("citaUuid") String citaUuid);

    List<Cita> findByStartAtUtcBetween(Instant start, Instant end);
}
