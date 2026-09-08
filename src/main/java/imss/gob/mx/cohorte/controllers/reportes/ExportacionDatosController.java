package imss.gob.mx.cohorte.controllers.reportes;

import imss.gob.mx.cohorte.application.reportes.ExportacionDatosApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes/exportacion")
@RequiredArgsConstructor
@Tag(name = "Exportación de datos",
     description = "Bajar los datos de varios participantes, con las fórmulas ya aplicadas")
@SecurityRequirement(name = "bearerAuth")
public class ExportacionDatosController {

    private final ExportacionDatosApplicationService applicationService;

    @Data
    public static class ExportacionRequestDTO {

        /**
         * Las columnas, por su clave del catálogo de campos.
         *
         * <p>Las mismas que se pueden poner en una plantilla, fórmulas incluidas:
         * {@code participante.folio}, {@code estudio.7.param.85}, {@code formula.3}.</p>
         */
        @NotEmpty(message = "Hay que elegir al menos una columna")
        private List<String> claves;

        /** Qué participantes. Vacío significa todos los que el usuario alcanza. */
        private List<String> uuids;

        /**
         * Coma o punto y coma.
         *
         * <p>La coma es lo que esperan las herramientas de análisis; el punto y coma lo
         * que espera Excel en español, que con comas mete la fila entera en una celda.</p>
         */
        private String separador;
    }

    @PostMapping
    @Operation(summary = "Descargar los datos en CSV",
               description = "El archivo sale en UTF-8 con marca de orden de bytes, para que Excel "
                           + "no rompa los acentos. Los valores se calculan con el mismo motor que "
                           + "usa la emisión de reportes.")
    public ResponseEntity<byte[]> descargar(@RequestBody ExportacionRequestDTO dto) {
        var descarga = applicationService.aCsv(dto.getClaves(), dto.getUuids(), dto.getSeparador());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + descarga.nombreArchivo() + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(descarga.contenido());
    }
}
