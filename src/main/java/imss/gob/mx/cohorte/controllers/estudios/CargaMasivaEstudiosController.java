package imss.gob.mx.cohorte.controllers.estudios;

import imss.gob.mx.cohorte.services.importacion.CargaMasivaEstudiosService;
import imss.gob.mx.cohorte.services.importacion.PrevisualizacionCarga;
import imss.gob.mx.cohorte.services.importacion.TablaLeida;
import imss.gob.mx.cohorte.utils.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Carga masiva de resultados de estudios desde un archivo de instrumento.
 *
 * <p>Va en dos pasos a proposito. Este endpoint solo previsualiza: interpreta el
 * archivo y devuelve lo que se guardaria, con cada problema pegado a su celda.
 * Nada se escribe hasta que el usuario revisa y confirma en una llamada aparte.
 * Un archivo mal interpretado puede meter cientos de mediciones equivocadas de
 * golpe, y corregirlas despues cuesta mucho mas que mirarlas antes.</p>
 */
@RestController
@RequestMapping("/api/estudios/carga-masiva")
@RequiredArgsConstructor
public class CargaMasivaEstudiosController {

    private final CargaMasivaEstudiosService cargaMasivaService;


    /**
     * Cuerpo de la revalidacion: la tabla tal como quedo tras las correcciones
     * hechas en pantalla.
     */
    public record RevalidarRequest(Long idTipoEstudio, TablaLeida tabla) {}

    /**
     * Vuelve a analizar la tabla ya corregida, sin volver a subir el archivo.
     *
     * <p>Comparte el analisis con la previsualizacion a proposito. Validar en el
     * navegador seria mas rapido, pero acabaria habiendo dos reglas para el mismo
     * dato: la del navegador, que el usuario ve, y la del servidor, que es la que
     * manda. Cuando dejaran de coincidir, la pantalla diria que todo esta bien y
     * el guardado fallaria sin explicar por que.</p>
     */
    @PostMapping("/revalidar")
    public ResponseEntity<APIResponse> revalidar(@RequestBody RevalidarRequest peticion) {
        PrevisualizacionCarga previsualizacion =
                cargaMasivaService.revalidar(peticion.tabla(), peticion.idTipoEstudio());

        return ResponseEntity.ok(new APIResponse(
                previsualizacion,
                previsualizacion.puedeConfirmarse()
                        ? "Ya no queda nada por corregir"
                        : "Todavia hay datos por corregir",
                HttpStatus.OK, false));
    }

    /**
     * Interpreta el archivo contra un tipo de estudio y devuelve la
     * previsualizacion. No escribe nada.
     */
    @PostMapping("/previsualizar")
    public ResponseEntity<APIResponse> previsualizar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam("idTipoEstudio") Long idTipoEstudio) {

        PrevisualizacionCarga previsualizacion =
                cargaMasivaService.previsualizar(archivo, idTipoEstudio);

        return ResponseEntity.ok(new APIResponse(
                previsualizacion,
                previsualizacion.puedeConfirmarse()
                        ? "El archivo se leyo correctamente y esta listo para confirmarse"
                        : "El archivo se leyo, pero hay que corregir algunos datos antes de guardar",
                HttpStatus.OK, false));
    }
}
