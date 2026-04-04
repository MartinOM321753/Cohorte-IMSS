package imss.gob.mx.cohorte.controllers.estudios.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EstudioMapper {

    public static EstudioMedico toEntity(EstudioMedicoRequestDTO dto) {
        EstudioMedico estudio = new EstudioMedico();

        Paciente paciente = new Paciente();
        paciente.setUuid(dto.getPacienteUUID());
        estudio.setPaciente(paciente);

        BeanUser usuario = new BeanUser();
        usuario.setUUID(dto.getUsuarioRealizaUUID());
        estudio.setUsuarioRealiza(usuario);

        TipoEstudio tipoEstudio = new TipoEstudio();
        tipoEstudio.setId(dto.getIdTipoEstudio());
        estudio.setTipoEstudio(tipoEstudio);

        estudio.setFechaEstudio(dto.getFechaEstudio());
        estudio.setObservaciones(dto.getObservaciones());

        if (dto.getResultados() != null && !dto.getResultados().isEmpty()) {
            List<ResultadoEstudio> resultados = dto.getResultados().stream()
                .map(r -> {
                    ResultadoEstudio resultado = new ResultadoEstudio();
                    ParametroEstudio parametro = new ParametroEstudio();
                    parametro.setId(r.getIdParametro());
                    resultado.setParametro(parametro);
                    resultado.setValorNumerico(r.getValorNumerico());
                    resultado.setValorTexto(r.getValorTexto());
                    return resultado;
                })
                .collect(Collectors.toList());
            estudio.setResultadoEstudio(resultados);
        } else {
            estudio.setResultadoEstudio(new ArrayList<>());
        }

        return estudio;
    }

    public static EstudioMedicoResponseDTO toResponseDTO(EstudioMedico e) {
        PacienteResumenDTO pacienteDTO = null;
        if (e.getPaciente() != null) {
            pacienteDTO = PacienteMapper.toResumenDTO(e.getPaciente());
        }

        UsuarioResumenDTO usuarioDTO = null;
        if (e.getUsuarioRealiza() != null) {
            usuarioDTO = UserMapper.toResumenDTO(e.getUsuarioRealiza());
        }

        TipoEstudioResponseDTO tipoDTO = null;
        if (e.getTipoEstudio() != null) {
            tipoDTO = TipoEstudioResponseDTO.builder()
                .id(e.getTipoEstudio().getId())
                .nombre(e.getTipoEstudio().getNombre())
                .descripcion(e.getTipoEstudio().getDescripcion())
                .activo(e.getTipoEstudio().getActivo())
                .build();
        }

        List<ResultadoEstudioResponseDTO> resultadosDTO = null;
        if (e.getResultadoEstudio() != null) {
            resultadosDTO = e.getResultadoEstudio().stream()
                .map(r -> ResultadoEstudioResponseDTO.builder()
                    .id(r.getId())
                    .valorNumerico(r.getValorNumerico())
                    .valorTexto(r.getValorTexto())
                    .parametro(r.getParametro() != null ? r.getParametro().getNombre() : null)
                    .build())
                .collect(Collectors.toList());
        }

        return EstudioMedicoResponseDTO.builder()
            .id(e.getId())
            .observaciones(e.getObservaciones())
            .fechaEstudio(e.getFechaEstudio())
            .fechaRegistro(e.getFechaRegistro())
            .paciente(pacienteDTO)
            .usuarioRealiza(usuarioDTO)
            .tipoEstudio(tipoDTO)
            .resultados(resultadosDTO)
            .build();
    }

    public static List<EstudioMedicoResponseDTO> toResponseDTOList(List<EstudioMedico> list) {
        return list.stream().map(EstudioMapper::toResponseDTO).collect(Collectors.toList());
    }
}
