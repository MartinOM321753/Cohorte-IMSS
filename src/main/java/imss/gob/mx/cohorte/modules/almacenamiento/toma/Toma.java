package imss.gob.mx.cohorte.modules.almacenamiento.toma;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Registro inmutable de la recolección de una muestra biológica a un participante.
 *
 * <p>Una {@code Toma} captura el hecho de que a un participante se le extrajo
 * un tipo de muestra específico, en una fecha y una institución de origen.
 * Es prueba de origen: nunca se modifica ni se borra.</p>
 *
 * <p>Sangre y heces del mismo día son dos tomas distintas (distinto
 * {@code tipoMuestra}). El mismo participante en otra fecha genera otra toma.</p>
 */
@Entity
@Table(name = "toma",
       uniqueConstraints = @UniqueConstraint(
           name = "uk_toma_paciente_tipo_fecha_inst",
           columnNames = {"id_paciente", "id_tipo_muestra", "fecha_recoleccion", "id_institucion_origen"}),
       indexes = @Index(name = "idx_toma_paciente", columnList = "id_paciente"))
@Getter
@Setter
@NoArgsConstructor
public class Toma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_toma")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_muestra", nullable = false)
    private TipoMuestra tipoMuestra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_origen", nullable = false)
    private Institucion institucionOrigen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_recolecta")
    private BeanUser usuarioRecolecta;

    @Column(name = "fecha_recoleccion", nullable = false)
    private LocalDateTime fechaRecoleccion;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;
}
