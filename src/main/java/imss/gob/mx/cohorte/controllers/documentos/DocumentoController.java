package imss.gob.mx.cohorte.controllers.documentos;

import imss.gob.mx.cohorte.controllers.documentos.dto.DocumentoResponseDTO;
import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.documentos.Documento;
import imss.gob.mx.cohorte.modules.documentos.DocumentoAccessToken;
import imss.gob.mx.cohorte.modules.documentos.TipoDocumentoPaciente;
import imss.gob.mx.cohorte.modules.documentos.TipoEntidadDocumento;
import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.documentos.DocumentoAccessTokenService;
import imss.gob.mx.cohorte.services.documentos.DocumentoService;
import imss.gob.mx.cohorte.services.impresion.ConfiguracionEtiquetaService;
import imss.gob.mx.cohorte.services.impresion.DirectPrintService;
import imss.gob.mx.cohorte.services.impresion.ZplLabelService;
import imss.gob.mx.cohorte.utils.APIResponse;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.MinioUnavailableException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
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
import java.util.Map;

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
    private final ZplLabelService zplLabelService;
    private final DirectPrintService directPrintService;
    private final ConfiguracionEtiquetaService configuracionEtiquetaService;
    private final InstitucionContextService institucionCtx;
    private final DocumentoAccessTokenService accessTokenService;

    public DocumentoController(DocumentoService documentoService,
                                MinioStorageService minioStorageService,
                                ZplLabelService zplLabelService,
                                DirectPrintService directPrintService,
                                ConfiguracionEtiquetaService configuracionEtiquetaService,
                                InstitucionContextService institucionCtx,
                                DocumentoAccessTokenService accessTokenService) {
        this.documentoService = documentoService;
        this.minioStorageService = minioStorageService;
        this.zplLabelService = zplLabelService;
        this.directPrintService = directPrintService;
        this.configuracionEtiquetaService = configuracionEtiquetaService;
        this.institucionCtx = institucionCtx;
        this.accessTokenService = accessTokenService;
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

    // ── Listado de documentos — metadata visible para todos los roles autenticados ──
    // La autorización para descargar el contenido se refleja en el campo
    // `puedeDescargar` de cada DocumentoResponseDTO y se aplica también en el
    // endpoint /download (doble capa de seguridad).

    @GetMapping("/estudio/{estudioId}")
    public ResponseEntity<APIResponse> getByEstudio(@PathVariable Long estudioId) {
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByEstudio(estudioId);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/paciente/{uuid}")
    public ResponseEntity<APIResponse> getByPaciente(@PathVariable String uuid) {
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByPaciente(uuid);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/paciente/{uuid}/tipo/{tipoDoc}")
    public ResponseEntity<APIResponse> getByPacienteYTipo(
            @PathVariable String uuid,
            @PathVariable TipoDocumentoPaciente tipoDoc
    ) {
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByPacienteYTipo(uuid, tipoDoc);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/muestra/{muestraId}")
    public ResponseEntity<APIResponse> getByMuestra(@PathVariable Long muestraId) {
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
     * <p>
     * Si MinIO no está disponible lanza {@link MinioUnavailableException} ANTES de
     * fijar el Content-Type del archivo; el GlobalExceptionHandler la convierte en
     * un 503 JSON limpio sin conflicto de serialización.
     */
    @GetMapping("/{id}/download")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable Long id,
            @RequestParam(value = "inline", defaultValue = "false") boolean inline
    ) {
        // ── Verificar MinIO ANTES de fijar Content-Type del archivo ─────────────
        // Lanzar excepción (no retornar ResponseEntity) para que el tipo de retorno
        // siga siendo ResponseEntity<StreamingResponseBody> y Spring resuelva el
        // converter correcto en el camino feliz.
        if (!minioStorageService.isAvailable()) {
            throw new MinioUnavailableException();
        }

        Documento doc = documentoService.getDocumentoById(id);
        documentoService.verificarPuedeVer(getCurrentRole(), doc.getTipoEntidad());

        // ── Verificar que el archivo existe en MinIO ANTES de comprometer los headers ──
        // Si se lanza aquí (antes del StreamingResponseBody), Spring aún no ha fijado
        // Content-Type del archivo, por lo que el GlobalExceptionHandler puede devolver
        // un 404 JSON limpio sin conflicto de serialización.
        if (!minioStorageService.objectExists(doc.getObjectKey())) {
            throw new ObjNotFoundException(
                    "El archivo no se encontró en el almacenamiento: " + doc.getNombreOriginal()
            );
        }

        String mimeType = (doc.getMimeType() != null && !doc.getMimeType().isBlank())
                ? doc.getMimeType()
                : "application/octet-stream";

        // RFC 5987 percent-encoded name (spaces as %20, not +)
        String rfc5987Name = URLEncoder.encode(doc.getNombreOriginal(), StandardCharsets.UTF_8)
                .replace("+", "%20");

        // ASCII-only fallback for older clients (replace non-ASCII with underscore)
        String asciiFallback = doc.getNombreOriginal().replaceAll("[^\\x20-\\x7E]", "_");

        // Use both filename= (ASCII fallback) and filename*= (RFC 5987, full Unicode)
        // Modern browsers prefer filename*; legacy browsers use filename=
        String disposition = (inline ? "inline" : "attachment")
                + "; filename=\"" + asciiFallback + "\""
                + "; filename*=UTF-8''" + rfc5987Name;

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

    // ─── Impresión de etiquetas ────────────────────────────────────────────────

    @GetMapping("/{id}/etiqueta/zpl")
    public ResponseEntity<String> getZplEtiqueta(
            @PathVariable Long id,
            @RequestParam(value = "configuracionId", required = false) Long configuracionId
    ) {
        Documento doc = documentoService.getDocumentoById(id);
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        String zpl = zplLabelService.generarZplDocumento(doc, config);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(zpl);
    }

    @PostMapping("/{id}/etiqueta/imprimir")
    public ResponseEntity<APIResponse> imprimirEtiqueta(
            @PathVariable Long id,
            @RequestParam("impresora") String impresora,
            @RequestParam(value = "configuracionId", required = false) Long configuracionId
    ) {
        Documento doc = documentoService.getDocumentoById(id);
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        String zpl = zplLabelService.generarZplDocumento(doc, config);
        directPrintService.imprimir(zpl, impresora);
        return ResponseEntity.ok(new APIResponse("Etiqueta enviada a impresora", null, false, HttpStatus.OK));
    }

    @GetMapping("/impresoras")
    public ResponseEntity<APIResponse> listarImpresoras() {
        List<String> impresoras = directPrintService.listarImpresoras();
        return ResponseEntity.ok(new APIResponse("Impresoras disponibles", impresoras, false, HttpStatus.OK));
    }

    private ConfiguracionEtiqueta resolverConfig(Long configuracionId) {
        Long idInst = institucionCtx.getIdInstitucionActual();
        if (configuracionId != null) {
            return configuracionEtiquetaService.obtenerPorId(configuracionId, idInst);
        }
        return configuracionEtiquetaService.obtenerPredeterminada(idInst);
    }

    // ─── Visualización por etiqueta (escaneo QR/barcode) ─────────────────────────

    @PostMapping("/etiqueta/{etiqueta}/token")
    public ResponseEntity<APIResponse> generarTokenAcceso(@PathVariable String etiqueta) {
        String role = getCurrentRole();
        if (!"ADMINISTRADOR".equals(role)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Solo el rol ADMINISTRADOR puede generar tokens de visualización de documentos");
        }
        DocumentoAccessToken token = accessTokenService.generarToken(etiqueta);
        Map<String, Object> response = Map.of(
                "token", token.getToken(),
                "expiresAt", token.getFechaExpiracion().toString(),
                "idDocumento", token.getIdDocumento()
        );
        return ResponseEntity.ok(new APIResponse("Token generado", response, false, HttpStatus.OK));
    }

    @GetMapping("/ver/{token}")
    public ResponseEntity<StreamingResponseBody> verConToken(@PathVariable String token) {
        if (!minioStorageService.isAvailable()) {
            throw new MinioUnavailableException();
        }

        Documento doc = accessTokenService.validarTokenYObtenerDocumento(token);

        if (!minioStorageService.objectExists(doc.getObjectKey())) {
            throw new ObjNotFoundException(
                    "El archivo no se encontró en el almacenamiento: " + doc.getNombreOriginal());
        }

        String mimeType = (doc.getMimeType() != null && !doc.getMimeType().isBlank())
                ? doc.getMimeType()
                : "application/octet-stream";

        String rfc5987Name = URLEncoder.encode(doc.getNombreOriginal(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        String asciiFallback = doc.getNombreOriginal().replaceAll("[^\\x20-\\x7E]", "_");
        String disposition = "inline; filename=\"" + asciiFallback + "\"; filename*=UTF-8''" + rfc5987Name;

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

    @GetMapping("/etiqueta/{etiqueta}/info")
    public ResponseEntity<APIResponse> getInfoPorEtiqueta(@PathVariable String etiqueta) {
        Documento doc = documentoService.getDocumentoPorEtiqueta(etiqueta);
        Map<String, Object> info = Map.of(
                "id", doc.getId(),
                "nombreOriginal", doc.getNombreOriginal(),
                "mimeType", doc.getMimeType() != null ? doc.getMimeType() : "",
                "etiqueta", doc.getEtiqueta(),
                "tipoEntidad", doc.getTipoEntidad().name(),
                "fechaSubida", doc.getFechaSubida().toString()
        );
        return ResponseEntity.ok(new APIResponse("Información del documento", info, false, HttpStatus.OK));
    }

    // ─── Eliminación ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse> delete(@PathVariable Long id) {
        documentoService.verificarPuedeEliminar(getCurrentRole());
        documentoService.deleteDocumento(id);
        return ResponseEntity.ok(new APIResponse("Documento eliminado correctamente", null, false, HttpStatus.OK));
    }
}
