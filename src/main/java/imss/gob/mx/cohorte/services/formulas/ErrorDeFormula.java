package imss.gob.mx.cohorte.services.formulas;

/**
 * La fórmula no se puede leer.
 *
 * <p>Esto es lo único que impide guardar. No es que el resultado parezca raro —eso se
 * advierte y se deja pasar— sino que no hay forma de saber qué se quiso escribir: un
 * paréntesis sin cerrar, una función que no existe, una operación sin su segundo
 * operando.</p>
 *
 * <p>El mensaje va dirigido a quien está escribiendo la fórmula, no a un programador,
 * y lleva la posición para poder señalar el punto exacto en el editor.</p>
 */
public class ErrorDeFormula extends RuntimeException {

    /** Posición en el texto donde se detectó el problema; −1 si no aplica a un punto. */
    private final int posicion;

    public ErrorDeFormula(String mensaje, int posicion) {
        super(mensaje);
        this.posicion = posicion;
    }

    public ErrorDeFormula(String mensaje) {
        this(mensaje, -1);
    }

    public int posicion() {
        return posicion;
    }
}
