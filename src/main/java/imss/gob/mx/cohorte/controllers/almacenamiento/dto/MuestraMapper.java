package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import imss.gob.mx.cohorte.controllers.pacientes.dto.PacienteMapper;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;

import java.util.List;

public class MuestraMapper {

    public static Muestra toEntity(MuestraRequestDTO dto) {
        Muestra m = new Muestra();
        m.setEtiqueta(dto.getEtiqueta());
        m.setValor(dto.getValor());
        m.setUnidad(dto.getUnidad());
        m.setFechaRecoleccion(dto.getFechaRecoleccion());
        m.setObservaciones(dto.getObservaciones());
        return m;
    }

    public static MuestraResponseDTO toResponseDTO(Muestra m) {
        PacienteResumenDTO pacienteDTO = null;
        if (m.getPaciente() != null) {
            pacienteDTO = PacienteMapper.toResumenDTO(m.getPaciente());
        }

        UsuarioResumenDTO usuarioDTO = null;
        if (m.getUsuarioRecolecta() != null) {
            var u = m.getUsuarioRecolecta();
            String nombreCompleto = "";
            if (u.getPersona() != null) {
                var p = u.getPersona();
                nombreCompleto = (p.getNombre() != null ? p.getNombre() : "") + " "
                    + (p.getApellidoPaterno() != null ? p.getApellidoPaterno() : "") + " "
                    + (p.getApellidoMaterno() != null ? p.getApellidoMaterno() : "");
            }
            usuarioDTO = UsuarioResumenDTO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .UUID(u.getUUID())
                .nombreCompleto(nombreCompleto.trim())
                .build();
        }

        UbicacionMuestraDTO ubicacionDTO = null;
        if (m.getPosicionCaja() != null) {
            var posCaja = m.getPosicionCaja();
            var caja = posCaja.getCaja();
            String codigoCaja = caja != null ? caja.getCodigoCaja() : null;
            String numeroPiso = null;
            String codigoRefrigerador = null;
            if (caja != null && caja.getPosicionPiso() != null) {
                var posPiso = caja.getPosicionPiso();
                numeroPiso = posPiso.getPiso() != null ? posPiso.getPiso().getNumeroPiso() : null;
                codigoRefrigerador = posPiso.getPiso() != null && posPiso.getPiso().getRefrigerador() != null
                    ? posPiso.getPiso().getRefrigerador().getCodigo() : null;
            }
            ubicacionDTO = UbicacionMuestraDTO.builder()
                .idPosicionCaja(posCaja.getId())
                .fila(posCaja.getFila() != null ? posCaja.getFila().toString() : null)
                .columna(posCaja.getColumna() != null ? posCaja.getColumna().toString() : null)
                .codigoCaja(codigoCaja)
                .numeroPiso(numeroPiso)
                .codigoRefrigerador(codigoRefrigerador)
                .build();
        }

        // Stream C — TipoMuestra / TuboMuestra resumen
        TipoMuestraResumenDTO tipoMuestraDTO = null;
        if (m.getTipoMuestra() != null) {
            var tm = m.getTipoMuestra();
            tipoMuestraDTO = TipoMuestraResumenDTO.builder()
                .id(tm.getId())
                .nombre(tm.getNombre())
                .temperaturaAlmacenamiento(tm.getTemperaturaAlmacenamiento())
                .build();
        }

        TuboMuestraResumenDTO tuboMuestraDTO = null;
        if (m.getTuboMuestra() != null) {
            var tb = m.getTuboMuestra();
            tuboMuestraDTO = TuboMuestraResumenDTO.builder()
                .id(tb.getId())
                .nombre(tb.getNombre())
                .prefijoCodigo(tb.getPrefijoCodigo())
                .numeroAlicuotas(tb.getNumeroAlicuotas())
                .build();
        }

        return MuestraResponseDTO.builder()
            .id(m.getId())
            .etiqueta(m.getEtiqueta())
            .valor(m.getValor())
            .unidad(m.getUnidad())
            .fechaRecoleccion(m.getFechaRecoleccion())
            .observaciones(m.getObservaciones())
            .paciente(pacienteDTO)
            .usuarioRecolecta(usuarioDTO)
            .ubicacion(ubicacionDTO)
            .tipoMuestra(tipoMuestraDTO)
            .tuboMuestra(tuboMuestraDTO)
            .idMuestraPadre(m.getMuestraPadre() != null ? m.getMuestraPadre().getId() : null)
            .numeroAlicuota(m.getNumeroAlicuota())
            .totalAlicuotas(m.getTotalAlicuotas())
            .estadoMuestra(m.getEstadoMuestra())
            .idInstitucion(m.getInstitucion() != null ? m.getInstitucion().getId() : null)
            .nombreInstitucion(m.getInstitucion() != null ? m.getInstitucion().getNombre() : null)
            .idInstitucionActual(m.getInstitucionActual() != null ? m.getInstitucionActual().getId() : null)
            .nombreInstitucionActual(m.getInstitucionActual() != null ? m.getInstitucionActual().getNombre() : null)
            .build();
    }

    public static List<MuestraResponseDTO> toResponseDTOList(List<Muestra> list) {
        return list.stream().map(MuestraMapper::toResponseDTO).toList();
    }
}
