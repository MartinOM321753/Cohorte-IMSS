package imss.gob.mx.cohorte.controllers.reportes;

import imss.gob.mx.cohorte.application.reportes.ImagenReporteApplicationService;
import imss.gob.mx.cohorte.controllers.reportes.dto.ImagenReporteResponseDTO;
import imss.gob.mx.cohorte.controllers.reportes.dto.RenombrarImagenDTO;
import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.reportes.ImagenReporte;
import imss.gob.mx.cohorte.utils.APIResponse;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.MinioUnavailableException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;

/**
 * La galería de imágenes con la que se arman las plantillas: logos, sellos, membretes.
 *
 * <p>Los bytes siempre pasan por aquí. MinIO no se expone al cliente y el diseño no
 * guarda ninguna URL, solo {@code imagen:{id}} — así la misma plantilla funciona en
 * local, en pruebas y en producción sin tocarle nada.</p>
 */
@RestController
@RequestMapping("/api/reportes/imagenes")
@RequiredArgsConstructor
@Tag(name = "Imágenes de reporte", description = "Galería de imágenes por institución")
@SecurityRequirement(name = "bearerAuth")
public class ImagenReporteController {

    private final ImagenReporteApplicationService applicationService;
    private final MinioStorageService minioStorageService;

    @GetMapping
    @Operation(summary = "Listar las imágenes de la institución")
    public ResponseEntity<APIResponse> listar() {
        List<ImagenReporteResponseDTO> imagenes = applicationService.listar().stream()
                .map(ImagenReporteResponseDTO::de).toList();
        return ResponseEntity.ok(new APIResponse(imagenes, HttpStatus.OK, false));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir una imagen",
               description = "PNG o JPEG, hasta 2 MB. Sin nombre se usa el del archivo; "
                           + "si ese ya está tomado se numera en vez de rechazar la subida.")
    public ResponseEntity<APIResponse> subir(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "nombre", required = false) String nombre) {
        ImagenReporte subida = applicationService.subir(file, nombre);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                "Imagen subida", ImagenReporteResponseDTO.de(subida), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}/nombre")
    @Operation(summary = "Renombrar una imagen de la galería")
    public ResponseEntity<APIResponse> renombrar(
            @PathVariable Long id, @Valid @RequestBody RenombrarImagenDTO dto) {
        ImagenReporte imagen = applicationService.renombrar(id, dto.getNombre());
        return ResponseEntity.ok(new APIResponse(
                "Imagen renombrada", ImagenReporteResponseDTO.de(imagen), false, HttpStatus.OK));
    }

    @GetMapping("/{id}/usos")
    @Operation(summary = "Qué plantillas usan esta imagen",
               description = "Para avisar antes de intentar borrarla.")
    public ResponseEntity<APIResponse> usos(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(
                applicationService.plantillasQueLaUsan(id), HttpStatus.OK, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una imagen",
               description = "Se rechaza si alguna plantilla la usa: el hueco no se vería "
                           + "hasta emitir un documento, y ahí ya nadie relaciona una cosa con la otra.")
    public ResponseEntity<APIResponse> eliminar(@PathVariable Long id) {
        applicationService.eliminar(id);
        return ResponseEntity.ok(new APIResponse("Imagen eliminada", HttpStatus.OK, false));
    }

    /**
     * Los bytes de la imagen.
     *
     * <p>Se comprueba MinIO <b>antes</b> de fijar el Content-Type del archivo, igual
     * que en la descarga de documentos: si se lanzara después, Spring ya habría
     * comprometido las cabeceras y el 503 saldría como un cuerpo roto en vez de como
     * un JSON legible.</p>
     */
    @GetMapping("/{id}/contenido")
    @Operation(summary = "El contenido de la imagen, servido por el backend")
    public ResponseEntity<StreamingResponseBody> contenido(@PathVariable Long id) {
        if (!minioStorageService.isAvailable()) {
            throw new MinioUnavailableException();
        }

        ImagenReporte imagen = applicationService.obtener(id);
        InputStream origen = applicationService.contenido(id);

        StreamingResponseBody cuerpo = salida -> {
            try (InputStream in = origen) {
                in.transferTo(salida);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getContentType()))
                // El contenido de un id no cambia nunca: sustituir una imagen crea otra
                // fila. Por eso se puede cachear sin miedo, y el editor deja de pedir el
                // mismo logo en cada repintado.
                .cacheControl(CacheControl.maxAge(Duration.ofHours(12)).cachePrivate())
                .body(cuerpo);
    }
}
