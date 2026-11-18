package imss.gob.mx.cohorte.modules.configuracion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
    name = "configuracion_clave",
    uniqueConstraints = @UniqueConstraint(columnNames = {"id_institucion", "clave"})
)
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionClave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion_clave")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @Column(name = "clave", nullable = false, length = 50)
    private String clave;

    @Column(name = "valor", columnDefinition = "TEXT")
    private String valor;

    @Column(name = "updated_at_utc")
    private Instant updatedAtUtc;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        updatedAtUtc = Instant.now();
    }
}
