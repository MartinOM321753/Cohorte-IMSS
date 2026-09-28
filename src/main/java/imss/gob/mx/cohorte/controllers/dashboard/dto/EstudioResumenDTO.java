package imss.gob.mx.cohorte.controllers.dashboard.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalle MÍNIMO de un estudio para el modal de la matriz de cobertura: solo lo
 * que se pinta (fecha, observaciones y las líneas parámetro→valor). Evita traer
 * paciente, usuario, institución y el tipo completo, que el modal no usa.
 */
public record EstudioResumenDTO(
    Long                   id,
    LocalDateTime          fechaEstudio,
    String                 observaciones,
    List<ResultadoLineaDTO> resultados
) {}
