package imss.gob.mx.cohorte.services.almacenamiento.procesamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.lote.Lote;
import imss.gob.mx.cohorte.modules.almacenamiento.lote.LoteRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.PacienteEstadoValidator;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.Protocolo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.TuboProtocolo;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.EtiquetaMuestra;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MaterializacionAlicuotaService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PlanificadorAlicuotas;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.RecetaTubo;
import imss.gob.mx.cohorte.services.almacenamiento.protocolo.ProtocoloService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Instancia un protocolo sobre un participante: genera las muestras padre (los
 * tubos primarios), engancha los tubos de estudio, y para los que alicuotan crea
 * las alícuotas agrupadas en lotes.
 *
 * <p>No reimplementa la contabilidad de volumen ni el cálculo de alícuotas:
 * reutiliza {@link PlanificadorAlicuotas}, {@link MaterializacionAlicuotaService}
 * y {@link MuestraService}. Lo propio de aquí es orquestar el protocolo y agrupar
 * las alícuotas del mismo tipo resultante en un {@link Lote} con numeración
 * continua 1…N.</p>
 */
@Service
@RequiredArgsConstructor
public class ProcesamientoService {

    private final ProtocoloService protocoloService;
    private final PacienteRepository pacienteRepository;
    private final MuestraService muestraService;
    private final MuestraRepository muestraRepository;
    private final MaterializacionAlicuotaService materializacionService;
    private final LoteRepository loteRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional
    public ResultadoProcesamiento procesar(String pacienteUUID, Long idProtocolo, LocalDateTime fechaRecoleccion,
                                           String observaciones, List<DecisionTubo> decisiones) {
        Institucion inst = institucionContextService.getInstitucionActual();
        BeanUser usuario = institucionContextService.getUsuarioActual();
        Protocolo protocolo = protocoloService.getById(idProtocolo); // valida institución

        Paciente paciente = pacienteRepository.findByUuid(pacienteUUID)
                .orElseThrow(() -> new ObjNotFoundException("El participante no existe"));
        if (!Boolean.TRUE.equals(paciente.getActivo())) {
            throw new ValidationException(
                    "No se puede procesar: el participante está inactivo (consentimiento retirado o baja). "
                    + "Reactívelo para procesar sus muestras.");
        }
        // Una sola ronda (F4) por (participante, protocolo): no se re-procesa. Para
        // alicuotar tubos que quedaron pendientes está alicuotarTubo (2ª pasada).
        if (muestraRepository.existsByPaciente_IdAndInstitucion_IdAndTuboProtocolo_Protocolo_IdAndMuestraPadreIsNull(
                paciente.getId(), inst.getId(), protocolo.getId())) {
            throw new ObjConflictException("El participante " + paciente.getFolio()
                    + " ya fue procesado con el protocolo «" + protocolo.getNombre()
                    + "». Para alicuotar más tubos, use la opción de alicuotar en su card.");
        }

        Map<Long, DecisionTubo> porTubo = new HashMap<>();
        if (decisiones != null) {
            for (DecisionTubo d : decisiones) {
                if (d != null && d.idTuboProtocolo() != null) {
                    porTubo.put(d.idTuboProtocolo(), d);
                }
            }
        }

        List<Muestra> padres = new ArrayList<>();
        List<Muestra> alicuotas = new ArrayList<>();
        // Lotes de esta tanda, uno por clave de agrupación, preservando el orden.
        Map<String, Lote> lotesPorClave = new LinkedHashMap<>();
        Map<String, Integer> contadorEnLote = new HashMap<>();
        // Tamaño CONFIGURADO de cada lote (suma de alícuotas de los tubos que lo
        // alimentan, estén o no en esta pasada). Es el «total» de la etiqueta
        // L{lote}/{pos}-{total}, y es fijo: 2 tubos × 6 agrupados → total 12.
        Map<String, Integer> totalConfiguradoPorClave = totalesPorClave(protocolo);
        // El consecutivo de lote es por (participante, protocolo): L1, L2…
        int siguienteNumeroLote = loteRepository.findMaxNumeroLoteByPacienteAndProtocolo(
                paciente.getId(), inst.getId(), protocolo.getId());

        boolean procesoAlguno = false;

        for (TuboProtocolo tubo : protocolo.getTubos()) {
            if (!Boolean.TRUE.equals(tubo.getActivo())) {
                continue;
            }
            DecisionTubo dec = porTubo.get(tubo.getId());
            // Sin decisión explícita se procesa con los valores del protocolo; una
            // decisión con incluir=false lo deja fuera de esta tanda.
            if (dec != null && !dec.incluir()) {
                continue;
            }
            procesoAlguno = true;
            AccionTubo accion = dec != null && dec.accion() != null ? dec.accion() : tubo.getAccion();

            Muestra padre = crearPadre(tubo, accion, dec, protocolo, paciente, usuario, inst,
                    fechaRecoleccion, observaciones);
            padres.add(padre);

            if (accion == AccionTubo.ALICUOTAR) {
                Lote lote = resolverLote(tubo, protocolo, paciente, usuario, inst,
                        lotesPorClave, contadorEnLote, siguienteNumeroLote);
                // Un lote nuevo tomó siguienteNumeroLote+1; uno reutilizado devuelve
                // el suyo (≤ actual). El máximo deja el consecutivo listo para el próximo.
                siguienteNumeroLote = Math.max(siguienteNumeroLote, lote.getNumeroLote());
                int totalLote = totalConfiguradoPorClave.getOrDefault(claveLote(tubo),
                        tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0);
                alicuotas.addAll(generarAlicuotas(padre, tubo, dec, lote, contadorEnLote,
                        claveLote(tubo), totalLote, usuario, inst, fechaRecoleccion));
            }
            // GUARDAR y ESTUDIO: la padre ya quedó creada. Los estudios se capturan
            // después con el flujo de Estudios de muestra sobre esa padre.
        }

        if (!procesoAlguno) {
            throw new ValidationException("No se seleccionó ningún tubo para procesar.");
        }

        return new ResultadoProcesamiento(padres, alicuotas, new ArrayList<>(lotesPorClave.values()));
    }

