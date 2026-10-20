package imss.gob.mx.cohorte.controllers.cuestionarios;

import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;

import imss.gob.mx.cohorte.services.cuestionarios.CargaCatalogoService;
import imss.gob.mx.cohorte.services.cuestionarios.CargaVolcadoService;
import imss.gob.mx.cohorte.services.cuestionarios.TareaVolcadoService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Las dos cargas masivas del módulo.
 *
 * <h3>Por qué el archivo se sube dos veces</h3>
 *
 * <p>Las dos cargas son en dos pasos: primero se previsualiza, después se confirma, y el
 * archivo viaja en ambas. Guardarlo entre peticiones evitaría la segunda subida, pero
 * obligaría a retener megabytes por usuario y por sesión, y dejaría la confirmación
 * trabajando contra una lectura vieja: si entre los dos pasos alguien cargó el mismo
 * participante o cambió el catálogo, la segunda lectura lo nota y la guardada no.</p>
 */
@RestController
@RequireModulo(ModuloSistema.CUESTIONARIOS)
@RequestMapping("/api/cuestionarios/carga")
@AllArgsConstructor
@Tag(name = "Cuestionarios - carga masiva",
        description = "Volcado de Remark y catalogo de los investigadores")
@SecurityRequirement(name = "bearerAuth")
public class CargaCuestionariosController {

    private final CargaVolcadoService volcado;
    private final TareaVolcadoService tareas;
    private final CargaCatalogoService catalogo;

    // ── Volcado de Remark ────────────────────────────────────────────────────

    @PostMapping(value = "/volcado/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Revisar un volcado sin guardar nada",
            description = "Lee el archivo, empareja las columnas con el catalogo del instrumento "
                    + "indicado en `idTipo` e interpreta cada celda. De la columna de etiqueta solo "
                    + "se saca el folio, venga la etiqueta completa o el folio pelado")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_CARGA_MASIVA')")
    public ResponseEntity<APIResponse> previsualizarVolcado(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam Long idTipo) {
        return ResponseEntity.ok(new APIResponse(volcado.previsualizar(archivo, idTipo),
                "Volcado revisado correctamente", HttpStatus.OK, false));
    }

    /**
     * Encola la carga y contesta de inmediato.
     *
     * <p>No contesta «cargado» porque no lo está: contesta con el número de la tarea.
     * Cada fila del volcado es un cuestionario de dos mil respuestas, así que un
     * archivo de cientos de participantes tarda minutos —más de lo que una petición
     * puede estar abierta sin que algo de enmedio la corte—. El cliente pregunta por
     * la tarea hasta que termina.</p>
     */
    @PostMapping(value = "/volcado/confirmar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Encolar la carga del volcado",
            description = "Contesta con la tarea, que corre en segundo plano: consultala en "
                    + "`/volcado/tareas/{id}`. Con `sobrescribir` reemplaza los cuestionarios "
                    + "que ya existian, salvo los validados: esos hay que reabrirlos a proposito")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_CARGA_MASIVA')")
    public ResponseEntity<APIResponse> confirmarVolcado(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam Long idTipo,
            @RequestParam(defaultValue = "false") boolean sobrescribir) {
        Long idTarea = tareas.encolar(archivo, idTipo, sobrescribir).getId();
        return ResponseEntity.accepted().body(new APIResponse(tareas.consultar(idTarea),
                "La carga empezo; se informa el avance", HttpStatus.ACCEPTED, false));
    }

    @GetMapping("/volcado/tareas/{id}")
    @Operation(summary = "En que va una carga de volcado",
            description = "Misma forma mientras corre y al terminar: cifras, estado y las "
                    + "filas que se omitieron con su motivo")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_CARGA_MASIVA')")
    public ResponseEntity<APIResponse> consultarCargaVolcado(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(tareas.consultar(id),
                "Carga consultada correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/volcado/tareas")
    @Operation(summary = "Las ultimas cargas de volcado de la institucion",
            description = "Para volver a ver como acabo una carga sin haberse quedado "
                    + "en la pantalla mientras corria")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_CARGA_MASIVA')")
    public ResponseEntity<APIResponse> cargasRecientes() {
        return ResponseEntity.ok(new APIResponse(tareas.recientes(),
                "Cargas consultadas correctamente", HttpStatus.OK, false));
    }

    // ── Catalogo ─────────────────────────────────────────────────────────────

    @PostMapping(value = "/catalogo/previsualizar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Revisar el catalogo sin guardar nada",
            description = "Informa los cambios que afectarian a respuestas ya guardadas")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_CATALOGO_CARGAR')")
    public ResponseEntity<APIResponse> previsualizarCatalogo(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam Long idTipo,
            @RequestParam(required = false) String nombreCatalogo) {
        return ResponseEntity.ok(new APIResponse(
                catalogo.previsualizar(archivo, idTipo, nombreCatalogo),
                "Catalogo revisado correctamente", HttpStatus.OK, false));
    }

    @PostMapping(value = "/catalogo/confirmar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cargar el catalogo",
            description = "No borra preguntas: las que el archivo ya no trae se informan y se dejan")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_CATALOGO_CARGAR')")
    public ResponseEntity<APIResponse> confirmarCatalogo(
            @RequestPart("archivo") MultipartFile archivo,
            @RequestParam Long idTipo,
            @RequestParam(required = false) String nombreCatalogo) {
        return ResponseEntity.ok(new APIResponse(
                catalogo.confirmar(archivo, idTipo, nombreCatalogo),
                "Catalogo cargado correctamente", HttpStatus.OK, false));
    }
}
