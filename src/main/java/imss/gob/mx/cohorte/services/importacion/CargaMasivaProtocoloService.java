package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenicaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.EtiquetaPosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCajaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.lote.Lote;
import imss.gob.mx.cohorte.modules.almacenamiento.lote.LoteRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.AccionTubo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.Protocolo;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.TuboProtocolo;
import imss.gob.mx.cohorte.modules.almacenamiento.toma.Toma;
import imss.gob.mx.cohorte.modules.almacenamiento.traslado.TrasladoMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.EtiquetaMuestra;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PlanificadorAlicuotas;
import imss.gob.mx.cohorte.services.almacenamiento.protocolo.ProtocoloService;
import imss.gob.mx.cohorte.services.almacenamiento.toma.TomaService;
import imss.gob.mx.cohorte.services.almacenamiento.traslado.TrasladoMuestraService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@RequiredArgsConstructor
public class CargaMasivaProtocoloService {

    private final LectorArchivoTabular lector;
    private final ProtocoloService protocoloService;
    private final PacienteRepository pacienteRepository;
    private final MuestraRepository muestraRepository;
    private final LoteRepository loteRepository;
    private final CajaCriogenicaRepository cajaRepository;
    private final PosicionCajaRepository posicionCajaRepository;
    private final InstitucionContextService institucionContextService;
    private final TomaService tomaService;
    private final TrasladoMuestraService trasladoMuestraService;

    // ── API pública ──────────────────────────────────────────────────────────

    public List<String> listarHojas(MultipartFile archivo) {
        List<LectorArchivoTabular.HojaLeida> hojas =
                lector.leerHojas(archivo, LectorArchivoTabular.Topes.porOmision());
        return hojas.stream().map(LectorArchivoTabular.HojaLeida::nombre).toList();
    }

