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
    /** Tipo de entidad al que pertenece (ESTUDIO, MUESTRA, PACIENTE_CONSENTIMIENTO, PACIENTE_GENERAL). */
    private String tipoEntidad;
    /** Código único imprimible del documento, formato D{YY}-{II}-{T}-{FOLIO}.{EXT} */
    private String etiqueta;
    private boolean puedeDescargar;
    private String url;
}
