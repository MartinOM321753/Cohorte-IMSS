package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.almacen.TipoInstitucion;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AlmacenResponseDTO {

    private Long id;
    private String nombre;
    private String estado;
    private String ciudad;
    private String direccion;
    private String responsable;
    private String telefono;
    private Boolean activo;
    private TipoInstitucion tipo;
    private Boolean tieneBiobanco;
    private EncargadoResumenDTO encargado;

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