    /**
     * 2ª pasada: alicuota un tubo primario que ya existe (p. ej. uno que se guardó
     * y ahora se decide alicuotar). Crea un lote NUEVO (L+1 dentro del participante,
     * protocolo) con las alícuotas de ese tubo.
     */
    @Transactional
    public ResultadoProcesamiento alicuotarTubo(Long idPadre, List<Double> planVolumenes) {
        Institucion inst = institucionContextService.getInstitucionActual();
        BeanUser usuario = institucionContextService.getUsuarioActual();
        Muestra padre = muestraService.getByIdComoTenedor(idPadre);
        PacienteEstadoValidator.requirePacienteActivo(padre, "alicuotar");

        TuboProtocolo tubo = padre.getTuboProtocolo();
        if (tubo == null || tubo.getProtocolo() == null) {
            throw new ValidationException("Este tubo no pertenece a un protocolo; no se puede alicuotar por aquí.");
        }
        if (tubo.getAccion() != AccionTubo.ALICUOTAR) {
            throw new ValidationException("El tubo «" + tubo.getNombre()
                    + "» no está configurado para alicuotar. Edite el protocolo (acción Alicuotar) y reintente.");
        }
        if (PlanificadorAlicuotas.agotado(padre.getValorDisponible())) {
            throw new ValidationException("El tubo ya no tiene volumen disponible para alicuotar.");
        }

        Protocolo proto = tubo.getProtocolo();
        int nextLote = loteRepository.findMaxNumeroLoteByPacienteAndProtocolo(
                padre.getPaciente().getId(), inst.getId(), proto.getId()) + 1;
        Lote lote = new Lote();
        lote.setInstitucion(inst);
        lote.setPaciente(padre.getPaciente());
        lote.setTipoResultante(tubo.getTipoResultante());
        lote.setProtocolo(proto);
        lote.setNumeroLote(nextLote);
        lote.setUsuarioProcesa(usuario);
        lote.setFechaCreacion(Timestamp.valueOf(LocalDateTime.now()));
        lote = loteRepository.save(lote);

        String clave = claveLote(tubo);
        Map<String, Integer> contador = new HashMap<>();
        contador.put(clave, 0);
        int totalLote = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        DecisionTubo dec = new DecisionTubo(tubo.getId(), true, AccionTubo.ALICUOTAR,
                padre.getValor(), null, planVolumenes);
        List<Muestra> alic = generarAlicuotas(padre, tubo, dec, lote, contador, clave, totalLote,
                usuario, inst, padre.getFechaRecoleccion());
        if (alic.isEmpty()) {
            throw new ValidationException("No alcanzó el volumen disponible para ninguna alícuota completa.");
        }
        return new ResultadoProcesamiento(List.of(padre), alic, List.of(lote));
    }

