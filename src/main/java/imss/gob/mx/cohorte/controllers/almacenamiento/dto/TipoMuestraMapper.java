package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;

import java.util.ArrayList;
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
        // null = «no lo menciono»: se deja en null para que la actualización
        // conserve la configuración por slot que ya tuviera el tubo. Presente,
        // se ajusta a tantos volúmenes como alícuotas, sembrando con el general.
        tubo.setVolumenesAlicuota(dto.getVolumenesAlicuota() != null
                ? ajustarVolumenes(dto.getNumeroAlicuotas(), dto.getVolumenAlicuota(), dto.getVolumenesAlicuota())
                : null);
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
                .volumenesAlicuota(copiaVolumenes(tubo.getVolumenesAlicuota()))
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
                .volumenesAlicuota(copiaVolumenes(tubo.getVolumenesAlicuota()))
                .unidadVolumen(tubo.getUnidadVolumen())
                .generacionAutomatica(tubo.esGeneracionAutomatica())
                .permiteAlicuotaParcial(tubo.admiteAlicuotaParcial())
                .build();
    }

    /**
     * Ajusta la lista por slot a tantos volúmenes como alícuotas: recorta lo que
     * sobra, y rellena lo que falta —o cualquier valor inválido— con el volumen
     * general. En un tubo directo (0 alícuotas) devuelve vacío.
     */
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
