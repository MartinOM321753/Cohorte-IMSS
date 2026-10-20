package imss.gob.mx.cohorte.modules.institucion;

/**
 * Módulos del sistema cuyo acceso puede otorgarse/revocarse por institución.
 * Sólo la institución raíz (sin institución padre) puede conceder permisos
 * sobre estos módulos a sus instituciones hijas — ver {@link InstitucionModulo}.
 */
public enum ModuloSistema {
    PARTICIPANTES,
    BIOBANCO,
    EXAMENES,
    ESTUDIOS_MEDICOS,
    /**
     * Instrumentos de la cohorte: catalogo de preguntas, llenado y carga de los
     * volcados escaneados. Aparte de ESTUDIOS_MEDICOS porque son dos circuitos
     * distintos: una sede puede aplicar cuestionarios sin tomar estudios, y al
     * reves.
     */
    CUESTIONARIOS,
    CITAS,
    COBERTURA,
    SOMATOMETRIA,
    DOCUMENTOS,
    /** Acceso al registro de eventos de login/logout. */
    BITACORA_ACCESOS,
    /** Acceso al registro de acciones de escritura (crear/actualizar/eliminar). */
    BITACORA_ACCIONES,
    /** Diseño de plantillas y emisión de reportes en PDF. */
    REPORTES
}
