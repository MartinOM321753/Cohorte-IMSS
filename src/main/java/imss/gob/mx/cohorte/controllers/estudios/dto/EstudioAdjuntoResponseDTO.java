package imss.gob.mx.cohorte.controllers.estudios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstudioAdjuntoResponseDTO {

    private Long id;
    private String tipo;
    private String nombreOriginal;
    private String mimeType;
    private String rutaUrl;
    private String descripcion;
    private Integer orden;
}
