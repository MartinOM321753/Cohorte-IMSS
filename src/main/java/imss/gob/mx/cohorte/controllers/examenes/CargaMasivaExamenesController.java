package imss.gob.mx.cohorte.controllers.examenes;

import imss.gob.mx.cohorte.services.importacion.CargaMasivaExamenesService;
import imss.gob.mx.cohorte.services.importacion.PrevisualizacionCargaExamenes;
import imss.gob.mx.cohorte.services.importacion.ResultadoCarga;
import imss.gob.mx.cohorte.services.importacion.TablaLeida;
import imss.gob.mx.cohorte.utils.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Carga masiva de resultados de laboratorio.
 *
 * <p>Mismo recorrido en tres pasos que la de estudios —previsualizar, revalidar y
 * confirmar—, pero sin elegir tipo: un archivo de laboratorio trae varios
 * examenes a la vez y cada columna se resuelve por su alias.</p>
 */
@RestController
@RequestMapping("/api/examenes/carga-masiva")
@RequiredArgsConstructor
public class CargaMasivaExamenesController {

    private final CargaMasivaExamenesService cargaMasivaService;

    /** Interpreta el archivo contra el catalogo de examenes. No escribe nada. */
    @PostMapping("/previsualizar")
    public ResponseEntity<APIResponse> previsualizar(@RequestParam("archivo") MultipartFile archivo) {
        PrevisualizacionCargaExamenes previa = cargaMasivaService.previsualizar(archivo);
        return ResponseEntity.ok(new APIResponse(previa, mensaje(previa), HttpStatus.OK, false));
    }

    public record RevalidarRequest(TablaLeida tabla) {}

    /** Vuelve a analizar la tabla ya corregida, sin volver a subir el archivo. */
    @PostMapping("/revalidar")
    public ResponseEntity<APIResponse> revalidar(@RequestBody RevalidarRequest peticion) {
        PrevisualizacionCargaExamenes previa = cargaMasivaService.revalidar(peticion.tabla());
        return ResponseEntity.ok(new APIResponse(previa, mensaje(previa), HttpStatus.OK, false));
    }

    public record ConfirmarRequest(TablaLeida tabla, String politicaDuplicados) {}

    /** Escribe los resultados. Unica llamada de este controlador que modifica datos. */
    @PostMapping("/confirmar")
    public ResponseEntity<APIResponse> confirmar(@RequestBody ConfirmarRequest peticion) {
        // Omitir por defecto: reemplazar pisa un resultado ya registrado y tiene
        // que pedirse a proposito.
        var politica = "REEMPLAZAR".equalsIgnoreCase(peticion.politicaDuplicados())
                ? CargaMasivaExamenesService.PoliticaDuplicados.REEMPLAZAR
                : CargaMasivaExamenesService.PoliticaDuplicados.OMITIR;

        ResultadoCarga r = cargaMasivaService.confirmar(peticion.tabla(), politica);
        return ResponseEntity.ok(new APIResponse(r, resumen(r), HttpStatus.OK, false));
    }

    private static String mensaje(PrevisualizacionCargaExamenes p) {
        if (!p.problemasDeEstructura().isEmpty()) {
            return "El archivo no encaja con el catalogo de examenes";
        }
        return p.puedeConfirmarse()
                ? "El archivo se leyo correctamente y esta listo para confirmarse"
                : "El archivo se leyo, pero hay que corregir algunos datos antes de guardar";
    }

    private static String resumen(ResultadoCarga r) {
        StringBuilder sb = new StringBuilder();
        if (r.registrados() > 0) sb.append(r.registrados()).append(" resultado(s) registrados");
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
}
