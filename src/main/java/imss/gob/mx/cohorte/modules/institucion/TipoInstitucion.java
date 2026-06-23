package imss.gob.mx.cohorte.modules.institucion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Catálogo configurable de tipos de institución (IMSS, INSP, HOSPITAL, etc.).
 * Reemplaza el enfoque de enum fijo para permitir agregar nuevas instituciones
 * sin requerir cambios de código (p. ej. nuevas sedes de reclutamiento).
 */
@Entity
@Table(name = "Tipo_Institucion",
    uniqueConstraints = @UniqueConstraint(name = "uk_tipo_institucion_nombre", columnNames = {"nombre"})
)
@Getter @Setter
@NoArgsConstructor
public class TipoInstitucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_institucion")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 60)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
