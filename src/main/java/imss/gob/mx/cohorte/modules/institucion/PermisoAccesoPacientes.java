package imss.gob.mx.cohorte.modules.institucion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "permiso_acceso_pacientes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_permiso_otorga_recibe",
        columnNames = {"id_institucion_otorga", "id_institucion_recibe"}
    ))
@Getter
@Setter
@NoArgsConstructor
public class PermisoAccesoPacientes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso_acceso")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_otorga", nullable = false)
    private Institucion institucionOtorga;

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
