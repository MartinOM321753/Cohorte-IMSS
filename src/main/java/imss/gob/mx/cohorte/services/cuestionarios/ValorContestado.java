package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionario;

import java.time.LocalDate;

/**
 * Qué cuenta como «lo que contestó» una llave cuando se evalúa una dependencia.
 *
 * <h3>Por qué existe</h3>
 *
 * <p>El evaluador recibe un mapa código de pregunta → valor contestado. Hasta que
 * existió {@link imss.gob.mx.cohorte.modules.cuestionarios.variables.OperadorDependencia#SIN_RESPUESTA},
 * ese valor era siempre el <em>código de opción</em>, porque todas las llaves eran de
 * opciones. Una llave de texto libre —la P191, «¿a qué se dedica?»— no tiene código: su
 * respuesta vive en {@code valorTexto}. Armando el mapa solo con el código, la P191
 * contestada habría salido como «sin contestar» y el salto no se habría activado nunca.</p>
 *
 * <p>Los tres sitios que arman ese mapa (al guardar, al importar un volcado y al
 * autoguardar en pantalla) pasan por aquí para no volver a divergir.</p>
 *
 * <h3>Orden</h3>
 *
 * <p>Código, luego texto, luego número y luego fecha. Una respuesta con marca de
 * ausencia («no contestó», «no autorizó»…) no es respuesta y devuelve {@code null}.
 * Un texto en blanco tampoco.</p>
 */
public final class ValorContestado {

    private ValorContestado() {}

    /** El valor de una respuesta ya guardada, o null si no hay respuesta. */
    public static String de(RespuestaCuestionario r) {
        if (r.getMarcaAusencia() != MarcaAusencia.NINGUNA) return null;
        return de(r.getValorCodigo(), r.getValorTexto(), r.getValorNumerico(), r.getValorFecha());
    }

    /** El valor de una celda recién interpretada, o null si no hay respuesta. */
    public static String de(InterpreteCelda.Interpretacion i) {
        if (i.marca() != MarcaAusencia.NINGUNA) return null;
        return de(i.codigo(), i.texto(), i.numero(), i.fecha());
    }

    private static String de(String codigo, String texto, Double numero, LocalDate fecha) {
        if (codigo != null && !codigo.isBlank()) return codigo;
        if (texto != null && !texto.isBlank()) return texto.trim();
        if (numero != null) {
            return numero == Math.rint(numero) && !Double.isInfinite(numero)
                    ? String.valueOf(numero.longValue())
                    : String.valueOf(numero);
        }
        if (fecha != null) return fecha.toString();
        return null;
    }
}
