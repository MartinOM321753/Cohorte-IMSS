package imss.gob.mx.cohorte.services.formulas;

/**
 * Una variable de una fórmula: cómo se llama, de dónde sale y en qué unidad entra.
 *
 * <p>Quien arma la fórmula no escribe {@code estudio.7.param.85}. Arrastra «Estatura»
 * de una lista y el sistema guarda las dos cosas: el nombre corto con el que la
 * fórmula la menciona y la clave del catálogo de la que se saca el dato. Guardar la
 * clave y no el nombre del parámetro es lo que hace que la fórmula sobreviva a que
 * alguien renombre el estudio.</p>
 *
 * <p><b>La unidad es la decisión más importante de las tres.</b> El dato está guardado
 * en la unidad en que se capturó —la estatura, en centímetros— y la fórmula puede
 * necesitarlo en otra. Aquí se registra en cuál lo quiere quien la escribió, a la
 * vista y elegido por él: el motor convierte a esa, y no adivina.</p>
 *
 * @param nombre cómo la menciona la fórmula: «estatura», «peso», «edad»
 * @param clave  de dónde sale el dato, en el catálogo de campos del reporte
 * @param unidad en cuál se quiere; si es {@code null}, la que traiga el dato
 */
public record VariableFormula(String nombre, String clave, Unidad unidad) {

    public VariableFormula {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("Una variable necesita nombre");
        }
    }

    /** Sin conversión: el dato entra tal como está guardado. */
    public static VariableFormula de(String nombre, String clave) {
        return new VariableFormula(nombre, clave, null);
    }

    public static VariableFormula de(String nombre, String clave, String unidad) {
        return new VariableFormula(nombre, clave, Unidad.de(unidad));
    }
}
