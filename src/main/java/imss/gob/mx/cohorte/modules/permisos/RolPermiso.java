package imss.gob.mx.cohorte.modules.permisos;

import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "rol_permiso",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_rol", "id_permiso"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RolPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol_permiso")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_rol", nullable = false)
    private Role rol;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_permiso", nullable = false)
    private Permiso permiso;
}
