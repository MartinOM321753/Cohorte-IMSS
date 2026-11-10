package imss.gob.mx.cohorte.controllers.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Resumen de resultados de un examen para un participante concreto.
 * Lo usa el modal de la matriz de cobertura cuando el catálogo es EXAMEN.
 */
public record ExamenResumenDTO(
    String unidad,
    List<LineaResultadoExamenDTO> resultados
) {
    public record LineaResultadoExamenDTO(
        long            id,
        LocalDateTime   fechaResultado,
        double          valorObtenido
    ) {}
}
