package imss.gob.mx.cohorte.modules.analisis;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Vista guardada del analista: una configuración de análisis (métrica, dimensiones
 * y filtros) con nombre, para reabrirla sin rearmarla. Privada del usuario que la
 * creó (el campo {@code usuarioUuid} la aísla); compartir vistas queda como mejora
 * posterior (ver preguntas abiertas del diseño).
 */
@Entity
@Table(name = "vista_analisis",
    uniqueConstraints = @UniqueConstraint(name = "uk_vista_analisis_uuid", columnNames = {"uuid"}))
@Getter
@Setter
@NoArgsConstructor
public class VistaAnalisis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vista")
    private Long id;

    @Column(name = "uuid", nullable = false, updatable = false, length = 36)
    private String uuid = UUID.randomUUID().toString();

    /** Dueño de la vista — su UUID de usuario. La vista solo la ve y edita él. */
    @Column(name = "usuario_uuid", nullable = false, length = 36)
    private String usuarioUuid;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /** La petición de agregación serializada en JSON (métrica + dimensiones + filtros). */
    @Lob
    @Column(name = "configuracion", nullable = false, columnDefinition = "TEXT")
    private String configuracion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    public void prePersist() {
        if (this.uuid == null || this.uuid.isBlank()) this.uuid = UUID.randomUUID().toString();
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    @PreUpdate
    public void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
