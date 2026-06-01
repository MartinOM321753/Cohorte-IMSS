package imss.gob.mx.cohorte.modules.auth;

import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Token de un solo uso para el flujo de recuperación de contraseña.
 * Expira a los 15 minutos de su creación.
 */
@Entity
@Table(name = "password_reset_token")
@Getter
@Setter
@NoArgsConstructor
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID aleatorio que viaja en el enlace de correo. */
    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private BeanUser usuario;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "usado", nullable = false)
    private boolean usado = false;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private Instant creadoEn;

    @PrePersist
    public void prePersist() {
        this.creadoEn = Instant.now();
    }

    public boolean isExpirado() {
        return Instant.now().isAfter(expiraEn);
    }
}
