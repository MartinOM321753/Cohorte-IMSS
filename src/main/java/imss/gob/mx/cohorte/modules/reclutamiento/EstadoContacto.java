package imss.gob.mx.cohorte.modules.reclutamiento;

/**
 * Resultado del contacto/recontacto con el participante (aplica principalmente
 * a participantes de tipo RETORNO, que responden a una de estas opciones).
 */
public enum EstadoContacto {
    PENDIENTE,
    ACEPTA_PARTICIPAR,
    ACEPTA_CUESTIONARIO_RECUPERACION,
    RECHAZA_CONTACTO_FUTURO
}
