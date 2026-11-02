package imss.gob.mx.cohorte.controllers.analisis.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Petición de dispersión temporal: por cada corte de seguimiento (bucket de la
 * fecha según la granularidad) se devuelven los valores de la métrica y la media
 * del corte, para dibujar una nube de puntos con su línea de tendencia.
 */
@Data
public class DispersionTemporalRequestDTO {

    @NotBlank(message = "Falta la métrica")
    private String metrica;

    /** Dimensión opcional para separar en series: {@code sexo} | {@code institucion} | {@code rango_edad}. */
    private String grupo;

    /** Granularidad del corte: {@code MES} | {@code TRIMESTRE} | {@code ANIO}. */
    private String granularidad = "MES";

    private AgregadoRequestDTO.Filtros filtros = new AgregadoRequestDTO.Filtros();
}
