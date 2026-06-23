package imss.gob.mx.cohorte.modules.reclutamiento;

/**
 * Clasifica el origen del participante:
 * - RETORNO: ya participó previamente en el seguimiento de cohorte y fue
 *   recontactado (vía teléfono/correo) para continuar.
 * - NUEVO: nunca ha participado; fue reclutado por otros medios desde
 *   una institución (p. ej. IMSS, INSP).
 */
public enum TipoReclutamiento {
    RETORNO,
    NUEVO
}
