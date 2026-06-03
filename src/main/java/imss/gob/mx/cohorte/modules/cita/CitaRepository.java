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

    List<Cita> findAllByPaciente_UuidOrderByStartAtUtcDesc(String pacienteUuid);

    // ── Dashboard ────────────────────────────────────────────────────────────

    /** Cuenta citas en estado Programada o Confirmada dentro del rango de fechas indicado. */
    @Query("SELECT COUNT(c) FROM Cita c " +
           "WHERE c.estadoCita IN ('Programada', 'Confirmada') " +
           "AND c.startAtUtc >= :start AND c.startAtUtc <= :end")
    long countCitasProgramadasEnMes(@Param("start") Instant start,
                                    @Param("end")   Instant end);

    /** Cuenta todas las citas no canceladas en el mes (para la tarjeta "Citas del mes"). */
    @Query("SELECT COUNT(c) FROM Cita c " +
           "WHERE c.estadoCita <> 'Cancelada' " +
           "AND c.startAtUtc >= :start AND c.startAtUtc <= :end")
    long countCitasMes(@Param("start") Instant start,
                       @Param("end")   Instant end);

    /**
     * Cuenta citas cuya hora de fin ya pasó pero aún están en estado Programada o Confirmada.
     * Estas son citas que el usuario olvidó actualizar ("sin actualizar").
     */
    @Query("SELECT COUNT(c) FROM Cita c " +
           "WHERE c.estadoCita IN ('Programada', 'Confirmada') " +
           "AND c.endAtUtc < :now")
    long countCitasSinActualizar(@Param("now") Instant now);

    /**
     * Devuelve todas las citas no canceladas cuyo inicio esté en el rango [start, end),
     * con paciente y persona en fetch eager para evitar N+1.
     * Ordenadas por hora de inicio ascendente.
     */
    @Query("SELECT c FROM Cita c " +
           "LEFT JOIN FETCH c.paciente pac " +
           "LEFT JOIN FETCH pac.persona " +
           "WHERE c.startAtUtc >= :start AND c.startAtUtc < :end " +
           "AND c.estadoCita <> 'Cancelada' " +
           "ORDER BY c.startAtUtc ASC")
    List<Cita> findCitasHoy(@Param("start") Instant start,
                             @Param("end")   Instant end);
}
