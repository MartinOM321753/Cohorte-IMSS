package imss.gob.mx.cohorte.controllers.estudios.dto;
import lombok.*;

import java.time.LocalDateTime;
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EstudioListRequestDTO {

    private Long id;
    private LocalDateTime fechaEstudio;
    private String paciente;
    private String pacienteuuid;
    private String usuarioRealiza;
    private String usuarioRealizauuid;
    private String tipoEstudio;
    private Long tipoEstudioid;
    private Integer cantidadResultados;
    private Integer cantidadAdjuntos;





}
