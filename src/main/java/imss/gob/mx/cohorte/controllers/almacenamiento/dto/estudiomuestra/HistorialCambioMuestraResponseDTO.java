package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HistorialCambioMuestraResponseDTO {

    private Long id;
    private String campo;
    private String valorAnterior;
    private String valorNuevo;
    private String usuario;
    private LocalDateTime fechaCambio;
    private String motivo;
}
