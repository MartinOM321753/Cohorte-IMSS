package imss.gob.mx.cohorte.controllers.estudios.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TipoEstudioResponseDTO {

    private Long id;
    private String nombre;
    private String descripcion;
    private String tipoCapturaDefecto;
    private Boolean activo;
    private Boolean tieneResultados;
    /** Parámetros serializados como DTO (incluye opciones para TEXTO_OPCIONES). */
    private List<ParametroEstudioResponseDTO> parametroEstudios;
}
