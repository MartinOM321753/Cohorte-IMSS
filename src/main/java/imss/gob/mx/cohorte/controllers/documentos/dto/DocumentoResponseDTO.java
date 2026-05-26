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
    /**
     * Indica si el usuario autenticado que recibe esta respuesta tiene permiso
     * para descargar / visualizar el contenido del archivo.
     * El frontend usa este campo para mostrar u ocultar los botones de acción.
     */
    private boolean puedeDescargar;
    /** URL firmada temporal de MinIO para descargar/visualizar el archivo. */
    private String url;
}
