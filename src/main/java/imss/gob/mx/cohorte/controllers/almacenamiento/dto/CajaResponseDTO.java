package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CajaResponseDTO {
    private Long id;
    private String codigoCaja;
    private Integer filas;
    private Integer columnas;
    private String color;
    private String observaciones;
    private Boolean activo;
    private Long idPosicionPiso;
    private String ubicacionPiso;
    private String ordenLlenado;
    private Double temperaturaRefrigerador;
    private Long idTipoMuestraRecomendado;
    private String nombreTipoMuestraRecomendado;
    private Integer totalPosiciones;
    private Integer posicionesLibres;
}
