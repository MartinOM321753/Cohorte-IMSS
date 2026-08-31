package imss.gob.mx.cohorte.controllers.reportes;

import imss.gob.mx.cohorte.application.reportes.EmisionReporteApplicationService;
import imss.gob.mx.cohorte.modules.reportes.TipoReporte;
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

    @GetMapping(value = "/estudio/{id}", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(summary = "Emitir el reporte de un estudio",
               description = "Devuelve el PDF. Los datos pasan por las mismas comprobaciones de "
                           + "institución y acceso al participante que la pantalla del expediente.")
    public ResponseEntity<byte[]> reporteDeEstudio(
            @Parameter(description = "Identificador del estudio médico", required = true)
            @PathVariable Long id,
            @Parameter(description = "Plantilla a usar. Si se omite, la predeterminada de la institución; "
                                   + "si tampoco hay, el formato base del sistema.")
            @RequestParam(required = false) Long plantilla) {

        EmisionReporteApplicationService.ReporteEmitido reporte = emisionService.deEstudio(id, plantilla);

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

    @GetMapping(value = "/estudio/{id}/previsualizar", produces = MediaType.TEXT_HTML_VALUE)
    @Operation(summary = "Vista previa del reporte de un estudio",
               description = "El mismo HTML que se convierte a PDF, para verlo en pantalla sin "
                           + "generar el archivo. Preview y documento salen del mismo sitio a "
                           + "propósito: si se generaran por caminos distintos, acabarían "
                           + "enseñando cosas distintas.")
    public ResponseEntity<String> previsualizarEstudio(
            @PathVariable Long id,
            @RequestParam(required = false) Long plantilla) {
        return ResponseEntity.ok()
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(emisionService.previsualizarEstudio(id, plantilla));
    }

    @GetMapping("/campos")
    @Operation(summary = "Qué se puede insertar en una plantilla de ese tipo",
               description = "El catálogo vive en el servidor y se expone aquí para que el editor "
                           + "no tenga su propia copia: dos listas separadas acabarían ofreciendo "
                           + "campos que nadie sabe resolver.")
    public ResponseEntity<APIResponse> campos(
            @Parameter(description = "Tipo de reporte", required = true)
            @RequestParam TipoReporte tipo) {
        return ResponseEntity.ok(new APIResponse(catalogo.paraTipo(tipo), HttpStatus.OK, false));
    }
}
