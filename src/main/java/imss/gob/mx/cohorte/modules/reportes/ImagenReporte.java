package imss.gob.mx.cohorte.modules.reportes;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Una imagen del catálogo de la institución: logos, sellos, membretes.
 *
 * <p>Se sube una vez y sirve para todas las plantillas. Por eso vive aquí y no dentro
 * del diseño: el logo institucional es el mismo en los diez formatos, y llevarlo
 * incrustado en cada uno significaría diez copias que hay que cambiar una por una el
 * día que cambie el escudo.</p>
 *
 * <p>Lo que se guarda es la <b>referencia</b>: los bytes viven en MinIO y el diseño
 * solo apunta con {@code imagen:{id}}. Guardar una URL en el diseño sería el error
 * clásico —las firmadas caducan, y una plantilla que dejó de mostrar el membrete a los
 * sesenta minutos no da ninguna pista de por qué.</p>
 */
@Entity
@Table(name = "imagen_reporte",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_imagen_reporte_nombre_institucion",
                columnNames = {"nombre", "id_institucion"}))
@Getter
@Setter
@NoArgsConstructor
public class ImagenReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen")
    private Long id;

    /** Cómo se llama en la galería. Único por institución, para poder distinguirlas. */
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    /** Dónde están los bytes dentro del bucket. */
    @Column(name = "object_key", nullable = false, length = 300)
    private String objectKey;

    @Column(name = "content_type", nullable = false, length = 80)
    private String contentType;

    @Column(name = "bytes", nullable = false)
    private Long bytes;

    /**
     * Medidas en píxeles, leídas del archivo al subirlo.
     *
     * <p>No son decorativas: con ellas el editor coloca la imagen respetando su
     * proporción en vez de meterla en una caja cuadrada que la deforma, y sirven para
     * avisar de que una imagen enorme solo engorda el PDF sin verse mejor.</p>
     */
    @Column(name = "ancho_px")
    private Integer anchoPx;

    @Column(name = "alto_px")
    private Integer altoPx;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }
}
