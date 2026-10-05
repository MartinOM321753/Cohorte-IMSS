package imss.gob.mx.cohorte.services.documentos_publicos;

import imss.gob.mx.cohorte.controllers.documentos_publicos.dto.DocumentoPublicoRequestDTO;
import imss.gob.mx.cohorte.controllers.documentos_publicos.dto.DocumentoPublicoResponseDTO;
import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.documentos_publicos.CategoriaDocumentoPublico;
import imss.gob.mx.cohorte.modules.documentos_publicos.CategoriaDocumentoPublicoRepository;
import imss.gob.mx.cohorte.modules.documentos_publicos.DocumentoPublico;
import imss.gob.mx.cohorte.modules.documentos_publicos.DocumentoPublicoRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoPublicoService {

    private static final String FOLDER_PREFIX = "documentos-publicos/";

    private final DocumentoPublicoRepository repository;
    private final CategoriaDocumentoPublicoRepository categoriaRepository;
    private final MinioStorageService minioStorageService;
    private final InstitucionContextService institucionContextService;

    private Long myInstId() {
        return institucionContextService.getIdInstitucionActual();
    }

    @Transactional
    public DocumentoPublicoResponseDTO upload(MultipartFile file, DocumentoPublicoRequestDTO dto, String usuarioUuid) {
        Long idInst = myInstId();
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento";
        String extension = "";
        int dotIdx = originalName.lastIndexOf('.');
        if (dotIdx > 0) {
            extension = originalName.substring(dotIdx);
        }
        String objectKey = FOLDER_PREFIX + idInst + "/" + UUID.randomUUID() + extension;

        try {
            minioStorageService.upload(file.getInputStream(), objectKey, file.getContentType(), file.getSize());
        } catch (IOException e) {
            throw new RuntimeException("Error al leer el archivo para subir: " + e.getMessage(), e);
        }

        DocumentoPublico doc = new DocumentoPublico();
        doc.setNombreOriginal(originalName);
        doc.setNombreMostrar(dto.getNombreMostrar() != null && !dto.getNombreMostrar().isBlank()
                ? dto.getNombreMostrar().trim() : null);
        doc.setObjectKey(objectKey);
        doc.setMimeType(file.getContentType());
        doc.setTamanioBytes(file.getSize());
        doc.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null);
        doc.setFechaPublicacion(dto.getFechaPublicacion());
        doc.setFase(dto.getFase() != null ? dto.getFase().trim() : null);
        doc.setAutor(dto.getAutor() != null ? dto.getAutor().trim() : null);
        doc.setFechaCreacion(LocalDateTime.now());
        doc.setSubidoPorUuid(usuarioUuid);
        doc.setIdInstitucion(idInst);
        doc.setActivo(true);

        if (dto.getCategoriaId() != null) {
            CategoriaDocumentoPublico cat = categoriaRepository.findByIdAndInstitucion_Id(dto.getCategoriaId(), idInst)
                    .orElseThrow(() -> new ObjNotFoundException("Categoría no encontrada: " + dto.getCategoriaId()));
            doc.setCategoria(cat);
        }

        DocumentoPublico saved = repository.save(doc);
        return toDTO(saved);
    }

    @Transactional
    public DocumentoPublicoResponseDTO update(Long id, DocumentoPublicoRequestDTO dto) {
        Long idInst = myInstId();
        DocumentoPublico doc = repository.findByIdAndIdInstitucion(id, idInst)
                .orElseThrow(() -> new ObjNotFoundException("Documento público no encontrado: " + id));

        doc.setNombreMostrar(dto.getNombreMostrar() != null && !dto.getNombreMostrar().isBlank()
                ? dto.getNombreMostrar().trim() : null);
        doc.setDescripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null);
        doc.setFechaPublicacion(dto.getFechaPublicacion());
        doc.setFase(dto.getFase() != null ? dto.getFase().trim() : null);
        doc.setAutor(dto.getAutor() != null ? dto.getAutor().trim() : null);

        if (dto.getCategoriaId() != null) {
            CategoriaDocumentoPublico cat = categoriaRepository.findByIdAndInstitucion_Id(dto.getCategoriaId(), idInst)
                    .orElseThrow(() -> new ObjNotFoundException("Categoría no encontrada: " + dto.getCategoriaId()));
            doc.setCategoria(cat);
        } else {
            doc.setCategoria(null);
        }

        return toDTO(repository.save(doc));
    }

    @Transactional
    public void delete(Long id) {
        Long idInst = myInstId();
        DocumentoPublico doc = repository.findByIdAndIdInstitucion(id, idInst)
                .orElseThrow(() -> new ObjNotFoundException("Documento público no encontrado: " + id));
        doc.setActivo(false);
        repository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<DocumentoPublicoResponseDTO> getAllForAdmin() {
        return repository.findAllByIdInstitucionOrderByFechaCreacionDesc(myInstId())
                .stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public DocumentoPublico getEntityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("Documento público no encontrado: " + id));
    }

    @Transactional(readOnly = true)
    public List<DocumentoPublicoResponseDTO> getAllPublicByInstitucion(Long idInstitucion) {
        return repository.findAllByIdInstitucionAndActivoOrderByFechaPublicacionDesc(idInstitucion, true)
                .stream().map(this::toDTO).toList();
    }

    private DocumentoPublicoResponseDTO toDTO(DocumentoPublico doc) {
        return DocumentoPublicoResponseDTO.builder()
                .id(doc.getId())
                .nombreMostrar(doc.getNombreMostrar())
                .nombreOriginal(doc.getNombreOriginal())
                .mimeType(doc.getMimeType())
                .tamanioBytes(doc.getTamanioBytes())
                .descripcion(doc.getDescripcion())
                .fechaPublicacion(doc.getFechaPublicacion())
                .fase(doc.getFase())
                .autor(doc.getAutor())
                .categoriaId(doc.getCategoria() != null ? doc.getCategoria().getId() : null)
                .categoriaNombre(doc.getCategoria() != null ? doc.getCategoria().getNombre() : null)
                .fechaCreacion(doc.getFechaCreacion())
                .activo(doc.getActivo())
                .build();
    }
}
