package imss.gob.mx.cohorte.modules.permisos;

import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_rol",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_usuario", "id_rol"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioRol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_rol")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private BeanUser usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Role rol;

    @Column(name = "fecha_asignacion", nullable = false)
    private LocalDateTime fechaAsignacion;

    @PrePersist
    public void prePersist() {
        if (this.fechaAsignacion == null) {
            this.fechaAsignacion = LocalDateTime.now();
        }
    }
}
