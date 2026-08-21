package imss.gob.mx.cohorte.utils.Exceptions;

import imss.gob.mx.cohorte.services.auth.PasswordResetService;
import imss.gob.mx.cohorte.utils.APIResponse;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.MinioUnavailableException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ObjNotFoundException.class)
    public ResponseEntity<APIResponse> handleObjNotFound(ObjNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new APIResponse(ex.getMessage(), HttpStatus.NOT_FOUND, true));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<APIResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new APIResponse("Sin permisos para esta acción: " + ex.getMessage(),
                        HttpStatus.FORBIDDEN, true));
    }

    @ExceptionHandler(ObjConflictException.class)
    public ResponseEntity<APIResponse> handleObjConflict(ObjConflictException ex){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new APIResponse(ex.getMessage(), HttpStatus.CONFLICT, true));
    }

    /**
     * Error de validación de negocio (p. ej. contraseña actual incorrecta).
     * HTTP 422 — el frontend lo trata como error de formulario, NO como sesión expirada.
     */
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<APIResponse> handleValidation(ValidationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new APIResponse(ex.getMessage(), HttpStatus.UNPROCESSABLE_ENTITY, true));
    }

    @ExceptionHandler(PasswordResetService.RateLimitException.class)
    public ResponseEntity<APIResponse> handleRateLimit(PasswordResetService.RateLimitException ex) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new APIResponse(ex.getMessage(), HttpStatus.TOO_MANY_REQUESTS, true));
    }

    @ExceptionHandler(PasswordResetService.TokenInvalidoException.class)
    public ResponseEntity<APIResponse> handleTokenInvalido(PasswordResetService.TokenInvalidoException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new APIResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, true));
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<APIResponse> handleOptimisticLock(OptimisticLockingFailureException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new APIResponse(
                        "El registro fue modificado por otro usuario. Por favor, recargue e intente de nuevo.",
                        HttpStatus.CONFLICT, true));
    }

    @ExceptionHandler(MinioUnavailableException.class)
    public ResponseEntity<APIResponse> handleMinioUnavailable(MinioUnavailableException ex) {
        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new APIResponse(ex.getMessage(), HttpStatus.SERVICE_UNAVAILABLE, true));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<APIResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new APIResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, true));
    }

    /**
     * El cuerpo de la peticion no se pudo interpretar: un campo llego con un tipo
     * o un formato que no corresponde.
     *
     * <p>Sin este manejador caia en el catch general y salia como 500 "Error
     * interno del servidor" con un volcado de pila. Es enganoso: el servidor esta
     * bien, lo que llego mal es la peticion. Y sobre todo deja al usuario sin
     * saber que corregir, porque el motivo real —que campo y con que valor— se
     * queda enterrado en el log.</p>
     */
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<APIResponse> handleCuerpoIlegible(
            org.springframework.http.converter.HttpMessageNotReadableException ex) {

        String detalle = "El formato de la solicitud no es valido.";

        if (ex.getCause() instanceof com.fasterxml.jackson.databind.exc.InvalidFormatException causa) {
            String campo = causa.getPath().stream()
                    .map(com.fasterxml.jackson.databind.JsonMappingException.Reference::getFieldName)
                    .filter(java.util.Objects::nonNull)
                    .reduce((a, b) -> a + "." + b)
                    .orElse(null);

            String tipo = causa.getTargetType() != null ? causa.getTargetType().getSimpleName() : "";
            String valor = String.valueOf(causa.getValue());

            if (campo != null) {
                detalle = "El campo \"" + campo + "\" recibio el valor \"" + valor
                        + "\", que no corresponde al formato esperado"
                        + ("LocalDateTime".equals(tipo) ? " (fecha y hora, por ejemplo 2026-08-07T14:30)" : "")
                        + ".";
            }
        }

        // WARN y no ERROR: no es un fallo del servidor, es una peticion mal formada.
        log.warn("Cuerpo de peticion ilegible: {}", ex.getMostSpecificCause().getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new APIResponse(detalle, HttpStatus.BAD_REQUEST, true));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse> handleGeneral(Exception ex){
        log.error("Error interno no controlado", ex);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new APIResponse(
                        "Error interno del servidor",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        true
                ));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<APIResponse> handleBadCredentials(BadCredentialsException ex){

        APIResponse response = new APIResponse(
                ex.getMessage(),
                true,
                HttpStatus.UNAUTHORIZED
        );

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }




}

