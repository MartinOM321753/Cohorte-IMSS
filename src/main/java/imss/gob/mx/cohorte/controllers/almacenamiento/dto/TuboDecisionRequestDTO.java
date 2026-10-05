package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TuboDecisionRequestDTO {

    @NotNull(message = "El id del tubo del protocolo es obligatorio")
    private Long idTuboProtocolo;

    /** Si este tubo se procesa en esta tanda. Null = sí. */
    private Boolean incluir;

    /** Acción a ejecutar; null = la del protocolo. */
    private AccionTubo accion;

    /** Volumen extraído que entra a la muestra padre. */
    private Double volumen;

    /** Unidad del volumen (GUARDAR/ESTUDIO); en ALICUOTAR manda el tubo. */
    private String unidad;

    /** Reparto explícito de las alícuotas; vacío/null = automático. */
    private List<Double> planVolumenes;
}
