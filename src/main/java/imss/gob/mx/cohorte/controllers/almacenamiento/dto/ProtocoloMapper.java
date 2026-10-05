package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.Protocolo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.TuboProtocolo;

import java.util.ArrayList;
import java.util.List;

public class ProtocoloMapper {

    public static Protocolo toEntity(ProtocoloRequestDTO dto) {
        Protocolo p = new Protocolo();
        p.setNombre(dto.getNombre());
        p.setDescripcion(dto.getDescripcion());
        return p;
    }

    public static TuboProtocolo tuboToEntity(TuboProtocoloRequestDTO dto) {
        TuboProtocolo tubo = new TuboProtocolo();
        tubo.setNombre(dto.getNombre());
        tubo.setPrefijoCodigo(dto.getPrefijoCodigo());
        tubo.setAccion(dto.getAccion() != null ? dto.getAccion() : AccionTubo.GUARDAR);
        tubo.setNumeroAlicuotas(dto.getNumeroAlicuotas() != null ? dto.getNumeroAlicuotas() : 0);
        tubo.setVolumenAlicuota(dto.getVolumenAlicuota());
        tubo.setVolumenesAlicuota(dto.getVolumenesAlicuota() != null
                ? ajustarVolumenes(dto.getNumeroAlicuotas(), dto.getVolumenAlicuota(), dto.getVolumenesAlicuota())
                : null);
        tubo.setUnidadVolumen(dto.getUnidadVolumen());
        tubo.setDestinoSugerido(dto.getDestinoSugerido());
        tubo.setOrden(dto.getOrden() != null ? dto.getOrden() : 0);
        tubo.setActivo(dto.getActivo() != null ? dto.getActivo() : true);
        tubo.setAgruparEnLote(dto.getAgruparEnLote() != null ? dto.getAgruparEnLote() : Boolean.FALSE);
        tubo.setGeneracionAutomatica(dto.getGeneracionAutomatica() != null ? dto.getGeneracionAutomatica() : Boolean.TRUE);
        tubo.setPermiteAlicuotaParcial(dto.getPermiteAlicuotaParcial() != null ? dto.getPermiteAlicuotaParcial() : Boolean.TRUE);
        return tubo;
    }

    public static TuboProtocoloResponseDTO tuboToResponseDTO(TuboProtocolo tubo) {
        TipoMuestra res = tubo.getTipoResultante();
        return TuboProtocoloResponseDTO.builder()
                .id(tubo.getId())
                .nombre(tubo.getNombre())
                .prefijoCodigo(tubo.getPrefijoCodigo())
                .accion(tubo.getAccion())
                .orden(tubo.getOrden())
                .activo(tubo.getActivo())
                .idTipoResultante(res != null ? res.getId() : null)
                .nombreTipoResultante(res != null ? res.getNombre() : null)
                .numeroAlicuotas(tubo.getNumeroAlicuotas())
                .volumenAlicuota(tubo.getVolumenAlicuota())
                .volumenesAlicuota(copiaVolumenes(tubo.getVolumenesAlicuota()))
                .unidadVolumen(tubo.getUnidadVolumen())
                .destinoSugerido(tubo.getDestinoSugerido())
                .agruparEnLote(tubo.esAgruparEnLote())
                .generacionAutomatica(tubo.esGeneracionAutomatica())
                .permiteAlicuotaParcial(tubo.admiteAlicuotaParcial())
                .build();
    }

    public static ProtocoloResponseDTO toResponseDTO(Protocolo p) {
        List<TuboProtocoloResponseDTO> tubosDTO = p.getTubos() != null
                ? p.getTubos().stream().map(ProtocoloMapper::tuboToResponseDTO).toList()
                : List.of();
        TipoMuestra origen = p.getTipoOrigen();
        return ProtocoloResponseDTO.builder()
                .id(p.getId())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .activo(p.getActivo())
                .idTipoOrigen(origen != null ? origen.getId() : null)
                .nombreTipoOrigen(origen != null ? origen.getNombre() : null)
                .tubos(tubosDTO)
                .build();
    }

    public static List<ProtocoloResponseDTO> toResponseDTOList(List<Protocolo> list) {
        return list.stream().map(ProtocoloMapper::toResponseDTO).toList();
    }

    private static List<Double> ajustarVolumenes(Integer numeroAlicuotas, Double general, List<Double> provistos) {
        int n = numeroAlicuotas != null ? numeroAlicuotas : 0;
        if (n <= 0) {
            return new ArrayList<>();
        }
        List<Double> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Double v = provistos != null && i < provistos.size() ? provistos.get(i) : null;
            out.add(v != null && !v.isNaN() && !v.isInfinite() && v > 0 ? v : general);
        }
        return out;
    }

    private static List<Double> copiaVolumenes(List<Double> volumenes) {
        return volumenes == null || volumenes.isEmpty() ? List.of() : new ArrayList<>(volumenes);
    }
}
