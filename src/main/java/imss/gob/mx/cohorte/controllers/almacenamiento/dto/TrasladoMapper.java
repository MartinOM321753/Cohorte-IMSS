package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.persona.Persona;

import java.util.List;

public class TrasladoMapper {

    private TrasladoMapper() {}

    public static TrasladoResponseDTO toResponseDTO(TrasladoMuestra t) {
        Persona persona = t.getAutorizadoPor().getPersona();
        String nombreCompleto = persona.getNombre() + " " + persona.getApellidoPaterno()
                + (persona.getApellidoMaterno() != null ? " " + persona.getApellidoMaterno() : "");

        return TrasladoResponseDTO.builder()
                .id(t.getId())
                .muestra(TrasladoResponseDTO.MuestraResumenDTO.builder()
                        .id(t.getMuestra().getId())
                        .etiqueta(t.getMuestra().getEtiqueta())
                        .unidad(t.getMuestra().getUnidad())
                        .build())
                .almacen(AlmacenMapper.toResponseDTO(t.getAlmacen()))
                .autorizadoPor(TrasladoResponseDTO.UsuarioAutorizaDTO.builder()
                        .id(t.getAutorizadoPor().getId())
                        .uuid(t.getAutorizadoPor().getUUID())
                        .username(t.getAutorizadoPor().getUsername())
                        .nombreCompleto(nombreCompleto.trim())
                        .build())
                .estado(t.getEstado().name())
                .fechaTraslado(t.getFechaTraslado())
                .fechaRetorno(t.getFechaRetorno())
                .motivo(t.getMotivo())
                .observaciones(t.getObservaciones())
                .build();
    }

    public static List<TrasladoResponseDTO> toResponseDTOList(List<TrasladoMuestra> list) {
        return list.stream().map(TrasladoMapper::toResponseDTO).toList();
    }
}
