package imss.gob.mx.cohorte.modules.institucion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Autorizacion para que una institucion registre participantes a nombre de otra
 * dentro de su grupo: la institucion padre y las hermanas que cuelgan de ese
 * mismo padre.
 *
 * <p>Es distinto de {@link PermisoAccesoPacientes}, que decide quien PUEDE VER
 * los pacientes de otra sede. Aqui se decide quien puede DARLOS DE ALTA a nombre
 * de otra. Se mantienen separados a proposito: activar uno no debe conceder el
 * otro, porque poder registrar para una sede no implica poder leer su padron.</p>
 *
 * <p>Solo la institucion padre otorga; la validacion de quien puede hacerlo vive
 * en {@code InstitucionRegistroService}, no aqui.</p>
 */
@Entity
@Table(name = "permiso_registro_participantes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_permiso_registro_otorga_recibe",
        columnNames = {"id_institucion_otorga", "id_institucion_recibe"}
    ))
@Getter
@Setter
@NoArgsConstructor
public class PermisoRegistroParticipantes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso_registro")
    private Long id;

    /** Institucion padre que concede la autorizacion. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_otorga", nullable = false)
    private Institucion institucionOtorga;

    /** Institucion hija que queda habilitada para registrar dentro del grupo. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_recibe", nullable = false)
    private Institucion institucionRecibe;

    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado = true;

    @Column(name = "fecha_otorgamiento", nullable = false, updatable = false)
    private Timestamp fechaOtorgamiento;

    @PrePersist
    public void prePersist() {
        this.fechaOtorgamiento = new Timestamp(System.currentTimeMillis());
    }
}
