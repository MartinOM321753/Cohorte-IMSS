package imss.gob.mx.cohorte.modules.almacenamiento.almacen;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Entity
@Table(name = "Almacen")
@Getter
@Setter
@NoArgsConstructor
public class Almacen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_almacen")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

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

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /** Usuario del sistema con rol ENCARGADO asignado a este almacén (opcional). */
    @ManyToOne
    @JoinColumn(name = "id_encargado", nullable = true)
    private BeanUser encargado;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;
}
