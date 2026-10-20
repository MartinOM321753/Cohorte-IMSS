package imss.gob.mx.cohorte.modules.cuestionarios.tipos;

import imss.gob.mx.cohorte.modules.persona.Persona;

/**
 * A quién va dirigido un instrumento por sexo.
 *
 * <p>Es un enum propio y no se reutiliza {@code Persona.Sexo} porque no son la misma
 * cosa: el sexo de una persona es {@code M} o {@code F}, mientras que lo que aquí se
 * declara es el <em>público</em> del cuestionario, y su valor más común —ambos— no existe
 * como sexo de nadie. Mezclarlos obligaría a representar «ambos» con un nulo, y un nulo
 * ya significa otra cosa: que no se ha configurado.</p>
 */
public enum DirigidoA {

    AMBOS,
    HOMBRES,
    MUJERES;

    /**
     * Si una persona de este sexo entra en el público del instrumento.
     *
     * <p>Un sexo sin registrar no decide nada: devuelve {@code false} y es quien llama
     * el que distingue «no entra» de «no se puede saber». Hacer que un nulo cuente como
     * coincidencia escondería el hueco del expediente; hacer que cuente como rechazo
     * dejaría fuera a un participante por un dato que falta en nuestra base, no en su
     * hoja.</p>
     */
    public boolean admite(Persona.Sexo sexo) {
        if (this == AMBOS) return true;
        if (sexo == null) return false;
        return (this == HOMBRES && sexo == Persona.Sexo.M)
                || (this == MUJERES && sexo == Persona.Sexo.F);
    }

    public String etiqueta() {
        return switch (this) {
            case AMBOS -> "hombres y mujeres";
            case HOMBRES -> "hombres";
            case MUJERES -> "mujeres";
        };
    }
}
