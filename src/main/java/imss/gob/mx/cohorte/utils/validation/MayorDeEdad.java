package imss.gob.mx.cohorte.utils.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida que la fecha de nacimiento corresponda a una persona mayor de edad.
 * Regla: fechaNacimiento <= hoy - 18 años + 3 meses (tolerancia de 3 meses).
 */
@Documented
@Constraint(validatedBy = MayorDeEdadValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface MayorDeEdad {

    String message() default "El usuario debe ser mayor de 18 años (tolerancia de 3 meses)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
