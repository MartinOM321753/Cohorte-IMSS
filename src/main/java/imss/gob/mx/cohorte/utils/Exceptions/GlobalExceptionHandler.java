package imss.gob.mx.cohorte.utils.Exceptions;

import imss.gob.mx.cohorte.utils.APIResponse;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.ExceptionsClass.ObjNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ObjNotFoundException.class)
    public ResponseEntity<APIResponse> handleObjNotFound(ObjNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new APIResponse(ex.getMessage(), HttpStatus.NOT_FOUND, true));
    }

    @ExceptionHandler(ObjConflictException.class)
    public ResponseEntity<APIResponse> handleObjConflict(ObjConflictException ex){
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new APIResponse(ex.getMessage(), HttpStatus.CONFLICT, true));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<APIResponse> handleGeneral(Exception ex){
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new APIResponse(
                        "Error interno del servidor",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        true
                ));
    }
}

