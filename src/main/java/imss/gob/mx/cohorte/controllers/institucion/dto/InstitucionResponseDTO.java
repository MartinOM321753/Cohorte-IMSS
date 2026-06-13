package imss.gob.mx.cohorte.controllers.institucion.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InstitucionResponseDTO {

    private Long id;
    private String uuid;
    private String nombre;
    private TipoInstitucionResumenDTO tipoInstitucion;
    private InstitucionResumenDTO institucionPadre;
    private Double latitud;
    private Double longitud;
    private String estado;
    private String ciudad;
    private String direccion;
    private String responsable;
    private String telefono;
    private EncargadoResumenDTO encargado;
    private Boolean tieneBiobanco;
    private Boolean activo;

    @Data
    @Builder
    public static class TipoInstitucionResumenDTO {
        private Long id;
        private String nombre;
    }

    /** Resumen ligero — evita serializar el árbol completo de instituciones (referencias circulares). */
    @Data
    @Builder
    public static class InstitucionResumenDTO {
        private Long id;
        private String uuid;
        private String nombre;
    }

    @Data
    @Builder
    public static class EncargadoResumenDTO {
        private Long id;
        private String uuid;
        private String username;
        private String nombreCompleto;
        private String email;
    }
}
