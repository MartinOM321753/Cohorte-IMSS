package imss.gob.mx.cohorte.modules.institucion;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * Institución de reclutamiento / sede ("sucursal"). Cada institución puede
 * reclutar sus propios participantes y administra de forma independiente sus
 * módulos (BIOBANCO, EXAMENES, ESTUDIOS_MEDICOS, etc.) mediante permisos
 * otorgados por una institución "padre".
 *
 * Self-FK `institucionPadre`: la raíz del árbol de instituciones es la única
 * con permisos para otorgar/revocar acceso a módulos de sus instituciones hijas
 * (ver {@link InstitucionModulo}).
 */
@Entity
@Table(name = "Institucion",
    uniqueConstraints = @UniqueConstraint(name = "uk_institucion_uuid", columnNames = {"uuid"})
)
@Getter @Setter
@NoArgsConstructor
public class Institucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_institucion")
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo_institucion", nullable = false)
    private TipoInstitucion tipoInstitucion;

    /** Institución padre — null indica que esta institución es la raíz del árbol. */
    @ManyToOne
    @JoinColumn(name = "id_institucion_padre", nullable = true)
    private Institucion institucionPadre;

    @Column(name = "latitud")
    private Double latitud;

    @Column(name = "longitud")
    private Double longitud;

    @Column(name = "estado", nullable = false, length = 60)
    private String estado;

    @Column(name = "ciudad", nullable = false, length = 60)
    private String ciudad;

    @Column(name = "direccion", length = 200)
    private String direccion;

    @Column(name = "responsable", length = 100)
    private String responsable;

    @Column(name = "telefono", length = 20)
    private String telefono;

    /** Usuario del sistema con rol ENCARGADO asignado a esta institución (opcional). */
    @ManyToOne
    @JoinColumn(name = "id_encargado", nullable = true)
    private BeanUser encargado;

    @Column(name = "tiene_biobanco", nullable = false)
    private Boolean tieneBiobanco = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;
}
