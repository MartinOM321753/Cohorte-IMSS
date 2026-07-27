package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;

/**
 * Valida el estado del participante dueño de una muestra antes de permitir
 * operaciones mutantes (prestar, alicuotar, aplicar estudios).
 *
 * <p>Cuando un participante se desactiva (retira consentimiento o baja
 * administrativa), sus muestras entran en "cuarentena": siguen visibles y
 * consultables, pero no se pueden mutar. Reactivar al participante restaura
 * las operaciones sin cambiar el estado físico de las muestras.
 */
public final class PacienteEstadoValidator {

    private PacienteEstadoValidator() {}

    public static void requirePacienteActivo(Muestra muestra, String operacion) {
        if (muestra.getPaciente() == null) {
            throw new ObjConflictException(
                    "No se puede " + operacion + ": la muestra no tiene participante asociado.");
        }
        if (!Boolean.TRUE.equals(muestra.getPaciente().getActivo())) {
            throw new ObjConflictException(
                    "No se puede " + operacion + ": el participante está inactivo "
                    + "(consentimiento retirado o baja administrativa). "
                    + "Las muestras están en cuarentena hasta reactivar al participante.");
        }
    }
}
