package imss.gob.mx.cohorte.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
@Getter
@Setter
public class APIResponse {

    private Object data;
    private String message;
    private HttpStatus status;
    private boolean error;

    public APIResponse(Object data, String message, HttpStatus status, boolean error) {
        this.data = data;
        this.message = message;
        this.status = status;
        this.error = error;
    }
    public APIResponse(Object data, HttpStatus status, boolean error) {
        this.data = data;
        this.status = status;
        this.error = error;
    }
    public APIResponse( String message, HttpStatus status, boolean error) {
        this.message = message;
        this.status = status;
        this.error = error;
    }
}
