package imss.gob.mx.cohorte.controllers.analisis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Petición de correlación entre dos métricas numéricas (panel "nube de
 * posibilidades"). El servidor empareja por participante (folio) el valor de
 * {@code metricaX} contra el de {@code metricaY} dentro del alcance.
 */
@Data
public class CorrelacionRequestDTO {

    @NotBlank(message = "Falta la métrica X")
    private String metricaX;

    @NotBlank(message = "Falta la métrica Y")
    private String metricaY;

    /** Dimensión opcional para colorear la nube: {@code sexo} | {@code institucion} | {@code rango_edad}. */
    private String grupo;

    private AgregadoRequestDTO.Filtros filtros = new AgregadoRequestDTO.Filtros();
}
