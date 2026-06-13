package imss.gob.mx.cohorte.controllers.institucion.dto;

import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import lombok.Builder;
import lombok.Data;

import java.sql.Timestamp;

@Data
@Builder
public class InstitucionModuloResponseDTO {
    private Long id;
    private Long idInstitucion;
    private String nombreInstitucion;
    private ModuloSistema modulo;
    private Boolean habilitado;
    private Long idOtorgadoPor;
    private String nombreOtorgadoPor;
    private Timestamp fechaOtorgamiento;
}
