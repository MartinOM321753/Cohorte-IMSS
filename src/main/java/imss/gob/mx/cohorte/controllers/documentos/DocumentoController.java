package imss.gob.mx.cohorte.controllers.documentos;

import imss.gob.mx.cohorte.controllers.documentos.dto.DocumentoResponseDTO;
import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.documentos.Documento;
import imss.gob.mx.cohorte.modules.documentos.TipoDocumentoPaciente;
import imss.gob.mx.cohorte.modules.documentos.TipoEntidadDocumento;
import imss.gob.mx.cohorte.services.documentos.DocumentoService;
import imss.gob.mx.cohorte.utils.APIResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * REST controller para gestión de documentos almacenados en MinIO.
 * Base: /api/documentos
 *
 * Endpoints de subida (multipart):
 *   POST /api/documentos/estudio/{estudioId}
 *   POST /api/documentos/paciente/{uuid}?tipoDoc=CONSENTIMIENTO|GENERAL
 *   POST /api/documentos/muestra/{muestraId}
 *
 * Endpoints de consulta:
 *   GET /api/documentos/estudio/{estudioId}
 *   GET /api/documentos/paciente/{uuid}
 *   GET /api/documentos/paciente/{uuid}/tipo/{tipoDoc}
 *   GET /api/documentos/muestra/{muestraId}
 *   GET /api/documentos/{id}/url        → URL firmada fresca
 *
 * Eliminación:
 *   DELETE /api/documentos/{id}
 */
@RestController
@RequestMapping("/api/documentos")
public class DocumentoController {

    private final DocumentoService documentoService;
    private final MinioStorageService minioStorageService;

    public DocumentoController(DocumentoService documentoService, MinioStorageService minioStorageService) {
        this.documentoService = documentoService;
        this.minioStorageService = minioStorageService;
    }

    // ─── Helper: extrae el rol del SecurityContext ────────────────────────────────

