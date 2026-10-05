package imss.gob.mx.cohorte.controllers.documentos_publicos.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class DocumentoPublicoResponseDTO {
    private Long id;
    private String nombreMostrar;
    private String nombreOriginal;
    private String mimeType;
    private Long tamanioBytes;
    private String descripcion;
    private LocalDate fechaPublicacion;
    private String fase;
    private String autor;
    private Long categoriaId;
    private String categoriaNombre;
    private LocalDateTime fechaCreacion;
    private Boolean activo;
}
