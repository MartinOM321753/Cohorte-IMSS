package imss.gob.mx.cohorte.modules.permisos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permiso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Permiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_permiso")
    private Long id;

    @Column(name = "codigo", nullable = false, unique = true, length = 60)
    private String codigo;

    @Column(name = "modulo", nullable = false, length = 30)
    private String modulo;

    @Column(name = "descripcion", length = 200)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
