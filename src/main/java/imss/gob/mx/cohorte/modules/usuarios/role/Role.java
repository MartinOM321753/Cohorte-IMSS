package imss.gob.mx.cohorte.modules.usuarios.role;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "rol")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_rol")
    private Long Id;

    @Column(name = "uuid", unique = true, length = 36)
    private String uuid;

    @Column(name = "role", nullable = false, unique = true, length = 50)
    private String role;

    /** Genera el UUID antes de la primera persistencia si aún no tiene uno. */
    @PrePersist
    public void prePersist() {
        if (this.uuid == null || this.uuid.isBlank()) {
            this.uuid = UUID.randomUUID().toString();
        }
    }
}
