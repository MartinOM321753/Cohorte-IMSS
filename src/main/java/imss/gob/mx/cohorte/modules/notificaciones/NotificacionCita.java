package imss.gob.mx.cohorte.modules.notificaciones;

import imss.gob.mx.cohorte.modules.cita.Cita;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Registro de cada notificación enviada (o fallida) para una cita.
 * Un registro por cita × tipo × canal.
 * Permite al scheduler saber qué ya fue enviado exitosamente.
 */
@Entity
@Table(name = "notificacion_cita")
@Getter
@Setter
@NoArgsConstructor
public class NotificacionCita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita", nullable = false)
    private Cita cita;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoNotificacion tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal", nullable = false, length = 20)
    private CanalNotificacion canal;

    @Column(name = "exitoso", nullable = false)
    private boolean exitoso;

    /** Mensaje de error si exitoso=false */
    @Column(name = "error", length = 500)
    private String error;

    @Column(name = "enviado_at")
    private Instant enviadoAt;

    // ── Factory methods ───────────────────────────────────────────────

    public static NotificacionCita exitosa(Cita cita, TipoNotificacion tipo, CanalNotificacion canal) {
        NotificacionCita n = new NotificacionCita();
        n.cita      = cita;
        n.tipo      = tipo;
        n.canal     = canal;
        n.exitoso   = true;
        n.enviadoAt = Instant.now();
        return n;
    }

    public static NotificacionCita fallida(Cita cita, TipoNotificacion tipo, CanalNotificacion canal, String error) {
        NotificacionCita n = new NotificacionCita();
        n.cita      = cita;
        n.tipo      = tipo;
        n.canal     = canal;
        n.exitoso   = false;
        n.error     = error != null && error.length() > 500 ? error.substring(0, 500) : error;
        n.enviadoAt = Instant.now();
        return n;
    }
}
