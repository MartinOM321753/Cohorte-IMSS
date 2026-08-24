package imss.gob.mx.cohorte.services.importacion;

/**
 * El archivo no se puede leer, y el motivo es del archivo, no del servidor.
 *
 * <p>Extiende la ValidationException del proyecto para que el GlobalExceptionHandler
 * la convierta en un 422 con su mensaje. Sin esto, todos estos casos —un ZIP que
 * se expande demasiado, un CSV con una celda de un megabyte, una hoja vacia—
 * llegarian al usuario como "Error interno del servidor", que no le dice que
 * arreglar.</p>
 */
public class ArchivoInvalidoException
        extends imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException {

    public ArchivoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
