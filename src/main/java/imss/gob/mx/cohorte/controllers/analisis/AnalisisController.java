package imss.gob.mx.cohorte.controllers.analisis;

import imss.gob.mx.cohorte.application.analisis.AnalisisApplicationService;
import imss.gob.mx.cohorte.controllers.analisis.dto.AgregadoRequestDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.CorrelacionDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.CorrelacionRequestDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.DispersionTemporalDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.DispersionTemporalRequestDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.InstitucionAlcanceDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.SerieAgregadaDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.VistaAnalisisDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.VistaAnalisisRequestDTO;
import imss.gob.mx.cohorte.modules.analisis.Metrica;
import imss.gob.mx.cohorte.services.analisis.AnalisisExportService;
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
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Módulo de Análisis: catálogo de métricas y (en fases siguientes) agregación
 * y exportación de resultados de la cohorte con alcance supra-institucional.
 */
@RestController
@RequestMapping("/api/analisis")
@RequiredArgsConstructor
@Tag(name = "Análisis",
     description = "Análisis agregado de resultados de la cohorte por institución, sexo, edad y rangos")
@SecurityRequirement(name = "bearerAuth")
public class AnalisisController {

    private final AnalisisApplicationService appService;

    @GetMapping("/metricas")
    @Operation(summary = "Catálogo de métricas analizables",
               description = "Variables de estudios médicos, exámenes y somatometría disponibles "
                           + "en el alcance del usuario (su subárbol de instituciones; todas si es ROOT).")
    @PreAuthorize("hasAuthority('ANALISIS_ACCEDER')")
    public ResponseEntity<APIResponse> getMetricas() {
        List<Metrica> metricas = appService.catalogoMetricas();
        return ResponseEntity.ok(
                new APIResponse("Catálogo de métricas obtenido", metricas, false, HttpStatus.OK));
    }

    @GetMapping("/instituciones")
    @Operation(summary = "Instituciones del alcance",
               description = "Instituciones que el usuario puede agregar (su subárbol; todas si es ROOT), "
                           + "para el filtro de institución del módulo.")
    @PreAuthorize("hasAuthority('ANALISIS_ACCEDER')")
    public ResponseEntity<APIResponse> getInstituciones() {
        List<InstitucionAlcanceDTO> inst = appService.institucionesAlcance();
        return ResponseEntity.ok(
                new APIResponse("Instituciones del alcance obtenidas", inst, false, HttpStatus.OK));
    }

    @PostMapping("/agregado")
    @Operation(summary = "Agregar métricas",
               description = "Devuelve series ya resumidas (no filas crudas) por las dimensiones pedidas "
                           + "(institución, sexo, rango de edad), con supresión de celdas con menos de k "
                           + "participantes. El alcance de instituciones se resuelve en el servidor.")
    @PreAuthorize("hasAuthority('ANALISIS_ACCEDER')")
    public ResponseEntity<APIResponse> agregar(@Validated @RequestBody AgregadoRequestDTO req) {
        List<SerieAgregadaDTO> series = appService.agregar(req);
        return ResponseEntity.ok(
                new APIResponse("Agregación calculada", series, false, HttpStatus.OK));
    }

    @PostMapping("/exportar")
    @Operation(summary = "Exportar agregados (CSV)",
               description = "Descarga en CSV las mismas series agregadas que se ven en pantalla, ya "
                           + "depuradas por la supresión k-anónima. El separador puede ser coma (herramientas "
                           + "de análisis) o punto y coma (Excel en español).")
    @PreAuthorize("hasAuthority('ANALISIS_EXPORTAR')")
    public ResponseEntity<byte[]> exportarAgregado(
            @Validated @RequestBody AgregadoRequestDTO req,
            @RequestParam(name = "separador", required = false) String separador) {
        AnalisisExportService.Descarga descarga = appService.exportarAgregado(req, separador);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + descarga.nombreArchivo() + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(descarga.contenido());
    }

