package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;

import java.util.List;

public class CajaMapper {

    public static CajaCriogenica toEntity(CajaRequestDTO dto) {
        CajaCriogenica c = new CajaCriogenica();
        c.setCodigoCaja(dto.getCodigoCaja());
        c.setFilas(dto.getFilas());
        c.setColumnas(dto.getColumnas());
        c.setTipoCaja(dto.getTipoCaja());
        c.setColor(dto.getColor());
        c.setObservaciones(dto.getObservaciones());
        return c;
    }

    public static CajaResponseDTO toResponseDTO(CajaCriogenica c) {
        String ubicacion = null;
        if (c.getPosicionPiso() != null) {
            var pos = c.getPosicionPiso();
            var piso = pos.getPiso();
            var ref = piso != null ? piso.getRefrigerador() : null;
            ubicacion = String.format("Refrigerador: %s | Piso: %s | Posición: F%s-C%s-A%s",
                ref != null ? ref.getCodigo() : "N/A",
                piso != null ? piso.getNumeroPiso() : "N/A",
                pos.getFila(),
                pos.getColumna(),
                pos.getAltura());
        }
        Long idPosicionPiso = c.getPosicionPiso() != null ? c.getPosicionPiso().getId() : null;

        return CajaResponseDTO.builder()
            .id(c.getId())
            .codigoCaja(c.getCodigoCaja())
            .filas(c.getFilas())
            .columnas(c.getColumnas())
            .tipoCaja(c.getTipoCaja())
            .color(c.getColor())
            .observaciones(c.getObservaciones())
            .activo(c.getActivo())
            .idPosicionPiso(idPosicionPiso)
            .ubicacionPiso(ubicacion)
            .build();
    }

    public static List<CajaResponseDTO> toResponseDTOList(List<CajaCriogenica> list) {
        return list.stream().map(CajaMapper::toResponseDTO).toList();
    }
}
