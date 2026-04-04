package imss.gob.mx.cohorte.controllers.pruebaescalon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EtapaResponseDTO {

    private Long id;
    private String etapa;
    private String observaciones;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;
    private MedicionResponseDTO medicion;
}
