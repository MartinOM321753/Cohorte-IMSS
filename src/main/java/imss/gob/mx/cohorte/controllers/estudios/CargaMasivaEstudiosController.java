package imss.gob.mx.cohorte.controllers.estudios;

import imss.gob.mx.cohorte.services.importacion.CargaMasivaEstudiosService;
import imss.gob.mx.cohorte.services.importacion.PrevisualizacionCarga;
import imss.gob.mx.cohorte.services.importacion.ResultadoCarga;
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
     * Cuerpo de la confirmacion.
     *
     * @param politicaDuplicados OMITIR (por defecto) o REEMPLAZAR
     */
    public record ConfirmarRequest(Long idTipoEstudio, TablaLeida tabla, String politicaDuplicados) {}

    /**
     * Escribe la carga. Es la unica llamada de este controlador que modifica datos.
     *
     * <p>El servidor vuelve a analizar la tabla entera antes de escribir: la
     * previsualizacion la calculo el, pero paso por el cliente y volvio, asi que
     * darla por buena permitiria guardar cualquier cosa manipulando la peticion.</p>
     */
    @PostMapping("/confirmar")
    public ResponseEntity<APIResponse> confirmar(@RequestBody ConfirmarRequest peticion) {
        // Por defecto se omiten los duplicados. Reemplazar destruye lo que ya
        // estaba registrado, asi que tiene que pedirse a proposito.
        var politica = "REEMPLAZAR".equalsIgnoreCase(peticion.politicaDuplicados())
                ? CargaMasivaEstudiosService.PoliticaDuplicados.REEMPLAZAR
                : CargaMasivaEstudiosService.PoliticaDuplicados.OMITIR;

        ResultadoCarga resultado =
                cargaMasivaService.confirmar(peticion.tabla(), peticion.idTipoEstudio(), politica);

        return ResponseEntity.ok(new APIResponse(
                resultado, resumen(resultado), HttpStatus.OK, false));
    }

    private static String resumen(ResultadoCarga r) {
        StringBuilder sb = new StringBuilder();
        if (r.registrados() > 0) sb.append(r.registrados()).append(" estudio(s) registrados");
        if (r.reemplazados() > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(r.reemplazados()).append(" reemplazados");
        }
        if (r.omitidosPorDuplicado() > 0) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(r.omitidosPorDuplicado()).append(" omitidos por estar ya registrados");
        }
        return sb.length() > 0 ? sb.toString() : "No habia nada que registrar";
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
