package imss.gob.mx.cohorte.controllers.almacenamiento.dto.colaborador;

import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.Colaborador;
import imss.gob.mx.cohorte.modules.persona.Persona;

import java.util.List;

public class ColaboradorMapper {

    private ColaboradorMapper() {}

    public static ColaboradorResponseDTO toResponseDTO(Colaborador c) {
        Persona p = c.getPersona();
        return ColaboradorResponseDTO.builder()
                .id(c.getId())
                .nombre(p != null ? p.getNombre() : null)
                .segundoNombre(p != null ? p.getSegundoNombre() : null)
                .apellidoPaterno(p != null ? p.getApellidoPaterno() : null)
                .apellidoMaterno(p != null ? p.getApellidoMaterno() : null)
                .nombreCompleto(nombreCompleto(p))
                .fechaNacimiento(p != null ? p.getFechaNacimiento() : null)
                .sexo(p != null && p.getSexo() != null ? p.getSexo().name() : null)
                .telefono(p != null ? p.getTelefono() : null)
                .curp(p != null ? p.getCurp() : null)
                .email(p != null ? p.getEmail() : null)
                .cargo(c.getCargo())
                .adscripcion(c.getAdscripcion())
                .esExterno(Boolean.TRUE.equals(c.getEsExterno()))
                .activo(Boolean.TRUE.equals(c.getActivo()))
                .fechaRegistro(c.getFechaRegistro())
                .build();
    }

    public static List<ColaboradorResponseDTO> toResponseDTOList(List<Colaborador> list) {
        return list.stream().map(ColaboradorMapper::toResponseDTO).toList();
    }

    /** Construye una Persona a partir de los datos del request (alta/edición). */
    public static Persona toPersona(ColaboradorRequestDTO dto) {
        Persona p = new Persona();
        p.setNombre(dto.getNombre());
        p.setSegundoNombre(trimOrNull(dto.getSegundoNombre()));
        p.setApellidoPaterno(dto.getApellidoPaterno());
        p.setApellidoMaterno(trimOrNull(dto.getApellidoMaterno()));
        p.setFechaNacimiento(dto.getFechaNacimiento());
        p.setSexo(parseSexo(dto.getSexo()));
        p.setTelefono(trimOrNull(dto.getTelefono()));
        p.setCurp(trimOrNull(dto.getCurp()));
        p.setEmail(trimOrNull(dto.getEmail()));
        return p;
    }

    public static String nombreCompleto(Persona p) {
        if (p == null) return null;
        return (p.getNombre()
                + (p.getSegundoNombre() != null ? " " + p.getSegundoNombre() : "")
                + " " + p.getApellidoPaterno()
                + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "")).trim();
    }

    private static Persona.Sexo parseSexo(String sexo) {
        if (sexo == null || sexo.isBlank()) return null;
        try {
            return Persona.Sexo.valueOf(sexo.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String trimOrNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
