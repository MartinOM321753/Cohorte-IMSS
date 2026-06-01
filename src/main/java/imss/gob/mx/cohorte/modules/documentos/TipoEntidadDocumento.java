package imss.gob.mx.cohorte.modules.documentos;

/**
 * Categoría de la entidad a la que pertenece un Documento.
 * Se persiste en la columna {@code tipo_entidad} de la tabla Documento.
 */
public enum TipoEntidadDocumento {
    ESTUDIO,
    MUESTRA,
    PACIENTE_CONSENTIMIENTO,
    PACIENTE_GENERAL
}
