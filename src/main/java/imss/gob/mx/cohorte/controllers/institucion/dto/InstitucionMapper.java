package imss.gob.mx.cohorte.controllers.institucion.dto;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.List;

public class InstitucionMapper {

    private InstitucionMapper() {}

    public static Institucion toEntity(InstitucionRequestDTO dto) {
        Institucion i = new Institucion();
        i.setNombre(dto.getNombre());
        i.setLatitud(dto.getLatitud());
        i.setLongitud(dto.getLongitud());
        i.setEstado(dto.getEstado());
        i.setCiudad(dto.getCiudad());
        i.setDireccion(dto.getDireccion());
        i.setResponsable(dto.getResponsable());
        i.setTelefono(dto.getTelefono());
        i.setTieneBiobanco(dto.getTieneBiobanco() != null ? dto.getTieneBiobanco() : false);
        i.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        return i;
    }

    public static InstitucionResponseDTO toResponseDTO(Institucion i) {
        return InstitucionResponseDTO.builder()
                .id(i.getId())
                .uuid(i.getUuid())
                .nombre(i.getNombre())
                .tipoInstitucion(mapTipo(i.getTipoInstitucion()))
                .institucionPadre(mapPadre(i.getInstitucionPadre()))
                .latitud(i.getLatitud())
                .longitud(i.getLongitud())
                .estado(i.getEstado())
                .ciudad(i.getCiudad())
                .direccion(i.getDireccion())
                .responsable(i.getResponsable())
                .telefono(i.getTelefono())
                .encargado(mapEncargado(i.getEncargado()))
                .tieneBiobanco(i.getTieneBiobanco())
                .activo(i.getActivo())
                .build();
    }

    public static List<InstitucionResponseDTO> toResponseDTOList(List<Institucion> list) {
        return list.stream().map(InstitucionMapper::toResponseDTO).toList();
    }

    private static InstitucionResponseDTO.TipoInstitucionResumenDTO mapTipo(imss.gob.mx.cohorte.modules.institucion.TipoInstitucion tipo) {
        if (tipo == null) return null;
        return InstitucionResponseDTO.TipoInstitucionResumenDTO.builder()
                .id(tipo.getId())
                .nombre(tipo.getNombre())
                .build();
    }

    private static InstitucionResponseDTO.InstitucionResumenDTO mapPadre(Institucion padre) {
        if (padre == null) return null;
        return InstitucionResponseDTO.InstitucionResumenDTO.builder()
                .id(padre.getId())
                .uuid(padre.getUuid())
                .nombre(padre.getNombre())
                .build();
    }

    private static InstitucionResponseDTO.EncargadoResumenDTO mapEncargado(BeanUser user) {
        if (user == null) return null;
        Persona p = user.getPersona();
        String nombreCompleto = p.getNombre() + " " + p.getApellidoPaterno()
                + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "");
        return InstitucionResponseDTO.EncargadoResumenDTO.builder()
                .id(user.getId())
                .uuid(user.getUUID())
                .username(user.getUsername())
                .nombreCompleto(nombreCompleto.trim())
                .email(p.getEmail())
                .build();
    }

    /** Resumen ligero usado en selects con autocompletado (server-side search). */
    public static InstitucionResponseDTO.InstitucionResumenDTO toResumenDTO(Institucion i) {
        return InstitucionResponseDTO.InstitucionResumenDTO.builder()
                .id(i.getId())
                .uuid(i.getUuid())
                .nombre(i.getNombre())
                .build();
    }

    public static List<InstitucionResponseDTO.InstitucionResumenDTO> toResumenDTOList(List<Institucion> list) {
        return list.stream().map(InstitucionMapper::toResumenDTO).toList();
    }
}
