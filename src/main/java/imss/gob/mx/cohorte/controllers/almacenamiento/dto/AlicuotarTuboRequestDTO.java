package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.*;

import java.util.List;

/** Cuerpo opcional para alicuotar un tubo primario (2ª pasada). */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AlicuotarTuboRequestDTO {
    /** Reparto explícito de las alícuotas; vacío/null = automático. */
    private List<Double> planVolumenes;
}
