package imss.gob.mx.cohorte.controllers.reportes;

import imss.gob.mx.cohorte.application.reportes.EmisionReporteApplicationService;
import imss.gob.mx.cohorte.services.reportes.CatalogoCamposReporte;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Emisión de reportes en PDF")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

    private final EmisionReporteApplicationService emisionService;
    private final CatalogoCamposReporte catalogo;

    // ── Participante: el reporte que puede combinar varios estudios ──────────

    @GetMapping(value = "/participante/{uuid}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Emitir un reporte de un participante",
               description = "La plantilla decide qué sale: puede combinar parámetros de estudios "
                           + "distintos, tablas de resultados y evidencias de unos u otros. Los datos "
                           + "pasan por las mismas comprobaciones de acceso que el expediente.")
    public ResponseEntity<byte[]> reporteDeParticipante(
            @PathVariable String uuid,
            @Parameter(description = "Plantilla con la que se emite", required = true)
            @RequestParam Long plantilla) {
        return comoPdf(emisionService.deParticipante(uuid, plantilla));
    }

    @GetMapping(value = "/participante/{uuid}/previsualizar", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Vista previa del reporte de un participante")
    public ResponseEntity<String> previsualizarParticipante(
            @PathVariable String uuid, @RequestParam Long plantilla) {
        return comoHtml(emisionService.previsualizarParticipante(uuid, plantilla));
    }

    // ── Estudio concreto ─────────────────────────────────────────────────────

    @GetMapping(value = "/estudio/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Emitir el reporte de un estudio",
               description = "Sin plantilla se usa el formato base del sistema, para que una "
                           + "institución que aún no ha diseñado ninguna pueda emitir igual.")
    public ResponseEntity<byte[]> reporteDeEstudio(
            @PathVariable Long id,
            @RequestParam(required = false) Long plantilla) {
        return comoPdf(emisionService.deEstudio(id, plantilla));
    }

    @GetMapping(value = "/estudio/{id}/previsualizar", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Vista previa del reporte de un estudio",
               description = "El mismo HTML que se convierte a PDF. Preview y documento salen del "
                           + "mismo sitio a propósito: por caminos distintos acabarían enseñando "
                           + "cosas distintas.")
    public ResponseEntity<String> previsualizarEstudio(
            @PathVariable Long id, @RequestParam(required = false) Long plantilla) {
        return comoHtml(emisionService.previsualizarEstudio(id, plantilla));
    }

    // ── Catálogo para el editor ──────────────────────────────────────────────

    @GetMapping("/campos")
    @Operation(summary = "Qué se puede insertar en una plantilla",
               description = "Se arma desde el catálogo real de la institución: sus tipos de estudio "
                           + "y los parámetros de cada uno. Vive en el servidor para que el editor no "
                           + "tenga su propia copia y acabe ofreciendo campos que nadie sabe resolver.")
    public ResponseEntity<APIResponse> campos() {
        return ResponseEntity.ok(new APIResponse(catalogo.todos(), HttpStatus.OK, false));
    }

    // ── Utilidades de respuesta ──────────────────────────────────────────────

    private ResponseEntity<byte[]> comoPdf(EmisionReporteApplicationService.ReporteEmitido reporte) {
        // inline: el navegador lo abre en su visor en vez de bajarlo a ciegas. Quien
        // quiera guardarlo lo hace desde ahí, y quien solo iba a mirarlo se ahorra un
        // archivo en la carpeta de descargas.
        ContentDisposition disposicion = ContentDisposition.inline()
                .filename(reporte.nombreArchivo(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicion.toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(reporte.contenido());
    }

    private ResponseEntity<String> comoHtml(String html) {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(html);
    }
}
