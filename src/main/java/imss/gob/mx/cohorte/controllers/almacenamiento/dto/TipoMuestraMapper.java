package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;

import java.util.List;

public class TipoMuestraMapper {

    public static TipoMuestra toEntity(TipoMuestraRequestDTO dto) {
        TipoMuestra tipo = new TipoMuestra();
        tipo.setNombre(dto.getNombre());
        tipo.setDescripcion(dto.getDescripcion());
        tipo.setTemperaturaAlmacenamiento(dto.getTemperaturaAlmacenamiento());
        return tipo;
    }

    public static TuboMuestra tuboToEntity(TuboMuestraRequestDTO dto) {
        TuboMuestra tubo = new TuboMuestra();
        tubo.setNombre(dto.getNombre());
        tubo.setPrefijoCodigo(dto.getPrefijoCodigo());
        tubo.setNumeroAlicuotas(dto.getNumeroAlicuotas() != null ? dto.getNumeroAlicuotas() : 0);
        tubo.setVolumenAlicuota(dto.getVolumenAlicuota());
        tubo.setUnidadVolumen(dto.getUnidadVolumen());
        tubo.setDestinoSugerido(dto.getDestinoSugerido());
        tubo.setOrden(dto.getOrden() != null ? dto.getOrden() : 0);
        tubo.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        tubo.setGeneracionAutomatica(dto.getGeneracionAutomatica() != null ? dto.getGeneracionAutomatica() : Boolean.TRUE);
        tubo.setPermiteAlicuotaParcial(dto.getPermiteAlicuotaParcial() != null ? dto.getPermiteAlicuotaParcial() : Boolean.TRUE);
        return tubo;
    }

    public static TuboMuestraResponseDTO tuboToResponseDTO(TuboMuestra tubo) {
        return TuboMuestraResponseDTO.builder()
                .id(tubo.getId())
                .nombre(tubo.getNombre())
                .prefijoCodigo(tubo.getPrefijoCodigo())
                .numeroAlicuotas(tubo.getNumeroAlicuotas())
                .volumenAlicuota(tubo.getVolumenAlicuota())
                .unidadVolumen(tubo.getUnidadVolumen())
                .destinoSugerido(tubo.getDestinoSugerido())
                .orden(tubo.getOrden())
                .activo(tubo.getActivo())
                .generacionAutomatica(tubo.esGeneracionAutomatica())
                .permiteAlicuotaParcial(tubo.admiteAlicuotaParcial())
                .build();
    }

    /** Resumen del tubo con la receta completa: el planificador de lotes la necesita. */
    public static TuboMuestraResumenDTO tuboToResumenDTO(TuboMuestra tubo) {
        if (tubo == null) return null;
        return TuboMuestraResumenDTO.builder()
                .id(tubo.getId())
                .nombre(tubo.getNombre())
                .prefijoCodigo(tubo.getPrefijoCodigo())
                .numeroAlicuotas(tubo.getNumeroAlicuotas())
                .volumenAlicuota(tubo.getVolumenAlicuota())
                .unidadVolumen(tubo.getUnidadVolumen())
                .generacionAutomatica(tubo.esGeneracionAutomatica())
                .permiteAlicuotaParcial(tubo.admiteAlicuotaParcial())
                .build();
    }

    public static TipoMuestraResponseDTO toResponseDTO(TipoMuestra tipo) {
        List<TuboMuestraResponseDTO> tubosDTO = tipo.getTubos() != null
                ? tipo.getTubos().stream().map(TipoMuestraMapper::tuboToResponseDTO).toList()
                : List.of();

        return TipoMuestraResponseDTO.builder()
                .id(tipo.getId())
                .nombre(tipo.getNombre())
                .descripcion(tipo.getDescripcion())
                .temperaturaAlmacenamiento(tipo.getTemperaturaAlmacenamiento())
                .activo(tipo.getActivo())
                .tubos(tubosDTO)
                .build();
    }

    public static List<TipoMuestraResponseDTO> toResponseDTOList(List<TipoMuestra> list) {
        return list.stream().map(TipoMuestraMapper::toResponseDTO).toList();
    }
}
