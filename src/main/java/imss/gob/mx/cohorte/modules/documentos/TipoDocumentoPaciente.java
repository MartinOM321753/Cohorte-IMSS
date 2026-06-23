package imss.gob.mx.cohorte.modules.documentos;

/** Categoría de un documento asociado a un paciente. */
public enum TipoDocumentoPaciente {
    /** Documento de consentimiento informado firmado. Etiqueta: CI/{folio}/F4. */
    CONSENTIMIENTO,
    /** Cualquier otro documento del expediente del paciente. */
    GENERAL,
    /** @deprecated reemplazado por los 4 subtipos de cuestionario. Se conserva por datos históricos. */
    @Deprecated
    CUESTIONARIO,
    /** Cuestionario general de la Cohorte. Etiqueta: C1/{folio}/F4. */
    CUESTIONARIO_GENERAL,
    /** Minimental (>45 años). Etiqueta: C2/{folio}/F4. */
    CUESTIONARIO_MINIMENTAL,
    /** Afluencia verbal (>45 años). Etiqueta: C3/{folio}/F4. */
    CUESTIONARIO_AFLUENCIA_VERBAL,
    /** AGES. Etiqueta: C4/{folio}/F4. */
    CUESTIONARIO_AGES
}
