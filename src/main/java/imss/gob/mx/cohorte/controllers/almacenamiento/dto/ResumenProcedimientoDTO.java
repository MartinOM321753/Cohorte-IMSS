package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenProcedimientoDTO {
    private String uuidPaciente;
    private String folioPaciente;
    private String nombrePaciente;
    private Boolean pacienteActivo;
    private Long idToma;
    private String nombreTipo;
    private Long idProtocolo;
    private int tubos;
    private int alicuotas;
    private int almacenadas;
    private int pendientes;
}
