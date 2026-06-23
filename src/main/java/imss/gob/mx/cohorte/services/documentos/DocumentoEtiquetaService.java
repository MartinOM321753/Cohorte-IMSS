package imss.gob.mx.cohorte.services.documentos;

import imss.gob.mx.cohorte.modules.documentos.DocumentoFolioSeq;
import imss.gob.mx.cohorte.modules.documentos.DocumentoFolioSeqRepository;
import imss.gob.mx.cohorte.modules.documentos.TipoDocumentoPaciente;
import imss.gob.mx.cohorte.modules.documentos.TipoEntidadDocumento;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentoEtiquetaService {

    private final DocumentoFolioSeqRepository folioSeqRepository;

    private static final Map<TipoEntidadDocumento, String> PREFIJO_TIPO = Map.of(
            TipoEntidadDocumento.ESTUDIO, "E",
            TipoEntidadDocumento.MUESTRA, "M",
            TipoEntidadDocumento.PACIENTE_CONSENTIMIENTO, "C",
            TipoEntidadDocumento.PACIENTE_GENERAL, "G",
            TipoEntidadDocumento.PACIENTE_CUESTIONARIO, "Q"
    );

    /** Literal fijo requerido en la etiqueta de Consentimiento/Cuestionario: {PREFIJO}/{folio}/F4. */
    private static final String LITERAL_F4 = "F4";

    private static final Map<TipoDocumentoPaciente, String> PREFIJO_PACIENTE = Map.of(
            TipoDocumentoPaciente.CUESTIONARIO_GENERAL, "C1",
            TipoDocumentoPaciente.CUESTIONARIO_MINIMENTAL, "C2",
            TipoDocumentoPaciente.CUESTIONARIO_AFLUENCIA_VERBAL, "C3",
            TipoDocumentoPaciente.CUESTIONARIO_AGES, "C4",
            TipoDocumentoPaciente.CONSENTIMIENTO, "CI"
    );

    private static final Map<String, String> MIME_A_EXTENSION = Map.ofEntries(
            Map.entry("application/pdf", "PDF"),
            Map.entry("image/jpeg", "JPEG"),
            Map.entry("image/png", "PNG"),
            Map.entry("image/webp", "WEBP"),
            Map.entry("image/gif", "GIF"),
            Map.entry("image/bmp", "BMP"),
            Map.entry("image/svg+xml", "SVG"),
            Map.entry("video/mp4", "MP4"),
            Map.entry("video/mpeg", "MPEG"),
            Map.entry("video/quicktime", "MOV"),
            Map.entry("video/x-msvideo", "AVI"),
            Map.entry("audio/mpeg", "MP3"),
            Map.entry("audio/wav", "WAV"),
            Map.entry("text/plain", "TXT"),
            Map.entry("text/csv", "CSV"),
            Map.entry("text/xml", "XML"),
            Map.entry("application/xml", "XML"),
            Map.entry("application/json", "JSON"),
            Map.entry("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "DOCX"),
            Map.entry("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "XLSX"),
            Map.entry("application/msword", "DOC"),
            Map.entry("application/vnd.ms-excel", "XLS"),
            Map.entry("application/zip", "ZIP"),
            Map.entry("application/x-rar-compressed", "RAR"),
            Map.entry("application/dicom", "DCM"),
            Map.entry("application/octet-stream", "BIN")
    );

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generarEtiqueta(Long idInstitucion, TipoEntidadDocumento tipo,
                                   String mimeType, String nombreOriginal) {
        int anio = LocalDate.now().getYear();
        int yy = anio % 100;

        int folio = siguienteFolio(idInstitucion, anio);

        String prefTipo = PREFIJO_TIPO.getOrDefault(tipo, "X");
        String extension = resolverExtension(mimeType, nombreOriginal);

        return String.format("D%02d-%02d-%s-%05d.%s", yy, idInstitucion, prefTipo, folio, extension);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public String generarEtiquetaSinArchivo(Long idInstitucion, TipoEntidadDocumento tipo) {
        int anio = LocalDate.now().getYear();
        int yy = anio % 100;

        int folio = siguienteFolio(idInstitucion, anio);

        String prefTipo = PREFIJO_TIPO.getOrDefault(tipo, "X");

        return String.format("D%02d-%02d-%s-%05d.PND", yy, idInstitucion, prefTipo, folio);
    }

    /** Indica si {@code tipoDoc} usa el formato nuevo {PREFIJO}/{folio}/F4 en vez del formato D{YY}-{II}-{T}-{FOLIO}. */
    public static boolean usaFormatoPaciente(TipoDocumentoPaciente tipoDoc) {
        return PREFIJO_PACIENTE.containsKey(tipoDoc);
    }

    /**
     * Genera la etiqueta de Consentimiento/Cuestionario con formato {PREFIJO}/{folio}/F4.
     * <p>
     * Los cuestionarios siempre usan el folio fijo del paciente. Para Consentimiento, el
     * primer registro usa ese mismo folio; los posteriores agregan la revision al folio para
     * mantener etiquetas unicas sin mover el literal final F4: CI/{folio}-2/F4.
     */
    public String generarEtiquetaPaciente(TipoDocumentoPaciente tipoDoc, String folioPaciente, int revisionConsentimiento) {
        String prefijo = PREFIJO_PACIENTE.getOrDefault(tipoDoc, "X");
        String valor = tipoDoc == TipoDocumentoPaciente.CONSENTIMIENTO
                ? folioConsentimiento(folioPaciente, revisionConsentimiento)
                : folioPaciente;
        return prefijo + "/" + valor + "/" + LITERAL_F4;
    }

    private String folioConsentimiento(String folioPaciente, int revisionConsentimiento) {
        return revisionConsentimiento <= 1
                ? folioPaciente
                : folioPaciente + "-" + revisionConsentimiento;
    }

    private int siguienteFolio(Long idInstitucion, int anio) {
        DocumentoFolioSeq seq = folioSeqRepository
                .findByInstitucionAndAnioForUpdate(idInstitucion, anio)
                .orElseGet(() -> {
                    DocumentoFolioSeq nuevo = new DocumentoFolioSeq();
                    nuevo.setIdInstitucion(idInstitucion);
                    nuevo.setAnio(anio);
                    nuevo.setUltimoFolio(0);
                    return folioSeqRepository.save(nuevo);
                });

        seq.setUltimoFolio(seq.getUltimoFolio() + 1);
        folioSeqRepository.save(seq);
        return seq.getUltimoFolio();
    }

    private String resolverExtension(String mimeType, String nombreOriginal) {
        if (mimeType != null && MIME_A_EXTENSION.containsKey(mimeType.toLowerCase())) {
            return MIME_A_EXTENSION.get(mimeType.toLowerCase());
        }

        if (nombreOriginal != null && nombreOriginal.contains(".")) {
            String ext = nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1);
            if (!ext.isBlank() && ext.length() <= 10) {
                return ext.toUpperCase();
            }
        }

        return "BIN";
    }
}
