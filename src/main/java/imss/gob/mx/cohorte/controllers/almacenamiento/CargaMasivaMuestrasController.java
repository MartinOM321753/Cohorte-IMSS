package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.services.importacion.CargaMasivaMuestrasService;
import imss.gob.mx.cohorte.services.importacion.PlantillaCargaMuestras;
import imss.gob.mx.cohorte.services.importacion.PrevisualizacionCargaMuestras;
import imss.gob.mx.cohorte.services.importacion.ResultadoCargaMuestras;
import imss.gob.mx.cohorte.services.importacion.TablaLeida;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Carga masiva de muestras y alícuotas desde una hoja de cálculo.
 *
 * <p>Va en dos pasos a propósito. Previsualizar y revalidar no escriben nada:
 * interpretan el archivo y devuelven lo que se guardaría, con cada problema
 * pegado a su celda. Solo confirmar escribe, y vuelve a analizar la tabla entera
 * antes de hacerlo.</p>
 *
 * <p>Aquí importa más que en ninguna otra carga: una muestra mal cargada no solo
 * mete un dato equivocado, además <b>ocupa un hueco físico</b> de una caja
 * criogénica. Deshacerlo significa abrir congeladores.</p>
 */
@RestController
@RequestMapping("/api/almacenamiento/muestras/carga-masiva")
@RequiredArgsConstructor
@Tag(name = "Muestras", description = "Carga masiva de muestras y alícuotas")
@SecurityRequirement(name = "bearerAuth")
public class CargaMasivaMuestrasController {

    private final CargaMasivaMuestrasService cargaMasivaService;
    private final PlantillaCargaMuestras plantillaService;

    /**
     * Interpreta el archivo y devuelve la previsualización. No escribe nada.
     *
     * @param fechaPorOmision la que se aplica a las filas sin fecha, y la hora
     *                        que se pone a las que traen día pero no hora. La
     *                        calcula la pantalla con el horario configurado de la
     *                        institución, que es donde vive esa regla.
     */
    @PostMapping("/previsualizar")
    @Operation(summary = "Previsualizar una carga de muestras",
            description = "Lee el archivo, lo resuelve contra el catálogo y devuelve lo que se "
                    + "guardaría. No escribe nada.")
    @PreAuthorize("hasAuthority('MUESTRAS_CARGA_MASIVA')")
    public ResponseEntity<APIResponse> previsualizar(
            @RequestParam("archivo") MultipartFile archivo,
            @RequestParam(value = "fechaPorOmision", required = false) String fechaPorOmision) {

        PrevisualizacionCargaMuestras previa =
                cargaMasivaService.previsualizar(archivo, fechaPorOmision);

        return ResponseEntity.ok(new APIResponse(previa, mensaje(previa), HttpStatus.OK, false));
    }

    /** Cuerpo de la revalidación y de la confirmación: la tabla ya corregida. */
    public record TablaRequest(TablaLeida tabla, String fechaPorOmision) {}

    /**
     * Vuelve a analizar la tabla corregida, sin volver a subir el archivo.
     *
     * <p>Comparte el análisis con la previsualización a propósito. Validar en el
     * navegador sería más rápido, pero acabaría habiendo dos reglas para el mismo
     * dato: la del navegador, que el usuario ve, y la del servidor, que es la que
     * manda. Cuando dejaran de coincidir, la pantalla diría que todo está bien y
     * el guardado fallaría sin explicar por qué.</p>
     */
    @PostMapping("/revalidar")
    @Operation(summary = "Revalidar la tabla corregida")
    @PreAuthorize("hasAuthority('MUESTRAS_CARGA_MASIVA')")
    public ResponseEntity<APIResponse> revalidar(@RequestBody TablaRequest peticion) {
        PrevisualizacionCargaMuestras previa =
                cargaMasivaService.revalidar(peticion.tabla(), peticion.fechaPorOmision());

        return ResponseEntity.ok(new APIResponse(previa, mensaje(previa), HttpStatus.OK, false));
    }

    /** Escribe la carga. Es la única llamada de este controlador que modifica datos. */
    @PostMapping("/confirmar")
    @Operation(summary = "Guardar la carga de muestras",
            description = "Crea una muestra padre por lote y sus alícuotas, ocupando los huecos "
                    + "indicados. Todo en una transacción.")
    @PreAuthorize("hasAuthority('MUESTRAS_CARGA_MASIVA')")
    public ResponseEntity<APIResponse> confirmar(@RequestBody TablaRequest peticion) {
        ResultadoCargaMuestras resultado =
                cargaMasivaService.confirmar(peticion.tabla(), peticion.fechaPorOmision());

        return ResponseEntity.ok(new APIResponse(resultado, resumen(resultado), HttpStatus.OK, false));
    }

    /**
     * La plantilla vacía, con su hoja de instrucciones.
     *
     * <p>Se sirve desde la propia pantalla para que el formato no se degrade con
     * el uso: si cada tanda parte del archivo de la anterior, en tres cargas las
     * columnas ya no se llaman igual.</p>
     *
     * <p>Se arma en cada descarga en vez de guardarse como archivo en el
     * classpath. Un .xlsx es un ZIP y no sobrevive a que alguien lo trate como
     * texto —el filtrado de recursos, la copia del IDE, o Git con
     * {@code core.autocrlf}—; el síntoma es que Excel ofrece «recuperar el
     * máximo de contenido posible» y no aparece nada. Generarlo cuesta
     * milisegundos y quita el binario de en medio.</p>
     */
    @GetMapping("/plantilla")
    @Operation(summary = "Descargar la plantilla vacía")
    @PreAuthorize("hasAuthority('MUESTRAS_CARGA_MASIVA')")
    public ResponseEntity<byte[]> plantilla() {
        byte[] libro = plantillaService.generar();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + PlantillaCargaMuestras.NOMBRE_ARCHIVO + "\"")
                // Sin esto algunos proxys y el propio navegador pueden servir una
                // copia vieja después de que la plantilla cambie.
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(libro.length)
                .body(libro);
    }

    private static String mensaje(PrevisualizacionCargaMuestras previa) {
        if (!previa.problemasDeEstructura().isEmpty()) {
            return "El archivo no tiene la estructura que espera la carga de muestras";
        }
        if (previa.puedeConfirmarse()) {
            return "El archivo se leyó correctamente y está listo para confirmarse";
        }
        return "El archivo se leyó, pero hay que corregir algunos datos antes de guardar";
    }

    private static String resumen(ResultadoCargaMuestras r) {
        StringBuilder sb = new StringBuilder();
        sb.append(r.alicuotasCreadas()).append(" alícuota(s) registradas en ")
          .append(r.padresCreadas()).append(" lote(s)");
        if (r.alicuotasUbicadas() > 0) {
            sb.append(", ").append(r.alicuotasUbicadas()).append(" con hueco asignado");
        }
        return sb.toString();
    }
}
