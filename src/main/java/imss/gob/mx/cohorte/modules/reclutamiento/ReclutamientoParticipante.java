package imss.gob.mx.cohorte.modules.reclutamiento;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Clasificación de reclutamiento de un participante (1:1 con {@link Paciente}).
 * Registra si el participante es de retorno (ya colaboró antes y fue
 * recontactado) o nuevo (reclutado desde una institución), junto con el
 * resultado del contacto y la institución/usuario responsables.
 */
@Entity
@Table(name = "Reclutamiento_Participante",
    uniqueConstraints = @UniqueConstraint(name = "uk_reclutamiento_paciente", columnNames = {"id_paciente"})
)
@Getter @Setter
@NoArgsConstructor
public class ReclutamientoParticipante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reclutamiento")
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "id_paciente", nullable = false, unique = true)
    private Paciente paciente;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reclutamiento", nullable = false, length = 20)
    private TipoReclutamiento tipoReclutamiento;

    /** Sólo aplica de forma típica a participantes RETORNO; null mientras no se ha definido respuesta. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_contacto", length = 40)
    private EstadoContacto estadoContacto;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio_contacto", length = 20)
    private MedioContacto medioContacto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_reclutamiento", nullable = false)
    private Institucion institucionReclutamiento;

    @Column(name = "fecha_contacto")
    private Timestamp fechaContacto;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_recluta", nullable = false)
    private BeanUser usuarioRecluta;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;
}
