package imss.gob.mx.cohorte.modules.estudios.parametros;

public enum TipoParametro {
    NUMERICO,
    TEXTO,
    BOOLEANO,
    /** Texto con opciones predefinidas configurables (como un enum dinámico). */
    TEXTO_OPCIONES,
    /** Selector de usuario del sistema (excluye ROOT). Se guarda como texto (nombre completo). */
    USUARIO
}
