package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.controllers.almacenamiento.dto.MuestraRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ZplLoteResponseDTO;
import imss.gob.mx.cohorte.controllers.impresion.dto.ConfiguracionEtiquetaMapper;
import imss.gob.mx.cohorte.controllers.impresion.dto.LabelDataDTO;
import imss.gob.mx.cohorte.controllers.impresion.dto.PrintableLabelBatchDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucion;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d.Ubicacion3DDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.CriteriosMuestra;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PaginaMuestras;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.almacenamiento.ubicacion3d.Ubicacion3DService;
import imss.gob.mx.cohorte.services.almacenamiento.caja.PosicionCajaService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.EtiquetaMuestra;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.HistorialCambioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MaterializacionAlicuotaService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraTipoInstitucionService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PlanAlicuotas;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PlanificadorAlicuotas;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.RecetaTubo;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.TipoMuestraService;
import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.services.impresion.ConfiguracionEtiquetaService;
import imss.gob.mx.cohorte.services.impresion.DirectPrintService;
import imss.gob.mx.cohorte.services.impresion.ZplLabelService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.LocalDate;
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
    private final ParticipanteAccesoService participanteAccesoService;
    private final UserService userService;
    private final PosicionCajaService posicionCajaService;
    private final TipoMuestraService tipoMuestraService;
    private final MuestraTipoInstitucionService muestraTipoInstitucionService;
    private final HistorialCambioMuestraService historialService;
    private final InstitucionContextService institucionContextService;
    private final ZplLabelService zplLabelService;
    private final DirectPrintService directPrintService;
    private final ConfiguracionEtiquetaService configuracionEtiquetaService;
    private final Ubicacion3DService ubicacion3DService;
    private final MaterializacionAlicuotaService materializacionService;

    @Transactional(readOnly = true)
    public List<Muestra> getAllMuestras() {
        return muestraService.getAllVisibles(false);
    }

    @Transactional(readOnly = true)
    public List<Muestra> getAllMuestras(boolean incluirHistorico) {
        return muestraService.getAllVisibles(incluirHistorico);
    }

    @Transactional(readOnly = true)
    public Page<Muestra> getAllMuestrasPaginado(Pageable pageable) {
        return muestraService.getAllPaginado(pageable);
    }

    /**
     * El listado de muestras, de veinte en veinte y situado por cursor.
     *
     * <p>Aquí se fija la institución —el resto de los criterios vienen de la
     * pantalla, esta no— y se normaliza todo lo demás antes de consultar. Los
     * filtros y la búsqueda se resuelven en la base y no en el navegador: con
     * la lista completa a la vista daba igual dónde ocurriera, pero teniendo
     * solo veinte tarjetas cargadas, filtrar en el cliente contestaría sobre
     * esas veinte y escondería el resto sin decirlo.</p>
     */
    @Transactional(readOnly = true)
    public PaginaMuestras buscarPaginaMuestras(
            String cursor, boolean haciaAtras, int tamano,
            boolean incluirHistorico, boolean ocultarDevueltasHuerfanas,
            String busqueda, LocalDate fechaDesde, LocalDate fechaHasta,
            List<String> tipos, String sexo, String folioDesde, String folioHasta) {

        CriteriosMuestra criterios = CriteriosMuestra.de(
                institucionContextService.getIdInstitucionActual(),
                incluirHistorico, ocultarDevueltasHuerfanas,
                busqueda, fechaDesde, fechaHasta, tipos, sexo, folioDesde, folioHasta);

        return muestraService.buscarPagina(criterios, cursor, haciaAtras, tamano);
    }

    @Transactional(readOnly = true)
    public Muestra getMuestra(Long id) {
        return muestraService.getById(id);
    }

    /**
     * Escena completa de ubicacion para el visualizador 3D.
     *
     * <p>Usa el acceso ampliado (propietaria o tenedora) porque durante un
     * prestamo ambas instituciones necesitan poder consultar donde esta la
     * muestra: la que la presto para saber que sigue fuera, y la que la tiene
     * para localizarla en su propio biobanco.
     */
    @Transactional(readOnly = true)
    public Ubicacion3DDTO getUbicacion3D(Long id) {
        return ubicacion3DService.construir(muestraService.getByIdConAcceso(id));
    }

    /** Resuelve la etiqueta que devolvió el lector de códigos. */
    @Transactional(readOnly = true)
    public Muestra buscarPorEtiquetaEscaneada(String etiqueta) {
        return muestraService.buscarPorEtiquetaEscaneada(etiqueta);
    }

    /** La institución del usuario, para que el controlador sepa cómo situar la muestra. */
    @Transactional(readOnly = true)
    public Long getIdInstitucionActual() {
        return institucionContextService.getIdInstitucionActual();
    }

    @Transactional(readOnly = true)
    public List<Muestra> getMuestrasByPacienteUUID(String uuid) {
        return muestraService.getAll().stream()
            .filter(m -> m.getPaciente().getUuid().equals(uuid))
            .toList();
    }

    @Transactional(readOnly = true)
    public long countMuestrasByPacienteUuid(String uuid) {
        // Sin puerta por participante: una muestra es inventario de quien la tomo y se
        // sigue alicuotando y estudiando aunque el participante ya no este a su alcance.
        // El aislamiento de muestras va por propietaria/tenedora, no por participante.
        return muestraService.countByPacienteUuid(uuid);
    }

    /**
     * Registra una muestra padre y, si procede, su primer lote de alícuotas.
     *
     * @param generarAlicuotas {@code null} = lo que diga la configuración del
     *                         tubo; {@code true}/{@code false} = decisión
     *                         explícita de quien registra, que manda sobre ella
     * @param planVolumenes    volumen de cada alícuota; {@code null} = el plan
     *                         por omisión que calcule el planificador
     */
    @Transactional
    public ResultadoRegistroMuestra createMuestra(Muestra muestra, Boolean generarAlicuotas,
                                                  List<Double> planVolumenes) {
        // La institución propietaria de la muestra la determina el contexto del
        // usuario logueado, no el paciente. Y ahora pueden no coincidir: al atender
        // participantes de otra sede, la muestra es de quien la toma aunque el
        // participante sea de otra institución. Eso es deliberado — el registro
        // guarda quién lo hizo.
        Institucion miInstitucion = institucionContextService.getInstitucionActual();
        Paciente paciente = participanteAccesoService.resolver(muestra.getPaciente().getUuid());
        muestra.setPaciente(paciente);
        muestra.setInstitucion(miInstitucion);
        muestra.setInstitucionActual(miInstitucion);

        BeanUser usuario = userService.getByUUID(muestra.getUsuarioRecolecta().getUUID());
        muestra.setUsuarioRecolecta(usuario);

        if (muestra.getPosicionCaja() != null && muestra.getPosicionCaja().getId() != null) {
            PosicionCaja posicion = posicionCajaService.getById(muestra.getPosicionCaja().getId());
            // Igual que en MuestraService.asignarPosicion: el hueco tiene que ser
            // del biobanco propio. Sin esto se podía ocupar una posición de otra
            // institución pasando su id, y ese hueco quedaba tomado sin que su
            // dueño pudiera liberarlo.
            Long idInstActual = institucionContextService.getIdInstitucionActual();
            if (posicion.getCaja() == null || posicion.getCaja().getInstitucion() == null
                    || !idInstActual.equals(posicion.getCaja().getInstitucion().getId())) {
                throw new ValidationException(
                        "La posición seleccionada pertenece al biobanco de otra institución.");
            }
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

        // La unidad la manda el tubo, no quien captura: si el tubo se configuró
        // como 5 alícuotas de 50 mL, la extracción se registra en mL y punto.
        // Así no hay discrepancia que conciliar ni conversiones que hacer —el
        // sistema no tiene factores de conversión— y el descuento a la padre
        // siempre resta la misma magnitud que reservó.
        TuboMuestra tuboElegido = muestra.getTuboMuestra();
        if (tuboElegido != null && tuboElegido.getUnidadVolumen() != null
                && !tuboElegido.getUnidadVolumen().isBlank()) {
            muestra.setUnidad(tuboElegido.getUnidadVolumen());
        }

        muestra.setValorComprometido(0.0);

        // Auto-generar etiqueta: {prefijoCodigo}/{folio}/F4
        muestra.setEtiqueta(generarEtiquetaPadre(muestra));

        Muestra saved = muestraService.create(muestra);

        if (saved.getPosicionCaja() != null) {
            marcarPosicionCajaOcupada(saved.getPosicionCaja().getId(), true);
        }

        // Generar el lote si procede. El tubo solo fija el valor por omisión:
        // no toda muestra se alicuota en la unidad que la tomó —muchas se
        // guardan y se alicuotan en otra, o nunca—, así que quien registra puede
        // activarlo o saltárselo, y siempre puede generarlo después desde la
        // lista de muestras.
        List<Muestra> alicuotas = List.of();
        TuboMuestra tubo = saved.getTuboMuestra();
        if (tubo != null && tubo.getNumeroAlicuotas() != null && tubo.getNumeroAlicuotas() > 0) {
            boolean generar = generarAlicuotas != null ? generarAlicuotas : tubo.esGeneracionAutomatica();
            if (generar) {
                alicuotas = generarLote(saved, saved.getTipoMuestra(), tubo, planVolumenes);
            }
        }

        return new ResultadoRegistroMuestra(saved, alicuotas);
    }

    /**
     * Lo que dejó un registro: la muestra y las alícuotas que realmente se
     * crearon.
     *
     * <p>El conteo tiene que viajar aparte porque no se deduce de la muestra:
     * el controlador respondía antes {@code tubo.numeroAlicuotas}, que ahora
     * mentiría dos veces —en el número, que puede ser menor si el volumen no
     * alcanzó, y en si se creó alguna—.</p>
     */
    public record ResultadoRegistroMuestra(Muestra muestra, List<Muestra> alicuotas) {
        public int totalAlicuotas() {
            return alicuotas == null ? 0 : alicuotas.size();
        }
    }

    @Transactional
    public Muestra updateMuestra(Long id, MuestraRequestDTO dto) {
        Muestra anterior = muestraService.getById(id);

        // No se puede corregir el volumen por debajo de lo ya prometido a
        // alícuotas que aún no se ubican: esas alícuotas se quedarían sin
        // respaldo y la materialización acabaría truncando el descuento.
        double comprometido = anterior.getValorComprometido() != null ? anterior.getValorComprometido() : 0.0;
        if (dto.getValor() != null && PlanificadorAlicuotas.mayorQue(comprometido, dto.getValor())) {
            long pendientes = muestraRepository
                    .countByMuestraPadre_IdAndFechaMaterializacionIsNull(id);
            throw new ValidationException(
                    "No se puede dejar la muestra en " + PlanificadorAlicuotas.fmt(dto.getValor())
                    + " " + (anterior.getUnidad() != null ? anterior.getUnidad() : "")
                    + ": tiene " + PlanificadorAlicuotas.fmt(comprometido) + " comprometidos en "
                    + pendientes + " alícuota(s) pendientes de ubicar.");
        }

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

        // Asignar posición desde la edición cuenta igual que hacerlo desde el
        // botón de ubicar: es el mismo hecho físico.
        if (idPosNueva != null) {
            materializacionService.materializar(actualizada, institucionContextService.getUsuarioActual(),
                    descripcionPosicion(actualizada));
        }
        materializacionService.sellarAgotamientoSiProcede(actualizada,
                institucionContextService.getUsuarioActual());

        return actualizada;
    }

    private String str(Object val) {
        return val == null ? null : String.valueOf(val);
    }

    @Transactional
    public void deleteMuestra(Long id) {
        muestraService.delete(id);
    }

    /**
     * Dar de baja una muestra de forma irreversible. Solo la institución propietaria
     * puede hacerlo. Requiere motivo obligatorio y quedará registrado en el historial.
     */
    @Transactional
    public Muestra darDeBajaMuestra(Long id, String motivo) {
        BeanUser usuario = institucionContextService.getUsuarioActual();
        return muestraService.darDeBaja(id, motivo, usuario.getUUID());
    }

    /** Muestras cuyo tenedor actual es la institución del usuario (biobanco propio). */
    @Transactional(readOnly = true)
    public List<Muestra> getMuestrasEnBiobanco() {
        return muestraService.getAllEnBiobanco();
    }

    @Transactional(readOnly = true)
    public Page<Muestra> getMuestrasEnBiobancoPage(Pageable pageable) {
        return muestraService.getAllEnBiobancoPage(pageable, false);
    }

    /** @param incluirAgotadas lo que enciende el interruptor "mostrar agotadas" de la pantalla. */
    @Transactional(readOnly = true)
    public Page<Muestra> getMuestrasEnBiobancoPage(Pageable pageable, boolean incluirAgotadas) {
        return muestraService.getAllEnBiobancoPage(pageable, incluirAgotadas);
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

        // Ubicar una alícuota es la prueba de que el vial se llenó de verdad:
        // aquí es donde el volumen deja de estar reservado y se descuenta.
        materializacionService.materializar(actualizada, institucionContextService.getUsuarioActual(),
                descripcionPosicion(actualizada));

        return actualizada;
    }

    /**
     * Ubica de una sola vez todas las alícuotas pendientes de un lote.
     *
     * <p>El cliente manda la lista ya resuelta —qué alícuota va a qué hueco—
     * porque es quien tiene la rejilla de la caja pintada y quien conoce la
     * regla de llenado, que además va a cambiar con el uso. El servidor la
     * valida entera y la aplica en una sola transacción: si un hueco se ocupó
     * mientras el usuario decidía, no queda medio lote ubicado.</p>
     */
    @Transactional
    public List<Muestra> ubicarLote(Long idMuestraPadre, List<UbicacionAlicuotaDTO> asignaciones) {
        if (asignaciones == null || asignaciones.isEmpty()) {
            throw new ValidationException("No se indicó ninguna alícuota que ubicar.");
        }

        Muestra padre = muestraService.getByIdComoTenedor(idMuestraPadre);
        java.util.Set<Long> idsPosicion = new java.util.HashSet<>();
        List<Muestra> ubicadas = new java.util.ArrayList<>(asignaciones.size());
        List<String> detalles = new java.util.ArrayList<>(asignaciones.size());

        for (UbicacionAlicuotaDTO asignacion : asignaciones) {
            if (asignacion.getIdAlicuota() == null || asignacion.getIdPosicionCaja() == null) {
                throw new ValidationException("Cada alícuota del lote necesita una posición.");
            }
            if (!idsPosicion.add(asignacion.getIdPosicionCaja())) {
                throw new ValidationException(
                        "Se asignó la misma posición a más de una alícuota del lote.");
            }

            Muestra alicuota = muestraService.getByIdComoTenedor(asignacion.getIdAlicuota());
            if (alicuota.getMuestraPadre() == null
                    || !alicuota.getMuestraPadre().getId().equals(idMuestraPadre)) {
                throw new ValidationException(
                        "La muestra " + alicuota.getEtiqueta() + " no es alícuota de "
                        + padre.getEtiqueta() + ".");
            }

            Muestra actualizada = muestraService.asignarPosicion(
                    asignacion.getIdAlicuota(), asignacion.getIdPosicionCaja(), "Ubicación de lote");

            historialService.registrarEvento(actualizada, institucionContextService.getUsuarioActual(),
                    imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra.POSICION_ASIGNADA,
                    null, "PosicionCaja#" + asignacion.getIdPosicionCaja(), "Ubicación de lote", null);

            ubicadas.add(actualizada);
            detalles.add(descripcionPosicion(actualizada));
        }

        // Un solo descuento acumulado sobre la padre, no uno por vial.
        materializacionService.materializarLote(ubicadas, institucionContextService.getUsuarioActual(),
                resumenUbicaciones(detalles));

        return ubicadas;
    }

    /** Alícuotas de una padre que siguen sin ubicar: lo que el lote tiene pendiente. */
    @Transactional(readOnly = true)
    public List<Muestra> getAlicuotasPendientes(Long idMuestraPadre) {
        muestraService.getByIdConAcceso(idMuestraPadre);
        return muestraRepository
                .findAllByMuestraPadre_IdAndFechaMaterializacionIsNullOrderByNumeroAlicuotaAsc(idMuestraPadre);
    }

    /** Par alícuota → hueco para la ubicación en bloque. */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UbicacionAlicuotaDTO {
        private Long idAlicuota;
        private Long idPosicionCaja;
    }

    private String descripcionPosicion(Muestra muestra) {
        PosicionCaja pos = muestra.getPosicionCaja();
        if (pos == null) {
            return null;
        }
        String caja = pos.getCaja() != null ? pos.getCaja().getCodigoCaja() : "caja";
        return caja + " " + letraFila(pos.getFila()) + (pos.getColumna() != null ? pos.getColumna() : "");
    }

    private String resumenUbicaciones(List<String> detalles) {
        List<String> limpios = detalles.stream().filter(d -> d != null && !d.isBlank()).toList();
        if (limpios.isEmpty()) {
            return null;
        }
        if (limpios.size() <= 4) {
            return String.join(", ", limpios);
        }
        return limpios.get(0) + " … " + limpios.get(limpios.size() - 1);
    }

    /**
     * Fila en letra, como viene rotulada la caja física: 1→A, 26→Z, 27→AA.
     *
     * <p>Se recorre en base 26 en vez de sumar al carácter 'A' porque ese atajo
     * produce símbolos sueltos en cuanto una caja pasa de 26 filas.</p>
     */
    private static String letraFila(Integer fila) {
        if (fila == null || fila < 1) {
            return "";
        }
        int n = fila;
        StringBuilder etiqueta = new StringBuilder();
        while (n > 0) {
            n--;
            etiqueta.insert(0, (char) ('A' + (n % 26)));
            n = n / 26;
        }
        return etiqueta.toString();
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
        String prefijo = EtiquetaMuestra.prefijo(
                muestra.getTuboMuestra() != null ? muestra.getTuboMuestra().getPrefijoCodigo() : null);
        String folio = muestra.getPaciente().getFolio();
        int lote = muestraRepository.findMaxLoteByFolioAndTuboPrefix(folio, prefijo) + 1;
        muestra.setNumeroLote(lote);
        return EtiquetaMuestra.padre(prefijo, folio, muestra.getInstitucion().getId(), lote);
    }

    // ── Lotes de alícuotas ───────────────────────────────────────────────────

    /**
     * Previsualiza el lote que saldría de un tubo con un volumen dado, sin
     * crear nada. Lo consume el formulario de registro mientras se teclea la
     * cantidad extraída.
     */
    @Transactional(readOnly = true)
    public PlanAlicuotas previsualizarPlan(Long idTuboMuestra, Double valor) {
        TuboMuestra tubo = tipoMuestraService.getTuboById(idTuboMuestra);
        return PlanificadorAlicuotas.planificar(recetaDe(tubo), valor);
    }

    /**
     * Previsualiza el lote para una muestra padre que ya existe.
     *
     * <p>Planifica contra su volumen <em>disponible</em>, no contra su valor
     * bruto: lo ya prometido a alícuotas sin ubicar no se puede prometer dos
     * veces.</p>
     */
    @Transactional(readOnly = true)
    public PlanAlicuotas previsualizarPlanDeMuestra(Long idMuestraPadre, Long idTuboMuestra) {
        Muestra padre = muestraService.getByIdComoTenedor(idMuestraPadre);
        TuboMuestra tubo = tipoMuestraService.getTuboById(idTuboMuestra);
        PlanificadorAlicuotas.validarUnidad(tubo.getUnidadVolumen(), padre.getUnidad());

        // Contra los huecos que le quedan al lote, no contra el tubo entero: si
        // ya hay una alícuota hecha, pedir de nuevo el volumen del lote completo
        // reclamaría volumen que ya se gastó.
        int configuradas = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        int ocupados = configuradas - slotsLibresDelLote(
                idMuestraPadre, tubo, institucionContextService.getIdInstitucionActual()).size();

        return PlanificadorAlicuotas.planificar(recetaDe(tubo), padre.getValorDisponible(), ocupados);
    }

    /**
     * Genera un lote de alícuotas sobre una muestra padre ya registrada.
     *
     * <p>Cubre dos casos que antes eran uno solo mal nombrado: la unidad que
     * recibe una muestra en préstamo y la alicuota con su propia configuración,
     * y la unidad propietaria que no alicuotó al registrar —porque su tubo está
     * en manual, o porque entonces no hacía falta— y lo hace ahora.</p>
     */
    @Transactional
    public List<Muestra> generarLoteAlicuotas(Long idMuestraPadre, Long idTipoMuestra,
                                              Long idTuboMuestra, List<Double> planVolumenes) {
        Muestra padre = muestraService.getByIdComoTenedor(idMuestraPadre);
        Long idInst = institucionContextService.getIdInstitucionActual();

        if (!padre.getInstitucionActual().getId().equals(idInst)) {
            throw new ObjConflictException("La muestra no se encuentra actualmente en su institución.");
        }
        if (padre.getMuestraPadre() != null) {
            throw new ValidationException("Solo se pueden generar alícuotas de muestras padre.");
        }
        if (padre.getEstadoMuestra() == EstadoMuestra.PRESTADA) {
            throw new ObjConflictException(
                    "No se pueden generar alícuotas de una muestra en tránsito (PRESTADA). "
                    + "Confirma primero la recepción o espera a que la devolución se complete.");
        }
        if (padre.getEstadoMuestra() == EstadoMuestra.BAJA) {
            throw new ObjConflictException("La muestra está dada de baja; no se pueden generar más alícuotas.");
        }
        if (padre.isAgotada()) {
            throw new ObjConflictException(
                    "La muestra está agotada: ya no tiene volumen del que tomar alícuotas.");
        }

        TipoMuestra tipo = tipoMuestraService.getById(idTipoMuestra);
        TuboMuestra tubo = tipoMuestraService.getTuboById(idTuboMuestra);

        if (!tubo.getTipoMuestra().getId().equals(tipo.getId())) {
            throw new ValidationException("El tubo seleccionado no pertenece al tipo de muestra indicado.");
        }

        Institucion miInstitucion = institucionContextService.getInstitucionActual();

        // Un lote no se cierra al crearse. Si la extracción salió corta se hacen
        // las que alcanzan, y cuando aparece más volumen se completan las que
        // faltan. Lo que no se puede es pasar del número de huecos que el tubo
        // define, ni reutilizar uno ya ocupado.
        int configuradas = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        List<Integer> slotsLibres = slotsLibresDelLote(idMuestraPadre, tubo, miInstitucion.getId());
        if (slotsLibres.isEmpty()) {
            throw new ObjConflictException(
                    "El lote ya está completo: el tubo \"" + tubo.getNombre() + "\" define "
                    + configuradas + " alícuota(s) y todas existen en su biobanco.");
        }

        // Upsert tipo/tubo por institución: cada unidad puede alicuotar la misma
        // muestra padre con su propia receta.
        muestraTipoInstitucionService.asignarTipoTubo(idMuestraPadre, idTipoMuestra, idTuboMuestra);

        List<Muestra> generadas = generarLote(padre, tipo, tubo, planVolumenes, slotsLibres);
        if (generadas.isEmpty()) {
            throw new ValidationException(PlanificadorAlicuotas
                    .planificar(recetaDe(tubo), padre.getValorDisponible(), configuradas - slotsLibres.size())
                    .mensaje());
        }
        return generadas;
    }

    @Transactional(readOnly = true)
    public java.util.Optional<MuestraTipoInstitucion> getTipoInstitucion(Long idMuestra) {
        return muestraTipoInstitucionService.getByMuestraYMiInstitucion(idMuestra);
    }

    /**
     * Crea las alícuotas del lote y reserva su volumen en la muestra padre.
     *
     * <p>Las alícuotas nacen sin posición y sin descontar nada: su volumen queda
     * <em>comprometido</em> en la padre hasta que cada una se ubique. Ver
     * {@link MaterializacionAlicuotaService} para el porqué.</p>
     *
     * <p>Devuelve lista vacía —sin lanzar— cuando no se pidió un plan concreto y
     * el volumen no da ni para una alícuota completa: el alta de la muestra no
     * puede reventar porque la extracción saliera corta. Cuando el plan viene
     * explícito, en cambio, se valida y se rechaza si no cabe.</p>
     */
    private List<Muestra> generarLote(Muestra padre, TipoMuestra tipo, TuboMuestra tubo,
                                      List<Double> planVolumenes) {
        return generarLote(padre, tipo, tubo, planVolumenes,
                slotsLibresDelLote(padre.getId(), tubo, padre.getInstitucion().getId()));
    }

    /**
     * Numeros de alicuota que el lote todavia tiene libres, en orden.
     *
     * <p>Se calculan como los huecos del tubo que nadie ocupa, en lugar de
     * tomar el siguiente al mayor: si una alicuota se elimina, su hueco vuelve
     * a quedar disponible, y reutilizarlo mantiene la numeracion compacta sin
     * chocar nunca con la restriccion de etiqueta unica por institucion.</p>
     */
    private List<Integer> slotsLibresDelLote(Long idMuestraPadre, TuboMuestra tubo, Long idInstitucion) {
        int configuradas = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        java.util.Set<Integer> ocupados = muestraRepository
                .findAllByMuestraPadre_IdAndTuboMuestra_IdAndInstitucion_Id(
                        idMuestraPadre, tubo.getId(), idInstitucion)
                .stream()
                .map(Muestra::getNumeroAlicuota)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());

        List<Integer> libres = new java.util.ArrayList<>();
        for (int i = 1; i <= configuradas; i++) {
            if (!ocupados.contains(i)) {
                libres.add(i);
            }
        }
        return libres;
    }

    private List<Muestra> generarLote(Muestra padre, TipoMuestra tipo, TuboMuestra tubo,
                                      List<Double> planVolumenes, List<Integer> slotsLibres) {
        // Cuarentena: bloquear si el participante está inactivo.
        imss.gob.mx.cohorte.modules.almacenamiento.muestra.PacienteEstadoValidator
                .requirePacienteActivo(padre, "generar alícuotas");

        RecetaTubo receta = recetaDe(tubo);
        PlanificadorAlicuotas.validarUnidad(tubo.getUnidadVolumen(), padre.getUnidad());

        int configuradas = tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
        int ocupados = configuradas - slotsLibres.size();

        List<Double> volumenes;
        if (planVolumenes == null || planVolumenes.isEmpty()) {
            volumenes = PlanificadorAlicuotas.planificar(receta, padre.getValorDisponible(), ocupados)
                    .volumenesSugeridos();
            if (volumenes.isEmpty()) {
                return List.of();
            }
        } else {
            volumenes = PlanificadorAlicuotas.validarPlan(
                    planVolumenes, receta, padre.getValorDisponible(), ocupados);
        }

        BeanUser usuario = institucionContextService.getUsuarioActual();
        Institucion miInstitucion = institucionContextService.getInstitucionActual();
        String unidad = tubo.getUnidadVolumen() != null && !tubo.getUnidadVolumen().isBlank()
                ? tubo.getUnidadVolumen() : padre.getUnidad();

        String etiquetaBase = etiquetaBaseDelLote(padre, tubo, miInstitucion);
        int total = volumenes.size();
        int lote = padre.getNumeroLote() != null ? padre.getNumeroLote() : 1;

        List<Muestra> generadas = new java.util.ArrayList<>(total);
        for (int i = 1; i <= total; i++) {
            int slot = slotsLibres.get(i - 1);
            Muestra alicuota = new Muestra();
            // El número de alícuotas de la etiqueta es el del lote real, no el
            // que el tubo define: un "4-5" impreso sugeriría que existe un
            // quinto vial extraviado.
            alicuota.setEtiqueta(EtiquetaMuestra.alicuota(etiquetaBase, slot, configuradas));
            alicuota.setValor(volumenes.get(i - 1));
            alicuota.setUnidad(unidad);
            alicuota.setFechaRecoleccion(padre.getFechaRecoleccion());
            alicuota.setPaciente(padre.getPaciente());
            alicuota.setUsuarioRecolecta(usuario != null ? usuario : padre.getUsuarioRecolecta());
            alicuota.setTipoMuestra(tipo);
            alicuota.setTuboMuestra(tubo);
            alicuota.setMuestraPadre(padre);
            alicuota.setNumeroAlicuota(slot);
            alicuota.setTotalAlicuotas(configuradas);
            alicuota.setNumeroLote(lote);
            alicuota.setValorComprometido(0.0);
            alicuota.setInstitucion(miInstitucion);
            alicuota.setInstitucionActual(miInstitucion);
            alicuota.setFechaRegistro(Timestamp.valueOf(LocalDateTime.now()));
            generadas.add(muestraService.createAlicuota(alicuota));
        }

        double suma = PlanificadorAlicuotas.sumar(volumenes);
        materializacionService.reservar(padre, suma, usuario,
                detalleDelLote(tubo, total, suma, unidad, receta, ocupados));

        return generadas;
    }

    /**
     * Base de la etiqueta de las alícuotas.
     *
     * <p>Si las genera la propia dueña, cuelgan de la etiqueta de la padre. Si
     * las genera otra unidad, la base se reconstruye con el identificador de
     * <em>esa</em> unidad: la alícuota le pertenece a quien la creó, aunque la
     * padre sea de otra.</p>
     */
    private String etiquetaBaseDelLote(Muestra padre, TuboMuestra tubo, Institucion miInstitucion) {
        if (padre.getInstitucion() != null && miInstitucion.getId().equals(padre.getInstitucion().getId())) {
            return padre.getEtiqueta();
        }
        int lote = padre.getNumeroLote() != null ? padre.getNumeroLote() : 1;
        return EtiquetaMuestra.padre(tubo.getPrefijoCodigo(), padre.getPaciente().getFolio(),
                miInstitucion.getId(), lote);
    }

    /**
     * Motivo del renglon de historial al reservar un lote.
     *
     * <p>Se redacta corto a proposito: `historial_cambio_muestra.motivo` es un
     * VARCHAR(200) y un texto mas largo hacia fallar el alta entera de la
     * muestra. {@code HistorialCambioMuestraService} recorta como red de
     * seguridad, pero un motivo que llega recortado ya perdio informacion, asi
     * que conviene que quepa de origen.</p>
     */
    private String detalleDelLote(TuboMuestra tubo, int total, double suma, String unidad,
                                  RecetaTubo receta, int ocupados) {
        int configuradas = receta.numeroAlicuotas() == null ? 0 : receta.numeroAlicuotas();
        int faltan = configuradas - ocupados - total;

        StringBuilder sb = new StringBuilder();
        sb.append(ocupados > 0 ? "Lote completado con " : "Lote de ")
          .append(total).append(" alícuota").append(total == 1 ? "" : "s")
          .append(" (").append(PlanificadorAlicuotas.fmt(suma))
          .append(unidad == null || unidad.isBlank() ? "" : " " + unidad)
          .append(") del tubo «").append(tubo.getNombre()).append("»");

        if (faltan > 0) {
            sb.append(". Faltan ").append(faltan).append(" de ").append(configuradas)
              .append(": el volumen no alcanzaba");
        } else if (ocupados > 0) {
            sb.append(". Completo en ").append(configuradas);
        }
        sb.append(". Se descuenta al ubicar cada una.");
        return sb.toString();
    }

    private static RecetaTubo recetaDe(TuboMuestra tubo) {
        return new RecetaTubo(
                tubo.getNombre(),
                tubo.getNumeroAlicuotas(),
                tubo.getVolumenAlicuota(),
                tubo.getUnidadVolumen(),
                tubo.admiteAlicuotaParcial());
    }

    // ── Impresión ZPL ────────────────────────────────────────────────────────

    private ConfiguracionEtiqueta resolverConfig(Long configuracionId) {
        Long idInst = institucionContextService.getIdInstitucionActual();
        if (configuracionId != null) {
            return configuracionEtiquetaService.obtenerPorId(configuracionId, idInst);
        }
        ConfiguracionEtiqueta pred = configuracionEtiquetaService.obtenerPredeterminada(idInst);
        if (pred == null) {
            throw new IllegalArgumentException("No hay configuración de etiqueta predeterminada. Cree una en Configuración > Etiquetas y márquela como predeterminada.");
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

    /**
     * ZPL con las etiquetas en los carriles que eligió el operador.
     *
     * Cada posición de {@code slots} es un carril del rollo, en orden de avance;
     * las nulas quedan en blanco. Se resuelve cada muestra con la comprobación de
     * acceso habitual: el acomodo llega del navegador y no puede servir para
     * imprimir etiquetas de muestras que no estén en el biobanco de quien lo pide.
     */
    @Transactional(readOnly = true)
    public ZplLoteResponseDTO generarZplAcomodado(List<Long> slots, Long configuracionId,
                                                   boolean marcoDepuracion) {
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);

        List<Muestra> acomodo = new java.util.ArrayList<>(slots.size());
        int impresas = 0;
        for (Long id : slots) {
            if (id == null) {
                acomodo.add(null);
            } else {
                acomodo.add(muestraService.getByIdConAcceso(id));
                impresas++;
            }
        }
        if (impresas == 0) {
            throw new ValidationException("El acomodo no tiene ninguna etiqueta que imprimir.");
        }

        String zpl = zplLabelService.generarZplAcomodado(acomodo, config, marcoDepuracion);
        return new ZplLoteResponseDTO(zpl, impresas);
    }

    // ── Datos estructurados para impresión por navegador ──────────────────

    @Transactional(readOnly = true)
    public PrintableLabelBatchDTO obtenerDatosEtiqueta(Long idMuestra, Long configuracionId) {
        Muestra muestra = muestraService.getById(idMuestra);
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        return PrintableLabelBatchDTO.builder()
                .configuracion(ConfiguracionEtiquetaMapper.toResponseDTO(config))
                .etiquetas(List.of(zplLabelService.extraerDatosMuestra(muestra)))
                .build();
    }

    @Transactional(readOnly = true)
    public PrintableLabelBatchDTO obtenerDatosAlicuotas(Long idMuestraPadre, Long configuracionId) {
        ConfiguracionEtiqueta config = resolverConfig(configuracionId);
        List<Muestra> alicuotas = muestraService.getAlicuotas(idMuestraPadre);
        return PrintableLabelBatchDTO.builder()
                .configuracion(ConfiguracionEtiquetaMapper.toResponseDTO(config))
                .etiquetas(zplLabelService.extraerDatosMuestras(alicuotas))
                .build();
    }

    @Transactional(readOnly = true)
    public PrintableLabelBatchDTO obtenerDatosLoteCompleto(Long idMuestraPadre, Long configuracionId) {
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
        return PrintableLabelBatchDTO.builder()
                .configuracion(ConfiguracionEtiquetaMapper.toResponseDTO(config))
                .etiquetas(zplLabelService.extraerDatosMuestras(aImprimir))
                .build();
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
