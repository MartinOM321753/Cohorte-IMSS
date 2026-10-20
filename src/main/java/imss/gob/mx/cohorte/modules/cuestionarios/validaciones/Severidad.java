package imss.gob.mx.cohorte.modules.cuestionarios.validaciones;

/**
 * Qué tan grave es que una regla no se cumpla.
 *
 * <h3>La misma regla no puede actuar igual en los dos caminos</h3>
 *
 * <p>Cuando alguien contesta en pantalla hay una persona delante que puede corregir,
 * así que un {@link #ERROR} le impide seguir y se arregla en el momento. Cuando se
 * importa un volcado no hay nadie: la hoja ya se contestó, en papel, y el dato existe
 * aunque incumpla. Ahí <b>nada se rechaza</b>; todo queda marcado.</p>
 *
 * <p>Tirar una respuesta porque no cuadra sería perder el dato y, con él, la señal de
 * que hubo un error de aplicación.</p>
 */
public enum Severidad {

    /** En pantalla no deja continuar. Al importar, se marca para revisión. */
    ERROR,

    /** Nunca impide nada; queda señalado para que alguien lo mire. */
    REVISION
}
