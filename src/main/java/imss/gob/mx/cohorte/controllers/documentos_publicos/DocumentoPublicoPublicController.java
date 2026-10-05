package imss.gob.mx.cohorte.controllers.documentos_publicos;

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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/publico/documentos")
@RequiredArgsConstructor
public class DocumentoPublicoPublicController {

    private final DocumentoPublicoService documentoService;
    private final CategoriaDocumentoPublicoService categoriaService;
    private final MinioStorageService minioStorageService;

    @GetMapping("/{idInstitucion}")
    public ResponseEntity<APIResponse> getDocumentosPublicos(@PathVariable Long idInstitucion) {
        List<DocumentoPublicoResponseDTO> docs = documentoService.getAllPublicByInstitucion(idInstitucion);
        return ResponseEntity.ok(new APIResponse("Documentos públicos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/{idInstitucion}/categorias")
    public ResponseEntity<APIResponse> getCategoriasPublicas(@PathVariable Long idInstitucion) {
        List<CategoriaDocumentoPublico> cats = categoriaService.getAllActivasByInstitucion(idInstitucion);
        return ResponseEntity.ok(new APIResponse("Categorías", cats, false, HttpStatus.OK));
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<StreamingResponseBody> descargar(
            @PathVariable Long id,
            @RequestParam(value = "inline", defaultValue = "true") boolean inline
    ) {
        if (!minioStorageService.isAvailable()) {
            throw new MinioUnavailableException();
        }
        DocumentoPublico doc = documentoService.getEntityById(id);
        if (!Boolean.TRUE.equals(doc.getActivo())) {
            throw new ObjNotFoundException("Documento no disponible");
        }
        if (doc.getObjectKey() == null) {
            throw new ObjNotFoundException("El documento no tiene archivo asociado");
        }
        if (!doc.getObjectKey().startsWith("documentos-publicos/")) {
            throw new ObjNotFoundException("Documento no disponible");
        }
        if (!minioStorageService.objectExists(doc.getObjectKey())) {
            throw new ObjNotFoundException("El archivo no se encontró en el almacenamiento");
        }

        String mimeType = doc.getMimeType() != null && !doc.getMimeType().isBlank()
                ? doc.getMimeType() : "application/octet-stream";
        String fileName = doc.getNombreMostrar() != null ? doc.getNombreMostrar() : doc.getNombreOriginal();
        String rfc5987Name = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
        String asciiFallback = fileName.replaceAll("[^\\x20-\\x7E]", "_");
        String disposition = (inline ? "inline" : "attachment")
                + "; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + rfc5987Name;

        StreamingResponseBody stream = out -> {
            try (InputStream is = minioStorageService.getObjectStream(doc.getObjectKey())) {
                is.transferTo(out);
            }
        };
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(stream);
    }
}
