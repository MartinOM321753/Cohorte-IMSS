package imss.gob.mx.cohorte.utils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class APIResponse {

    private Object data;
    private String message;
    private HttpStatus status;
    private boolean error;

}
