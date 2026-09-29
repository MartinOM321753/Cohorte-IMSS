package imss.gob.mx.cohorte.controllers.estudios;

import imss.gob.mx.cohorte.services.importacion.CargaMasivaEstudiosService;
import imss.gob.mx.cohorte.services.importacion.PlantillaCargaResultados;
import imss.gob.mx.cohorte.services.importacion.PrevisualizacionCarga;
import imss.gob.mx.cohorte.services.importacion.ResultadoCarga;
import imss.gob.mx.cohorte.services.importacion.TablaLeida;
import imss.gob.mx.cohorte.utils.APIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;

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
    private final PlantillaCargaResultados plantillaService;


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

    /** Cuántas versiones de plantilla hay para un tipo, para que la pantalla ofrezca elegir. */
    public record VersionesPlantilla(int versiones) {}

    /**
     * Cuántas versiones tiene la plantilla de un tipo de estudio.
     *
     * <p>Es una por cada juego de alias: si algún parámetro tiene el aparato
     * titulando su columna de dos formas distintas, hay dos versiones. Con una
     * sola la pantalla no ofrece elegir.</p>
     */
    @GetMapping("/plantilla/versiones")
    public ResponseEntity<APIResponse> versionesPlantilla(@RequestParam("idTipoEstudio") Long idTipoEstudio) {
        var datos = cargaMasivaService.datosPlantilla(idTipoEstudio);
        int versiones = plantillaService.numeroDeVersiones(datos.columnas());
        return ResponseEntity.ok(new APIResponse(
                new VersionesPlantilla(versiones), "OK", HttpStatus.OK, false));
    }

    /**
     * La plantilla vacía de un tipo de estudio, con folio, fecha y una columna por
     * parámetro en uso.
     *
     * <p>Se arma en cada descarga en vez de guardarse como archivo: un .xlsx es un
     * ZIP y no sobrevive a que Git o el filtrado de recursos lo traten como texto.
     * El {@code no-store} evita que un proxy sirva una copia vieja después de
     * cambiar los alias.</p>
     *
     * @param version qué juego de alias usar en los encabezados (base 1)
     */
    @GetMapping("/plantilla")
    public ResponseEntity<byte[]> plantilla(
            @RequestParam("idTipoEstudio") Long idTipoEstudio,
            @RequestParam(value = "version", defaultValue = "1") int version) {

        var datos = cargaMasivaService.datosPlantilla(idTipoEstudio);
        byte[] libro = plantillaService.generar(datos.titulo(), datos.columnas(), version);
        String nombre = nombreArchivo("plantilla-estudio-" + datos.titulo(),
                plantillaService.numeroDeVersiones(datos.columnas()) > 1 ? version : null);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion(nombre))
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentLength(libro.length)
                .body(libro);
    }

    /** Un nombre de archivo legible: sin acentos, con guiones y con la versión si hay varias. */
    private static String nombreArchivo(String base, Integer version) {
        String slug = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("(^-|-$)", "")
                .toLowerCase();
        if (slug.isEmpty()) slug = "plantilla";
        return version != null ? slug + "-v" + version + ".xlsx" : slug + ".xlsx";
    }

    /**
     * La cabecera Content-Disposition con el nombre en ASCII y también en UTF-8:
     * el ASCII es el respaldo para clientes viejos y el {@code filename*} es el que
     * conserva cualquier carácter especial.
     */
    private static String disposicion(String nombre) {
        String utf8 = URLEncoder.encode(nombre, StandardCharsets.UTF_8).replace("+", "%20");
        return "attachment; filename=\"" + nombre + "\"; filename*=UTF-8''" + utf8;
    }
}