    @PostMapping("/correlacion")
    @Operation(summary = "Correlación entre dos métricas",
               description = "Empareja por participante dos métricas numéricas y devuelve la recta de ajuste "
                           + "y R². Los estadísticos de ajuste son agregados (con supresión k-anónima); los "
                           + "puntos individuales solo se incluyen si el usuario tiene ANALISIS_EXPORTA_CRUDO.")
    @PreAuthorize("hasAuthority('ANALISIS_ACCEDER')")
    public ResponseEntity<APIResponse> correlacion(@Validated @RequestBody CorrelacionRequestDTO req) {
        CorrelacionDTO correlacion = appService.correlacion(req);
        return ResponseEntity.ok(
                new APIResponse("Correlación calculada", correlacion, false, HttpStatus.OK));
    }

    @PostMapping("/dispersion-temporal")
    @Operation(summary = "Dispersión temporal con línea de tendencia",
               description = "Por cada corte de seguimiento (bucket de la fecha) devuelve la media (línea de "
                           + "tendencia) y, con ANALISIS_EXPORTA_CRUDO, los puntos individuales. Aplica "
                           + "supresión k-anónima por celda (grupo × corte).")
    @PreAuthorize("hasAuthority('ANALISIS_ACCEDER')")
    public ResponseEntity<APIResponse> dispersionTemporal(@Validated @RequestBody DispersionTemporalRequestDTO req) {
        DispersionTemporalDTO dto = appService.dispersionTemporal(req);
        return ResponseEntity.ok(
                new APIResponse("Dispersión temporal calculada", dto, false, HttpStatus.OK));
    }

    // ── Vistas guardadas ──────────────────────────────────────────────────────

    @GetMapping("/vistas")
    @Operation(summary = "Listar vistas guardadas del usuario")
    @PreAuthorize("hasAuthority('ANALISIS_ACCEDER')")
    public ResponseEntity<APIResponse> listarVistas() {
        List<VistaAnalisisDTO> vistas = appService.listarVistas();
        return ResponseEntity.ok(new APIResponse("Vistas obtenidas", vistas, false, HttpStatus.OK));
    }

    @PostMapping("/vistas")
    @Operation(summary = "Guardar una vista de análisis")
    @PreAuthorize("hasAuthority('ANALISIS_ACCEDER')")
    public ResponseEntity<APIResponse> guardarVista(@Validated @RequestBody VistaAnalisisRequestDTO req) {
        VistaAnalisisDTO v = appService.guardarVista(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Vista guardada", v, false, HttpStatus.CREATED));
    }

    @DeleteMapping("/vistas/{uuid}")
    @Operation(summary = "Eliminar una vista guardada del usuario")
    @PreAuthorize("hasAuthority('ANALISIS_ACCEDER')")
    public ResponseEntity<APIResponse> eliminarVista(@PathVariable String uuid) {
        appService.eliminarVista(uuid);
        return ResponseEntity.ok(new APIResponse("Vista eliminada", null, false, HttpStatus.OK));
    }

    @PostMapping("/exportar-crudo")
    @Operation(summary = "Exportar datos crudos (CSV/XLSX)",
               description = "Descarga una fila por resultado a nivel participante, SIN supresión k-anónima. "
                           + "Es el carril de mayor exposición: requiere el permiso ANALISIS_EXPORTA_CRUDO y "
                           + "queda registrado en la bitácora de acciones.")
    @PreAuthorize("hasAuthority('ANALISIS_EXPORTA_CRUDO')")
    public ResponseEntity<byte[]> exportarCrudo(
            @Validated @RequestBody AgregadoRequestDTO req,
            @RequestParam(name = "formato", required = false, defaultValue = "csv") String formato,
            @RequestParam(name = "separador", required = false) String separador) {
        AnalisisExportService.Descarga descarga = appService.exportarCrudo(req, formato, separador);
        MediaType tipo = "xlsx".equalsIgnoreCase(formato)
                ? MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                : new MediaType("text", "csv", StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + descarga.nombreArchivo() + "\"")
                .contentType(tipo)
                .body(descarga.contenido());
    }
}
