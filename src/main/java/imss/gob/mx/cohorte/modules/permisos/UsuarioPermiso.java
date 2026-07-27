package imss.gob.mx.cohorte.modules.permisos;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario_permiso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPermiso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario_permiso")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private BeanUser usuario;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_permiso", nullable = false)
    private Permiso permiso;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPermisoIndividual tipo;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "fecha_inicio")
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @ManyToOne
    @JoinColumn(name = "id_otorgado_por")
    private BeanUser otorgadoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @PrePersist
    public void prePersist() {
        if (this.fechaCreacion == null) {
            this.fechaCreacion = LocalDateTime.now();
        }
        if (this.fechaInicio == null) {
            this.fechaInicio = LocalDateTime.now();
        }
    }
}
