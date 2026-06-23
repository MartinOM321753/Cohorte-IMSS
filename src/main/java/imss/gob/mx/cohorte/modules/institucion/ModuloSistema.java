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
    CITAS,
    COBERTURA,
    SOMATOMETRIA,
    DOCUMENTOS,
    /** Acceso al registro de eventos de login/logout. */
    BITACORA_ACCESOS,
    /** Acceso al registro de acciones de escritura (crear/actualizar/eliminar). */
    BITACORA_ACCIONES
}
