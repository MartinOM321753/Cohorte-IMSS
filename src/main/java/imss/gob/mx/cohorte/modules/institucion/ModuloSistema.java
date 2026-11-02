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
    REPORTES,
    /**
     * Análisis agregado de resultados de la cohorte (estudios, exámenes,
     * somatometría) con desglose por institución, sexo, edad y rangos.
     *
     * <p>A diferencia de los demás módulos —que operan sobre los datos de la
     * propia institución— ANALISIS lee de forma agregada todo el subárbol de la
     * institución (ella y sus descendientes; ver {@code AnalisisScopeService}).
     * Que la raíz lo conceda a una institución padre habilita el análisis cruzado
     * de esa rama; una sede hoja solo se analiza a sí misma.</p>
     */
    ANALISIS
}
