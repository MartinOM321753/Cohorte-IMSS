package imss.gob.mx.cohorte.controllers.users.dto;

import imss.gob.mx.cohorte.controllers.DTO.PersonaResponseDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.List;

public class UserMapper {

    public static BeanUser toEntity(UserRequestDTO dto) {
        BeanUser user = new BeanUser();
        // username se genera automáticamente en UserApplicationService.saveUser()
        // La contraseña también la genera el sistema ahí mismo

        Persona persona = new Persona();
        persona.setNombre(dto.getPersona().getNombre());
        persona.setApellidoPaterno(dto.getPersona().getApellidoPaterno());
        persona.setApellidoMaterno(dto.getPersona().getApellidoMaterno());
        persona.setFechaNacimiento(dto.getPersona().getFechaNacimiento());
        persona.setSexo(Persona.Sexo.valueOf(dto.getPersona().getSexo()));
        persona.setTelefono(dto.getPersona().getTelefono());
        persona.setEmail(dto.getPersona().getEmail());
        user.setPersona(persona);

        // El UUID del rol se resuelve a la entidad completa en UserApplicationService
        Role role = new Role();
        role.setUuid(dto.getRolUuid());
        user.setRol(role);

        // El UUID de la institución también se resuelve a la entidad completa en UserApplicationService
        Institucion institucion = new Institucion();
        institucion.setUuid(dto.getInstitucionUuid());
        user.setInstitucion(institucion);

        return user;
    }

    public static UserResponseDTO toResponseDTO(BeanUser user) {
        PersonaResponseDTO personaDTO = null;
        if (user.getPersona() != null) {
            Persona p = user.getPersona();
            personaDTO = PersonaResponseDTO.builder()
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
                .debeResetear(user.getDebeResetear())
                .rol(user.getRol() != null
                        ? new UserResponseDTO.RolDTO(user.getRol().getUuid(), user.getRol().getRole())
                        : null)
                .fechaCreacion(user.getFechaCreacion())
                .persona(personaDTO)
                .institucion(user.getInstitucion() != null
                        ? new UserResponseDTO.InstitucionResumenDTO(user.getInstitucion().getId(), user.getInstitucion().getUuid(), user.getInstitucion().getNombre())
                        : null)
                .build();
    }

    public static List<UserResponseDTO> toResponseDTOList(List<BeanUser> list) {
        return list.stream().map(UserMapper::toResponseDTO).toList();
    }

    public static UsuarioResumenDTO toResumenDTO(BeanUser usuarioRegistro) {
        return new UsuarioResumenDTO().toUserEntity(usuarioRegistro);
    }
}
