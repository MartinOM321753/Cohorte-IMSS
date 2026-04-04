package imss.gob.mx.cohorte.controllers.examenes.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.List;

public class ResultadoExamenMapper {

    public static ResultadoExamen toEntity(ResultadoExamenRequestDTO dto) {
        ResultadoExamen resultado = new ResultadoExamen();

        Paciente paciente = new Paciente();
        paciente.setUUID(dto.getPacienteUUID());
        resultado.setPaciente(paciente);

        BeanUser usuario = new BeanUser();
        usuario.setUUID(dto.getUsuarioRegistroUUID());
        resultado.setUsuarioRegistro(usuario);

        Examen examen = new Examen();
        examen.setId(dto.getIdExamen());
        resultado.setExamen(examen);

        resultado.setValorObtenido(dto.getValorObtenido());
        resultado.setObservaciones(dto.getObservaciones());
        resultado.setFechaResultado(dto.getFechaResultado());

        return resultado;
    }

    public static ResultadoExamenResponseDTO toResponseDTO(ResultadoExamen r, ExamenResponseDTO examenDTO, Boolean dentroDeRango) {
        PacienteResumenDTO pacienteDTO = PacienteMapper.toResumenDTO(r.getPaciente());
        UsuarioResumenDTO usuarioDTO = UserMapper.toResumenDTO(r.getUsuarioRegistro());

        return ResultadoExamenResponseDTO.builder()
            .id(r.getId())
            .valorObtenido(r.getValorObtenido())
            .observaciones(r.getObservaciones())
            .fechaResultado(r.getFechaResultado())
            .paciente(pacienteDTO)
            .usuarioRegistro(usuarioDTO)
            .examen(examenDTO)
            .dentroDeRango(dentroDeRango)
            .build();
    }

    public static List<ResultadoExamenResponseDTO> toResponseDTOList(List<ResultadoExamen> list) {
        return list.stream().map(r -> {
            ExamenResponseDTO examenDTO = ExamenMapper.toResponseDTO(r.getExamen());
            Boolean dentroDeRango = calcularDentroDeRango(r.getValorObtenido(), r.getExamen());
            return toResponseDTO(r, examenDTO, dentroDeRango);
        }).toList();
    }

    private static Boolean calcularDentroDeRango(Double valor, Examen examen) {
        if (valor == null || examen == null) return null;
        Double min = examen.getValorMinMujeres();
        Double max = examen.getValorMaxMujeres();
        if (min != null && valor < min) return false;
        if (max != null && valor > max) return false;
        return true;
    }
}
