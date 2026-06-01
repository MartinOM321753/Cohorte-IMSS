package imss.gob.mx.cohorte.utils.Exceptions.exceptions;

/**
 * Error de validación de negocio (p. ej. contraseña actual incorrecta).
 * Retorna HTTP 422 — el frontend lo trata como error de formulario,
 * NO como sesión expirada.
 */
public class ValidationException extends RuntimeException {
    public ValidationException(String message) {
        super(message);
    }
}
