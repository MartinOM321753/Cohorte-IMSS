package imss.gob.mx.cohorte.utils.Exceptions.exceptions;

/**
 * Lanzada cuando se intenta usar MinIO y el servicio no está disponible.
 * El GlobalExceptionHandler la captura y devuelve HTTP 503.
 */
public class MinioUnavailableException extends RuntimeException {

    public MinioUnavailableException() {
        super("El servicio de almacenamiento no está disponible en este momento.");
    }

    public MinioUnavailableException(String message) {
        super(message);
    }
}
