package imss.gob.mx.cohorte.controllers.documentos.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoResponseDTO {
    private Long id;
    private String nombreOriginal;
    private String mimeType;
    private Long tamanioBytes;
    private String descripcion;
    private LocalDateTime fechaSubida;
    private String subidoPorUUID;
    /** URL firmada temporal de MinIO para descargar/visualizar el archivo. */
    private String url;
}
