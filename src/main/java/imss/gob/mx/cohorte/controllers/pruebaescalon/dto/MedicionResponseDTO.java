package imss.gob.mx.cohorte.controllers.pruebaescalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MedicionResponseDTO {

    private Long id;
    private String parametro;
    private Double valor;
    private String unidad;
}
