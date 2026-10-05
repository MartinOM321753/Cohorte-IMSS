package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ProcesarProtocoloRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ProcesarResultadoResponseDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.TuboDecisionRequestDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.lote.Lote;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.TuboProtocolo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.almacenamiento.procesamiento.DecisionTubo;
import imss.gob.mx.cohorte.services.almacenamiento.procesamiento.ProcesamientoService;
import imss.gob.mx.cohorte.services.almacenamiento.procesamiento.ResultadoProcesamiento;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class ProcesamientoApplicationService {

    private final ProcesamientoService procesamientoService;

    @Transactional
    public ProcesarResultadoResponseDTO procesar(ProcesarProtocoloRequestDTO dto) {
        List<DecisionTubo> decisiones = new ArrayList<>();
        if (dto.getTubos() != null) {
            for (TuboDecisionRequestDTO t : dto.getTubos()) {
                decisiones.add(new DecisionTubo(
                        t.getIdTuboProtocolo(),
                        t.getIncluir() == null || t.getIncluir(),
                        t.getAccion(),
                        t.getVolumen(),
                        t.getUnidad(),
                        t.getPlanVolumenes()));
            }
        }

        ResultadoProcesamiento r = procesamientoService.procesar(
                dto.getPacienteUUID(), dto.getIdProtocolo(),
                dto.getFechaRecoleccion(), dto.getObservaciones(), decisiones);

        return mapear(r);
    }

    @Transactional
    public ProcesarResultadoResponseDTO alicuotarTubo(Long idPadre, java.util.List<Double> planVolumenes) {
        return mapear(procesamientoService.alicuotarTubo(idPadre, planVolumenes));
    }

    private ProcesarResultadoResponseDTO mapear(ResultadoProcesamiento r) {
        var paciente = !r.padres().isEmpty() ? r.padres().get(0).getPaciente() : null;
        String folio = paciente != null ? paciente.getFolio() : null;
        Long idPaciente = paciente != null ? paciente.getId() : null;

        List<ProcesarResultadoResponseDTO.PadreBreveDTO> padres = new ArrayList<>();
        for (Muestra p : r.padres()) {
            TuboProtocolo tubo = p.getTuboProtocolo();
            AccionTubo accion = tubo != null ? tubo.getAccion() : null;
            padres.add(ProcesarResultadoResponseDTO.PadreBreveDTO.builder()
                    .id(p.getId())
                    .etiqueta(p.getEtiqueta())
                    .accion(accion)
                    .nombreTubo(tubo != null ? tubo.getNombre() : null)
                    .valor(p.getValor())
                    .unidad(p.getUnidad())
                    .build());
        }

        // Cuenta de alícuotas por lote preservando el orden de aparición.
        Map<Long, Integer> conteo = new LinkedHashMap<>();
        for (Muestra a : r.alicuotas()) {
            if (a.getLote() != null) {
                conteo.merge(a.getLote().getId(), 1, Integer::sum);
            }
        }
        List<ProcesarResultadoResponseDTO.LoteResumenDTO> lotes = new ArrayList<>();
        for (Lote l : r.lotes()) {
            lotes.add(ProcesarResultadoResponseDTO.LoteResumenDTO.builder()
                    .id(l.getId())
                    .numeroLote(l.getNumeroLote())
                    .idTipoResultante(l.getTipoResultante() != null ? l.getTipoResultante().getId() : null)
                    .nombreTipoResultante(l.getTipoResultante() != null ? l.getTipoResultante().getNombre() : null)
                    .numeroAlicuotas(conteo.getOrDefault(l.getId(), 0))
                    .build());
        }

        return ProcesarResultadoResponseDTO.builder()
                .idPaciente(idPaciente)
                .folio(folio)
                .numeroPadres(r.padres().size())
                .numeroAlicuotas(r.alicuotas().size())
                .padres(padres)
                .lotes(lotes)
                .build();
    }
}
