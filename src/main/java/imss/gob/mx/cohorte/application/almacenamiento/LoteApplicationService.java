package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.controllers.almacenamiento.dto.CartaFolioResponseDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.CartaFolioResponseDTO.AlicuotaLoteDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.CartaFolioResponseDTO.LoteDetalleDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.CartaFolioResponseDTO.TuboPrimarioCartaDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.MuestraMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.MuestraResponseDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ProcesamientoCardResponseDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ProcesamientoCardResponseDTO.LoteCardDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.lote.Lote;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.Protocolo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.TuboProtocolo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.almacenamiento.lote.LoteService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class LoteApplicationService {

    private final LoteService loteService;

    /**
     * Lista las cards de la vista de Lotes: una por procesamiento ejecutado
     * (participante × protocolo), con sus tubos primarios y sus lotes de alícuotas.
     */
    @Transactional(readOnly = true)
    public List<ProcesamientoCardResponseDTO> listarCards() {
        List<Muestra> all = loteService.getMuestrasProtocolo();
        Map<String, List<Muestra>> grupos = new LinkedHashMap<>();
        for (Muestra m : all) {
            TuboProtocolo t = m.getTuboProtocolo();
            if (t == null || t.getProtocolo() == null || m.getPaciente() == null) continue;
            String key = m.getPaciente().getId() + "/" + t.getProtocolo().getId();
            grupos.computeIfAbsent(key, k -> new ArrayList<>()).add(m);
        }
        List<ProcesamientoCardResponseDTO> cards = new ArrayList<>();
        for (List<Muestra> grupo : grupos.values()) {
            cards.add(construirCard(grupo));
        }
        cards.sort(Comparator
                .comparing((ProcesamientoCardResponseDTO c) -> c.getFolio() == null ? "" : c.getFolio())
                .thenComparing(c -> c.getNombreProtocolo() == null ? "" : c.getNombreProtocolo()));
        return cards;
    }

    private ProcesamientoCardResponseDTO construirCard(List<Muestra> grupo) {
        Muestra ref = grupo.get(0);
        Paciente pac = ref.getPaciente();
        Protocolo proto = ref.getTuboProtocolo().getProtocolo();

        List<Muestra> alic = new ArrayList<>();
        List<Muestra> padres = new ArrayList<>();
        for (Muestra m : grupo) {
            if (m.getMuestraPadre() == null) padres.add(m); else alic.add(m);
        }
        padres.sort(Comparator.comparing(p -> {
            Integer o = p.getTuboProtocolo() != null ? p.getTuboProtocolo().getOrden() : null;
            return o == null ? Integer.MAX_VALUE : o;
        }));

        // Pendientes de ubicar por padre, para el botón «ubicar lote».
        Map<Long, Integer> pendientesPorPadre = new HashMap<>();
        for (Muestra a : alic) {
            if (a.getMuestraPadre() != null && a.getFechaMaterializacion() == null) {
                pendientesPorPadre.merge(a.getMuestraPadre().getId(), 1, Integer::sum);
            }
        }
        List<MuestraResponseDTO> tubos = new ArrayList<>(padres.size());
        for (Muestra p : padres) {
            tubos.add(MuestraMapper.toResponseDTO(p, pendientesPorPadre.getOrDefault(p.getId(), 0)));
        }

        // Alícuotas agrupadas por lote, numeradas por numeroEnLote.
        Map<Long, List<Muestra>> porLote = new LinkedHashMap<>();
        for (Muestra a : alic) {
            if (a.getLote() != null) {
                porLote.computeIfAbsent(a.getLote().getId(), k -> new ArrayList<>()).add(a);
            }
        }
        List<LoteCardDTO> lotes = new ArrayList<>();
        for (List<Muestra> list : porLote.values()) {
            list.sort(Comparator.comparing(m -> m.getNumeroEnLote() == null ? Integer.MAX_VALUE : m.getNumeroEnLote()));
            Lote lote = list.get(0).getLote();
            List<MuestraResponseDTO> aliDTO = new ArrayList<>(list.size());
            for (Muestra a : list) aliDTO.add(MuestraMapper.toResponseDTO(a));
            lotes.add(LoteCardDTO.builder()
                    .idLote(lote.getId())
                    .numeroLote(lote.getNumeroLote())
                    .tipoResultante(lote.getTipoResultante() != null ? lote.getTipoResultante().getNombre() : null)
                    .alicuotas(aliDTO)
                    .build());
        }
        lotes.sort(Comparator.comparing(l -> l.getNumeroLote() == null ? 0 : l.getNumeroLote()));

        return ProcesamientoCardResponseDTO.builder()
                .uuid(pac != null ? pac.getUuid() : null)
                .folio(pac != null ? pac.getFolio() : null)
                .nombrePaciente(pac != null ? pac.getFolio() : null)
                .idProtocolo(proto != null ? proto.getId() : null)
                .nombreProtocolo(proto != null ? proto.getNombre() : null)
                .tipoOrigen(proto != null && proto.getTipoOrigen() != null ? proto.getTipoOrigen().getNombre() : null)
                .tubosPrimarios(tubos)
                .lotes(lotes)
                .build();
    }

    @Transactional(readOnly = true)
    public CartaFolioResponseDTO carta(String uuid) {
        List<Muestra> muestras = loteService.getMuestrasDeFolio(uuid);
        Paciente pac = muestras.isEmpty() ? null : muestras.get(0).getPaciente();

        List<TuboPrimarioCartaDTO> tubos = new ArrayList<>();
        Map<String, List<Muestra>> grupos = new LinkedHashMap<>();

        for (Muestra m : muestras) {
            if (m.getMuestraPadre() == null) {
                tubos.add(mapearPadre(m));
            } else {
                String clave = m.getLote() != null
                        ? "L:" + m.getLote().getId()
                        : "P:" + m.getMuestraPadre().getId();
                grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(m);
            }
        }

        List<LoteDetalleDTO> lotes = new ArrayList<>();
        for (List<Muestra> grupo : grupos.values()) {
            lotes.add(mapearLote(grupo));
        }

        return CartaFolioResponseDTO.builder()
                .folio(pac != null ? pac.getFolio() : null)
                .uuid(pac != null ? pac.getUuid() : uuid)
                .nombrePaciente(pac != null ? pac.getFolio() : null)
                .tubosPrimarios(tubos)
                .lotes(lotes)
                .build();
    }

    private TuboPrimarioCartaDTO mapearPadre(Muestra m) {
        TuboProtocolo tubo = m.getTuboProtocolo();
        return TuboPrimarioCartaDTO.builder()
                .id(m.getId())
                .etiqueta(m.getEtiqueta())
                .accion(tubo != null ? tubo.getAccion() : null)
                .nombreTubo(tubo != null ? tubo.getNombre() : null)
                .tipoMuestra(m.getTipoMuestra() != null ? m.getTipoMuestra().getNombre() : null)
                .valor(m.getValor())
                .unidad(m.getUnidad())
                .estado(m.getEstadoMuestra())
                .tienePosicion(m.getPosicionCaja() != null)
                .posicionLabel(posicionLabel(m.getPosicionCaja()))
                .agotada(m.isAgotada())
                .build();
    }

    private LoteDetalleDTO mapearLote(List<Muestra> grupo) {
        Muestra primera = grupo.get(0);
        Lote lote = primera.getLote();

        grupo.sort(Comparator
                .comparing((Muestra a) -> a.getNumeroEnLote() == null ? Integer.MAX_VALUE : a.getNumeroEnLote())
                .thenComparing(a -> a.getNumeroAlicuota() == null ? Integer.MAX_VALUE : a.getNumeroAlicuota()));

        List<AlicuotaLoteDTO> alicuotas = new ArrayList<>(grupo.size());
        int secuencia = 0;
        for (Muestra a : grupo) {
            secuencia++;
            Integer numeroEnLote = a.getNumeroEnLote() != null ? a.getNumeroEnLote() : secuencia;
            alicuotas.add(AlicuotaLoteDTO.builder()
                    .id(a.getId())
                    .etiqueta(a.getEtiqueta())
                    .numeroEnLote(numeroEnLote)
                    .numeroAlicuota(a.getNumeroAlicuota())
                    .totalAlicuotas(a.getTotalAlicuotas())
                    .valor(a.getValor())
                    .unidad(a.getUnidad())
                    .estado(a.getEstadoMuestra())
                    .tienePosicion(a.getPosicionCaja() != null)
                    .posicionLabel(posicionLabel(a.getPosicionCaja()))
                    .materializada(a.isMaterializada())
                    .build());
        }

        LoteDetalleDTO.LoteDetalleDTOBuilder b = LoteDetalleDTO.builder().alicuotas(alicuotas);
        if (lote != null) {
            b.id(lote.getId())
             .numeroLote(lote.getNumeroLote())
             .tipoResultante(lote.getTipoResultante() != null ? lote.getTipoResultante().getNombre() : null)
             .protocolo(lote.getProtocolo() != null ? lote.getProtocolo().getNombre() : null)
             .heredado(false);
        } else {
            Muestra padre = primera.getMuestraPadre();
            b.id(padre != null ? padre.getId() : null)
             .numeroLote(padre != null ? padre.getNumeroLote() : null)
             .tipoResultante(primera.getTipoMuestra() != null ? primera.getTipoMuestra().getNombre() : null)
             .protocolo(null)
             .heredado(true);
        }
        return b.build();
    }

    private String posicionLabel(PosicionCaja pos) {
        if (pos == null) {
            return null;
        }
        String codigo = pos.getCaja() != null ? pos.getCaja().getCodigoCaja() : null;
        StringBuilder sb = new StringBuilder();
        if (codigo != null) {
            sb.append(codigo).append(' ');
        }
        sb.append('R').append(pos.getFila()).append('C').append(pos.getColumna());
        return sb.toString().trim();
    }
}
