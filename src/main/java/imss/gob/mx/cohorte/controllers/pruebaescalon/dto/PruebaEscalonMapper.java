package imss.gob.mx.cohorte.controllers.pruebaescalon.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.time.LocalDate;
import java.util.List;

public class PruebaEscalonMapper {

    public static PruebaEscalon toEntity(PruebaEscalonRequestDTO dto) {
        PruebaEscalon prueba = new PruebaEscalon();
        
        Paciente paciente = new Paciente();
        paciente.setUUID(dto.getPacienteUUID());
        prueba.setPaciente(paciente);
        
        BeanUser usuario = new BeanUser();
        usuario.setUUID(dto.getUsuarioRealizaUUID());
        prueba.setUsuarioRealiza(usuario);
        
        prueba.setFechaEstudio(dto.getFechaEstudio());
        
        return prueba;
    }

    public static PruebaEscalonResponseDTO toResponseDTO(PruebaEscalon p) {
        List<EtapaResponseDTO> etapasDTO = null;
        if (p.getEtapas() != null && !p.getEtapas().isEmpty()) {
            etapasDTO = p.getEtapas().stream().map(PruebaEscalonMapper::toEtapaResponseDTO).toList();
        }

        return PruebaEscalonResponseDTO.builder()
            .id(p.getId())
            .fechaEstudio(p.getFechaEstudio())
            .fechaRegistro(p.getFechaRegistro())
            .fechaActualizacion(p.getFechaActualizacion())
            .paciente(p.getPaciente() != null ? PacienteMapper.toResumenDTO(p.getPaciente()) : null)
            .usuarioRealiza(p.getUsuarioRealiza() != null ? UserMapper.toResumenDTO(p.getUsuarioRealiza()) : null)
            .etapas(etapasDTO)
            .build();
    }

    public static List<PruebaEscalonResponseDTO> toResponseDTOList(List<PruebaEscalon> list) {
        return list.stream().map(PruebaEscalonMapper::toResponseDTO).toList();
    }

    private static EtapaResponseDTO toEtapaResponseDTO(PruebaEscalonEtapa etapa) {
        MedicionResponseDTO medicionDTO = null;
        if (etapa.getMedicion() != null) {
            medicionDTO = toMedicionResponseDTO(etapa.getMedicion());
        }

        return EtapaResponseDTO.builder()
            .id(etapa.getId())
            .etapa(etapa.getEtapa() != null ? etapa.getEtapa().name() : null)
            .observaciones(etapa.getObservaciones())
            .fechaRegistro(etapa.getFechaRegistro())
            .fechaActualizacion(etapa.getFechaActualizacion())
            .medicion(medicionDTO)
            .build();
    }

    private static MedicionResponseDTO toMedicionResponseDTO(PruebaEscalonMedicion medicion) {
        return MedicionResponseDTO.builder()
            .id(medicion.getId())
            .parametro(medicion.getParametro() != null ? medicion.getParametro().name() : null)
            .valor(medicion.getValor())
            .unidad(medicion.getUnidad())
            .build();
    }
}