    /** Suma de alícuotas configuradas por clave de lote (todos los tubos ALICUOTAR del protocolo). */
    private Map<String, Integer> totalesPorClave(Protocolo protocolo) {
        Map<String, Integer> totales = new HashMap<>();
        for (TuboProtocolo t : protocolo.getTubos()) {
            if (Boolean.TRUE.equals(t.getActivo()) && t.getAccion() == AccionTubo.ALICUOTAR) {
                int n = t.getNumeroAlicuotas() != null ? t.getNumeroAlicuotas() : 0;
                totales.merge(claveLote(t), n, Integer::sum);
            }
        }
        return totales;
    }

    // ── Padre ────────────────────────────────────────────────────────────────

    private Muestra crearPadre(TuboProtocolo tubo, AccionTubo accion, DecisionTubo dec, Protocolo protocolo,
                               Paciente paciente, BeanUser usuario, Institucion inst,
                               LocalDateTime fechaRecoleccion, String observaciones) {
        String prefijoRaw = tubo.getPrefijoCodigo();
        String folio = paciente.getFolio();
        // El tubo primario se etiqueta por su ORDEN en el protocolo (T1…TN), no por
        // número de lote: su identidad es el tubo, y de él pueden salir varios lotes.
        int orden = tubo.getOrden() != null ? tubo.getOrden() : 1;

        Muestra padre = new Muestra();
        padre.setEtiqueta(EtiquetaMuestra.primario(prefijoRaw, folio, inst.getId(), orden));
        padre.setNumeroLote(orden); // en una padre, numeroLote guarda el orden T (vestigial)
        padre.setTipoMuestra(protocolo.getTipoOrigen());
        padre.setTuboProtocolo(tubo);
        padre.setPaciente(paciente);
        padre.setUsuarioRecolecta(usuario);
        padre.setInstitucion(inst);
        padre.setInstitucionActual(inst);
        padre.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
        padre.setObservaciones(observaciones);
        padre.setFechaRecoleccion(fechaRecoleccion);
        padre.setValorComprometido(0.0);
        // Unidad: en ALICUOTAR la manda el tubo (la misma que descontará); en los
        // demás, la que venga de la decisión.
        padre.setUnidad(accion == AccionTubo.ALICUOTAR ? tubo.getUnidadVolumen()
                : (dec != null ? dec.unidad() : null));
        padre.setValor(dec != null ? dec.volumen() : null);

        return muestraService.create(padre);
    }

    // ── Lote ─────────────────────────────────────────────────────────────────

    /** Clave de agrupación: los que agrupan comparten lote por tipo resultante. */
    private String claveLote(TuboProtocolo tubo) {
        Long tipoRes = tubo.getTipoResultante() != null ? tubo.getTipoResultante().getId() : null;
        return tubo.esAgruparEnLote() ? "G:" + tipoRes : "S:" + tubo.getId();
    }

    private Lote resolverLote(TuboProtocolo tubo, Protocolo protocolo, Paciente paciente, BeanUser usuario,
                              Institucion inst, Map<String, Lote> lotesPorClave,
                              Map<String, Integer> contadorEnLote, int siguienteNumeroLote) {
        String clave = claveLote(tubo);
        Lote existente = lotesPorClave.get(clave);
        if (existente != null) {
            return existente;
        }
        Lote lote = new Lote();
        lote.setInstitucion(inst);
        lote.setPaciente(paciente);
        lote.setTipoResultante(tubo.getTipoResultante());
        lote.setProtocolo(protocolo);
        lote.setNumeroLote(siguienteNumeroLote + 1);
        lote.setUsuarioProcesa(usuario);
        lote.setFechaCreacion(Timestamp.valueOf(LocalDateTime.now()));
        lote = loteRepository.save(lote);
        lotesPorClave.put(clave, lote);
        contadorEnLote.put(clave, 0);
        return lote;
    }