    @Transactional(readOnly = true)
    public ResultadoCargaProtocolo previsualizar(MultipartFile archivo, Long idProtocolo, String fechaPorOmision) {
        return analizarTodo(contenidoDe(archivo), Map.of("", idProtocolo), fechaPorOmision, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultadoCargaProtocolo confirmar(MultipartFile archivo, Long idProtocolo, String fechaPorOmision) {
        return analizarTodo(contenidoDe(archivo), Map.of("", idProtocolo), fechaPorOmision, true);
    }

    @Transactional(readOnly = true)
    public ResultadoCargaProtocolo previsualizarMultiHoja(MultipartFile archivo,
                                                          Map<String, Long> hojaProtocolo,
                                                          String fechaPorOmision) {
        return analizarTodo(contenidoDe(archivo), hojaProtocolo, fechaPorOmision, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultadoCargaProtocolo confirmarMultiHoja(MultipartFile archivo,
                                                      Map<String, Long> hojaProtocolo,
                                                      String fechaPorOmision) {
        return analizarTodo(contenidoDe(archivo), hojaProtocolo, fechaPorOmision, true);
    }

    // ── Núcleo ───────────────────────────────────────────────────────────────

    private ResultadoCargaProtocolo analizarTodo(byte[] contenido, Map<String, Long> hojaProtocolo,
                                                  String fechaPorOmision, boolean escribir) {
        Institucion inst = institucionContextService.getInstitucionActual();
        BeanUser usuario = institucionContextService.getUsuarioActual();
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());

        List<LectorArchivoTabular.HojaLeida> hojas =
                lector.leerHojas(contenido, LectorArchivoTabular.Topes.porOmision());

        List<String> errores = new ArrayList<>();
        List<String> avisos = new ArrayList<>();
        Set<String> procesados = new HashSet<>();
        Contadores c = new Contadores();
        Map<Long, List<TrasladoMuestra>> trasladosPorDestino = new LinkedHashMap<>();

        for (LectorArchivoTabular.HojaLeida hoja : hojas) {
            Long idProtocolo = hojaProtocolo.get(hoja.nombre());
            if (idProtocolo == null && hojaProtocolo.containsKey("")) {
                idProtocolo = hojaProtocolo.get("");
            }
            if (idProtocolo == null) continue;

            Protocolo protocolo = protocoloService.getById(idProtocolo);
            String prefHoja = hojas.size() > 1 ? "Hoja «" + hoja.nombre() + "»: " : "";

            analizarHoja(hoja.tabla(), protocolo, fechaPorOmision, escribir,
                    inst, usuario, ahora, procesados, c, trasladosPorDestino,
                    errores, avisos, prefHoja);
        }

        if (escribir && !errores.isEmpty()) {
            throw new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException(
                    "La carga tiene " + errores.size() + " error(es); no se escribió nada.");
        }

        if (escribir) {
            for (List<TrasladoMuestra> grupo : trasladosPorDestino.values()) {
                if (!grupo.isEmpty()) {
                    trasladoMuestraService.enviarEmailPrestamoEnviado(
                            grupo, grupo.get(0).getInstitucionDestino());
                }
            }
        }

        return new ResultadoCargaProtocolo(c.procesamientos, c.padres, c.alicuotas,
                c.lotes, c.ubicadas, c.transferencias, errores, avisos);
    }

    private void analizarHoja(TablaLeida tabla, Protocolo protocolo, String fechaPorOmision,
                              boolean escribir, Institucion inst, BeanUser usuario, Timestamp ahora,
                              Set<String> procesados, Contadores c,
                              Map<Long, List<TrasladoMuestra>> trasladosPorDestino,
                              List<String> errores, List<String> avisos, String prefHoja) {

        List<TuboProtocolo> tubosActivos = new ArrayList<>();
        List<TuboProtocolo> tubosAlic = new ArrayList<>();
        for (TuboProtocolo t : protocolo.getTubos()) {
            if (!Boolean.TRUE.equals(t.getActivo())) continue;
            tubosActivos.add(t);
            if (t.getAccion() == AccionTubo.ALICUOTAR) tubosAlic.add(t);
        }
        tubosActivos.sort(Comparator.comparingInt(t -> t.getOrden() != null ? t.getOrden() : 0));
        tubosAlic.sort(Comparator.comparingInt(t -> t.getOrden() != null ? t.getOrden() : 0));

        List<RangoTubo> rangos = construirRangos(tubosAlic);
        int totalEsperado = rangos.isEmpty() ? 0 : rangos.get(rangos.size() - 1).hasta;

        Cols cols = colIndices(tabla.encabezados(), errores, prefHoja);
        if (cols == null || tabla.vacia()) {
            if (tabla.vacia()) errores.add(prefHoja + "La hoja no tiene filas de datos.");
            return;
        }

        Map<String, List<FilaAlic>> porFolio = new LinkedHashMap<>();
        List<List<String>> filas = tabla.filas();
        int autoNumero = 0;
        String folioAnterior = null;

        for (int i = 0; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            int numFila = tabla.numerosDeFila().get(i);
            String folio = celda(fila, cols.folio);
            if (folio == null || folio.isBlank()) {
                errores.add(prefHoja + "Fila " + numFila + ": falta el folio.");
                continue;
            }
            folio = folio.trim();

            if (!folio.equals(folioAnterior)) { autoNumero = 0; folioAnterior = folio; }
            autoNumero++;

            Integer numAlic = cols.numeroAlicuota >= 0 ? parseInt(celda(fila, cols.numeroAlicuota)) : null;
            if (numAlic == null) numAlic = autoNumero;

            Double volumen = parseDouble(celda(fila, cols.volumen));
            if (volumen == null) {
                errores.add(prefHoja + "Fila " + numFila + ": volumen vacío o inválido.");
                continue;
            }
            if (volumen < 0) {
                errores.add(prefHoja + "Fila " + numFila + ": el volumen no puede ser negativo.");
                continue;
            }

            FilaAlic f = new FilaAlic();
            f.numFila = numFila;
            f.folio = folio;
            f.numeroAlicuota = numAlic;
            f.volumen = volumen;
            f.unidad = celda(fila, cols.unidad);
            f.codigoCaja = celda(fila, cols.caja);
            f.posTexto = celda(fila, cols.posicion);
            f.fecha = celda(fila, cols.fecha);
            f.observaciones = celda(fila, cols.observaciones);
            porFolio.computeIfAbsent(f.folio, k -> new ArrayList<>()).add(f);
        }

        for (Map.Entry<String, List<FilaAlic>> e : porFolio.entrySet()) {
            String folio = e.getKey();
            List<FilaAlic> filasFolio = e.getValue();

            String clave = folio + "|" + protocolo.getId();
            if (procesados.contains(clave)) {
                errores.add(prefHoja + "Folio " + folio + ": aparece en más de una hoja para el mismo protocolo.");
                continue;
            }

            Paciente paciente = resolverPaciente(folio);
            if (paciente == null) {
                errores.add(prefHoja + "Folio " + folio + ": participante no encontrado en la institución.");
                continue;
            }
            if (!Boolean.TRUE.equals(paciente.getActivo())) {
                errores.add(prefHoja + "Folio " + folio + ": participante inactivo.");
                continue;
            }
            if (muestraRepository.existsByPaciente_IdAndInstitucion_IdAndTuboProtocolo_Protocolo_IdAndMuestraPadreIsNull(
                    paciente.getId(), inst.getId(), protocolo.getId())) {
                errores.add(prefHoja + "Folio " + folio + ": ya fue procesado con «" + protocolo.getNombre() + "».");
                continue;
            }

            LocalDateTime fechaToma = resolverFecha(filasFolio, fechaPorOmision);
            if (fechaToma == null) {
                errores.add(prefHoja + "Folio " + folio + ": no se pudo determinar la fecha (ni en el archivo ni por omisión).");
                continue;
            }

            boolean errorEnRango = false;
            Map<Integer, FilaAlic> porNumero = new LinkedHashMap<>();
            for (FilaAlic f : filasFolio) {
                if (f.numeroAlicuota < 1 || f.numeroAlicuota > totalEsperado) {
                    errores.add(prefHoja + "Fila " + f.numFila + ": número de alícuota " + f.numeroAlicuota
                            + " fuera del rango esperado (1-" + totalEsperado + ").");
                    errorEnRango = true;
                    continue;
                }
                if (porNumero.containsKey(f.numeroAlicuota)) {
                    errores.add(prefHoja + "Fila " + f.numFila + ": número de alícuota " + f.numeroAlicuota + " duplicado.");
                    errorEnRango = true;
                    continue;
                }
                porNumero.put(f.numeroAlicuota, f);
            }
            if (errorEnRango) continue;

            procesados.add(clave);
            c.procesamientos++;

            Toma toma = null;
            if (escribir) {
                toma = tomaService.findOrCreate(paciente, protocolo.getTipoOrigen(),
                        fechaToma, inst, usuario, null);
            }

            // Crear TODOS los padres del protocolo
            Map<Integer, Muestra> padrePorOrden = new LinkedHashMap<>();
            for (TuboProtocolo tubo : tubosActivos) {
                int orden = tubo.getOrden() != null ? tubo.getOrden() : 1;
                boolean esAlicuotar = tubo.getAccion() == AccionTubo.ALICUOTAR;

                Muestra padre = new Muestra();
                padre.setEtiqueta(etiquetaLibre(
                        EtiquetaMuestra.primario(tubo.getPrefijoCodigo(), folio, inst.getId(), orden),
                        inst.getId()));
                padre.setNumeroLote(orden);
                padre.setPaciente(paciente);
                padre.setUsuarioRecolecta(usuario);
                padre.setInstitucion(inst);
                padre.setInstitucionActual(inst);
                padre.setTipoMuestra(protocolo.getTipoOrigen());
                padre.setTuboProtocolo(tubo);
                padre.setToma(toma);
                padre.setFechaRecoleccion(fechaToma);
                padre.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
                padre.setFechaRegistro(ahora);
                padre.setUnidad(tubo.getUnidadVolumen());

                if (esAlicuotar) {
                    padre.setValor(0.0);
                    padre.setValorComprometido(0.0);
                    padre.setFechaAgotamiento(ahora);
                } else {
                    double vol = tubo.getVolumenEsperado() != null ? tubo.getVolumenEsperado() : 0.0;
                    padre.setValor(vol);
                    padre.setValorComprometido(0.0);
                    if (PlanificadorAlicuotas.agotado(vol)) padre.setFechaAgotamiento(ahora);
                }

                if (escribir) padre = muestraRepository.save(padre);
                padrePorOrden.put(orden, padre);
                c.padres++;
            }

            // Lotes y alícuotas para tubos ALICUOTAR
            Map<String, Lote> lotesPorClave = new LinkedHashMap<>();
            Map<String, Integer> contadorEnLote = new LinkedHashMap<>();
            int siguienteNumLote = 0;

            for (RangoTubo rango : rangos) {
                TuboProtocolo tubo = rango.tubo;
                int orden = tubo.getOrden() != null ? tubo.getOrden() : 1;
                Muestra padre = padrePorOrden.get(orden);
                String claveLote = claveLote(tubo);

                Lote lote = lotesPorClave.get(claveLote);
                if (lote == null) {
                    siguienteNumLote++;
                    lote = new Lote();
                    lote.setInstitucion(inst);
                    lote.setPaciente(paciente);
                    lote.setTipoResultante(tubo.getTipoResultante());
                    lote.setProtocolo(protocolo);
                    lote.setToma(toma);
                    lote.setNumeroLote(siguienteNumLote);
                    lote.setUsuarioProcesa(usuario);
                    lote.setFechaCreacion(ahora);
                    if (escribir) lote = loteRepository.save(lote);
                    lotesPorClave.put(claveLote, lote);
                    contadorEnLote.put(claveLote, 0);
                    c.lotes++;
                }

                int totalEnLote = totalParaLote(claveLote, rangos);

                for (int num = rango.desde; num <= rango.hasta; num++) {
                    int posEnLote = contadorEnLote.merge(claveLote, 1, Integer::sum);
                    FilaAlic f = porNumero.get(num);

                    Muestra a = new Muestra();
                    a.setEtiqueta(etiquetaLibre(
                            EtiquetaMuestra.alicuotaLote(tubo.getPrefijoCodigo(), folio, inst.getId(),
                                    lote.getNumeroLote(), posEnLote, totalEnLote),
                            inst.getId()));
                    a.setPaciente(paciente);
                    a.setUsuarioRecolecta(usuario);
                    a.setInstitucion(inst);
                    a.setInstitucionActual(inst);
                    a.setTipoMuestra(tubo.getTipoResultante());
                    a.setTuboProtocolo(tubo);
                    a.setToma(toma);
                    a.setMuestraPadre(padre);
                    a.setLote(lote);
                    a.setNumeroAlicuota(num);
                    a.setTotalAlicuotas(totalEsperado);
                    a.setNumeroEnLote(posEnLote);
                    a.setNumeroLote(lote.getNumeroLote());
                    a.setFechaRecoleccion(fechaToma);
                    a.setFechaRegistro(ahora);

                    if (f != null) {
                        a.setValor(f.volumen);
                        a.setUnidad(f.unidad != null && !f.unidad.isBlank() ? f.unidad : tubo.getUnidadVolumen());
                        a.setObservaciones(f.observaciones);
                        a.setValorComprometido(0.0);

                        if (PlanificadorAlicuotas.agotado(f.volumen)) {
                            a.setFechaAgotamiento(ahora);
                        }

                        PosicionCaja posicion = resolverPosicion(f, inst, errores, prefHoja);
                        if (posicion != null) {
                            a.setPosicionCaja(posicion);
                            a.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
                            a.setFechaMaterializacion(ahora);
                            a.setCantidadDescontadaPadre(f.volumen);
                            if (escribir) { posicion.setOcupada(true); posicionCajaRepository.save(posicion); }
                            c.ubicadas++;
                        } else {
                            a.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
                        }
                    } else {
                        a.setValor(0.0);
                        a.setUnidad(tubo.getUnidadVolumen());
                        a.setObservaciones("No recolectada");
                        a.setValorComprometido(0.0);
                        a.setFechaAgotamiento(ahora);
                        a.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
                        avisos.add(prefHoja + "Folio " + folio + ": alícuota " + num
                                + " no venía en el archivo; se crea como agotada.");
                    }

                    if (escribir) muestraRepository.save(a);
                    c.alicuotas++;
                }
            }

            // Transferencias automáticas para tubos TRANSFERIR
            for (TuboProtocolo tubo : tubosActivos) {
                if (tubo.getAccion() != AccionTubo.TRANSFERIR) continue;
                int orden = tubo.getOrden() != null ? tubo.getOrden() : 1;
                Muestra padre = padrePorOrden.get(orden);

                Long idDestino = tubo.getInstitucionDestino() != null
                        ? tubo.getInstitucionDestino().getId() : null;
                if (idDestino == null) {
                    avisos.add(prefHoja + "Folio " + folio + ": tubo «" + tubo.getNombre()
                            + "» configurado para transferir pero sin institución destino.");
                    continue;
                }
                if (PlanificadorAlicuotas.agotado(padre.getValor())) {
                    avisos.add(prefHoja + "Folio " + folio + ": tubo «" + tubo.getNombre()
                            + "» sin volumen; no se transfiere.");
                    continue;
                }

                c.transferencias++;
                if (escribir) {
                    String motivo = tubo.getMotivoTransferencia() != null
                            && !tubo.getMotivoTransferencia().isBlank()
                            ? tubo.getMotivoTransferencia()
                            : "Transferencia automática — protocolo «" + protocolo.getNombre() + "»";
                    List<TrasladoMuestra> creados = trasladoMuestraService.iniciarPrestamo(
                            List.of(padre.getId()), inst.getId(), idDestino,
                            usuario.getUUID(), motivo, null, null, false);
                    trasladosPorDestino.computeIfAbsent(idDestino, k -> new ArrayList<>()).addAll(creados);
                }
            }
        }
    }

    // ── Rangos de alícuotas ──────────────────────────────────────────────────

    private record RangoTubo(TuboProtocolo tubo, int desde, int hasta) {}

    private List<RangoTubo> construirRangos(List<TuboProtocolo> tubosAlic) {
        List<RangoTubo> rangos = new ArrayList<>();
        int acum = 0;
        for (TuboProtocolo t : tubosAlic) {
            int cupo = t.getNumeroAlicuotas() != null ? t.getNumeroAlicuotas() : 0;
            if (cupo <= 0) continue;
            rangos.add(new RangoTubo(t, acum + 1, acum + cupo));
            acum += cupo;
        }
        return rangos;
    }

    private int totalParaLote(String claveLote, List<RangoTubo> rangos) {
        int total = 0;
        for (RangoTubo r : rangos) {
            if (claveLote(r.tubo).equals(claveLote)) {
                total += r.hasta - r.desde + 1;
            }
        }
        return total;
    }

    private String claveLote(TuboProtocolo tubo) {
        Long tipoRes = tubo.getTipoResultante() != null ? tubo.getTipoResultante().getId() : null;
        return tubo.esAgruparEnLote() ? "G:" + tipoRes : "S:" + tubo.getId();
    }

    // ── Columnas ─────────────────────────────────────────────────────────────

    private record Cols(int folio, int numeroAlicuota, int volumen, int unidad,
                        int fecha, int caja, int posicion, int observaciones) {}

    private Cols colIndices(List<String> enc, List<String> errores, String prefHoja) {
        int folio = -1, numAlic = -1, vol = -1, uni = -1, fecha = -1, caja = -1, pos = -1, obs = -1;
        for (int i = 0; i < enc.size(); i++) {
            String h = enc.get(i) == null ? "" : enc.get(i).trim().toLowerCase()
                    .replace("á", "a").replace("é", "e").replace("í", "i")
                    .replace("ó", "o").replace("ú", "u");
            if (h.equals("folio")) folio = i;
            else if (h.equals("numero_alicuota") || h.equals("numeroalicuota")
                    || h.equals("numero alicuota") || h.equals("alicuota") || h.equals("no.")) numAlic = i;
            else if (h.startsWith("volumen") || h.equals("vol")) vol = i;
            else if (h.startsWith("unidad")) uni = i;
            else if (h.startsWith("fecha")) fecha = i;
            else if (h.startsWith("caja") || h.equals("codigocaja") || h.equals("codigo caja")
                    || h.equals("codigo_caja")) caja = i;
            else if (h.startsWith("posicion")) pos = i;
            else if (h.startsWith("observacion") || h.equals("obs")) obs = i;
        }
        if (folio < 0) errores.add(prefHoja + "Falta la columna obligatoria «folio».");
        if (vol < 0) errores.add(prefHoja + "Falta la columna obligatoria «volumen».");
        return (folio >= 0 && vol >= 0) ? new Cols(folio, numAlic, vol, uni, fecha, caja, pos, obs) : null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private LocalDateTime resolverFecha(List<FilaAlic> filas, String fechaPorOmision) {
        for (FilaAlic f : filas) {
            if (f.fecha != null && !f.fecha.isBlank()) {
                LocalDateTime dt = parseFecha(f.fecha);
                if (dt != null) return dt;
            }
        }
        if (fechaPorOmision != null && !fechaPorOmision.isBlank()) {
            return parseFecha(fechaPorOmision);
        }
        return null;
    }

    private static final DateTimeFormatter[] FORMATOS_FECHA = {
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
    };

    private LocalDateTime parseFecha(String texto) {
        if (texto == null || texto.isBlank()) return null;
        texto = texto.trim();
        for (DateTimeFormatter fmt : FORMATOS_FECHA) {
            try {
                return LocalDateTime.parse(texto, fmt);
            } catch (DateTimeParseException ignored) {}
            try {
                return LocalDate.parse(texto, fmt).atStartOfDay();
            } catch (DateTimeParseException ignored) {}
        }
        return null;
    }

    private Paciente resolverPaciente(String folio) {
        Long idInst = institucionContextService.getIdInstitucionActual();
        Paciente p = pacienteRepository.findByFolioAndInstitucion_Id(folio, idInst).orElse(null);
        if (p == null) {
            String padded = folio.chars().allMatch(Character::isDigit)
                    ? String.format("%06d", Integer.parseInt(folio)) : folio;
            if (!padded.equals(folio))
                p = pacienteRepository.findByFolioAndInstitucion_Id(padded, idInst).orElse(null);
        }
        return p;
    }

    private PosicionCaja resolverPosicion(FilaAlic f, Institucion inst,
                                          List<String> errores, String prefHoja) {
        if (f.codigoCaja == null || f.codigoCaja.isBlank()
                || f.posTexto == null || f.posTexto.isBlank()) return null;
        CajaCriogenica caja = cajaRepository.findByCodigoCajaAndInstitucion_Id(
                f.codigoCaja.trim(), inst.getId()).orElse(null);
        if (caja == null) {
            errores.add(prefHoja + "Fila " + f.numFila + ": caja «" + f.codigoCaja + "» no existe.");
            return null;
        }
        EtiquetaPosicionCaja.Coordenada coord;
        try { coord = EtiquetaPosicionCaja.parsear(f.posTexto.trim()); }
        catch (RuntimeException ex) {
            errores.add(prefHoja + "Fila " + f.numFila + ": posición «" + f.posTexto + "» inválida.");
            return null;
        }
        PosicionCaja pos = posicionCajaRepository.findByCaja_IdAndFilaAndColumna(
                caja.getId(), coord.fila(), coord.columna()).orElse(null);
        if (pos == null) {
            errores.add(prefHoja + "Fila " + f.numFila + ": la posición " + f.posTexto + " no existe en la caja.");
            return null;
        }
        if (Boolean.TRUE.equals(pos.getOcupada())) {
            errores.add(prefHoja + "Fila " + f.numFila + ": la posición " + f.codigoCaja + " " + f.posTexto + " ya está ocupada.");
            return null;
        }
        return pos;
    }

    private String etiquetaLibre(String base, Long idInstitucion) {
        if (muestraRepository.findByEtiquetaIgnoreCaseAndInstitucion_Id(base, idInstitucion).isEmpty()) {
            return base;
        }
        for (int n = 2; n < 1000; n++) {
            String candidata = base + "." + n;
            if (muestraRepository.findByEtiquetaIgnoreCaseAndInstitucion_Id(candidata, idInstitucion).isEmpty()) {
                return candidata;
            }
        }
        throw new ArchivoInvalidoException("No se encontró una etiqueta libre para «" + base + "».");
    }

    private byte[] contenidoDe(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("No se recibió ningún archivo.");
        }
        try { return archivo.getBytes(); }
        catch (IOException e) { throw new ArchivoInvalidoException("No se pudo leer el archivo."); }
    }

    private static String celda(List<String> fila, int idx) {
        if (idx < 0 || idx >= fila.size()) return null;
        String v = fila.get(idx);
        return v == null ? null : v.trim();
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim().replace(",", ".")); }
        catch (NumberFormatException e) { return null; }
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return null; }
    }

    // ── Modelos internos ─────────────────────────────────────────────────────

    private static final class FilaAlic {
        int numFila;
        String folio;
        int numeroAlicuota;
        Double volumen;
        String unidad;
        String codigoCaja;
        String posTexto;
        String fecha;
        String observaciones;
    }

    private static final class Contadores {
        int procesamientos, padres, alicuotas, lotes, ubicadas, transferencias;
    }
}
