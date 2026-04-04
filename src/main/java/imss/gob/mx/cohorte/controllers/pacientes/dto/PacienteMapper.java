package imss.gob.mx.cohorte.controllers.pacientes.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.PersonaResponseDTO;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;

import java.util.List;

public class PacienteMapper {

    public static Paciente toEntity(PacienteRequestDTO dto) {
        Paciente paciente = new Paciente();
        paciente.setFolio(dto.getFolio());

        Persona persona = new Persona();
        persona.setNombre(dto.getPersona().getNombre());
        persona.setApellidoPaterno(dto.getPersona().getApellidoPaterno());
        persona.setApellidoMaterno(dto.getPersona().getApellidoMaterno());
        persona.setFechaNacimiento(dto.getPersona().getFechaNacimiento());
        persona.setSexo(Persona.Sexo.valueOf(dto.getPersona().getSexo()));
        persona.setTelefono(dto.getPersona().getTelefono());
        persona.setEmail(dto.getPersona().getEmail());
        paciente.setPersona(persona);

        return paciente;
    }

    public static PacienteResponseDTO toResponseDTO(Paciente p) {
        PersonaResponseDTO personaDTO = null;
        if (p.getPersona() != null) {
            Persona per = p.getPersona();
            personaDTO = PersonaResponseDTO.builder()
                .id(per.getId())
                .nombre(per.getNombre())
                .apellidoPaterno(per.getApellidoPaterno())
                .apellidoMaterno(per.getApellidoMaterno())
                .fechaNacimiento(per.getFechaNacimiento())
                .sexo(per.getSexo() != null ? per.getSexo().name() : null)
                .telefono(per.getTelefono())
                .email(per.getEmail())
                .build();
        }

        return PacienteResponseDTO.builder()
            .id(p.getId())
            .UUID(p.getUUID())
            .folio(p.getFolio())
            .activo(p.getActivo())
            .fechaRegistro(p.getFechaRegistro())
            .fechaActualizacion(p.getFechaActualizacion())
            .persona(personaDTO)
            .build();
    }

    public static PacienteResumenDTO toResumenDTO(Paciente p) {
        String nombreCompleto = "";
        if (p.getPersona() != null) {
            nombreCompleto = p.getPersona().getNombre() + " "
                + p.getPersona().getApellidoPaterno() + " "
                + (p.getPersona().getApellidoMaterno() != null ? p.getPersona().getApellidoMaterno() : "");
        }
        return PacienteResumenDTO.builder()
            .id(p.getId())
            .UUID(p.getUUID())
            .folio(p.getFolio())
            .nombreCompleto(nombreCompleto.trim())
            .sexo(p.getPersona() != null && p.getPersona().getSexo() != null ? p.getPersona().getSexo().name() : null)
            .build();
    }

    public static List<PacienteResponseDTO> toResponseDTOList(List<Paciente> list) {
        return list.stream().map(PacienteMapper::toResponseDTO).toList();
    }
}
