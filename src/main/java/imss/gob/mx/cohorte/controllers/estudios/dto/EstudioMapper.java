package imss.gob.mx.cohorte.controllers.estudios.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EstudioMapper {

    private static final String ROOT_GROUP_CODE = "ROOT";
    private static final int ROOT_ORDER = 0;

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
                        resultado.setValorBooleano(r.getValorBooleano());
                        resultado.setGrupoCodigo(r.getGrupoCodigo());
                        resultado.setGrupoEtiqueta(r.getGrupoEtiqueta());
                        resultado.setOrdenResultado(r.getOrden());
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
        PacienteResumenDTO pacienteDTO = e.getPaciente() != null ? PacienteMapper.toResumenDTO(e.getPaciente()) : null;
        UsuarioResumenDTO usuarioDTO = e.getUsuarioRealiza() != null ? UserMapper.toResumenDTO(e.getUsuarioRealiza()) : null;

        TipoEstudioResponseDTO tipoDTO = null;
        if (e.getTipoEstudio() != null) {
            tipoDTO = TipoEstudioResponseDTO.builder()
                    .id(e.getTipoEstudio().getId())
                    .nombre(e.getTipoEstudio().getNombre())
                    .descripcion(e.getTipoEstudio().getDescripcion())
                    .activo(e.getTipoEstudio().getActivo())
                    .parametroEstudios(e.getTipoEstudio().getParametros() != null
                        ? e.getTipoEstudio().getParametros().stream()
                            .map(p -> ParametroEstudioResponseDTO.builder()
                                .id(p.getId())
                                .nombre(p.getNombre())
                                .unidad(p.getUnidad())
                                .tipo(p.getTipo())
                                .valorMinimo(p.getValorMinimo())
                                .valorMaximo(p.getValorMaximo())
                                .opciones(p.getTipo() == imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro.TEXTO_OPCIONES && p.getOpciones() != null
                                    ? p.getOpciones().stream().map(op -> op.getValor()).collect(java.util.stream.Collectors.toList())
                                    : null)
                                .build())
                            .collect(java.util.stream.Collectors.toList())
                        : java.util.List.of())
                    .build();
        }

        List<ResultadoEstudioResponseDTO> resultadosDTO = null;
        if (e.getResultadoEstudio() != null) {
            resultadosDTO = e.getResultadoEstudio().stream()
                    .map(r -> ResultadoEstudioResponseDTO.builder()
                            .id(r.getId())
                            .valorNumerico(r.getValorNumerico())
                            .valorTexto(r.getValorTexto())
                            .valorBooleano(r.getValorBooleano())
                            .parametro(r.getParametro() != null ? r.getParametro().getNombre() : null)
                            .grupoCodigo(ROOT_GROUP_CODE.equals(r.getGrupoCodigo()) ? null : r.getGrupoCodigo())
                            .grupoEtiqueta(ROOT_GROUP_CODE.equals(r.getGrupoCodigo()) ? null : r.getGrupoEtiqueta())
                            .orden(ROOT_GROUP_CODE.equals(r.getGrupoCodigo()) && r.getOrdenResultado() == ROOT_ORDER ? null : r.getOrdenResultado())
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
                .institucionUuid(e.getInstitucion() != null ? e.getInstitucion().getUuid() : null)
                .institucionNombre(e.getInstitucion() != null ? e.getInstitucion().getNombre() : null)
                .build();
    }

    public static EstudioListRequestDTO toResponseDTOList(EstudioMedico e) {
        int numResultados = (e.getResultadoEstudio() != null) ? e.getResultadoEstudio().size() : 0;
        int numAdjuntos   = (e.getAdjuntos() != null)        ? e.getAdjuntos().size()          : 0;
        return EstudioListRequestDTO.builder()
                .id(e.getId())
                .fechaEstudio(e.getFechaEstudio())
                .paciente(e.getPaciente().getPersona().getNombre() + " " + e.getPaciente().getPersona().getApellidoPaterno() + " " + e.getPaciente().getPersona().getApellidoMaterno())
                .pacienteuuid(e.getPaciente().getUuid())
                .usuarioRealiza(e.getUsuarioRealiza().getPersona().getNombre() + " " + e.getUsuarioRealiza().getPersona().getApellidoPaterno() + " " + e.getUsuarioRealiza().getPersona().getApellidoMaterno())
                .usuarioRealizauuid(e.getUsuarioRealiza().getUUID())
                .tipoEstudio(e.getTipoEstudio().getNombre())
                .tipoEstudioid(e.getTipoEstudio().getId())
                .cantidadResultados(numResultados)
                .cantidadAdjuntos(numAdjuntos)
                .build();
    }

    public static ParametroEstudioResponseDTO toParametroDTO(ParametroEstudio p) {
        List<String> opciones = (p.getTipo() == TipoParametro.TEXTO_OPCIONES && p.getOpciones() != null)
            ? p.getOpciones().stream().map(op -> op.getValor()).collect(Collectors.toList())
            : null;
        return ParametroEstudioResponseDTO.builder()
            .id(p.getId())
            .nombre(p.getNombre())
            .unidad(p.getUnidad())
            .tipo(p.getTipo())
            .tipoEstudio(p.getTipoEstudio() != null ? p.getTipoEstudio().getNombre() : null)
            .valorMinimo(p.getValorMinimo())
            .valorMaximo(p.getValorMaximo())
            .opciones(opciones)
            .build();
    }

    public static List<EstudioListRequestDTO> toResponseDTOList(List<EstudioMedico> list) {
        return list.stream().map(EstudioMapper::toResponseDTOList).collect(Collectors.toList());
    }
}
