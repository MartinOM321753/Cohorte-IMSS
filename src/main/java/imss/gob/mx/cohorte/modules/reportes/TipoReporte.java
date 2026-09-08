package imss.gob.mx.cohorte.modules.reportes;

/**
 * Sobre qué se emite un reporte. Determina qué datos puede insertar el diseño: una
 * plantilla de estudio no ofrece los campos de somatometría, porque al emitirla no
 * habría de dónde sacarlos.
 */
public enum TipoReporte {
    /** Un estudio médico concreto y sus resultados. */
    ESTUDIO,
    /** Los resultados de laboratorio de un participante. */
    EXAMENES,
    /** Mediciones de somatometría de un participante. */
    SOMATOMETRIA,
    /** Varios orígenes a la vez para un mismo participante. */
    EXPEDIENTE,
    /** Cifras del conjunto, sin participante concreto. */
    AGREGADO
}
