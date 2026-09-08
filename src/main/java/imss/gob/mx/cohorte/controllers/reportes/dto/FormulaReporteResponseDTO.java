package imss.gob.mx.cohorte.controllers.reportes.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class FormulaReporteResponseDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private String expresion;
    private List<FormulaReporteRequestDTO.VariableFormulaDTO> variables;
    private String expresionMinimo;
    private String expresionMaximo;
    private String unidadSalida;
    private Integer decimales;
    private Integer version;
    private Boolean activo;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    /**
     * Lo que hay que mirar aunque se haya podido guardar.
     *
     * <p>Viaja en la respuesta de crear y actualizar para que el editor pueda mostrarlo
     * después de guardar: son avisos, no errores, y por eso no impidieron la
     * operación.</p>
     */
    private List<AvisoDTO> advertencias;

    @Data
    @Builder
    public static class AvisoDTO {
        private String nivel;
        private String mensaje;
        /** Dónde señalar en el texto; −1 cuando el aviso no es de un punto concreto. */
        private Integer posicion;
    }
}
