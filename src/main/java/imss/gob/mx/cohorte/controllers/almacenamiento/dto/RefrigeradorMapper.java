package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;

import java.util.List;
import java.util.Map;

public class RefrigeradorMapper {

    public static Refrigerador toEntity(RefrigeradorRequestDTO dto) {
        Refrigerador r = new Refrigerador();
        r.setCodigo(dto.getCodigo());
        r.setNombre(dto.getNombre());
        r.setMarca(dto.getMarca());
        r.setModelo(dto.getModelo());
        return r;
    }

    public static RefrigeradorResponseDTO toResponseDTO(Refrigerador r) {
        return toResponseDTO(r, List.of());
    }

    public static RefrigeradorResponseDTO toResponseDTO(Refrigerador r, List<PisoResumenDTO> pisosResumen) {
        return RefrigeradorResponseDTO.builder()
            .id(r.getId())
            .codigo(r.getCodigo())
            .nombre(r.getNombre())
            .marca(r.getMarca())
            .modelo(r.getModelo())
            .activo(r.getActivo())
            .totalPisos(r.getPisos() != null ? r.getPisos().size() : 0)
            .pisos(pisosResumen)
            .build();
    }

    public static List<RefrigeradorResponseDTO> toResponseDTOList(List<Refrigerador> list) {
        return list.stream().map(RefrigeradorMapper::toResponseDTO).toList();
    }

    public static List<RefrigeradorResponseDTO> toResponseDTOList(
            List<Refrigerador> list, Map<Long, List<PisoResumenDTO>> pisosMap) {
        return list.stream()
            .map(r -> toResponseDTO(r, pisosMap.getOrDefault(r.getId(), List.of())))
            .toList();
    }
}
