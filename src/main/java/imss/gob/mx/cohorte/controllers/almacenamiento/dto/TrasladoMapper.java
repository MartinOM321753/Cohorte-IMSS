package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.List;

public class TrasladoMapper {

    private TrasladoMapper() {}

    public static TrasladoResponseDTO toResponseDTO(TrasladoMuestra t) {
        return TrasladoResponseDTO.builder()
                .id(t.getId())
                .muestra(TrasladoResponseDTO.MuestraResumenDTO.builder()
                        .id(t.getMuestra().getId())
                        .etiqueta(t.getMuestra().getEtiqueta())
                        .unidad(t.getMuestra().getUnidad())
                        .estadoMuestra(t.getMuestra().getEstadoMuestra() != null
                                ? t.getMuestra().getEstadoMuestra().name() : null)
                        .esAlicuota(t.getMuestra().getMuestraPadre() != null)
                        .build())
                .institucionOrigen(mapInstitucion(t.getInstitucionOrigen()))
                .institucionDestino(mapInstitucion(t.getInstitucionDestino()))
                .autorizadoPor(mapUsuario(t.getAutorizadoPor()))
                .recibidoPor(mapUsuario(t.getRecibidoPor()))
                .estado(t.getEstado().name())
                .fechaTraslado(t.getFechaTraslado())
                .fechaRetorno(t.getFechaRetorno())
                .motivo(t.getMotivo())
                .observaciones(t.getObservaciones())
                .grupoTraslado(t.getGrupoTraslado())
                .build();
    }

    public static List<TrasladoResponseDTO> toResponseDTOList(List<TrasladoMuestra> list) {
        return list.stream().map(TrasladoMapper::toResponseDTO).toList();
    }

    private static TrasladoResponseDTO.InstitucionResumenDTO mapInstitucion(Institucion i) {
        if (i == null) return null;
        return TrasladoResponseDTO.InstitucionResumenDTO.builder()
                .id(i.getId())
                .uuid(i.getUuid())
                .nombre(i.getNombre())
                .ciudad(i.getCiudad())
                .estado(i.getEstado())
                .build();
    }

    private static TrasladoResponseDTO.UsuarioResumenDTO mapUsuario(BeanUser u) {
        if (u == null) return null;
        Persona p = u.getPersona();
        String nombre = p != null
                ? (p.getNombre() + " " + p.getApellidoPaterno()
                   + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "")).trim()
                : u.getUsername();
        return TrasladoResponseDTO.UsuarioResumenDTO.builder()
                .id(u.getId())
                .uuid(u.getUUID())
                .username(u.getUsername())
                .nombreCompleto(nombre)
                .build();
    }
}
