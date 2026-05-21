package imss.gob.mx.cohorte.services.documentos;

import imss.gob.mx.cohorte.controllers.documentos.dto.DocumentoResponseDTO;
import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.documentos.*;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DocumentoService {

    private final MinioStorageService minioService;
    private final DocumentoRepository documentoRepository;
    private final EstudioDocumentoRepository estudioDocumentoRepository;
    private final PacienteDocumentoRepository pacienteDocumentoRepository;
    private final MuestraDocumentoRepository muestraDocumentoRepository;
    private final EstudioMedicoRepository estudioMedicoRepository;
    private final PacienteRepository pacienteRepository;
    private final MuestraRepository muestraRepository;
    private final DocumentoPermisosConfig permisosConfig;

    public DocumentoService(
            MinioStorageService minioService,
            DocumentoRepository documentoRepository,
            EstudioDocumentoRepository estudioDocumentoRepository,
            PacienteDocumentoRepository pacienteDocumentoRepository,
            MuestraDocumentoRepository muestraDocumentoRepository,
            EstudioMedicoRepository estudioMedicoRepository,
            PacienteRepository pacienteRepository,
            MuestraRepository muestraRepository,
            DocumentoPermisosConfig permisosConfig
    ) {
        this.minioService = minioService;
        this.documentoRepository = documentoRepository;
        this.estudioDocumentoRepository = estudioDocumentoRepository;
        this.pacienteDocumentoRepository = pacienteDocumentoRepository;
        this.muestraDocumentoRepository = muestraDocumentoRepository;
        this.estudioMedicoRepository = estudioMedicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.muestraRepository = muestraRepository;
        this.permisosConfig = permisosConfig;
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
                .orElseThrow(() -> new ObjNotFoundException("Paciente no encontrado con UUID: " + pacienteUUID));

        String objectKey = buildKey("pacientes/" + pacienteUUID + "/" + tipoDoc.name().toLowerCase(),
                file.getOriginalFilename());
        uploadToMinio(file, objectKey);

        TipoEntidadDocumento tipoEntidad = (tipoDoc == TipoDocumentoPaciente.CONSENTIMIENTO)
                ? TipoEntidadDocumento.PACIENTE_CONSENTIMIENTO
                : TipoEntidadDocumento.PACIENTE_GENERAL;
        Documento doc = crearDocumento(file, objectKey, descripcion, usuarioUUID, tipoEntidad);

        PacienteDocumento rel = new PacienteDocumento();
        rel.setPaciente(paciente);
        rel.setDocumento(doc);
        rel.setTipoDoc(tipoDoc);
        pacienteDocumentoRepository.save(rel);

        return toDTO(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> getDocumentosByPaciente(String uuid) {
        return pacienteDocumentoRepository
                .findByPaciente_UuidOrderByDocumento_FechaSubidaDesc(uuid)
                .stream()
                .map(pd -> toDTO(pd.getDocumento()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> getDocumentosByPacienteYTipo(String uuid, TipoDocumentoPaciente tipoDoc) {
        return pacienteDocumentoRepository
                .findByPaciente_UuidAndTipoDocOrderByDocumento_FechaSubidaDesc(uuid, tipoDoc)
                .stream()
                .map(pd -> toDTO(pd.getDocumento()))
                .collect(Collectors.toList());
    }

    // ─── Muestras ────────────────────────────────────────────────────────────────

    @Transactional
    public DocumentoResponseDTO uploadParaMuestra(
            MultipartFile file, Long muestraId, String descripcion, String usuarioUUID
    ) {
        var muestra = muestraRepository.findById(muestraId)
                .orElseThrow(() -> new ObjNotFoundException("Muestra no encontrada con id: " + muestraId));

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
        return muestraDocumentoRepository
                .findByMuestra_IdOrderByDocumento_FechaSubidaDesc(muestraId)
                .stream()
                .map(md -> toDTO(md.getDocumento()))
                .collect(Collectors.toList());
    }

    // ─── Operaciones comunes ─────────────────────────────────────────────────────

    /** Obtiene el Documento por id (usado por el endpoint de descarga). */
    @Transactional(readOnly = true)
    public Documento getDocumentoById(Long documentoId) {
        return documentoRepository.findById(documentoId)
                .orElseThrow(() -> new ObjNotFoundException("Documento no encontrado con id: " + documentoId));
    }

    /**
     * @deprecated Usar el endpoint /download en su lugar para garantizar autenticación.
     *             Mantenido solo para compatibilidad interna.
     */
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

        // Eliminar relaciones
        estudioDocumentoRepository.deleteByDocumento_Id(documentoId);
        pacienteDocumentoRepository.deleteByDocumento_Id(documentoId);
        muestraDocumentoRepository.deleteByDocumento_Id(documentoId);

        // Eliminar registro en BD
        documentoRepository.delete(doc);

        // Eliminar archivo de MinIO (best-effort, no se hace rollback si falla)
        try {
            minioService.delete(doc.getObjectKey());
        } catch (Exception ignored) {
            // El archivo puede no existir ya; la entrada de BD ya fue eliminada
        }
    }

    // ─── Helpers privados ────────────────────────────────────────────────────────

    private void uploadToMinio(MultipartFile file, String objectKey) {
        try {
            minioService.upload(file.getInputStream(), objectKey, file.getContentType(), file.getSize());
        } catch (Exception e) {
            throw new RuntimeException("Error al procesar el archivo: " + e.getMessage(), e);
        }
    }

    private Documento crearDocumento(MultipartFile file, String objectKey, String descripcion,
                                     String usuarioUUID, TipoEntidadDocumento tipo) {
        Documento doc = new Documento();
        doc.setNombreOriginal(file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo");
        doc.setObjectKey(objectKey);
        doc.setMimeType(file.getContentType());
        doc.setTamanioBytes(file.getSize());
        doc.setDescripcion(descripcion);
        doc.setFechaSubida(LocalDateTime.now());
        doc.setSubidoPorUUID(usuarioUUID);
        doc.setTipoEntidad(tipo);
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

    private DocumentoResponseDTO toDTO(Documento doc) {
        // No se expone URL firmada de MinIO. El acceso a archivos va por
        // GET /api/documentos/{id}/download (requiere JWT válido).
        return DocumentoResponseDTO.builder()
                .id(doc.getId())
                .nombreOriginal(doc.getNombreOriginal())
                .mimeType(doc.getMimeType())
                .tamanioBytes(doc.getTamanioBytes())
                .descripcion(doc.getDescripcion())
                .fechaSubida(doc.getFechaSubida())
                .subidoPorUUID(doc.getSubidoPorUUID())
                .url(null) // acceso controlado vía endpoint autenticado
                .build();
    }
}
