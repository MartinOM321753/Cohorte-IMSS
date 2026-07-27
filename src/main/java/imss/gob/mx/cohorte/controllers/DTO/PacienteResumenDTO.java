package imss.gob.mx.cohorte.controllers.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PacienteResumenDTO {
    private Long id;
    private String uuid;
    private String folio;
    private String nombreCompleto;
    private String sexo;
    /**
     * Estado del participante. Cuando es {@code false}, sus muestras están en
     * cuarentena: siguen visibles pero las mutaciones (prestar, alicuotar, estudios
     * nuevos) están bloqueadas. La UI usa este flag para mostrar un indicador visual.
     */
    private Boolean activo;
}
