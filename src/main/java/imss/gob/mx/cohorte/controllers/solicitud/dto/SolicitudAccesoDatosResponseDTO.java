package imss.gob.mx.cohorte.controllers.solicitud.dto;

import imss.gob.mx.cohorte.modules.solicitud.EstadoSolicitudDatos;
import imss.gob.mx.cohorte.modules.solicitud.SolicitudAccesoDatos;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Getter
@Setter
public class SolicitudAccesoDatosResponseDTO {

    private Long id;
    private String nombreCompleto;
    private String organizacion;
    private String email;
    private String proposito;
    private String propositoOtro;
    private String proyecto;
    private String informacionSolicitada;
    private List<String> productos;
    private String productoOtro;
    private EstadoSolicitudDatos estado;
    private String ipSolicitante;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaActualizacion;

    public static SolicitudAccesoDatosResponseDTO from(SolicitudAccesoDatos e) {
        SolicitudAccesoDatosResponseDTO dto = new SolicitudAccesoDatosResponseDTO();
        dto.setId(e.getId());
        dto.setNombreCompleto(e.getNombreCompleto());
        dto.setOrganizacion(e.getOrganizacion());
        dto.setEmail(e.getEmail());
        dto.setProposito(e.getProposito());
        dto.setPropositoOtro(e.getPropositoOtro());
        dto.setProyecto(e.getProyecto());
        dto.setInformacionSolicitada(e.getInformacionSolicitada());
        dto.setProductos(e.getProductos() != null
                ? Arrays.asList(e.getProductos().split(","))
                : List.of());
        dto.setProductoOtro(e.getProductoOtro());
        dto.setEstado(e.getEstado());
        dto.setIpSolicitante(e.getIpSolicitante());
        dto.setFechaSolicitud(e.getFechaSolicitud());
        dto.setFechaActualizacion(e.getFechaActualizacion());
        return dto;
    }
}
