package imss.gob.mx.cohorte.controllers.reclutamiento.dto;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.reclutamiento.ReclutamientoParticipante;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

public class ReclutamientoParticipanteMapper {

    private ReclutamientoParticipanteMapper() {}

    public static ReclutamientoParticipanteResponseDTO toResponseDTO(ReclutamientoParticipante r) {
        if (r == null) return null;
        return ReclutamientoParticipanteResponseDTO.builder()
                .id(r.getId())
                .tipoReclutamiento(r.getTipoReclutamiento())
                .estadoContacto(r.getEstadoContacto())
                .medioContacto(r.getMedioContacto())
                .institucionReclutamiento(mapInstitucion(r.getInstitucionReclutamiento()))
                .usuarioRecluta(mapUsuario(r.getUsuarioRecluta()))
                .observaciones(r.getObservaciones())
                .fechaContacto(r.getFechaContacto())
                .fechaRegistro(r.getFechaRegistro())
                .build();
    }

    private static ReclutamientoParticipanteResponseDTO.InstitucionResumenDTO mapInstitucion(Institucion i) {
        if (i == null) return null;
        return ReclutamientoParticipanteResponseDTO.InstitucionResumenDTO.builder()
                .id(i.getId())
                .uuid(i.getUuid())
                .nombre(i.getNombre())
                .build();
    }

    private static ReclutamientoParticipanteResponseDTO.UsuarioResumenDTO mapUsuario(BeanUser u) {
        if (u == null) return null;
        Persona p = u.getPersona();
        String nombreCompleto = p != null
                ? (p.getNombre()
                   + (p.getSegundoNombre() != null ? " " + p.getSegundoNombre() : "")
                   + " " + p.getApellidoPaterno()
                   + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "")).trim()
                : u.getUsername();
        return ReclutamientoParticipanteResponseDTO.UsuarioResumenDTO.builder()
                .id(u.getId())
                .uuid(u.getUUID())
                .nombreCompleto(nombreCompleto)
                .build();
    }
}
