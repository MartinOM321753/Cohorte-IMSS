package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class MuestraRequestDTO {

    @Size(max = 100, message = "Etiqueta máximo 100 caracteres")
    private String etiqueta;

    private Double valor;

    @Size(max = 50, message = "Unidad máximo 50 caracteres")
    private String unidad;

    private LocalDateTime fechaRecoleccion;

    @Size(max = 200, message = "Observaciones máximo 200 caracteres")
    private String observaciones;

    private String pacienteUUID;

    private String usuarioRecolectaUUID;

    private Long idPosicionCaja;

    private Long idTipoMuestra;
    private Long idTuboMuestra;
}
