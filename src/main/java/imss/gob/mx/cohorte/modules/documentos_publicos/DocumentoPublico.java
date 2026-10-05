package imss.gob.mx.cohorte.modules.documentos_publicos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "documento_publico")
@Getter @Setter
@NoArgsConstructor
public class DocumentoPublico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento_publico")
    private Long id;

    @Column(name = "nombre_mostrar", length = 255)
    private String nombreMostrar;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "object_key", nullable = false, length = 500, unique = true)
    private String objectKey;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDate fechaPublicacion;

    @Column(name = "fase", length = 60)
    private String fase;

    @Column(name = "autor", length = 200)
    private String autor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria")
    private CategoriaDocumentoPublico categoria;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "subido_por_uuid", length = 36)
    private String subidoPorUuid;

    @Column(name = "id_institucion", nullable = false)
    private Long idInstitucion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
