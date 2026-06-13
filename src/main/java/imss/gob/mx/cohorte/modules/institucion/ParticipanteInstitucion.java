package imss.gob.mx.cohorte.modules.institucion;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Vínculo M:N entre Paciente (participante) e Institución. Un participante
 * puede estar asociado a más de una institución a lo largo del estudio
 * (p. ej. reclutado por una sede y posteriormente seguido por otra).
 * El vínculo "principal"/origen vive en ReclutamientoParticipante; esta
 * tabla registra TODAS las instituciones con las que el participante
 * tiene relación activa.
 */
@Entity
@Table(name = "Participante_Institucion",
    uniqueConstraints = @UniqueConstraint(name = "uk_participante_institucion", columnNames = {"id_paciente", "id_institucion"})
)
@Getter @Setter
@NoArgsConstructor
public class ParticipanteInstitucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_participante_institucion")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @Column(name = "fecha_asignacion", nullable = false)
    private Timestamp fechaAsignacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /** Observaciones libres sobre el motivo del vínculo (traslado, seguimiento conjunto, etc.). */
    @Column(name = "observaciones", length = 250)
    private String observaciones;
}
