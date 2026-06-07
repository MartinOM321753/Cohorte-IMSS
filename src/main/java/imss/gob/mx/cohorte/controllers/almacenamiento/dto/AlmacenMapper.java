package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.almacen.Almacen;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.List;

public class AlmacenMapper {

    private AlmacenMapper() {}

    public static Almacen toEntity(AlmacenRequestDTO dto) {
        Almacen a = new Almacen();
        a.setNombre(dto.getNombre());
        a.setEstado(dto.getEstado());
        a.setCiudad(dto.getCiudad());
        a.setDireccion(dto.getDireccion());
        a.setResponsable(dto.getResponsable());
        a.setTelefono(dto.getTelefono());
        a.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        a.setTipo(dto.getTipo() != null ? dto.getTipo()
                : imss.gob.mx.cohorte.modules.almacenamiento.almacen.TipoInstitucion.OTRA);
        a.setTieneBiobanco(dto.getTieneBiobanco() != null ? dto.getTieneBiobanco() : true);
        return a;
    }

    public static AlmacenResponseDTO toResponseDTO(Almacen a) {
        return AlmacenResponseDTO.builder()
                .id(a.getId())
                .nombre(a.getNombre())
                .estado(a.getEstado())
                .ciudad(a.getCiudad())
                .direccion(a.getDireccion())
                .responsable(a.getResponsable())
                .telefono(a.getTelefono())
                .activo(a.getActivo())
                .tipo(a.getTipo())
                .tieneBiobanco(a.getTieneBiobanco())
                .encargado(mapEncargado(a.getEncargado()))
                .build();
    }

    public static List<AlmacenResponseDTO> toResponseDTOList(List<Almacen> list) {
        return list.stream().map(AlmacenMapper::toResponseDTO).toList();
    }

    private static AlmacenResponseDTO.EncargadoResumenDTO mapEncargado(BeanUser user) {
        if (user == null) return null;
        Persona p = user.getPersona();
        String nombreCompleto = p.getNombre() + " " + p.getApellidoPaterno()
                + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "");
        return AlmacenResponseDTO.EncargadoResumenDTO.builder()
                .id(user.getId())
                .uuid(user.getUUID())
                .username(user.getUsername())
                .nombreCompleto(nombreCompleto.trim())
                .email(p.getEmail())
                .build();
    }
}
