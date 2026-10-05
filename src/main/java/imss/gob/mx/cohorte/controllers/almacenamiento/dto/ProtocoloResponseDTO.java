package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProtocoloResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private Boolean activo;
    private Long idTipoOrigen;
    private String nombreTipoOrigen;
    private List<TuboProtocoloResponseDTO> tubos;
}
