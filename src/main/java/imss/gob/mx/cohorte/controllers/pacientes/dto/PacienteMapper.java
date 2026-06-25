package imss.gob.mx.cohorte.controllers.pacientes.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.PersonaResponseDTO;
import imss.gob.mx.cohorte.controllers.reclutamiento.dto.ReclutamientoParticipanteMapper;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.reclutamiento.ReclutamientoParticipante;

import java.util.List;

public class PacienteMapper {

    public static Paciente toEntity(PacienteRequestDTO dto) {
        Paciente paciente = new Paciente();
        paciente.setFolio(dto.getFolio());

        Persona persona = new Persona();
        persona.setNombre(dto.getPersona().getNombre());
        persona.setSegundoNombre(dto.getPersona().getSegundoNombre());
        persona.setApellidoPaterno(dto.getPersona().getApellidoPaterno());
        persona.setApellidoMaterno(dto.getPersona().getApellidoMaterno());
        persona.setFechaNacimiento(dto.getPersona().getFechaNacimiento());
        persona.setSexo(dto.getPersona().getSexo() != null ? Persona.Sexo.valueOf(dto.getPersona().getSexo()) : null);
        persona.setCurp(dto.getPersona().getCurp());
        persona.setTelefono(dto.getPersona().getTelefono());
        persona.setEmail(dto.getPersona().getEmail());
        paciente.setPersona(persona);

        return paciente;
    }

    public static PacienteResponseDTO toResponseDTO(Paciente p) {
        return toResponseDTO(p, null, null);
    }

    public static PacienteResponseDTO toResponseDTO(Paciente p, ReclutamientoParticipante reclutamiento) {
        return toResponseDTO(p, reclutamiento, null);
    }

    public static PacienteResponseDTO toResponseDTO(Paciente p, ReclutamientoParticipante reclutamiento, Long idInstitucionActual) {
        PersonaResponseDTO personaDTO = null;
        if (p.getPersona() != null) {
            Persona per = p.getPersona();
            personaDTO = PersonaResponseDTO.builder()
                .id(per.getId())
                .nombre(per.getNombre())
                .segundoNombre(per.getSegundoNombre())
                .apellidoPaterno(per.getApellidoPaterno())
                .apellidoMaterno(per.getApellidoMaterno())
                .fechaNacimiento(per.getFechaNacimiento())
                .sexo(per.getSexo() != null ? per.getSexo().name() : null)
                .curp(per.getCurp())
                .telefono(per.getTelefono())
                .email(per.getEmail())
                .build();
        }

        Long instId = p.getInstitucion() != null ? p.getInstitucion().getId() : null;
        String instNombre = p.getInstitucion() != null ? p.getInstitucion().getNombre() : null;
        Boolean propia = (idInstitucionActual != null && instId != null)
                ? instId.equals(idInstitucionActual)
                : null;

        return PacienteResponseDTO.builder()
            .id(p.getId())
            .UUID(p.getUuid())
            .folio(p.getFolio())
            .activo(p.getActivo())
            .fechaRegistro(p.getFechaRegistro())
            .fechaActualizacion(p.getFechaActualizacion())
            .persona(personaDTO)
            .reclutamiento(ReclutamientoParticipanteMapper.toResponseDTO(reclutamiento))
            .institucionId(instId)
            .institucionNombre(instNombre)
            .propiaInstitucion(propia)
            .build();
    }

    public static PacienteResumenDTO toResumenDTO(Paciente p) {
        String nombreCompleto = "";
        if (p.getPersona() != null) {
            nombreCompleto = p.getPersona().getNombre()
                + (p.getPersona().getSegundoNombre() != null ? " " + p.getPersona().getSegundoNombre() : "")
                + " " + p.getPersona().getApellidoPaterno()
                + (p.getPersona().getApellidoMaterno() != null ? " " + p.getPersona().getApellidoMaterno() : "");
        }
        return PacienteResumenDTO.builder()
            .id(p.getId())
            .UUID(p.getUuid())
            .folio(p.getFolio())
            .nombreCompleto(nombreCompleto.trim())
            .sexo(p.getPersona() != null && p.getPersona().getSexo() != null ? p.getPersona().getSexo().name() : null)
            .build();
    }

    public static List<PacienteResponseDTO> toResponseDTOList(List<Paciente> list) {
        return list.stream().map(PacienteMapper::toResponseDTO).toList();
    }

    public static List<PacienteResponseDTO> toResponseDTOList(List<Paciente> list, Long idInstitucionActual) {
        return list.stream().map(p -> toResponseDTO(p, null, idInstitucionActual)).toList();
    }
}