    /**
     * Extrae el nombre del rol activo (sin prefijo ROLE_) del token JWT actual.
     * Ejemplo: "ROLE_ADMINISTRADOR" → "ADMINISTRADOR"
     */
    private String getCurrentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return "";
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("");
    }

    // ─── Upload para Estudio ──────────────────────────────────────────────────────

    @PostMapping(value = "/estudio/{estudioId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse> uploadParaEstudio(
            @PathVariable Long estudioId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "usuarioUUID") String usuarioUUID,
            @RequestParam(value = "orden", defaultValue = "0") int orden
    ) {
        documentoService.verificarPuedeSubir(getCurrentRole(), TipoEntidadDocumento.ESTUDIO);
        DocumentoResponseDTO dto = documentoService.uploadParaEstudio(file, estudioId, descripcion, usuarioUUID, orden);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Documento subido correctamente", dto, false, HttpStatus.CREATED));
    }

    // ─── Upload para Paciente ─────────────────────────────────────────────────────

    @PostMapping(value = "/paciente/{uuid}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse> uploadParaPaciente(
            @PathVariable String uuid,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "tipoDoc", defaultValue = "GENERAL") TipoDocumentoPaciente tipoDoc,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "usuarioUUID") String usuarioUUID
    ) {
        TipoEntidadDocumento tipoEntidad = tipoDoc == TipoDocumentoPaciente.CONSENTIMIENTO
                ? TipoEntidadDocumento.PACIENTE_CONSENTIMIENTO
                : TipoEntidadDocumento.PACIENTE_GENERAL;
        documentoService.verificarPuedeSubir(getCurrentRole(), tipoEntidad);
        DocumentoResponseDTO dto = documentoService.uploadParaPaciente(file, uuid, tipoDoc, descripcion, usuarioUUID);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Documento subido correctamente", dto, false, HttpStatus.CREATED));
    }

    // ─── Upload para Muestra ──────────────────────────────────────────────────────

    @PostMapping(value = "/muestra/{muestraId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse> uploadParaMuestra(
            @PathVariable Long muestraId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "usuarioUUID") String usuarioUUID
    ) {
        documentoService.verificarPuedeSubir(getCurrentRole(), TipoEntidadDocumento.MUESTRA);
        DocumentoResponseDTO dto = documentoService.uploadParaMuestra(file, muestraId, descripcion, usuarioUUID);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Documento subido correctamente", dto, false, HttpStatus.CREATED));
    }

    // ─── Consulta ────────────────────────────────────────────────────────────────

    @GetMapping("/estudio/{estudioId}")
    public ResponseEntity<APIResponse> getByEstudio(@PathVariable Long estudioId) {
        documentoService.verificarPuedeVer(getCurrentRole(), TipoEntidadDocumento.ESTUDIO);
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByEstudio(estudioId);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/paciente/{uuid}")
    public ResponseEntity<APIResponse> getByPaciente(@PathVariable String uuid) {
        documentoService.verificarPuedeVer(getCurrentRole(), TipoEntidadDocumento.PACIENTE_GENERAL);
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByPaciente(uuid);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/paciente/{uuid}/tipo/{tipoDoc}")
    public ResponseEntity<APIResponse> getByPacienteYTipo(
            @PathVariable String uuid,
            @PathVariable TipoDocumentoPaciente tipoDoc
    ) {
        TipoEntidadDocumento tipoEntidadVer = tipoDoc == TipoDocumentoPaciente.CONSENTIMIENTO
                ? TipoEntidadDocumento.PACIENTE_CONSENTIMIENTO
                : TipoEntidadDocumento.PACIENTE_GENERAL;
        documentoService.verificarPuedeVer(getCurrentRole(), tipoEntidadVer);
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByPacienteYTipo(uuid, tipoDoc);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/muestra/{muestraId}")
    public ResponseEntity<APIResponse> getByMuestra(@PathVariable Long muestraId) {
        documentoService.verificarPuedeVer(getCurrentRole(), TipoEntidadDocumento.MUESTRA);
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByMuestra(muestraId);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    /** Devuelve una URL firmada fresca (útil cuando la URL del DTO ya expiró). */
    @GetMapping("/{id}/url")
    public ResponseEntity<APIResponse> getDownloadUrl(@PathVariable Long id) {
        String url = documentoService.getDownloadUrl(id);
        return ResponseEntity.ok(new APIResponse("URL generada", url, false, HttpStatus.OK));
    }

    // ─── Descarga segura (streaming autenticado) ──────────────────────────────────

    /**
     * Descarga un archivo pasando el contenido a través del backend.
     * Requiere JWT válido — MinIO nunca queda expuesto directamente al cliente.
     * Soporta tanto descarga (attachment) como visualización inline (inline).
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable Long id,
            @RequestParam(value = "inline", defaultValue = "false") boolean inline
    ) {
        Documento doc = documentoService.getDocumentoById(id);
        documentoService.verificarPuedeVer(getCurrentRole(), doc.getTipoEntidad());

        String mimeType = (doc.getMimeType() != null && !doc.getMimeType().isBlank())
                ? doc.getMimeType()
                : "application/octet-stream";

        String encodedName = URLEncoder.encode(doc.getNombreOriginal(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        String disposition = inline
                ? "inline; filename=\"" + encodedName + "\""
                : "attachment; filename=\"" + encodedName + "\"";

        StreamingResponseBody stream = outputStream -> {
            try (InputStream is = minioStorageService.getObjectStream(doc.getObjectKey())) {
                is.transferTo(outputStream);
            }
        };

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(stream);
    }

    // ─── Eliminación ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse> delete(@PathVariable Long id) {
        documentoService.verificarPuedeEliminar(getCurrentRole());
        documentoService.deleteDocumento(id);
        return ResponseEntity.ok(new APIResponse("Documento eliminado correctamente", null, false, HttpStatus.OK));
    }
}
