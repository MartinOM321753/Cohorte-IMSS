package imss.gob.mx.cohorte.modules.almacenamiento.almacen;

/**
 * Clasifica el tipo de institución externa a la que se trasladan muestras.
 * Preparación para el modelo de "sucursales" donde cada institución
 * podría tener su propio biobanco en el futuro.
 */
public enum TipoInstitucion {
    INMEGEN,
    INSP,
    HOSPITAL,
    LABORATORIO,
    OTRA
}
