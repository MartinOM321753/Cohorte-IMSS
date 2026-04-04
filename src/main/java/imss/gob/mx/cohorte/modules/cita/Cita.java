package imss.gob.mx.cohorte.modules.cita;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;


@Entity
@Table(name = "Cita")
@Getter
@Setter
@NoArgsConstructor
public class Cita {
    public enum EstadoCita { Programada, Confirmada, Realizada, Cancelada, No_asistió }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cita")
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "id_usuario_agenda", nullable = false)
    private BeanUser usuarioAgenda;

    @Column(name = "fecha_cita", nullable = false)
    private LocalDateTime fechaCita;

    @Column(name = "duracion_minutos", nullable = false)
    private Integer duracionMinutos = 60;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_cita", length = 20, nullable = false, columnDefinition = "ENUM('Programada','Confirmada','Realizada','Cancelada','No asistió')")
    private EstadoCita estadoCita = EstadoCita.Programada;

    @Column(name = "observaciones", length = 250)
    private String observaciones;

    @Column(name = "fecha_registro", nullable = false)
    private Timestamp fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private Timestamp fechaActualizacion;
}
