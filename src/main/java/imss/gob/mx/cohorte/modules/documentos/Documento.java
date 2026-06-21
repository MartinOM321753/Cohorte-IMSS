package imss.gob.mx.cohorte.modules.documentos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro central de un archivo almacenado en MinIO.
 * Contiene solo metadatos; el contenido real está en el bucket MinIO referenciado por {@code objectKey}.
 */
@Entity
@Table(name = "Documento")
@Getter
@Setter
@NoArgsConstructor
public class Documento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_documento")
    private Long id;

    /** Nombre original del archivo tal como lo subió el usuario. */
    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    /** Ruta del objeto dentro del bucket MinIO, ej. "estudios/12/uuid-archivo.pdf". */
    @Column(name = "object_key", nullable = false, length = 500, unique = true)
    private String objectKey;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "tamanio_bytes")
    private Long tamanioBytes;

    @Column(name = "descripcion", length = 300)
    private String descripcion;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    /** UUID del usuario que subió el archivo. */
    @Column(name = "subido_por_uuid", length = 36)
    private String subidoPorUUID;

    /**
     * Categoría del documento — determina qué roles pueden verlo/subirlo.
     * Se usa en {@link imss.gob.mx.cohorte.services.documentos.DocumentoPermisosConfig}.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_entidad", length = 30, nullable = false)
    private TipoEntidadDocumento tipoEntidad;

    @Column(name = "etiqueta", length = 40, unique = true)
    private String etiqueta;

    @Column(name = "id_institucion")
    private Long idInstitucion;
}
