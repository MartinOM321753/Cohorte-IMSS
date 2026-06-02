package imss.gob.mx.cohorte.controllers.somatometria.dto;

import imss.gob.mx.cohorte.modules.somatometria.Somatometria;

public class SomatometriaMapper {

    private SomatometriaMapper() {}

    public static SomatometriaResponseDTO toDTO(Somatometria s) {
        String nombrePaciente = s.getPaciente() != null && s.getPaciente().getPersona() != null
                ? buildNombre(
                    s.getPaciente().getPersona().getNombre(),
                    s.getPaciente().getPersona().getApellidoPaterno(),
                    s.getPaciente().getPersona().getApellidoMaterno())
                : null;

        String nombreUsuario = s.getUsuarioRegistra() != null && s.getUsuarioRegistra().getPersona() != null
                ? buildNombre(
                    s.getUsuarioRegistra().getPersona().getNombre(),
                    s.getUsuarioRegistra().getPersona().getApellidoPaterno(),
                    s.getUsuarioRegistra().getPersona().getApellidoMaterno())
                : (s.getUsuarioRegistra() != null ? s.getUsuarioRegistra().getUsername() : null);

        return SomatometriaResponseDTO.builder()
                .id(s.getId())
                .pacienteUUID(s.getPaciente() != null ? s.getPaciente().getUuid() : null)
                .pacienteNombre(nombrePaciente)
                .fechaMedicion(s.getFechaMedicion())
                .pesoKg(s.getPesoKg())
                .tallaM(s.getTallaM())
                .imc(s.getImc())
                .presionSistolica(s.getPresionSistolica())
                .presionDiastolica(s.getPresionDiastolica())
                .circunferenciaAbdominalCm(s.getCircunferenciaAbdominalCm())
                .frecuenciaCardiacaReposo(s.getFrecuenciaCardiacaReposo())
                .observaciones(s.getObservaciones())
                .usuarioRegistraNombre(nombreUsuario)
                .fechaRegistro(s.getFechaRegistro())
                .build();
    }

    private static String buildNombre(String nombre, String apPaterno, String apMaterno) {
        StringBuilder sb = new StringBuilder();
        if (nombre != null) sb.append(nombre);
        if (apPaterno != null) sb.append(" ").append(apPaterno);
        if (apMaterno != null && !apMaterno.isBlank()) sb.append(" ").append(apMaterno);
        return sb.toString().trim();
    }
}
