package imss.gob.mx.cohorte.modules.solicitud;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name="solicitud_correo_verificacion", indexes={
    @Index(name="idx_solicitud_verificacion_ip_fecha", columnList="ip,created_at"),
    @Index(name="idx_solicitud_verificacion_correo_fecha", columnList="email,created_at"),
    @Index(name="idx_solicitud_verificacion_expira", columnList="expires_at")
})
@Getter @Setter
public class SolicitudCorreoVerificacion {
    @Id @Column(length=43) private String id;
    @Column(nullable=false,length=180) private String email;
    @Column(nullable=false,length=45) private String ip;
    @Column(name="code_hash",nullable=false,length=64) private String codeHash;
    @Column(name="failed_attempts",nullable=false) private int failedAttempts;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Column(name="expires_at",nullable=false) private Instant expiresAt;
    @Column(name="verified_at") private Instant verifiedAt;
    @Column(name="used_at") private Instant usedAt;
    @Version private Long version;
}
