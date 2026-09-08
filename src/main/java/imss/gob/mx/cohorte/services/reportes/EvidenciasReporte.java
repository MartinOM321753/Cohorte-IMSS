package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.documentos.Documento;
import imss.gob.mx.cohorte.modules.documentos.EstudioDocumento;
import imss.gob.mx.cohorte.modules.documentos.EstudioDocumentoRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Base64;
import java.util.List;

/**
 * Los archivos adjuntos de un estudio, para anexarlos al reporte como evidencia.
 *
 * <p>El caso que lo motiva es un DEXA: el estudio está capturado y además tiene la
 * imagen del equipo adjunta, y el documento que se entrega tiene que llevarla.</p>
 *
 * <p><b>Los adjuntos viven en {@code EstudioDocumento}, no en {@code EstudioAdjunto}.</b>
 * Esa segunda tabla es anterior al módulo de documentos y hoy no la escribe nadie
 * —está anotado en el propio código, donde se explica que contar sobre ella daba
 * siempre cero aunque el estudio tuviera archivos—. Buscar ahí habría hecho que el
 * reporte saliera siempre sin evidencias, y sin ningún error que lo delatara.</p>
 */
@Slf4j
@Service
@AllArgsConstructor
public class EvidenciasReporte {

    /**
     * Tope de lo que se incrusta por imagen. Una imagen entra en el PDF como texto
     * en base64, que abulta un tercio más que el archivo: sin límite, tres
     * radiografías dejarían un documento imposible de mandar por correo.
     */
    private static final long MAXIMO_BYTES = 4L * 1024 * 1024;

    private final EstudioDocumentoRepository estudioDocumentoRepository;
    private final MinioStorageService minioStorageService;

    /** Una evidencia lista para dibujarse. */
    public record Evidencia(String nombre, String mimeType, String dataUri, boolean incrustable, String motivo) {}

    /**
     * El bloque de evidencias listo para el documento.
     *
     * <p>Que el participante no tenga ese estudio no es un error: la misma plantilla
     * se usa con gente distinta y no todos tienen los mismos estudios hechos.</p>
     */
    public String html(imss.gob.mx.cohorte.modules.estudios.EstudioMedico estudio) {
        if (estudio == null || estudio.getId() == null) {
            return "<p class=\"evid-nota\">El participante no tiene este estudio registrado.</p>";
        }
        List<Evidencia> lista = deEstudio(estudio.getId());
        if (lista.isEmpty()) {
            return "<p class=\"evid-nota\">Este estudio no tiene archivos adjuntos.</p>";
        }

        StringBuilder sb = new StringBuilder();
        for (Evidencia ev : lista) {
            sb.append("<div class=\"evid\">");
            if (ev.incrustable() && ev.dataUri() != null) {
                sb.append("<img src=\"").append(ev.dataUri()).append("\" class=\"evid-img\"/>");
            }
            sb.append("<div class=\"evid-pie\">").append(Html.escapar(ev.nombre()));
            if (!ev.incrustable() && ev.motivo() != null) {
                sb.append(" <span class=\"evid-nota\">— ").append(Html.escapar(ev.motivo())).append("</span>");
            }
            sb.append("</div></div>");
        }
        return sb.toString();
    }

    /**
     * Las evidencias de un estudio, en el orden en que se adjuntaron.
     *
     * <p>Solo se incrustan imágenes. Un PDF adjunto no se puede meter dentro de otro
     * PDF como si fuera una figura: hay que concatenarlo, y eso ocurre después, al
     * ensamblar el documento. Aquí se anota que existe para poder mencionarlo.</p>
     */
    public List<Evidencia> deEstudio(Long idEstudio) {
        List<EstudioDocumento> vinculos =
                estudioDocumentoRepository.findByEstudio_IdOrderByOrdenAsc(idEstudio);

        return vinculos.stream()
                .map(EstudioDocumento::getDocumento)
                .filter(java.util.Objects::nonNull)
                .map(this::aEvidencia)
                .toList();
    }

    private Evidencia aEvidencia(Documento doc) {
        String mime = doc.getMimeType() != null ? doc.getMimeType() : "";
        String nombre = doc.getNombreOriginal() != null ? doc.getNombreOriginal() : "documento";

        if (!mime.startsWith("image/")) {
            return new Evidencia(nombre, mime, null, false, "No es una imagen");
        }
        if (doc.getTamanioBytes() != null && doc.getTamanioBytes() > MAXIMO_BYTES) {
            return new Evidencia(nombre, mime, null, false, "La imagen pesa demasiado para incrustarla");
        }

        try (InputStream in = minioStorageService.getObjectStream(doc.getObjectKey())) {
            byte[] bytes = in.readAllBytes();
            String dataUri = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
            return new Evidencia(nombre, mime, dataUri, true, null);
        } catch (Exception e) {
            // Que falte un archivo no puede impedir que se emita el reporte: el resto
            // del documento es válido y el dato clínico está capturado igual.
            log.warn("No se pudo leer la evidencia {} del estudio: {}", doc.getObjectKey(), e.getMessage());
            return new Evidencia(nombre, mime, null, false, "No se pudo leer el archivo");
        }
    }
}
