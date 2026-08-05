package imss.gob.mx.cohorte.controllers.documentos;

import imss.gob.mx.cohorte.application.PacienteApplicationService;
import imss.gob.mx.cohorte.controllers.documentos.dto.DocumentoResponseDTO;
import imss.gob.mx.cohorte.controllers.impresion.dto.ConfiguracionEtiquetaMapper;
import imss.gob.mx.cohorte.controllers.impresion.dto.LabelDataDTO;
import imss.gob.mx.cohorte.controllers.impresion.dto.PrintableLabelBatchDTO;
import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.documentos.Documento;
import imss.gob.mx.cohorte.modules.documentos.DocumentoAccessToken;
import imss.gob.mx.cohorte.modules.documentos.TipoDocumentoPaciente;
import imss.gob.mx.cohorte.modules.documentos.TipoEntidadDocumento;
import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.documentos.DocumentoAccessTokenService;
import imss.gob.mx.cohorte.services.documentos.DocumentoPermisosConfig;
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
    private final DocumentoPermisosConfig permisosConfig;
    private final PacienteApplicationService pacienteApplicationService;

    public DocumentoController(DocumentoService documentoService,
                                MinioStorageService minioStorageService,
                                ZplLabelService zplLabelService,
                                DirectPrintService directPrintService,
                                ConfiguracionEtiquetaService configuracionEtiquetaService,
                                InstitucionContextService institucionCtx,
                                DocumentoAccessTokenService accessTokenService,
                                DocumentoPermisosConfig permisosConfig,
                                PacienteApplicationService pacienteApplicationService) {
        this.documentoService = documentoService;
        this.minioStorageService = minioStorageService;
        this.zplLabelService = zplLabelService;
        this.directPrintService = directPrintService;
        this.configuracionEtiquetaService = configuracionEtiquetaService;
        this.institucionCtx = institucionCtx;
        this.accessTokenService = accessTokenService;
        this.permisosConfig = permisosConfig;
        this.pacienteApplicationService = pacienteApplicationService;
    }

    // ─── Helper: mapeo TipoDocumentoPaciente → TipoEntidadDocumento ───────────

    private TipoEntidadDocumento mapTipoEntidad(TipoDocumentoPaciente tipoDoc) {
        return switch (tipoDoc) {
            case CONSENTIMIENTO -> TipoEntidadDocumento.PACIENTE_CONSENTIMIENTO;
            case CUESTIONARIO, CUESTIONARIO_GENERAL, CUESTIONARIO_MINIMENTAL,
                 CUESTIONARIO_AFLUENCIA_VERBAL, CUESTIONARIO_AGES -> TipoEntidadDocumento.PACIENTE_CUESTIONARIO;
            case GENERAL        -> TipoEntidadDocumento.PACIENTE_GENERAL;
        };
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
        documentoService.verificarPuedeSubir();
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
        TipoEntidadDocumento tipoEntidad = mapTipoEntidad(tipoDoc);
        documentoService.verificarPuedeSubir();
        DocumentoResponseDTO dto = documentoService.uploadParaPaciente(file, uuid, tipoDoc, descripcion, usuarioUUID);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Documento subido correctamente", dto, false, HttpStatus.CREATED));
    }

    @PostMapping("/paciente/{uuid}/sin-archivo")
    public ResponseEntity<APIResponse> crearSinArchivo(
            @PathVariable String uuid,
            @RequestParam(value = "tipoDoc", defaultValue = "CUESTIONARIO") TipoDocumentoPaciente tipoDoc,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "usuarioUUID") String usuarioUUID
    ) {
        TipoEntidadDocumento tipoEntidad = mapTipoEntidad(tipoDoc);
        documentoService.verificarPuedeSubir();
        DocumentoResponseDTO dto = documentoService.crearDocumentoSinArchivo(uuid, tipoDoc, descripcion, usuarioUUID);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Registro creado — etiqueta generada", dto, false, HttpStatus.CREATED));
    }

    @PostMapping(value = "/{id}/adjuntar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse> adjuntarArchivo(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file
    ) {
        DocumentoResponseDTO dto = documentoService.adjuntarArchivo(id, file);
        return ResponseEntity.ok(new APIResponse("Archivo adjuntado correctamente", dto, false, HttpStatus.OK));
    }

    // ─── Upload para Muestra ──────────────────────────────────────────────────────

    @PostMapping(value = "/muestra/{muestraId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse> uploadParaMuestra(
            @PathVariable Long muestraId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "usuarioUUID") String usuarioUUID
    ) {
        documentoService.verificarPuedeSubir();
        DocumentoResponseDTO dto = documentoService.uploadParaMuestra(file, muestraId, descripcion, usuarioUUID);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Documento subido correctamente", dto, false, HttpStatus.CREATED));
    }

    // ─── Upload para Resultado de Examen ─────────────────────────────────────────

    @PostMapping(value = "/resultado-examen/{resultadoId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<APIResponse> uploadParaResultadoExamen(
            @PathVariable Long resultadoId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "usuarioUUID") String usuarioUUID
    ) {
        documentoService.verificarPuedeSubir();
        DocumentoResponseDTO dto = documentoService.uploadParaResultadoExamen(file, resultadoId, descripcion, usuarioUUID);
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
        pacienteApplicationService.verificarAccesoPropioSiEsPaciente(uuid);
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByPaciente(uuid);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/paciente/{uuid}/tipo/{tipoDoc}")
    public ResponseEntity<APIResponse> getByPacienteYTipo(
            @PathVariable String uuid,
            @PathVariable TipoDocumentoPaciente tipoDoc
    ) {
        pacienteApplicationService.verificarAccesoPropioSiEsPaciente(uuid);
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByPacienteYTipo(uuid, tipoDoc);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/muestra/{muestraId}")
    public ResponseEntity<APIResponse> getByMuestra(@PathVariable Long muestraId) {
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByMuestra(muestraId);
        return ResponseEntity.ok(new APIResponse("Documentos obtenidos", docs, false, HttpStatus.OK));
    }

    @GetMapping("/resultado-examen/{resultadoId}")
    public ResponseEntity<APIResponse> getByResultadoExamen(@PathVariable Long resultadoId) {
        List<DocumentoResponseDTO> docs = documentoService.getDocumentosByResultadoExamen(resultadoId);
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
        documentoService.verificarPuedeDescargar();

        if (!doc.isArchivoSubido() || doc.getObjectKey() == null) {
            throw new ObjNotFoundException("Este documento aún no tiene archivo adjunto");
        }

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

    /**
     * {@code incluirEnlace} decide que se codifica dentro del simbolo: el enlace para
     * abrir el documento, o solo el codigo de la etiqueta. En Code 128 se ignora y
     * siempre va el codigo, porque el enlace no cabe en una etiqueta lineal.
     */
    @GetMapping("/{id}/etiqueta/zpl")
    public ResponseEntity<String> getZplEtiqueta(
            @PathVariable Long id,
            @RequestParam(value = "configuracionId", required = false) Long configuracionId,
            @RequestParam(value = "incluirEnlace", defaultValue = "true") boolean incluirEnlace
    ) {
        Documento doc = documentoService.getDocumentoById(id);
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        String zpl = zplLabelService.generarZplDocumento(doc, config, incluirEnlace);
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(zpl);
    }

    @GetMapping("/{id}/etiqueta/datos")
    public ResponseEntity<APIResponse> getDatosEtiqueta(
            @PathVariable Long id,
            @RequestParam(value = "configuracionId", required = false) Long configuracionId,
            @RequestParam(value = "incluirEnlace", defaultValue = "true") boolean incluirEnlace
    ) {
        Documento doc = documentoService.getDocumentoById(id);
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        LabelDataDTO labelData = zplLabelService.extraerDatosDocumento(doc, config, incluirEnlace);
        PrintableLabelBatchDTO batch = PrintableLabelBatchDTO.builder()
                .configuracion(ConfiguracionEtiquetaMapper.toResponseDTO(config))
                .etiquetas(java.util.List.of(labelData))
                .build();
        return ResponseEntity.ok(new APIResponse("Datos de etiqueta", batch, false, HttpStatus.OK));
    }

    /**
     * Datos de varias etiquetas en una sola llamada, para imprimir en lote desde el navegador.
     * Respeta el orden en que llegan los identificadores: es el orden con el que el usuario
     * las acomoda despues sobre la hoja.
     */
    @GetMapping("/etiquetas/datos")
    public ResponseEntity<APIResponse> getDatosEtiquetasLote(
            @RequestParam("ids") List<Long> ids,
            @RequestParam(value = "configuracionId", required = false) Long configuracionId,
            @RequestParam(value = "incluirEnlace", defaultValue = "true") boolean incluirEnlace
    ) {
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        List<LabelDataDTO> etiquetas = ids.stream()
                .map(documentoService::getDocumentoById)
                .map(doc -> zplLabelService.extraerDatosDocumento(doc, config, incluirEnlace))
                .toList();
        PrintableLabelBatchDTO batch = PrintableLabelBatchDTO.builder()
                .configuracion(ConfiguracionEtiquetaMapper.toResponseDTO(config))
                .etiquetas(etiquetas)
                .build();
        return ResponseEntity.ok(new APIResponse("Datos de etiquetas", batch, false, HttpStatus.OK));
    }

    @PostMapping("/{id}/etiqueta/imprimir")
    public ResponseEntity<APIResponse> imprimirEtiqueta(
            @PathVariable Long id,
            @RequestParam("impresora") String impresora,
            @RequestParam(value = "configuracionId", required = false) Long configuracionId,
            @RequestParam(value = "incluirEnlace", defaultValue = "true") boolean incluirEnlace
    ) {
        Documento doc = documentoService.getDocumentoById(id);
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        String zpl = zplLabelService.generarZplDocumento(doc, config, incluirEnlace);
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
        ConfiguracionEtiqueta pred = configuracionEtiquetaService.obtenerPredeterminada(idInst);
        if (pred == null) {
            throw new IllegalArgumentException(
                    "No hay configuración de etiqueta predeterminada. Cree una en Configuración > Etiquetas.");
        }
        return pred;
    }

    // ─── Visualización por etiqueta (escaneo QR/barcode) ─────────────────────────

    @PostMapping("/etiqueta/token")
    public ResponseEntity<APIResponse> generarTokenAcceso(@RequestParam String etiqueta) {
        if (!permisosConfig.puedeDescargar()) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "No tiene permiso para generar tokens de visualización de documentos");
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

        if (!doc.isArchivoSubido() || doc.getObjectKey() == null) {
            throw new ObjNotFoundException("Este documento aún no tiene archivo adjunto");
        }

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

    @GetMapping("/etiqueta/info")
    public ResponseEntity<APIResponse> getInfoPorEtiqueta(@RequestParam String etiqueta) {
        Documento doc = documentoService.getDocumentoPorEtiqueta(etiqueta);
        Map<String, Object> info = Map.of(
                "id", doc.getId(),
                "nombreOriginal", doc.getNombreOriginal() != null ? doc.getNombreOriginal() : "",
                "mimeType", doc.getMimeType() != null ? doc.getMimeType() : "",
                "etiqueta", doc.getEtiqueta(),
                "tipoEntidad", doc.getTipoEntidad().name(),
                "fechaSubida", doc.getFechaSubida().toString(),
                "archivoSubido", doc.isArchivoSubido()
        );
        return ResponseEntity.ok(new APIResponse("Información del documento", info, false, HttpStatus.OK));
    }

    // ─── Eliminación ─────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<APIResponse> delete(@PathVariable Long id) {
        documentoService.verificarPuedeEliminar();
        documentoService.deleteDocumento(id);
        return ResponseEntity.ok(new APIResponse("Documento eliminado correctamente", null, false, HttpStatus.OK));
    }
}
