package imss.gob.mx.cohorte.controllers.documentos_publicos;

import imss.gob.mx.cohorte.controllers.documentos_publicos.dto.CategoriaDocumentoPublicoRequestDTO;
import imss.gob.mx.cohorte.controllers.documentos_publicos.dto.DocumentoPublicoRequestDTO;
import imss.gob.mx.cohorte.controllers.documentos_publicos.dto.DocumentoPublicoResponseDTO;
import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.documentos_publicos.CategoriaDocumentoPublico;
import imss.gob.mx.cohorte.modules.documentos_publicos.DocumentoPublico;
import imss.gob.mx.cohorte.services.documentos_publicos.CategoriaDocumentoPublicoService;
import imss.gob.mx.cohorte.services.documentos_publicos.DocumentoPublicoService;
import imss.gob.mx.cohorte.utils.APIResponse;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.MinioUnavailableException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/documentos-publicos")
@RequiredArgsConstructor
public class DocumentoPublicoController {

    private final DocumentoPublicoService documentoService;
    private final CategoriaDocumentoPublicoService categoriaService;
    private final MinioStorageService minioStorageService;

    // ─── Documentos ────────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("fechaPublicacion") LocalDate fechaPublicacion,
            @RequestParam(value = "nombreMostrar", required = false) String nombreMostrar,
            @RequestParam(value = "fase", required = false) String fase,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "categoriaId", required = false) Long categoriaId,
            @RequestParam(value = "autor", required = false) String autor,
            @RequestParam("usuarioUUID") String usuarioUUID
    ) {
        DocumentoPublicoRequestDTO dto = new DocumentoPublicoRequestDTO();
        dto.setFechaPublicacion(fechaPublicacion);
        dto.setNombreMostrar(nombreMostrar);
        dto.setFase(fase);
        dto.setDescripcion(descripcion);
        dto.setCategoriaId(categoriaId);
        dto.setAutor(autor);

        DocumentoPublicoResponseDTO result = documentoService.upload(file, dto, usuarioUUID);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Documento público subido correctamente", result, false, HttpStatus.CREATED));
    }

    @GetMapping
    public ResponseEntity<APIResponse> getAll() {
        List<DocumentoPublicoResponseDTO> docs = documentoService.getAllForAdmin();
        return ResponseEntity.ok(new APIResponse("Documentos públicos", docs, false, HttpStatus.OK));
    }

    @PutMapping("/{id}")
    public ResponseEntity<APIResponse> update(
            @PathVariable Long id,
            @Validated @RequestBody DocumentoPublicoRequestDTO dto
    ) {
        DocumentoPublicoResponseDTO result = documentoService.update(id, dto);
        return ResponseEntity.ok(new APIResponse("Documento actualizado", result, false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse> delete(@PathVariable Long id) {
        documentoService.delete(id);
        return ResponseEntity.ok(new APIResponse("Documento eliminado", null, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id) {
        if (!minioStorageService.isAvailable()) {
            throw new MinioUnavailableException();
        }
        DocumentoPublico doc = documentoService.getEntityById(id);
        if (doc.getObjectKey() == null) {
            throw new ObjNotFoundException("El documento no tiene archivo asociado");
        }
        if (!minioStorageService.objectExists(doc.getObjectKey())) {
            throw new ObjNotFoundException("El archivo no se encontró en el almacenamiento");
        }
        String mimeType = doc.getMimeType() != null && !doc.getMimeType().isBlank()
                ? doc.getMimeType() : "application/octet-stream";
        String rfc5987Name = URLEncoder.encode(doc.getNombreOriginal(), StandardCharsets.UTF_8).replace("+", "%20");
        String asciiFallback = doc.getNombreOriginal().replaceAll("[^\\x20-\\x7E]", "_");
        String disposition = "inline; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + rfc5987Name;

        StreamingResponseBody stream = out -> {
            try (InputStream is = minioStorageService.getObjectStream(doc.getObjectKey())) {
                is.transferTo(out);
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .body(stream);
    }

    // ─── Categorías ──────────────────────────────────────────────────────────

    @GetMapping("/categorias")
    public ResponseEntity<APIResponse> getAllCategorias() {
        List<CategoriaDocumentoPublico> cats = categoriaService.getAll();
        return ResponseEntity.ok(new APIResponse("Categorías", cats, false, HttpStatus.OK));
    }

    @GetMapping("/categorias/activas")
    public ResponseEntity<APIResponse> getCategoriasActivas() {
        List<CategoriaDocumentoPublico> cats = categoriaService.getAllActivas();
        return ResponseEntity.ok(new APIResponse("Categorías activas", cats, false, HttpStatus.OK));
    }

    @PostMapping("/categorias")
    public ResponseEntity<APIResponse> createCategoria(@Validated @RequestBody CategoriaDocumentoPublicoRequestDTO dto) {
        CategoriaDocumentoPublico cat = categoriaService.create(dto.getNombre());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Categoría creada", cat, false, HttpStatus.CREATED));
    }

    @PutMapping("/categorias/{id}")
    public ResponseEntity<APIResponse> updateCategoria(
            @PathVariable Long id,
            @Validated @RequestBody CategoriaDocumentoPublicoRequestDTO dto
    ) {
        CategoriaDocumentoPublico cat = categoriaService.update(id, dto.getNombre());
        return ResponseEntity.ok(new APIResponse("Categoría actualizada", cat, false, HttpStatus.OK));
    }

    @PatchMapping("/categorias/{id}/toggle")
    public ResponseEntity<APIResponse> toggleCategoria(@PathVariable Long id) {
        CategoriaDocumentoPublico cat = categoriaService.toggleActivo(id);
        String msg = cat.getActivo() ? "Categoría activada" : "Categoría desactivada";
        return ResponseEntity.ok(new APIResponse(msg, cat, false, HttpStatus.OK));
    }
}
