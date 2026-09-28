package imss.gob.mx.cohorte.controllers.dashboard.dto;

/** Una línea del detalle de estudio para el modal de la matriz de cobertura. */
public record ResultadoLineaDTO(
    String  parametro,
    String  unidad,
    String  grupoEtiqueta,
    Integer orden,
    Double  valorNumerico,
    String  valorTexto,
    Boolean valorBooleano
) {}
