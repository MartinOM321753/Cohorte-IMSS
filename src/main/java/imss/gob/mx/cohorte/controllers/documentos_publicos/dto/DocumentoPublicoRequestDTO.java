package imss.gob.mx.cohorte.controllers.documentos_publicos.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter @Setter
public class DocumentoPublicoRequestDTO {
    private String nombreMostrar;
    @NotNull(message = "La fecha de publicación es obligatoria")
    private LocalDate fechaPublicacion;
    private String fase;
    private String descripcion;
    private Long categoriaId;
    private Long seccionId;
    private String autor;
}
