package imss.gob.mx.cohorte.modules.documentos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "documento_access_token")
@Getter
@Setter
@NoArgsConstructor
public class DocumentoAccessToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "id_documento", nullable = false)
    private Long idDocumento;

    @Column(name = "id_institucion", nullable = false)
    private Long idInstitucion;

    @Column(name = "usuario_uuid", nullable = false, length = 36)
    private String usuarioUuid;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;
}
