package imss.gob.mx.cohorte.controllers.users.dto;

import imss.gob.mx.cohorte.controllers.DTO.PersonaResponseDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;


import java.util.List;

public class UserMapper {

    public static BeanUser toEntity(UserRequestDTO dto) {
        BeanUser user = new BeanUser();
        user.setUsername(dto.getUsername());
        user.setPassword(dto.getPassword());

        Persona persona = new Persona();
        persona.setNombre(dto.getPersona().getNombre());
        persona.setApellidoPaterno(dto.getPersona().getApellidoPaterno());
        persona.setApellidoMaterno(dto.getPersona().getApellidoMaterno());
        persona.setFechaNacimiento(dto.getPersona().getFechaNacimiento());
        persona.setSexo(Persona.Sexo.valueOf(dto.getPersona().getSexo()));
        persona.setTelefono(dto.getPersona().getTelefono());
        persona.setEmail(dto.getPersona().getEmail());
        user.setPersona(persona);

        return user;
    }

    public static UserResponseDTO toResponseDTO(BeanUser user) {
        PersonaResponseDTO personaDTO = null;
        if (user.getPersona() != null) {
            Persona p = user.getPersona();
            personaDTO = PersonaResponseDTO.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .apellidoPaterno(p.getApellidoPaterno())
                .apellidoMaterno(p.getApellidoMaterno())
                .fechaNacimiento(p.getFechaNacimiento())
                .sexo(p.getSexo() != null ? p.getSexo().name() : null)
                .telefono(p.getTelefono())
                .email(p.getEmail())
                .build();
        }

        return UserResponseDTO.builder()
            .id(user.getId())
            .username(user.getUsername())
            .UUID(user.getUUID())
            .activo(user.getActivo())
            .rol(user.getRol() != null ? user.getRol().getRole() : null)
            .fechaCreacion(user.getFechaCreacion())
            .persona(personaDTO)
            .build();
    }

    public static List<UserResponseDTO> toResponseDTOList(List<BeanUser> list) {
        return list.stream().map(UserMapper::toResponseDTO).toList();
    }

    public static UsuarioResumenDTO toResumenDTO(BeanUser usuarioRegistro) {
        return null;
    }
}
