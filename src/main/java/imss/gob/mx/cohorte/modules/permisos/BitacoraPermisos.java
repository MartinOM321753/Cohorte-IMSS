package imss.gob.mx.cohorte.modules.permisos;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "bitacora_permisos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BitacoraPermisos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_bitacora_permiso")
    private Long id;

    @Column(name = "usuario_afectado_uuid", length = 36)
    private String usuarioAfectadoUuid;

    @Column(name = "accion", nullable = false, length = 50)
    private String accion;

    @Column(name = "detalle", length = 500)
    private String detalle;

    @Column(name = "realizado_por_uuid", length = 36)
    private String realizadoPorUuid;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() {
        if (this.timestamp == null) {
            this.timestamp = LocalDateTime.now();
        }
    }
}
