package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;

import java.util.List;

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
        return RefrigeradorResponseDTO.builder()
            .id(r.getId())
            .codigo(r.getCodigo())
            .nombre(r.getNombre())
            .marca(r.getMarca())
            .modelo(r.getModelo())
            .activo(r.getActivo())
            .build();
    }

    public static List<RefrigeradorResponseDTO> toResponseDTOList(List<Refrigerador> list) {
        return list.stream().map(RefrigeradorMapper::toResponseDTO).toList();
    }
}
