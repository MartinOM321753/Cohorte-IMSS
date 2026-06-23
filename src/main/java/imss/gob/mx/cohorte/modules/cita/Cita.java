package imss.gob.mx.cohorte.modules.cita;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.Instant;
import java.util.UUID;


@Entity
@Table(name = "Cita")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Long id;

    @Column(name = "uuid", unique = true, length = 36)
    private String uuid;

    @ManyToOne
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "id_usuario_agenda", nullable = false)
    private BeanUser usuarioAgenda;

    /** Institución responsable de la cita — define el ámbito de aislamiento de datos. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @Column(name = "start_at_utc")
    private Instant startAtUtc;

    @Column(name = "end_at_utc")
    private Instant endAtUtc;

    @Column(name = "duracion_minutos")
    private Integer durationMinutes = 60;

    @Column(name = "timezone", length = 50)
    private String timezone;

    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Formato de color hexadecimal inválido")
    @Column(name = "color_hex", length = 7)
    private String colorHex;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cita", length = 20, nullable = false)
    private EstadoCita estadoCita = EstadoCita.Programada;

    @Column(name = "observaciones", length = 250)
    private String observaciones;

    // Auditoría
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "created_at_utc", updatable = false)
    private Instant createdAtUtc;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        if (this.uuid == null) {
            this.uuid = UUID.randomUUID().toString();
        }
        this.createdAtUtc = Instant.now();
        this.updatedAtUtc = Instant.now();

        if (this.startAtUtc != null && this.durationMinutes != null) {
            this.endAtUtc = this.startAtUtc.plusSeconds(this.durationMinutes * 60L);
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAtUtc = Instant.now();

        if (this.startAtUtc != null && this.durationMinutes != null) {
            this.endAtUtc = this.startAtUtc.plusSeconds(this.durationMinutes * 60L);
        }
    }
}
