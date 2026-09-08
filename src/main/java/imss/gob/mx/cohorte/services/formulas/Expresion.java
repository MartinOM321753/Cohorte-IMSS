package imss.gob.mx.cohorte.services.formulas;

import java.math.BigDecimal;
import java.util.List;

/**
 * Una fórmula ya analizada, lista para calcular.
 *
 * <p>El texto que escribe quien arma el reporte se convierte una vez en este árbol y
 * después se evalúa tantas veces como participantes tenga el reporte. Separar las dos
 * cosas es lo que permite avisar de un error <b>mientras se escribe</b>, sin llegar a
 * calcular nada: el análisis solo necesita el texto.</p>
 *
 * <p>El árbol es cerrado a propósito —solo existen los seis nodos de aquí abajo— y
 * ninguno de ellos puede invocar código. Es la diferencia entre un evaluador de
 * fórmulas y un intérprete: estas expresiones vienen de la base de datos, escritas por
 * usuarios, y un intérprete de propósito general ahí sería una puerta abierta.</p>
 */
public sealed interface Expresion {

    /** Un número escrito literalmente en la fórmula: el 18.5 de «18.5 × talla²». */
    record Numero(BigDecimal valor) implements Expresion {}

    /**
     * Un texto entre comillas: el {@code 'Derecha'} de
     * {@code si(manoDominante = 'Derecha', …, …)}.
     *
     * <p>Solo sirve para comparar. Con él se pueden escribir reglas sobre parámetros de
     * opciones, que antes quedaban fuera del motor por no ser numéricos.</p>
     */
    record Texto(String valor) implements Expresion {}

    /**
     * Una variable, por el nombre con el que la fórmula la llama.
     *
     * <p>Quién es cada nombre no se resuelve aquí: el evaluador recibe de fuera con
     * qué contestarlas. Así la misma fórmula sirve para calcular con un participante
     * real, para la vista previa del editor y para las pruebas.</p>
     */
    record Variable(String nombre) implements Expresion {}

    /** El menos de «−309», que no es una resta. */
    record Negacion(Expresion sobre) implements Expresion {}

    /** Suma, resta, multiplicación, división y potencia. */
    record Operacion(Operador operador, Expresion izquierda, Expresion derecha) implements Expresion {}

    /** Una comparación, que solo tiene sentido dentro de «si(...)». */
    record Comparacion(Comparador comparador, Expresion izquierda, Expresion derecha)
            implements Expresion {}

    /** Una de las funciones permitidas, con sus argumentos. */
    record Llamada(String funcion, List<Expresion> argumentos) implements Expresion {}

    enum Operador {
        SUMA("+"), RESTA("−"), MULTIPLICACION("×"), DIVISION("÷"), POTENCIA("^");

        private final String simbolo;

        Operador(String simbolo) { this.simbolo = simbolo; }

        public String simbolo() { return simbolo; }
    }

    enum Comparador {
        IGUAL("="), DISTINTO("≠"), MENOR("<"), MENOR_IGUAL("≤"), MAYOR(">"), MAYOR_IGUAL("≥");

        private final String simbolo;

        Comparador(String simbolo) { this.simbolo = simbolo; }

        public String simbolo() { return simbolo; }
    }
}
