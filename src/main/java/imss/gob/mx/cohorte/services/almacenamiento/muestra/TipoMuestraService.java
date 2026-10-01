package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucionRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
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

@Service
@RequiredArgsConstructor
public class TipoMuestraService {

    private final TipoMuestraRepository tipoMuestraRepository;
    private final TuboMuestraRepository tuboMuestraRepository;
    private final MuestraRepository muestraRepository;
    private final MuestraTipoInstitucionRepository muestraTipoInstitucionRepository;
    private final InstitucionContextService institucionContextService;

    private Long myInstId() {
        return institucionContextService.getIdInstitucionActual();
    }

    // ── TipoMuestra ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TipoMuestra> getAll() {
        return tipoMuestraRepository.findAllByInstitucion_IdOrderByNombreAsc(myInstId());
    }

    @Transactional(readOnly = true)
    public List<TipoMuestra> getAllActivos() {
        return tipoMuestraRepository.findAllByInstitucion_IdAndActivoTrueOrderByNombreAsc(myInstId());
    }

    @Transactional(readOnly = true)
    public TipoMuestra getById(Long id) {
        TipoMuestra tipo = tipoMuestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tipo de muestra con id: " + id));
        // El listado ya filtra por institución; esta consulta también tiene que
        // hacerlo, o el catálogo ajeno queda a la vista —y editable— con solo
        // pasar su id.
        institucionContextService.verificarPertenece(tipo.getInstitucion());
        return tipo;
    }

    @Transactional
    public TipoMuestra create(TipoMuestra tipoMuestra) {
        Long idInst = myInstId();
        tipoMuestraRepository.findByNombreIgnoreCaseAndInstitucion_Id(tipoMuestra.getNombre(), idInst).ifPresent(t -> {
            throw new ObjConflictException("Ya existe un tipo de muestra con ese nombre");
        });
        Institucion inst = institucionContextService.getInstitucionActual();
        tipoMuestra.setInstitucion(inst);
        return tipoMuestraRepository.save(tipoMuestra);
    }

    @Transactional
    public TipoMuestra update(Long id, TipoMuestra datos) {
        TipoMuestra tipo = getById(id);
        Long idInst = myInstId();
        if (!tipo.getNombre().equalsIgnoreCase(datos.getNombre())) {
            tipoMuestraRepository.findByNombreIgnoreCaseAndInstitucion_Id(datos.getNombre(), idInst).ifPresent(t -> {
                throw new ObjConflictException("Ya existe un tipo de muestra con ese nombre");
            });
            tipo.setNombre(datos.getNombre());
        }
        if (datos.getDescripcion() != null) tipo.setDescripcion(datos.getDescripcion());
        if (datos.getTemperaturaAlmacenamiento() != null) tipo.setTemperaturaAlmacenamiento(datos.getTemperaturaAlmacenamiento());
        return tipoMuestraRepository.save(tipo);
    }

    @Transactional
    public TipoMuestra toggleActivo(Long id) {
        TipoMuestra tipo = getById(id);
        tipo.setActivo(!tipo.getActivo());
        return tipoMuestraRepository.save(tipo);
    }

    /**
     * Elimina un tipo de muestra, solo si no tiene tubos configurados.
     *
     * <p>«Sin tubos» basta como garantía: un tubo con muestras o alícuotas no se
     * puede eliminar ({@link #deleteTubo}), así que un tipo sin tubos tampoco
     * tiene muestras que lo referencien, y el borrado no choca con ninguna clave
     * foránea. Para retirar un tipo que sí tiene tubos en uso está
     * {@link #toggleActivo}: deja de ofrecerse sin perder el historial.</p>
     */
    @Transactional
    public void deleteTipo(Long id) {
        TipoMuestra tipo = getById(id);
        int tubos = tipo.getTubos() != null ? tipo.getTubos().size() : 0;
        if (tubos > 0) {
            throw new ObjConflictException("No se puede eliminar el tipo de muestra '" + tipo.getNombre()
                    + "': tiene " + tubos + " tubo(s) configurado(s). Elimine primero sus tubos, "
                    + "o desactive el tipo para retirarlo conservando el historial.");
        }
        tipoMuestraRepository.delete(tipo);
    }

    // ── TuboMuestra ──────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TuboMuestra getTuboById(Long id) {
        TuboMuestra tubo = tuboMuestraRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el tubo con id: " + id));
        // Un tubo no guarda institución: la hereda del tipo de muestra del que cuelga.
        institucionContextService.verificarPertenece(tubo.getTipoMuestra().getInstitucion());
        return tubo;
    }

    @Transactional
    public TuboMuestra addTubo(Long idTipoMuestra, TuboMuestra tubo) {
        TipoMuestra tipo = getById(idTipoMuestra);
        tubo.setTipoMuestra(tipo);
        if (tubo.getOrden() == null || tubo.getOrden() == 0) {
            int maxOrden = tipo.getTubos().stream()
                    .mapToInt(TuboMuestra::getOrden)
                    .max().orElse(0);
            tubo.setOrden(maxOrden + 1);
        }
        validarRecetaAlicuotado(tubo);
        normalizarVolumenesAlicuota(tubo);
        return tuboMuestraRepository.save(tubo);
    }

    @Transactional
    public TuboMuestra updateTubo(Long idTubo, TuboMuestra datos) {
        TuboMuestra tubo = getTuboById(idTubo);
        if (datos.getNombre() != null) tubo.setNombre(datos.getNombre());
        if (datos.getPrefijoCodigo() != null) tubo.setPrefijoCodigo(datos.getPrefijoCodigo());
        if (datos.getNumeroAlicuotas() != null) tubo.setNumeroAlicuotas(datos.getNumeroAlicuotas());
        if (datos.getVolumenAlicuota() != null) tubo.setVolumenAlicuota(datos.getVolumenAlicuota());
        // null = no se toca la configuración por slot; presente, la reemplaza.
        if (datos.getVolumenesAlicuota() != null) tubo.setVolumenesAlicuota(datos.getVolumenesAlicuota());
        if (datos.getUnidadVolumen() != null) tubo.setUnidadVolumen(datos.getUnidadVolumen());
        if (datos.getDestinoSugerido() != null) tubo.setDestinoSugerido(datos.getDestinoSugerido());
        if (datos.getOrden() != null) tubo.setOrden(datos.getOrden());
        if (datos.getActivo() != null) tubo.setActivo(datos.getActivo());
        if (datos.getGeneracionAutomatica() != null) tubo.setGeneracionAutomatica(datos.getGeneracionAutomatica());
        if (datos.getPermiteAlicuotaParcial() != null) tubo.setPermiteAlicuotaParcial(datos.getPermiteAlicuotaParcial());
        validarRecetaAlicuotado(tubo);
        normalizarVolumenesAlicuota(tubo);
        return tuboMuestraRepository.save(tubo);
    }

    /**
     * Un tubo que alicuota tiene que decir de cuánto y en qué unidad.
     *
     * <p>Antes ambos datos eran opcionales y el sistema los suplía por su
     * cuenta: el volumen se daba por bueno aunque fuera nulo y la unidad se
     * heredaba de la muestra padre. Con la unidad imponiéndose ahora desde el
     * tubo hacia la muestra, un tubo sin unidad no tendría nada que imponer, y
     * uno sin volumen no permitiría calcular cuántas alícuotas alcanzan.</p>
     */
    private void validarRecetaAlicuotado(TuboMuestra tubo) {
        int alicuotas = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        if (alicuotas <= 0) {
            return; // tubo directo: no alicuota, no necesita receta
        }
        if (tubo.getVolumenAlicuota() == null || tubo.getVolumenAlicuota() <= 0) {
            throw new ValidationException(
                    "El tubo \"" + tubo.getNombre() + "\" genera " + alicuotas
                    + " alícuota(s): indique el volumen de cada una.");
        }
        if (tubo.getUnidadVolumen() == null || tubo.getUnidadVolumen().isBlank()) {
            throw new ValidationException(
                    "El tubo \"" + tubo.getNombre() + "\" genera " + alicuotas
                    + " alícuota(s): indique la unidad del volumen. La muestra padre se "
                    + "registrará en esa misma unidad.");
        }
        // La configuración por slot es opcional (vacía = uniforme), pero si viene
        // no puede traer un volumen inválido: cada vial tiene que decir de cuánto.
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

    /**
     * Deja la lista por slot con exactamente tantos volúmenes como alícuotas.
     *
     * <p>Recorta lo que sobra y rellena lo que falte —o venga sin valor— con el
     * volumen general, de modo que el planificador no tenga que adivinar. Un tubo
     * directo o uno uniforme (lista vacía) se dejan sin configuración por slot:
     * cada slot cae al volumen general y el tubo se comporta como siempre.</p>
     */
    private void normalizarVolumenesAlicuota(TuboMuestra tubo) {
        int n = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        List<Double> actuales = tubo.getVolumenesAlicuota();

        if (n <= 0) {
            if (actuales != null && !actuales.isEmpty()) {
                tubo.setVolumenesAlicuota(new ArrayList<>());
            }
            return;
        }
        if (actuales == null || actuales.isEmpty()) {
            return; // uniforme: sin lista por slot
        }

        Double general = tubo.getVolumenAlicuota();
        List<Double> out = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            Double v = i < actuales.size() ? actuales.get(i) : null;
            out.add(v != null && !v.isNaN() && !v.isInfinite() && v > 0 ? v : general);
        }
        tubo.setVolumenesAlicuota(out);
    }

    /**
     * Elimina un tubo de su tipo de muestra.
     *
     * El borrado se hace quitando el tubo de la colección del padre, no llamando
     * a delete() sobre el hijo: TipoMuestra.tubos es cascade = ALL con
     * orphanRemoval, y la relación inversa (TuboMuestra.tipoMuestra) es EAGER por
     * omisión. Al cargar el tubo se carga también el tipo con toda su colección,
     * que seguiría conteniendo el tubo borrado; al hacer flush, la cascada lo
     * volvía a persistir y el DELETE quedaba sin efecto.
     */
    @Transactional
    public void deleteTubo(Long idTubo) {
        TuboMuestra tubo = getTuboById(idTubo);

        // Un tubo solo puede eliminarse mientras no tenga registros asociados:
        // la eliminación existe para corregir un alta equivocada, no para retirar
        // un tubo en uso. Sin esta comprobación, el DELETE choca contra las claves
        // foráneas y aflora como error 500 sin explicación.
        long muestras = muestraRepository.countByTuboMuestra_Id(idTubo);
        if (muestras > 0) {
            throw new ObjConflictException("No se puede eliminar el tubo '" + tubo.getNombre()
                    + "': tiene " + muestras + " muestra(s) o alícuota(s) asociadas.");
        }
        long asignaciones = muestraTipoInstitucionRepository.countByTuboMuestra_Id(idTubo);
        if (asignaciones > 0) {
            throw new ObjConflictException("No se puede eliminar el tubo '" + tubo.getNombre()
                    + "': tiene " + asignaciones + " asignación(es) a instituciones.");
        }

        TipoMuestra tipo = tubo.getTipoMuestra();
        tipo.getTubos().removeIf(t -> t.getId().equals(idTubo));
        tipoMuestraRepository.save(tipo);
    }
}
