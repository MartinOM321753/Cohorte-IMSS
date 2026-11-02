package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

public enum EstadoMuestra {
    /** Llegó a la institución pero no tiene ubicación física asignada aún. */
    SIN_POSICION,
    /** Tiene PosicionCaja asignada en el biobanco de su institucionActual. */
    EN_BIOBANCO,
    /** En préstamo — institucionActual indica a quién se le prestó. */
    PRESTADA,
    /**
     * Entregada a un colaborador (persona física) para análisis u otro uso.
     * A diferencia de PRESTADA, {@code institucionActual} NO cambia: la muestra
     * sigue siendo de su institución, solo está físicamente fuera del biobanco.
     * Su posición queda liberada. Puede devolverse (vuelve a SIN_POSICION) o
     * marcarse CONSUMIDA si no regresa.
     */
    ENTREGADA,
    /**
     * Entregada a un colaborador y NO regresó porque se consumió. Estado terminal.
     *
     * <p>El volumen ({@code valor}) se CONSERVA a propósito: la captura de los
     * estudios que se le hicieron puede llegar después, por otra persona, y
     * necesita volumen contra el cual registrarse. No es una BAJA (la baja es una
     * decisión — contaminación, pérdida, retiro de consentimiento). El
     * agotamiento ({@code fechaAgotamiento}) se sella solo si los estudios llevan
     * el volumen a 0. No se puede prestar, entregar, ubicar, alicuotar ni dar de
     * baja; sí se le pueden capturar estudios.
     */
    CONSUMIDA,
    /** Dada de baja / descartada. */
    BAJA
}
