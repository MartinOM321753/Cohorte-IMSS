package imss.gob.mx.cohorte.controllers.reportes.dto;

import imss.gob.mx.cohorte.modules.reportes.ImagenReporte;
import imss.gob.mx.cohorte.services.reportes.ClaveImagen;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Una imagen de la galería, tal como la ve el editor.
 *
 * <p>Nunca lleva la ruta del objeto en MinIO: el cliente pide los bytes por
 * {@code /api/reportes/imagenes/{id}/contenido} y el backend los sirve. Exponer la
 * clave del bucket no aportaría nada y sí filtraría cómo está organizado por dentro.</p>
 */
@Data
@Builder
public class ImagenReporteResponseDTO {

    private Long id;
    private String nombre;
    private String contentType;
    private Long bytes;
    private Integer anchoPx;
    private Integer altoPx;
    private LocalDateTime fechaCreacion;

    /** Lo que se guarda en el diseño. Va calculado para que el editor no lo arme a mano. */
    private String clave;

    public static ImagenReporteResponseDTO de(ImagenReporte i) {
        return ImagenReporteResponseDTO.builder()
                .id(i.getId())
                .nombre(i.getNombre())
                .contentType(i.getContentType())
                .bytes(i.getBytes())
                .anchoPx(i.getAnchoPx())
                .altoPx(i.getAltoPx())
                .fechaCreacion(i.getFechaCreacion())
                .clave(ClaveImagen.de(i.getId()))
                .build();
    }
}
