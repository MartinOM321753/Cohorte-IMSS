package imss.gob.mx.cohorte.services.documentos;

import imss.gob.mx.cohorte.controllers.documentos.dto.DocumentoResponseDTO;
import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.documentos.*;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestraRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    private static final long TAMANIO_MAXIMO_BYTES = 20L * 1024 * 1024; // 20 MB

    private static final Set<String> MIME_TYPES_PERMITIDOS = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/webp"
    );

    private final MinioStorageService minioService;
    private final DocumentoRepository documentoRepository;
    private final EstudioDocumentoRepository estudioDocumentoRepository;
    private final PacienteDocumentoRepository pacienteDocumentoRepository;
    private final MuestraDocumentoRepository muestraDocumentoRepository;
    private final ResultadoExamenDocumentoRepository resultadoExamenDocumentoRepository;
    private final EstudioMedicoRepository estudioMedicoRepository;
    private final PacienteRepository pacienteRepository;
    private final MuestraRepository muestraRepository;
    private final ResultadoExamenRepository resultadoExamenRepository;
    private final DocumentoPermisosConfig permisosConfig;
    private final DocumentoEtiquetaService etiquetaService;
    private final InstitucionContextService institucionCtx;
    private final TrasladoMuestraRepository trasladoMuestraRepository;

    public DocumentoService(
            MinioStorageService minioService,
            DocumentoRepository documentoRepository,
            EstudioDocumentoRepository estudioDocumentoRepository,
            PacienteDocumentoRepository pacienteDocumentoRepository,
            MuestraDocumentoRepository muestraDocumentoRepository,
            ResultadoExamenDocumentoRepository resultadoExamenDocumentoRepository,
            EstudioMedicoRepository estudioMedicoRepository,
            PacienteRepository pacienteRepository,
            MuestraRepository muestraRepository,
            ResultadoExamenRepository resultadoExamenRepository,
            DocumentoPermisosConfig permisosConfig,
            DocumentoEtiquetaService etiquetaService,
            InstitucionContextService institucionCtx,
            TrasladoMuestraRepository trasladoMuestraRepository
    ) {
        this.minioService = minioService;
        this.documentoRepository = documentoRepository;
        this.estudioDocumentoRepository = estudioDocumentoRepository;
        this.pacienteDocumentoRepository = pacienteDocumentoRepository;
        this.muestraDocumentoRepository = muestraDocumentoRepository;
        this.resultadoExamenDocumentoRepository = resultadoExamenDocumentoRepository;
        this.estudioMedicoRepository = estudioMedicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.muestraRepository = muestraRepository;
        this.resultadoExamenRepository = resultadoExamenRepository;
        this.permisosConfig = permisosConfig;
        this.etiquetaService = etiquetaService;
        this.institucionCtx = institucionCtx;
        this.trasladoMuestraRepository = trasladoMuestraRepository;
    }

    // ─── Validación de permisos ───────────────────────────────────────────────────

    public void verificarPuedeVer(String role, TipoEntidadDocumento tipo) {
        if (!permisosConfig.puedeVer(role, tipo)) {
            throw new AccessDeniedException("El rol '" + role + "' no tiene permiso para ver documentos de tipo " + tipo);
        }
    }

    public void verificarPuedeSubir(String role, TipoEntidadDocumento tipo) {
        if (!permisosConfig.puedeSubir(role, tipo)) {
            throw new AccessDeniedException("El rol '" + role + "' no tiene permiso para subir documentos de tipo " + tipo);
        }
    }

    public void verificarPuedeEliminar(String role) {
        if (!permisosConfig.puedeEliminar(role)) {
            throw new AccessDeniedException("El rol '" + role + "' no tiene permiso para eliminar documentos");
        }
    }

    // ─── Estudios ────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentoResponseDTO uploadParaEstudio(
            MultipartFile file, Long estudioId, String descripcion, String usuarioUUID, int orden
    ) {
        EstudioMedico estudio = estudioMedicoRepository.findById(estudioId)
                .orElseThrow(() -> new ObjNotFoundException("Estudio no encontrado con id: " + estudioId));
        validarEstudioPertenece(estudio);

        String objectKey = buildKey("estudios/" + estudioId, file.getOriginalFilename());
        uploadToMinio(file, objectKey);

        Documento doc = crearDocumento(file, objectKey, descripcion, usuarioUUID, TipoEntidadDocumento.ESTUDIO);

        EstudioDocumento rel = new EstudioDocumento();
        rel.setEstudio(estudio);
        rel.setDocumento(doc);
        rel.setOrden(orden);
        estudioDocumentoRepository.save(rel);

        return toDTO(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> getDocumentosByEstudio(Long estudioId) {
        EstudioMedico estudio = estudioMedicoRepository.findById(estudioId)
                .orElseThrow(() -> new ObjNotFoundException("Estudio no encontrado con id: " + estudioId));
        validarEstudioPertenece(estudio);

        return estudioDocumentoRepository.findByEstudio_IdOrderByOrdenAsc(estudioId)
                .stream()
                .map(ed -> toDTO(ed.getDocumento()))
                .collect(Collectors.toList());
    }

    // ─── Pacientes ───────────────────────────────────────────────────────────────

    @Transactional
    public DocumentoResponseDTO uploadParaPaciente(
            MultipartFile file,
            String pacienteUUID,
            TipoDocumentoPaciente tipoDoc,
            String descripcion,
            String usuarioUUID
    ) {
        Paciente paciente = pacienteRepository.findByUuid(pacienteUUID)
                .orElseThrow(() -> new ObjNotFoundException("Participante no encontrado con UUID: " + pacienteUUID));
        validarPacientePertenece(paciente);

        String objectKey = buildKey("pacientes/" + pacienteUUID + "/" + tipoDoc.name().toLowerCase(),
                file.getOriginalFilename());
        uploadToMinio(file, objectKey);

        TipoEntidadDocumento tipoEntidad = mapTipoEntidad(tipoDoc);
        String etiquetaOverride = DocumentoEtiquetaService.usaFormatoPaciente(tipoDoc)
                ? etiquetaService.generarEtiquetaPaciente(tipoDoc, paciente.getFolio(), revisionParaEtiqueta(paciente, tipoDoc))
                : null;
        Documento doc = crearDocumento(file, objectKey, descripcion, usuarioUUID, tipoEntidad, etiquetaOverride);

        PacienteDocumento rel = new PacienteDocumento();
        rel.setPaciente(paciente);
        rel.setDocumento(doc);
        rel.setTipoDoc(tipoDoc);
        pacienteDocumentoRepository.save(rel);

        return toDTO(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> getDocumentosByPaciente(String uuid) {
        Paciente paciente = pacienteRepository.findByUuid(uuid)
                .orElseThrow(() -> new ObjNotFoundException("Participante no encontrado con UUID: " + uuid));
        validarPacientePertenece(paciente);

        return pacienteDocumentoRepository
                .findByPaciente_UuidOrderByDocumento_FechaSubidaDesc(uuid)
                .stream()
                .map(pd -> toDTO(pd.getDocumento()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> getDocumentosByPacienteYTipo(String uuid, TipoDocumentoPaciente tipoDoc) {
        Paciente paciente = pacienteRepository.findByUuid(uuid)
                .orElseThrow(() -> new ObjNotFoundException("Participante no encontrado con UUID: " + uuid));
        validarPacientePertenece(paciente);

        return pacienteDocumentoRepository
                .findByPaciente_UuidAndTipoDocOrderByDocumento_FechaSubidaDesc(uuid, tipoDoc)
                .stream()
                .map(pd -> toDTO(pd.getDocumento()))
                .collect(Collectors.toList());
    }

    // ─── Creación sin archivo (etiqueta + placeholder) ─────────────────────────

    @Transactional
    public DocumentoResponseDTO crearDocumentoSinArchivo(
            String pacienteUUID,
            TipoDocumentoPaciente tipoDoc,
            String descripcion,
            String usuarioUUID
    ) {
        Paciente paciente = pacienteRepository.findByUuid(pacienteUUID)
                .orElseThrow(() -> new ObjNotFoundException("Participante no encontrado con UUID: " + pacienteUUID));
        validarPacientePertenece(paciente);

        TipoEntidadDocumento tipoEntidad = mapTipoEntidad(tipoDoc);
        Long idInstitucion = institucionCtx.getIdInstitucionActual();
        String etiqueta = DocumentoEtiquetaService.usaFormatoPaciente(tipoDoc)
                ? etiquetaService.generarEtiquetaPaciente(tipoDoc, paciente.getFolio(), revisionParaEtiqueta(paciente, tipoDoc))
                : etiquetaService.generarEtiquetaSinArchivo(idInstitucion, tipoEntidad);

        Documento doc = new Documento();
        doc.setNombreOriginal(etiqueta);
        doc.setDescripcion(descripcion);
        doc.setFechaSubida(LocalDateTime.now());
        doc.setSubidoPorUUID(usuarioUUID);
        doc.setTipoEntidad(tipoEntidad);
        doc.setIdInstitucion(idInstitucion);
        doc.setEtiqueta(etiqueta);
        doc.setArchivoSubido(false);
        doc = documentoRepository.save(doc);

        PacienteDocumento rel = new PacienteDocumento();
        rel.setPaciente(paciente);
        rel.setDocumento(doc);
        rel.setTipoDoc(tipoDoc);
        pacienteDocumentoRepository.save(rel);

        return toDTO(doc);
    }

    @Transactional
    public DocumentoResponseDTO adjuntarArchivo(Long documentoId, MultipartFile file) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ObjNotFoundException("Documento no encontrado con id: " + documentoId));
        validarDocumentoAccesible(doc);

        if (doc.isArchivoSubido()) {
            throw new ValidationException("Este documento ya tiene un archivo adjunto");
        }

        String prefix = "documentos/" + doc.getTipoEntidad().name().toLowerCase();
        String objectKey = buildKey(prefix, file.getOriginalFilename());
        uploadToMinio(file, objectKey);

        String nombreOriginal = file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo";
        doc.setNombreOriginal(nombreOriginal);
        doc.setObjectKey(objectKey);
        doc.setMimeType(file.getContentType());
        doc.setTamanioBytes(file.getSize());
        doc.setArchivoSubido(true);

        // La etiqueta de Consentimiento/Cuestionario (formato {PREFIJO}/{folio}/F4) no depende
        // de la extensión del archivo, así que se conserva la asignada al crear el registro.
        boolean esFormatoPaciente = doc.getTipoEntidad() == TipoEntidadDocumento.PACIENTE_CONSENTIMIENTO
                || doc.getTipoEntidad() == TipoEntidadDocumento.PACIENTE_CUESTIONARIO;
        if (!esFormatoPaciente) {
            String nuevaEtiqueta = etiquetaService.generarEtiqueta(
                    doc.getIdInstitucion(), doc.getTipoEntidad(), file.getContentType(), nombreOriginal);
            doc.setEtiqueta(nuevaEtiqueta);
        }

        doc = documentoRepository.save(doc);
        return toDTO(doc);
    }

    // ─── Muestras ────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentoResponseDTO uploadParaMuestra(
            MultipartFile file, Long muestraId, String descripcion, String usuarioUUID
    ) {
        Muestra muestra = muestraRepository.findById(muestraId)
                .orElseThrow(() -> new ObjNotFoundException("Muestra no encontrada con id: " + muestraId));
        validarMuestraAccesible(muestra);

        String objectKey = buildKey("muestras/" + muestraId, file.getOriginalFilename());
        uploadToMinio(file, objectKey);

        Documento doc = crearDocumento(file, objectKey, descripcion, usuarioUUID, TipoEntidadDocumento.MUESTRA);

        MuestraDocumento rel = new MuestraDocumento();
        rel.setMuestra(muestra);
        rel.setDocumento(doc);
        muestraDocumentoRepository.save(rel);

        return toDTO(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> getDocumentosByMuestra(Long muestraId) {
        Muestra muestra = muestraRepository.findById(muestraId)
                .orElseThrow(() -> new ObjNotFoundException("Muestra no encontrada con id: " + muestraId));
        validarMuestraAccesibleHistorico(muestra);

        return muestraDocumentoRepository
                .findByMuestra_IdOrderByDocumento_FechaSubidaDesc(muestraId)
                .stream()
                .map(md -> toDTO(md.getDocumento()))
                .collect(Collectors.toList());
    }

    // ─── Resultados de exámenes ─────────────────────────────────────────────────

    @Transactional
    public DocumentoResponseDTO uploadParaResultadoExamen(
            MultipartFile file, Long resultadoId, String descripcion, String usuarioUUID
    ) {
        ResultadoExamen resultado = resultadoExamenRepository.findById(resultadoId)
                .orElseThrow(() -> new ObjNotFoundException("Resultado de examen no encontrado con id: " + resultadoId));
        validarPacientePertenece(resultado.getPaciente());

        String objectKey = buildKey("resultados-examen/" + resultadoId, file.getOriginalFilename());
        uploadToMinio(file, objectKey);

        Documento doc = crearDocumento(file, objectKey, descripcion, usuarioUUID, TipoEntidadDocumento.RESULTADO_EXAMEN);

        ResultadoExamenDocumento rel = new ResultadoExamenDocumento();
        rel.setResultadoExamen(resultado);
        rel.setDocumento(doc);
        resultadoExamenDocumentoRepository.save(rel);

        return toDTO(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> getDocumentosByResultadoExamen(Long resultadoId) {
        ResultadoExamen resultado = resultadoExamenRepository.findById(resultadoId)
                .orElseThrow(() -> new ObjNotFoundException("Resultado de examen no encontrado con id: " + resultadoId));
        validarPacientePertenece(resultado.getPaciente());

        return resultadoExamenDocumentoRepository
                .findByResultadoExamen_IdOrderByDocumento_FechaSubidaDesc(resultadoId)
                .stream()
                .map(rd -> toDTO(rd.getDocumento()))
                .collect(Collectors.toList());
    }

    // ─── Operaciones comunes ─────────────────────────────────────────────────────

    /** Obtiene el Documento por id (usado por el endpoint de descarga). */
    @Transactional(readOnly = true)
    public Documento getDocumentoById(Long documentoId) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ObjNotFoundException("Documento no encontrado con id: " + documentoId));
        if (doc.getTipoEntidad() == TipoEntidadDocumento.MUESTRA) {
            validarAccesoDocumentoMuestra(documentoId);
        } else {
            validarDocumentoAccesible(doc);
        }
        return doc;
    }

    @Transactional(readOnly = true)
    public Documento getDocumentoPorEtiqueta(String etiqueta) {
        Documento doc = documentoRepository.findByEtiqueta(etiqueta)
                .orElseThrow(() -> new ObjNotFoundException("Documento no encontrado con etiqueta: " + etiqueta));
        validarDocumentoAccesible(doc);
        return doc;
    }

    /**
     * @deprecated Usar el endpoint /download en su lugar para garantizar autenticación.
     *             Mantenido solo para compatibilidad interna.
     */
    @Deprecated
    @Transactional(readOnly = true)
    public String getDownloadUrl(Long documentoId) {
        Documento doc = getDocumentoById(documentoId);
        return minioService.getPresignedUrl(doc.getObjectKey());
    }

    /** Elimina el documento de MinIO y de todas sus tablas de relación. */
    @Transactional
    public void deleteDocumento(Long documentoId) {
        Documento doc = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ObjNotFoundException("Documento no encontrado con id: " + documentoId));
        validarDocumentoAccesible(doc);

        if (doc.getTipoEntidad() == TipoEntidadDocumento.MUESTRA) {
            validarCustodiaMuestraDocumento(documentoId);
        }

        // Eliminar relaciones
        estudioDocumentoRepository.deleteByDocumento_Id(documentoId);
        pacienteDocumentoRepository.deleteByDocumento_Id(documentoId);
        muestraDocumentoRepository.deleteByDocumento_Id(documentoId);
        resultadoExamenDocumentoRepository.deleteByDocumento_Id(documentoId);

        // Eliminar registro en BD
        documentoRepository.delete(doc);

        // Eliminar archivo de MinIO (best-effort, no se hace rollback si falla)
        if (doc.isArchivoSubido() && doc.getObjectKey() != null) {
            try {
                minioService.delete(doc.getObjectKey());
            } catch (Exception ignored) {
                // El archivo puede no existir ya; la entrada de BD ya fue eliminada
            }
        }
    }

    // ─── Mapeo de tipo ────────────────────────────────────────────────────────

    private TipoEntidadDocumento mapTipoEntidad(TipoDocumentoPaciente tipoDoc) {
        return switch (tipoDoc) {
            case CONSENTIMIENTO -> TipoEntidadDocumento.PACIENTE_CONSENTIMIENTO;
            case CUESTIONARIO, CUESTIONARIO_GENERAL, CUESTIONARIO_MINIMENTAL,
                 CUESTIONARIO_AFLUENCIA_VERBAL, CUESTIONARIO_AGES -> TipoEntidadDocumento.PACIENTE_CUESTIONARIO;
            case GENERAL        -> TipoEntidadDocumento.PACIENTE_GENERAL;
        };
    }

    /**
     * Calcula el valor numérico a usar en el slot "folio" de la etiqueta nueva.
     * Para Consentimiento es una revisión incremental (1, 2, 3…) basada en cuántos
     * consentimientos previos tiene ya el paciente; para los demás tipos no se usa
     * (se usa directamente el folio fijo del paciente).
     */
    private int revisionParaEtiqueta(Paciente paciente, TipoDocumentoPaciente tipoDoc) {
        if (tipoDoc != TipoDocumentoPaciente.CONSENTIMIENTO) return 0;
        int previas = pacienteDocumentoRepository
                .findByPaciente_UuidAndTipoDocOrderByDocumento_FechaSubidaDesc(paciente.getUuid(), TipoDocumentoPaciente.CONSENTIMIENTO)
                .size();
        return previas + 1;
    }

    // ─── Helpers privados ────────────────────────────────────────────────────────

    /**
     * Valida mimeType y tamaño en el servidor antes de subir a MinIO.
     * El frontend ya filtra, pero esa validación es trivialmente evadible
     * (petición directa a la API), así que se repite aquí como última línea de defensa.
     */
    private void validarEstudioPertenece(EstudioMedico estudio) {
        institucionCtx.verificarPertenece(estudio.getInstitucion());
    }

    private void validarPacientePertenece(Paciente paciente) {
        institucionCtx.verificarPertenece(paciente.getInstitucion());
    }

    private void validarMuestraAccesible(Muestra muestra) {
        Long idInstitucionActual = institucionCtx.getIdInstitucionActual();
        Long idPropietaria = muestra.getInstitucion() != null ? muestra.getInstitucion().getId() : null;
        Long idTenedora = muestra.getInstitucionActual() != null ? muestra.getInstitucionActual().getId() : null;

        if (!idInstitucionActual.equals(idPropietaria) && !idInstitucionActual.equals(idTenedora)) {
            throw new AccessDeniedException("La muestra pertenece o se encuentra en otra institucion");
        }
    }

    private void validarAccesoDocumentoMuestra(Long documentoId) {
        List<MuestraDocumento> relaciones = muestraDocumentoRepository.findByDocumento_Id(documentoId);
        Long idInst = institucionCtx.getIdInstitucionActual();
        for (MuestraDocumento rel : relaciones) {
            Muestra muestra = rel.getMuestra();
            Long idPropietaria = muestra.getInstitucion() != null ? muestra.getInstitucion().getId() : null;
            Long idTenedora = muestra.getInstitucionActual() != null ? muestra.getInstitucionActual().getId() : null;
            if (idInst.equals(idPropietaria) || idInst.equals(idTenedora)) return;
            if (trasladoMuestraRepository.existsByMuestraAndInstitucion(muestra.getId(), idInst)) return;
        }
        throw new AccessDeniedException("No tiene acceso a los documentos de esta muestra.");
    }

    private void validarMuestraAccesibleHistorico(Muestra muestra) {
        Long idInst = institucionCtx.getIdInstitucionActual();
        Long idPropietaria = muestra.getInstitucion() != null ? muestra.getInstitucion().getId() : null;
        Long idTenedora = muestra.getInstitucionActual() != null ? muestra.getInstitucionActual().getId() : null;

        if (!idInst.equals(idPropietaria) && !idInst.equals(idTenedora)) {
            boolean tuvoAcceso = trasladoMuestraRepository.existsByMuestraAndInstitucion(muestra.getId(), idInst);
            if (!tuvoAcceso) {
                throw new AccessDeniedException("La muestra pertenece o se encuentra en otra institucion");
            }
        }
    }

    private void validarCustodiaMuestraDocumento(Long documentoId) {
        List<MuestraDocumento> relaciones = muestraDocumentoRepository.findByDocumento_Id(documentoId);
        Long idInst = institucionCtx.getIdInstitucionActual();
        for (MuestraDocumento rel : relaciones) {
            Muestra muestra = rel.getMuestra();
            if (muestra.getInstitucionActual() == null
                    || !muestra.getInstitucionActual().getId().equals(idInst)) {
                throw new AccessDeniedException(
                        "Solo se pueden eliminar documentos de muestras que estén bajo tu custodia.");
            }
        }
    }

    private void validarDocumentoAccesible(Documento doc) {
        if (doc.getIdInstitucion() == null) {
            throw new AccessDeniedException("El documento no tiene institucion asociada");
        }
        institucionCtx.verificarPerteneceOAncestra(doc.getIdInstitucion());
    }

    private void validarArchivo(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ValidationException("El archivo está vacío o no fue proporcionado");
        }
        if (file.getSize() > TAMANIO_MAXIMO_BYTES) {
            throw new ValidationException("El archivo excede el tamaño máximo permitido de "
                    + (TAMANIO_MAXIMO_BYTES / (1024 * 1024)) + " MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !MIME_TYPES_PERMITIDOS.contains(contentType.toLowerCase())) {
            throw new ValidationException("Tipo de archivo no permitido: "
                    + (contentType != null ? contentType : "desconocido"));
        }
    }

    private void uploadToMinio(MultipartFile file, String objectKey) {
        validarArchivo(file);
        try {
            minioService.upload(file.getInputStream(), objectKey, file.getContentType(), file.getSize());
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar el archivo: " + e.getMessage(), e);
        }
    }

    private Documento crearDocumento(MultipartFile file, String objectKey, String descripcion,
                                     String usuarioUUID, TipoEntidadDocumento tipo) {
        return crearDocumento(file, objectKey, descripcion, usuarioUUID, tipo, null);
    }

    private Documento crearDocumento(MultipartFile file, String objectKey, String descripcion,
                                     String usuarioUUID, TipoEntidadDocumento tipo, String etiquetaOverride) {
        Documento doc = new Documento();
        String nombreOriginal = file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo";
        doc.setNombreOriginal(nombreOriginal);
        doc.setObjectKey(objectKey);
        doc.setMimeType(file.getContentType());
        doc.setTamanioBytes(file.getSize());
        doc.setDescripcion(descripcion);
        doc.setFechaSubida(LocalDateTime.now());
        doc.setSubidoPorUUID(usuarioUUID);
        doc.setTipoEntidad(tipo);
        doc.setArchivoSubido(true);

        Long idInstitucion = institucionCtx.getIdInstitucionActual();
        doc.setIdInstitucion(idInstitucion);
        String etiqueta = etiquetaOverride != null
                ? etiquetaOverride
                : etiquetaService.generarEtiqueta(idInstitucion, tipo, file.getContentType(), nombreOriginal);
        doc.setEtiqueta(etiqueta);

        return documentoRepository.save(doc);
    }

    /**
     * Construye el object key para MinIO.
     * Formato: {prefijo}/{uuid}-{nombreSanitizado}
     * Ejemplo: "estudios/12/550e8400-e29b-41d4-a716-archivo.pdf"
     */
    private String buildKey(String prefix, String originalFilename) {
        String sanitized = (originalFilename != null)
                ? originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_")
                : "archivo";
        return prefix + "/" + UUID.randomUUID() + "-" + sanitized;
    }

    // ─── Helper: rol del usuario actual ─────────────────────────────────────────

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

    private DocumentoResponseDTO toDTO(Documento doc) {
        // No se expone URL firmada de MinIO. El acceso a archivos va por
        // GET /api/documentos/{id}/download (requiere JWT válido).
        // puedeDescargar se calcula en tiempo de respuesta según el rol activo,
        // de modo que el frontend pueda ocultar los botones sin necesitar otra llamada.
        boolean puedeDescargar = permisosConfig.puedeVer(getCurrentRole(), doc.getTipoEntidad());

        return DocumentoResponseDTO.builder()
                .id(doc.getId())
                .nombreOriginal(doc.getNombreOriginal())
                .mimeType(doc.getMimeType())
                .tamanioBytes(doc.getTamanioBytes())
                .descripcion(doc.getDescripcion())
                .fechaSubida(doc.getFechaSubida())
                .subidoPorUUID(doc.getSubidoPorUUID())
                .tipoEntidad(doc.getTipoEntidad() != null ? doc.getTipoEntidad().name() : null)
                .etiqueta(doc.getEtiqueta())
                .puedeDescargar(puedeDescargar && doc.isArchivoSubido())
                .archivoSubido(doc.isArchivoSubido())
                .url(null)
                .build();
    }
}
