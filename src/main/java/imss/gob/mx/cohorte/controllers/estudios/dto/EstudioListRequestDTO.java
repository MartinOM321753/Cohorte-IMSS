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

    /** Sede que realizó el estudio. Puede no ser la del usuario: con la atención entre sedes, el historial de un participante mezcla instituciones. */
    private Long institucionId;
    private String institucionNombre;

    /** false → el participante ya no esta al alcance: el registro se muestra, pero su expediente no se abre. */
    private Boolean pacienteAlcanzable;





}
