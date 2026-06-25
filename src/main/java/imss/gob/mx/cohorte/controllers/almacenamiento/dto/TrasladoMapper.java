package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.List;

public class TrasladoMapper {

    private TrasladoMapper() {}

    public static TrasladoResponseDTO toResponseDTO(TrasladoMuestra t, Long viewerInstitucionId) {
        Muestra muestra = t.getMuestra();
        return TrasladoResponseDTO.builder()
                .id(t.getId())
                .muestra(TrasladoResponseDTO.MuestraResumenDTO.builder()
                        .id(muestra.getId())
                        .etiqueta(muestra.getEtiqueta())
                        .unidad(muestra.getUnidad())
                        .estadoMuestra(muestra.getEstadoMuestra() != null
                                ? muestra.getEstadoMuestra().name() : null)
                        .esAlicuota(muestra.getMuestraPadre() != null)
                        .posicionLabel(buildPosicionLabel(muestra, viewerInstitucionId))
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

    public static List<TrasladoResponseDTO> toResponseDTOList(List<TrasladoMuestra> list, Long viewerInstitucionId) {
        return list.stream().map(t -> toResponseDTO(t, viewerInstitucionId)).toList();
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

    private static String buildPosicionLabel(Muestra muestra, Long viewerInstitucionId) {
        Institucion instActual = muestra.getInstitucionActual();
        boolean esMiMuestra = instActual != null && instActual.getId().equals(viewerInstitucionId);

        if (!esMiMuestra) {
            return instActual != null ? "En: " + instActual.getNombre() : null;
        }

        PosicionCaja pos = muestra.getPosicionCaja();
        if (pos == null) return null;
        CajaCriogenica caja = pos.getCaja();
        String cajaCode = caja != null ? caja.getCodigoCaja() : "?";
        return cajaCode + " [F" + pos.getFila() + ",C" + pos.getColumna() + "]";
    }

    private static TrasladoResponseDTO.UsuarioResumenDTO mapUsuario(BeanUser u) {
        if (u == null) return null;
        Persona p = u.getPersona();
        String nombre = p != null
                ? (p.getNombre()
                   + (p.getSegundoNombre() != null ? " " + p.getSegundoNombre() : "")
                   + " " + p.getApellidoPaterno()
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