    // ── Alícuotas ──────────────────────────────────────────────────────────────

    private List<Muestra> generarAlicuotas(Muestra padre, TuboProtocolo tubo, DecisionTubo dec, Lote lote,
                                            Map<String, Integer> contadorEnLote, String clave, int totalLote,
                                            BeanUser usuario, Institucion inst, LocalDateTime fechaRecoleccion) {
        RecetaTubo receta = recetaDe(tubo);
        PlanificadorAlicuotas.validarUnidad(tubo.getUnidadVolumen(), padre.getUnidad());

        int configuradas = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        if (configuradas <= 0) {
            return List.of();
        }
        List<Integer> slotsLibres = new ArrayList<>(configuradas);
        for (int i = 1; i <= configuradas; i++) {
            slotsLibres.add(i);
        }

        List<Double> planVolumenes = dec != null ? dec.planVolumenes() : List.of();
        List<Double> volumenes;
        if (planVolumenes == null || planVolumenes.isEmpty()) {
            volumenes = PlanificadorAlicuotas.planificar(receta, padre.getValorDisponible(), slotsLibres)
                    .volumenesSugeridos();
            if (volumenes.isEmpty()) {
                return List.of(); // no alcanzó para ninguna completa
            }
        } else {
            volumenes = PlanificadorAlicuotas.validarPlan(planVolumenes, receta, padre.getValorDisponible(), slotsLibres);
        }

        String folio = padre.getPaciente().getFolio();
        String prefijo = tubo.getPrefijoCodigo();
        String unidad = tubo.getUnidadVolumen();
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());

        List<Muestra> generadas = new ArrayList<>(volumenes.size());
        for (int i = 0; i < volumenes.size(); i++) {
            int slot = slotsLibres.get(i);
            int numeroEnLote = contadorEnLote.merge(clave, 1, Integer::sum);

            Muestra a = new Muestra();
            // Etiqueta por LOTE: prefijo del tubo + L{lote}/{pos}-{total}. La posición
            // es la del lote (continua entre tubos), no el hueco del tubo.
            a.setEtiqueta(EtiquetaMuestra.alicuotaLote(prefijo, folio, inst.getId(),
                    lote.getNumeroLote(), numeroEnLote, totalLote));
            a.setValor(volumenes.get(i));
            a.setUnidad(unidad);
            a.setFechaRecoleccion(fechaRecoleccion);
            a.setPaciente(padre.getPaciente());
            a.setUsuarioRecolecta(usuario != null ? usuario : padre.getUsuarioRecolecta());
            a.setTipoMuestra(tubo.getTipoResultante()); // RESULTANTE, no el origen del padre
            a.setTuboProtocolo(tubo);
            a.setMuestraPadre(padre); // conserva de qué tubo primario salió
            a.setNumeroAlicuota(slot); // hueco dentro del tubo (para la receta)
            a.setTotalAlicuotas(totalLote); // tamaño del LOTE, para "pos-total"
            a.setLote(lote);
            a.setNumeroEnLote(numeroEnLote);
            a.setNumeroLote(lote.getNumeroLote());
            a.setValorComprometido(0.0);
            a.setInstitucion(inst);
            a.setInstitucionActual(inst);
            a.setFechaRegistro(ahora);
            generadas.add(muestraService.createAlicuota(a));
        }

        double suma = PlanificadorAlicuotas.sumar(volumenes);
        materializacionService.reservar(padre, suma, usuario,
                "Procesado: " + generadas.size() + " alícuota(s) de «" + tubo.getNombre()
                + "» → lote " + lote.getNumeroLote() + ". Se descuenta al ubicar cada una.");

        return generadas;
    }

    private static RecetaTubo recetaDe(TuboProtocolo tubo) {
        return new RecetaTubo(
                tubo.getNombre(),
                tubo.getNumeroAlicuotas(),
                tubo.getVolumenAlicuota(),
                tubo.getUnidadVolumen(),
                tubo.admiteAlicuotaParcial(),
                tubo.getVolumenesAlicuota());
    }
}
