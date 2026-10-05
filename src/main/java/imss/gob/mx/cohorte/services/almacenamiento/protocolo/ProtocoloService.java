package imss.gob.mx.cohorte.services.almacenamiento.protocolo;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.Protocolo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.ProtocoloRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.TuboProtocolo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.TuboProtocoloRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de protocolos de procesamiento y de sus tubos primarios.
 *
 * <p>Calca a {@code TipoMuestraService}: todo va con alcance de institución, el
 * tipo origen y el tipo resultante se resuelven contra el catálogo de la propia
 * institución, y la receta de alicuotado solo se exige cuando el tubo alicuota.</p>
 */
@Service
@RequiredArgsConstructor
public class ProtocoloService {

    private final ProtocoloRepository protocoloRepository;
    private final TuboProtocoloRepository tuboProtocoloRepository;
    private final TipoMuestraRepository tipoMuestraRepository;
    private final InstitucionContextService institucionContextService;

    private Long myInstId() {
        return institucionContextService.getIdInstitucionActual();
    }

    // ── Protocolo ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Protocolo> getAll() {
        return protocoloRepository.findAllByInstitucion_IdOrderByNombreAsc(myInstId());
    }

    @Transactional(readOnly = true)
    public List<Protocolo> getAllActivos() {
        return protocoloRepository.findAllByInstitucion_IdAndActivoTrueOrderByNombreAsc(myInstId());
    }

    @Transactional(readOnly = true)
    public Protocolo getById(Long id) {
        Protocolo p = protocoloRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el protocolo con id: " + id));
        institucionContextService.verificarPertenece(p.getInstitucion());
        return p;
    }

    @Transactional
    public Protocolo create(Protocolo protocolo, Long idTipoOrigen) {
        Long idInst = myInstId();
        protocoloRepository.findByNombreIgnoreCaseAndInstitucion_Id(protocolo.getNombre(), idInst).ifPresent(p -> {
            throw new ObjConflictException("Ya existe un protocolo con ese nombre");
        });
        Institucion inst = institucionContextService.getInstitucionActual();
        protocolo.setInstitucion(inst);
        protocolo.setTipoOrigen(resolverTipo(idTipoOrigen, "origen"));
        return protocoloRepository.save(protocolo);
    }

    @Transactional
    public Protocolo update(Long id, Protocolo datos, Long idTipoOrigen) {
        Protocolo p = getById(id);
        Long idInst = myInstId();
        if (datos.getNombre() != null && !p.getNombre().equalsIgnoreCase(datos.getNombre())) {
            protocoloRepository.findByNombreIgnoreCaseAndInstitucion_Id(datos.getNombre(), idInst).ifPresent(o -> {
                throw new ObjConflictException("Ya existe un protocolo con ese nombre");
            });
            p.setNombre(datos.getNombre());
        }
        if (datos.getDescripcion() != null) p.setDescripcion(datos.getDescripcion());
        if (idTipoOrigen != null) p.setTipoOrigen(resolverTipo(idTipoOrigen, "origen"));
        return protocoloRepository.save(p);
    }

    @Transactional
    public Protocolo toggleActivo(Long id) {
        Protocolo p = getById(id);
        p.setActivo(!p.getActivo());
        return protocoloRepository.save(p);
    }

    @Transactional
    public void deleteProtocolo(Long id) {
        Protocolo p = getById(id);
        int tubos = p.getTubos() != null ? p.getTubos().size() : 0;
        if (tubos > 0) {
            throw new ObjConflictException("No se puede eliminar el protocolo '" + p.getNombre()
                    + "': tiene " + tubos + " tubo(s) configurado(s). Elimine primero sus tubos, "
                    + "o desactive el protocolo para retirarlo conservando la configuración.");
        }
        protocoloRepository.delete(p);
    }

    // ── TuboProtocolo ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TuboProtocolo getTuboById(Long id) {
        TuboProtocolo tubo = tuboProtocoloRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tubo con id: " + id));
        institucionContextService.verificarPertenece(tubo.getProtocolo().getInstitucion());
        return tubo;
    }

    @Transactional
    public TuboProtocolo addTubo(Long idProtocolo, TuboProtocolo tubo, Long idTipoResultante) {
        Protocolo p = getById(idProtocolo);
        tubo.setProtocolo(p);
        if (tubo.getOrden() == null || tubo.getOrden() == 0) {
            int maxOrden = p.getTubos().stream().mapToInt(TuboProtocolo::getOrden).max().orElse(0);
            tubo.setOrden(maxOrden + 1);
        }
        aplicarTipoResultante(tubo, idTipoResultante);
        validarRecetaAlicuotado(tubo);
        normalizarVolumenesAlicuota(tubo);
        return tuboProtocoloRepository.save(tubo);
    }

    @Transactional
    public TuboProtocolo updateTubo(Long idTubo, TuboProtocolo datos, Long idTipoResultante, boolean tipoResultantePresente) {
        TuboProtocolo tubo = getTuboById(idTubo);
        if (datos.getNombre() != null) tubo.setNombre(datos.getNombre());
        if (datos.getPrefijoCodigo() != null) tubo.setPrefijoCodigo(datos.getPrefijoCodigo());
        if (datos.getAccion() != null) tubo.setAccion(datos.getAccion());
        if (datos.getNumeroAlicuotas() != null) tubo.setNumeroAlicuotas(datos.getNumeroAlicuotas());
        if (datos.getVolumenAlicuota() != null) tubo.setVolumenAlicuota(datos.getVolumenAlicuota());
        if (datos.getVolumenesAlicuota() != null) tubo.setVolumenesAlicuota(datos.getVolumenesAlicuota());
        if (datos.getUnidadVolumen() != null) tubo.setUnidadVolumen(datos.getUnidadVolumen());
        if (datos.getDestinoSugerido() != null) tubo.setDestinoSugerido(datos.getDestinoSugerido());
        if (datos.getOrden() != null) tubo.setOrden(datos.getOrden());
        if (datos.getActivo() != null) tubo.setActivo(datos.getActivo());
        if (datos.getAgruparEnLote() != null) tubo.setAgruparEnLote(datos.getAgruparEnLote());
        if (datos.getGeneracionAutomatica() != null) tubo.setGeneracionAutomatica(datos.getGeneracionAutomatica());
        if (datos.getPermiteAlicuotaParcial() != null) tubo.setPermiteAlicuotaParcial(datos.getPermiteAlicuotaParcial());
        // null = «no lo menciono»; presente (aunque sea null explícito) lo reemplaza.
        if (tipoResultantePresente) aplicarTipoResultante(tubo, idTipoResultante);
        validarRecetaAlicuotado(tubo);
        normalizarVolumenesAlicuota(tubo);
        return tuboProtocoloRepository.save(tubo);
    }

    @Transactional
    public void deleteTubo(Long idTubo) {
        TuboProtocolo tubo = getTuboById(idTubo);
        Protocolo p = tubo.getProtocolo();
        p.getTubos().removeIf(t -> t.getId().equals(idTubo));
        protocoloRepository.save(p);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private TipoMuestra resolverTipo(Long idTipo, String rol) {
        if (idTipo == null) {
            throw new ValidationException("Debe indicar el tipo de muestra " + rol + ".");
        }
        TipoMuestra tipo = tipoMuestraRepository.findById(idTipo)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tipo de muestra con id: " + idTipo));
        institucionContextService.verificarPertenece(tipo.getInstitucion());
        return tipo;
    }

    private void aplicarTipoResultante(TuboProtocolo tubo, Long idTipoResultante) {
        tubo.setTipoResultante(idTipoResultante != null ? resolverTipo(idTipoResultante, "resultante") : null);
    }

    /**
     * Un tubo que alicuota tiene que decir en qué se convierte, de cuánto y en qué
     * unidad. Fuera de ALICUOTAR la receta es irrelevante y no se exige.
     */
    private void validarRecetaAlicuotado(TuboProtocolo tubo) {
        if (tubo.getAccion() != AccionTubo.ALICUOTAR) {
            return;
        }
        if (tubo.getTipoResultante() == null) {
            throw new ValidationException(
                    "El tubo \"" + tubo.getNombre() + "\" alicuota: indique el tipo de muestra resultante.");
        }
        int alicuotas = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        if (alicuotas <= 0) {
            throw new ValidationException(
                    "El tubo \"" + tubo.getNombre() + "\" alicuota: indique cuántas alícuotas genera.");
        }
        if (tubo.getVolumenAlicuota() == null || tubo.getVolumenAlicuota() <= 0) {
            throw new ValidationException(
                    "El tubo \"" + tubo.getNombre() + "\" genera " + alicuotas
                    + " alícuota(s): indique el volumen de cada una.");
        }
        if (tubo.getUnidadVolumen() == null || tubo.getUnidadVolumen().isBlank()) {
            throw new ValidationException(
                    "El tubo \"" + tubo.getNombre() + "\" genera " + alicuotas
                    + " alícuota(s): indique la unidad del volumen.");
        }
        List<Double> porSlot = tubo.getVolumenesAlicuota();
        if (porSlot != null) {
            for (int i = 0; i < porSlot.size(); i++) {
                Double v = porSlot.get(i);
                if (v != null && (v.isNaN() || v.isInfinite() || v <= 0)) {
                    throw new ValidationException(
                            "El tubo \"" + tubo.getNombre() + "\": el volumen de la alícuota "
                            + (i + 1) + " debe ser mayor a 0.");
                }
            }
        }
    }

    /** Deja la lista por slot con exactamente tantos volúmenes como alícuotas. */
    private void normalizarVolumenesAlicuota(TuboProtocolo tubo) {
        int n = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        List<Double> actuales = tubo.getVolumenesAlicuota();

        if (n <= 0 || tubo.getAccion() != AccionTubo.ALICUOTAR) {
            if (actuales != null && !actuales.isEmpty()) {
                tubo.setVolumenesAlicuota(new ArrayList<>());
            }
            return;
        }
        if (actuales == null || actuales.isEmpty()) {
            return;
        }
        Double general = tubo.getVolumenAlicuota();
        List<Double> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Double v = i < actuales.size() ? actuales.get(i) : null;
            out.add(v != null && !v.isNaN() && !v.isInfinite() && v > 0 ? v : general);
        }
        tubo.setVolumenesAlicuota(out);
    }
}
