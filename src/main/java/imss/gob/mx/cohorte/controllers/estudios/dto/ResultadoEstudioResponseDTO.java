package imss.gob.mx.cohorte.controllers.estudios.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoEstudioResponseDTO {

    private Long id;
    private Double valorNumerico;
    private String valorTexto;
    private String parametro;
}
