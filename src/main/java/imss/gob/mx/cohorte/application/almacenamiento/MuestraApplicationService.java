package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.controllers.almacenamiento.dto.MuestraRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ZplLoteResponseDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucion;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.almacenamiento.caja.PosicionCajaService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.HistorialCambioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraTipoInstitucionService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.TipoMuestraService;
import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.services.impresion.ConfiguracionEtiquetaService;
import imss.gob.mx.cohorte.services.impresion.DirectPrintService;
import imss.gob.mx.cohorte.services.impresion.ZplLabelService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class MuestraApplicationService {

    private final MuestraService muestraService;
    private final MuestraRepository muestraRepository;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final PosicionCajaService posicionCajaService;
    private final TipoMuestraService tipoMuestraService;
    private final MuestraTipoInstitucionService muestraTipoInstitucionService;
    private final HistorialCambioMuestraService historialService;
    private final InstitucionContextService institucionContextService;
    private final ZplLabelService zplLabelService;
    private final DirectPrintService directPrintService;
    private final ConfiguracionEtiquetaService configuracionEtiquetaService;

    @Transactional(readOnly = true)
    public List<Muestra> getAllMuestras() {
        return muestraService.getAllVisibles();
    }

    @Transactional(readOnly = true)
    public Page<Muestra> getAllMuestrasPaginado(Pageable pageable) {
        return muestraService.getAllPaginado(pageable);
    }

    @Transactional(readOnly = true)
    public Muestra getMuestra(Long id) {
        return muestraService.getById(id);
    }

    @Transactional(readOnly = true)
    public List<Muestra> getMuestrasByPacienteUUID(String uuid) {
        return muestraService.getAll().stream()
            .filter(m -> m.getPaciente().getUuid().equals(uuid))
            .toList();
    }

    @Transactional(readOnly = true)
    public long countMuestrasByPacienteUuid(String uuid) {
        return muestraService.countByPacienteUuid(uuid);
    }

    @Transactional
    public Muestra createMuestra(Muestra muestra) {
        Paciente paciente = pacienteService.getByUUID(muestra.getPaciente().getUuid(), institucionContextService.getIdInstitucionActual());
        muestra.setPaciente(paciente);
        muestra.setInstitucion(paciente.getInstitucion());
        muestra.setInstitucionActual(paciente.getInstitucion());

        BeanUser usuario = userService.getByUUID(muestra.getUsuarioRecolecta().getUUID());
        muestra.setUsuarioRecolecta(usuario);

        if (muestra.getPosicionCaja() != null && muestra.getPosicionCaja().getId() != null) {
            PosicionCaja posicion = posicionCajaService.getById(muestra.getPosicionCaja().getId());
            if (posicion.getOcupada()) {
                throw new ObjConflictException("La posición de caja ya está ocupada");
            }
            muestra.setPosicionCaja(posicion);
        }

        // Resolver TipoMuestra / TuboMuestra si vienen en el request
        if (muestra.getTipoMuestra() != null && muestra.getTipoMuestra().getId() != null) {
            TipoMuestra tipo = tipoMuestraService.getById(muestra.getTipoMuestra().getId());
            muestra.setTipoMuestra(tipo);
        }
        if (muestra.getTuboMuestra() != null && muestra.getTuboMuestra().getId() != null) {
            TuboMuestra tubo = tipoMuestraService.getTuboById(muestra.getTuboMuestra().getId());
            muestra.setTuboMuestra(tubo);
        }

        // Auto-generar etiqueta: {prefijoCodigo}/{folio}/F4
        muestra.setEtiqueta(generarEtiquetaPadre(muestra));

        Muestra saved = muestraService.create(muestra);

        if (saved.getPosicionCaja() != null) {
            marcarPosicionCajaOcupada(saved.getPosicionCaja().getId(), true);
        }

        // Auto-generar alícuotas si el tubo lo requiere
        if (saved.getTuboMuestra() != null) {
            int numAlicuotas = saved.getTuboMuestra().getNumeroAlicuotas() != null
                    ? saved.getTuboMuestra().getNumeroAlicuotas() : 0;
            if (numAlicuotas > 0) {
                generarAlicuotas(saved, numAlicuotas);
            }
        }

        return saved;
    }

    @Transactional
    public Muestra updateMuestra(Long id, MuestraRequestDTO dto) {
        Muestra anterior = muestraService.getById(id);

        // Snapshot de valores anteriores para historial
        Double valorAnterior = anterior.getValor();
        String unidadAnterior = anterior.getUnidad();
        java.time.LocalDateTime fechaAnterior = anterior.getFechaRecoleccion();
        String obsAnterior = anterior.getObservaciones();
        Long idPosAnterior = anterior.getPosicionCaja() != null ? anterior.getPosicionCaja().getId() : null;

        Muestra actualizada = muestraService.update(
                id,
                dto.getValor(),
                dto.getUnidad(),
                dto.getFechaRecoleccion(),
                dto.getObservaciones(),
                dto.getIdPosicionCaja()
        );

        // Registrar historial de cada campo que cambió
        BeanUser usuarioEditor = actualizada.getUsuarioRecolecta();

        if (!java.util.Objects.equals(valorAnterior, dto.getValor())) {
            historialService.registrar(actualizada, usuarioEditor, "valor",
                    str(valorAnterior), str(dto.getValor()), null);
        }
        if (!java.util.Objects.equals(unidadAnterior, dto.getUnidad())) {
            historialService.registrar(actualizada, usuarioEditor, "unidad",
                    unidadAnterior, dto.getUnidad(), null);
        }
        if (!java.util.Objects.equals(fechaAnterior, dto.getFechaRecoleccion())) {
            historialService.registrar(actualizada, usuarioEditor, "fechaRecoleccion",
                    str(fechaAnterior), str(dto.getFechaRecoleccion()), null);
        }
        if (!java.util.Objects.equals(obsAnterior, dto.getObservaciones())) {
            historialService.registrar(actualizada, usuarioEditor, "observaciones",
                    obsAnterior, dto.getObservaciones(), null);
        }
        Long idPosNueva = actualizada.getPosicionCaja() != null ? actualizada.getPosicionCaja().getId() : null;
        if (!java.util.Objects.equals(idPosAnterior, idPosNueva)) {
            historialService.registrar(actualizada, usuarioEditor, "posicionCaja",
                    idPosAnterior != null ? "PosicionCaja#" + idPosAnterior : null,
                    idPosNueva != null ? "PosicionCaja#" + idPosNueva : null, null);
        }

        return actualizada;
    }

    private String str(Object val) {
        return val == null ? null : String.valueOf(val);
    }

    @Transactional
    public void deleteMuestra(Long id) {
        muestraService.delete(id);
    }

    /** Muestras cuyo tenedor actual es la institución del usuario (biobanco propio). */
    @Transactional(readOnly = true)
    public List<Muestra> getMuestrasEnBiobanco() {
        return muestraService.getAllEnBiobanco();
    }

    @Transactional(readOnly = true)
    public Page<Muestra> getMuestrasEnBiobancoPage(Pageable pageable) {
        return muestraService.getAllEnBiobancoPage(pageable);
    }

    /** Alícuotas de una muestra padre. */
    @Transactional(readOnly = true)
    public List<Muestra> getAlicuotas(Long idMuestraPadre) {
        return muestraService.getAlicuotas(idMuestraPadre);
    }

    /**
     * Asigna o mueve la muestra a una PosicionCaja en el biobanco de su institucionActual.
     * Registra historial POSICION_ASIGNADA.
     */
    @Transactional
    public Muestra asignarPosicion(Long idMuestra, Long idPosicionCaja, String motivo) {
        Muestra anterior = muestraService.getByIdComoTenedor(idMuestra);
        Muestra actualizada = muestraService.asignarPosicion(idMuestra, idPosicionCaja, motivo);
        BeanUser usuario = actualizada.getUsuarioRecolecta();

        String posAnterior = anterior.getPosicionCaja() != null
                ? "PosicionCaja#" + anterior.getPosicionCaja().getId() : "Sin posición";
        String posNueva = "PosicionCaja#" + idPosicionCaja;

        historialService.registrarEvento(actualizada, usuario,
                imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra.POSICION_ASIGNADA,
                posAnterior, posNueva, motivo, null);

        return actualizada;
    }

    /**
     * Libera la posición física de una muestra sin moverla a otra.
     * Registra historial POSICION_LIBERADA.
     */
    @Transactional
    public Muestra liberarPosicion(Long idMuestra, String motivo) {
        Muestra anterior = muestraService.getByIdComoTenedor(idMuestra);
        String posAnterior = anterior.getPosicionCaja() != null
                ? "PosicionCaja#" + anterior.getPosicionCaja().getId() : null;

        Muestra actualizada = muestraService.liberarPosicion(idMuestra, motivo);

        historialService.registrarEvento(actualizada, actualizada.getUsuarioRecolecta(),
                imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra.POSICION_LIBERADA,
                posAnterior, null, motivo, null);

        return actualizada;
    }

    private void marcarPosicionCajaOcupada(Long idPosicion, Boolean ocupada) {
        PosicionCaja pos = posicionCajaService.getById(idPosicion);
        pos.setOcupada(ocupada);
        posicionCajaService.update(pos);
    }

    private String generarEtiquetaPadre(Muestra muestra) {
        String prefijo = "M";
        if (muestra.getTuboMuestra() != null && muestra.getTuboMuestra().getPrefijoCodigo() != null
                && !muestra.getTuboMuestra().getPrefijoCodigo().isBlank()) {
            prefijo = muestra.getTuboMuestra().getPrefijoCodigo();
        }
        String folio = muestra.getPaciente().getFolio();
        int lote = muestraRepository.findMaxLoteByFolioAndTuboPrefix(folio, prefijo) + 1;
        muestra.setNumeroLote(lote);
        Long idInst = muestra.getInstitucion().getId();
        return prefijo + "/" + folio + "/I" + idInst + "F4-L" + lote;
    }

    @Transactional
    public List<Muestra> generarAlicuotasEnReceptora(Long idMuestraPadre, Long idTipoMuestra, Long idTuboMuestra) {
        Muestra padre = muestraService.getByIdComoTenedor(idMuestraPadre);
        Long idInst = institucionContextService.getIdInstitucionActual();

        if (!padre.getInstitucionActual().getId().equals(idInst)) {
            throw new ObjConflictException("La muestra no se encuentra actualmente en su institución.");
        }
        if (padre.getMuestraPadre() != null) {
            throw new ValidationException("Solo se pueden generar alícuotas de muestras padre.");
        }

        TipoMuestra tipo = tipoMuestraService.getById(idTipoMuestra);
        TuboMuestra tubo = tipoMuestraService.getTuboById(idTuboMuestra);

        if (!tubo.getTipoMuestra().getId().equals(tipo.getId())) {
            throw new ValidationException("El tubo seleccionado no pertenece al tipo de muestra indicado.");
        }
        int numAlicuotas = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        if (numAlicuotas <= 0) {
            throw new ValidationException("El tubo seleccionado no genera alícuotas (tubo directo).");
        }

        Institucion miInstitucion = institucionContextService.getInstitucionActual();

        if (muestraRepository.existsByMuestraPadre_IdAndTipoMuestra_IdAndTuboMuestra_IdAndInstitucion_Id(
                idMuestraPadre, idTipoMuestra, idTuboMuestra, miInstitucion.getId())) {
            throw new ObjConflictException(
                    "Ya existen alícuotas de este tipo y tubo para esta muestra en su biobanco.");
        }

        // Upsert tipo/tubo por institución
        muestraTipoInstitucionService.asignarTipoTubo(idMuestraPadre, idTipoMuestra, idTuboMuestra);

        BeanUser usuario = institucionContextService.getUsuarioActual();

        String unidad = (tubo.getUnidadVolumen() != null && !tubo.getUnidadVolumen().isBlank())
                ? tubo.getUnidadVolumen() : padre.getUnidad();

        String prefijo = (tubo.getPrefijoCodigo() != null && !tubo.getPrefijoCodigo().isBlank())
                ? tubo.getPrefijoCodigo() : "M";
        String folio = padre.getPaciente().getFolio();
        int lote = padre.getNumeroLote();

        String etiquetaBase = prefijo + "/" + folio + "/I" + miInstitucion.getId() + "F4-L" + lote;

        java.util.List<Muestra> generadas = new java.util.ArrayList<>();

        for (int i = 1; i <= numAlicuotas; i++) {
            Muestra alicuota = new Muestra();
            alicuota.setEtiqueta(etiquetaBase + "/" + i + "-" + numAlicuotas);
            alicuota.setValor(tubo.getVolumenAlicuota());
            alicuota.setUnidad(unidad);
            alicuota.setFechaRecoleccion(padre.getFechaRecoleccion());
            alicuota.setPaciente(padre.getPaciente());
            alicuota.setUsuarioRecolecta(usuario);
            alicuota.setTipoMuestra(tipo);
            alicuota.setTuboMuestra(tubo);
            alicuota.setMuestraPadre(padre);
            alicuota.setNumeroAlicuota(i);
            alicuota.setTotalAlicuotas(numAlicuotas);
            alicuota.setNumeroLote(lote);
            alicuota.setInstitucion(miInstitucion);
            alicuota.setInstitucionActual(miInstitucion);
            alicuota.setFechaRegistro(Timestamp.valueOf(LocalDateTime.now()));
            generadas.add(muestraService.createAlicuota(alicuota));
        }

        return generadas;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<MuestraTipoInstitucion> getTipoInstitucion(Long idMuestra) {
        return muestraTipoInstitucionService.getByMuestraYMiInstitucion(idMuestra);
    }

    private void generarAlicuotas(Muestra primaria, int cantidad) {
        TuboMuestra tubo = primaria.getTuboMuestra();
        String unidad = (tubo.getUnidadVolumen() != null && !tubo.getUnidadVolumen().isBlank())
                ? tubo.getUnidadVolumen() : primaria.getUnidad();

        String etiquetaBase = primaria.getEtiqueta();

        for (int i = 1; i <= cantidad; i++) {
            Muestra alicuota = new Muestra();
            alicuota.setEtiqueta(etiquetaBase + "/" + i + "-" + cantidad);
            alicuota.setValor(tubo.getVolumenAlicuota());
            alicuota.setUnidad(unidad);
            alicuota.setFechaRecoleccion(primaria.getFechaRecoleccion());
            alicuota.setPaciente(primaria.getPaciente());
            alicuota.setUsuarioRecolecta(primaria.getUsuarioRecolecta());
            alicuota.setTipoMuestra(primaria.getTipoMuestra());
            alicuota.setTuboMuestra(tubo);
            alicuota.setMuestraPadre(primaria);
            alicuota.setNumeroAlicuota(i);
            alicuota.setTotalAlicuotas(cantidad);
            alicuota.setNumeroLote(primaria.getNumeroLote());
            alicuota.setInstitucion(primaria.getInstitucion());
            alicuota.setInstitucionActual(primaria.getInstitucionActual());
            alicuota.setFechaRegistro(Timestamp.valueOf(LocalDateTime.now()));
            muestraService.createAlicuota(alicuota);
        }
    }

    // ── Impresión ZPL ────────────────────────────────────────────────────────

    private ConfiguracionEtiqueta resolverConfig(Long configuracionId) {
        Long idInst = institucionContextService.getIdInstitucionActual();
        if (configuracionId != null) {
            return configuracionEtiquetaService.obtenerPorId(configuracionId, idInst);
        }
        ConfiguracionEtiqueta pred = configuracionEtiquetaService.obtenerPredeterminada(idInst);
        if (pred == null) {
            throw new RuntimeException("No hay configuración de etiqueta predeterminada. Cree una en Configuración > Etiquetas y márquela como predeterminada.");
        }
        return pred;
    }

    @Transactional(readOnly = true)
    public String generarZplEtiqueta(Long idMuestra, Long configuracionId) {
        Muestra muestra = muestraService.getById(idMuestra);
        return zplLabelService.generarZplMuestra(muestra, resolverConfig(configuracionId));
    }

    @Transactional(readOnly = true)
    public ZplLoteResponseDTO generarZplAlicuotas(Long idMuestraPadre, Long configuracionId) {
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        List<Muestra> alicuotas = muestraService.getAlicuotas(idMuestraPadre);
        String zpl = zplLabelService.generarZplLote(alicuotas, config);
        return new ZplLoteResponseDTO(zpl, alicuotas.size());
    }

    @Transactional(readOnly = true)
    public ZplLoteResponseDTO generarZplLoteCompleto(Long idMuestraPadre, Long configuracionId) {
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        Muestra padre = muestraService.getByIdConAcceso(idMuestraPadre);
        Long idInst = institucionContextService.getIdInstitucionActual();
        boolean padreEnMiBiobanco = padre.getInstitucionActual().getId().equals(idInst);
        List<Muestra> alicuotas = muestraRepository.findAllByMuestraPadre_IdAndInstitucionActual_Id(
                idMuestraPadre, idInst);
        if (!padreEnMiBiobanco && alicuotas.isEmpty()) {
            throw new ObjConflictException("No tiene muestras de este lote en su biobanco.");
        }
        List<Muestra> aImprimir = new java.util.ArrayList<>();
        if (padreEnMiBiobanco) {
            aImprimir.add(padre);
        }
        aImprimir.addAll(alicuotas);
        String zpl = zplLabelService.generarZplLote(aImprimir, config);
        return new ZplLoteResponseDTO(zpl, aImprimir.size());
    }

    // ── Impresión directa ───────────────────────────────────────────────────

    public List<String> listarImpresoras() {
        return directPrintService.listarImpresoras();
    }

    @Transactional(readOnly = true)
    public void imprimirEtiqueta(Long idMuestra, String nombreImpresora, Long configuracionId) {
        Muestra muestra = muestraService.getByIdConAcceso(idMuestra);
        Long idInst = institucionContextService.getIdInstitucionActual();
        if (!muestra.getInstitucionActual().getId().equals(idInst)) {
            throw new ObjConflictException("Solo puede imprimir etiquetas de muestras que se encuentran en su biobanco.");
        }
        String zpl = zplLabelService.generarZplMuestra(muestra, resolverConfig(configuracionId));
        directPrintService.imprimir(zpl, nombreImpresora);
    }

    @Transactional(readOnly = true)
    public int imprimirAlicuotas(Long idMuestraPadre, String nombreImpresora, Long configuracionId) {
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        Muestra padre = muestraService.getByIdConAcceso(idMuestraPadre);
        Long idInst = institucionContextService.getIdInstitucionActual();
        List<Muestra> alicuotas = muestraRepository.findAllByMuestraPadre_IdAndInstitucionActual_Id(
                idMuestraPadre, idInst);
        if (!alicuotas.isEmpty()) {
            String zpl = zplLabelService.generarZplLote(alicuotas, config);
            directPrintService.imprimir(zpl, nombreImpresora);
        }
        return alicuotas.size();
    }

    @Transactional(readOnly = true)
    public int imprimirLoteCompleto(Long idMuestraPadre, String nombreImpresora, Long configuracionId) {
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        Muestra padre = muestraService.getByIdConAcceso(idMuestraPadre);
        Long idInst = institucionContextService.getIdInstitucionActual();
        boolean padreEnMiBiobanco = padre.getInstitucionActual().getId().equals(idInst);
        List<Muestra> alicuotas = muestraRepository.findAllByMuestraPadre_IdAndInstitucionActual_Id(
                idMuestraPadre, idInst);
        if (!padreEnMiBiobanco && alicuotas.isEmpty()) {
            throw new ObjConflictException("No tiene muestras de este lote en su biobanco.");
        }
        List<Muestra> aImprimir = new java.util.ArrayList<>();
        if (padreEnMiBiobanco) {
            aImprimir.add(padre);
        }
        aImprimir.addAll(alicuotas);
        String zpl = zplLabelService.generarZplLote(aImprimir, config);
        directPrintService.imprimir(zpl, nombreImpresora);
        return aImprimir.size();
    }
}
