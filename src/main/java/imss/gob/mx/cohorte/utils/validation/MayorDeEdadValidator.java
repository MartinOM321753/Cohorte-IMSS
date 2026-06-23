package imss.gob.mx.cohorte.utils.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

/**
 * Valida que la fecha de nacimiento corresponda a una persona mayor de edad.
 * La fecha máxima permitida = hoy - 18 años + 3 meses (tolerancia de 3 meses).
 * Un valor null se considera inválido (usar @NotNull por separado para el mensaje específico).
 */
public class MayorDeEdadValidator implements ConstraintValidator<MayorDeEdad, LocalDate> {

    @Override
    public boolean isValid(LocalDate fechaNacimiento, ConstraintValidatorContext context) {
        if (fechaNacimiento == null) {
            return true;
        }
        // Fecha límite: hoy - 18 años + 3 meses
        LocalDate maxFecha = LocalDate.now().minusYears(18).plusMonths(3);
        return !fechaNacimiento.isAfter(maxFecha);
    }
}
