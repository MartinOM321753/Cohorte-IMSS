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
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.EtiquetaMuestra;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PlanificadorAlicuotas;
import imss.gob.mx.cohorte.services.almacenamiento.protocolo.ProtocoloService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Carga masiva asociada a un <b>protocolo</b>. El archivo trae ALÍCUOTAS; los
 * tubos primarios (T1…TN) se generan a partir de la configuración del protocolo,
 * y de cada alícuota se conserva de qué tubo salió. Es el equivalente, para el
 * flujo nuevo, de {@code CargaMasivaMuestrasService} (que sigue vivo para el
 * formato anterior basado en tipo+tubo).
 *
 * <p>Columnas de la plantilla: {@code folio}, {@code tubo} (número T del tubo
 * primario; si falta, se infiere por orden), {@code lote} (número L; por omisión
 * 1), {@code volumen}, {@code unidad} (opcional), {@code caja} y {@code posicion}
 * (opcionales, p. ej. A1), {@code fecha} (opcional).</p>
 */
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

    @Transactional(readOnly = true)
    public ResultadoCargaProtocolo previsualizar(MultipartFile archivo, Long idProtocolo, String fechaPorOmision) {
        return analizar(lector.leer(archivo), idProtocolo, fechaPorOmision, false);
    }

    @Transactional(rollbackFor = Exception.class)
    public ResultadoCargaProtocolo confirmar(MultipartFile archivo, Long idProtocolo, String fechaPorOmision) {
        return analizar(lector.leer(archivo), idProtocolo, fechaPorOmision, true);
    }

    // ── Análisis / escritura ──────────────────────────────────────────────────

    private ResultadoCargaProtocolo analizar(TablaLeida tabla, Long idProtocolo, String fechaPorOmision, boolean escribir) {
        Institucion inst = institucionContextService.getInstitucionActual();
        BeanUser usuario = institucionContextService.getUsuarioActual();
        Protocolo protocolo = protocoloService.getById(idProtocolo);

        // Tubos del protocolo por orden (T).
        Map<Integer, TuboProtocolo> tuboPorOrden = new LinkedHashMap<>();
        List<TuboProtocolo> tubosAlic = new ArrayList<>();
        for (TuboProtocolo t : protocolo.getTubos()) {
            if (Boolean.TRUE.equals(t.getActivo())) {
                int o = t.getOrden() != null ? t.getOrden() : 0;
                tuboPorOrden.put(o, t);
                if (t.getAccion() == AccionTubo.ALICUOTAR) tubosAlic.add(t);
            }
        }
        tubosAlic.sort((a, b) -> (a.getOrden() == null ? 0 : a.getOrden()) - (b.getOrden() == null ? 0 : b.getOrden()));

        List<String> errores = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        Cols cols = colIndices(tabla.encabezados(), errores);
        if (cols == null || tabla.vacia()) {
            if (tabla.vacia()) errores.add("El archivo no tiene filas.");
            return new ResultadoCargaProtocolo(0, 0, 0, 0, 0, errores, avisos);
        }

        // Parseo de filas → agrupadas por folio.
        Map<String, List<FilaAlic>> porFolio = new LinkedHashMap<>();
        List<List<String>> filas = tabla.filas();
        for (int i = 0; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            int numFila = tabla.numerosDeFila().get(i);
            String folio = celda(fila, cols.folio);
            if (folio == null || folio.isBlank()) { errores.add("Fila " + numFila + ": falta el folio."); continue; }
            Double volumen = parseDouble(celda(fila, cols.volumen));
            if (volumen == null || volumen <= 0) { errores.add("Fila " + numFila + ": volumen inválido."); continue; }
            Integer tuboT = parseInt(celda(fila, cols.tubo));
            Integer loteL = parseInt(celda(fila, cols.lote));
            FilaAlic f = new FilaAlic();
            f.numFila = numFila;
            f.folio = folio.trim();
            f.tuboT = tuboT;
            f.loteL = loteL != null ? loteL : 1;
            f.volumen = volumen;
            f.unidad = celda(fila, cols.unidad);
            f.codigoCaja = celda(fila, cols.caja);
            f.posTexto = celda(fila, cols.posicion);
            porFolio.computeIfAbsent(f.folio, k -> new ArrayList<>()).add(f);
        }

        int nProc = 0, nPadres = 0, nAlic = 0, nLotes = 0, nUbic = 0;
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());

        for (Map.Entry<String, List<FilaAlic>> e : porFolio.entrySet()) {
            String folio = e.getKey();
            List<FilaAlic> filasFolio = e.getValue();

            Paciente paciente = resolverPaciente(folio);
            if (paciente == null) { errores.add("Folio " + folio + ": participante no encontrado en la institución."); continue; }
            if (!Boolean.TRUE.equals(paciente.getActivo())) { errores.add("Folio " + folio + ": participante inactivo."); continue; }
            if (muestraRepository.existsByPaciente_IdAndInstitucion_IdAndTuboProtocolo_Protocolo_IdAndMuestraPadreIsNull(
                    paciente.getId(), inst.getId(), protocolo.getId())) {
                errores.add("Folio " + folio + ": ya fue procesado con el protocolo «" + protocolo.getNombre() + "».");
                continue;
            }

            // Inferir tubo (T) cuando falta: repartir en orden entre los tubos ALICUOTAR.
            inferirTubos(filasFolio, tubosAlic, errores);
            if (tieneErrorDeFolio(filasFolio, tuboPorOrden, errores, folio)) continue;

            nProc++;

            // Crear padres sintéticos por tubo T usado.
            Map<Integer, Muestra> padrePorTubo = new LinkedHashMap<>();
            Map<Integer, List<FilaAlic>> porTubo = new LinkedHashMap<>();
            for (FilaAlic f : filasFolio) porTubo.computeIfAbsent(f.tuboT, k -> new ArrayList<>()).add(f);

            for (Map.Entry<Integer, List<FilaAlic>> te : porTubo.entrySet()) {
                TuboProtocolo tubo = tuboPorOrden.get(te.getKey());
                double pendiente = 0.0, repartido = 0.0;
                for (FilaAlic f : te.getValue()) {
                    repartido = PlanificadorAlicuotas.sumar(repartido, f.volumen);
                    if (f.posTexto == null || f.posTexto.isBlank()) pendiente = PlanificadorAlicuotas.sumar(pendiente, f.volumen);
                }
                Muestra padre = new Muestra();
                padre.setEtiqueta(EtiquetaMuestra.primario(tubo.getPrefijoCodigo(), folio, inst.getId(), te.getKey()));
                padre.setNumeroLote(te.getKey());
                padre.setPaciente(paciente);
                padre.setUsuarioRecolecta(usuario);
                padre.setInstitucion(inst);
                padre.setInstitucionActual(inst);
                padre.setTipoMuestra(protocolo.getTipoOrigen());
                padre.setTuboProtocolo(tubo);
                padre.setUnidad(tubo.getUnidadVolumen());
                padre.setValor(pendiente);
                padre.setValorComprometido(pendiente);
                padre.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
                padre.setFechaRegistro(ahora);
                if (PlanificadorAlicuotas.agotado(pendiente)) padre.setFechaAgotamiento(ahora);
                if (escribir) padre = muestraRepository.save(padre);
                padrePorTubo.put(te.getKey(), padre);
                nPadres++;
            }

            // Crear lotes por L y sus alícuotas.
            Map<Integer, List<FilaAlic>> porLote = new LinkedHashMap<>();
            for (FilaAlic f : filasFolio) porLote.computeIfAbsent(f.loteL, k -> new ArrayList<>()).add(f);

            for (Map.Entry<Integer, List<FilaAlic>> le : porLote.entrySet()) {
                int loteL = le.getKey();
                List<FilaAlic> filasLote = le.getValue();
                TuboProtocolo tuboRef = tuboPorOrden.get(filasLote.get(0).tuboT);

                Lote lote = new Lote();
                lote.setInstitucion(inst);
                lote.setPaciente(paciente);
                lote.setTipoResultante(tuboRef != null ? tuboRef.getTipoResultante() : null);
                lote.setProtocolo(protocolo);
                lote.setNumeroLote(loteL);
                lote.setUsuarioProcesa(usuario);
                lote.setFechaCreacion(ahora);
                if (escribir) lote = loteRepository.save(lote);
                nLotes++;

                int total = filasLote.size();
                int pos = 0;
                for (FilaAlic f : filasLote) {
                    pos++;
                    TuboProtocolo tubo = tuboPorOrden.get(f.tuboT);
                    Muestra padre = padrePorTubo.get(f.tuboT);
                    Muestra a = new Muestra();
                    a.setEtiqueta(EtiquetaMuestra.alicuotaLote(tubo.getPrefijoCodigo(), folio, inst.getId(), loteL, pos, total));
                    a.setPaciente(paciente);
                    a.setUsuarioRecolecta(usuario);
                    a.setInstitucion(inst);
                    a.setInstitucionActual(inst);
                    a.setTipoMuestra(tubo.getTipoResultante());
                    a.setTuboProtocolo(tubo);
                    a.setMuestraPadre(padre);
                    a.setLote(lote);
                    a.setNumeroAlicuota(pos);
                    a.setTotalAlicuotas(total);
                    a.setNumeroEnLote(pos);
                    a.setNumeroLote(loteL);
                    a.setValor(f.volumen);
                    a.setUnidad(f.unidad != null && !f.unidad.isBlank() ? f.unidad : tubo.getUnidadVolumen());
                    a.setValorComprometido(0.0);
                    a.setFechaRegistro(ahora);

                    PosicionCaja posicion = resolverPosicion(f, inst, errores);
                    if (posicion != null) {
                        a.setPosicionCaja(posicion);
                        a.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
                        a.setFechaMaterializacion(ahora);
                        a.setCantidadDescontadaPadre(f.volumen);
                        if (escribir) { posicion.setOcupada(true); posicionCajaRepository.save(posicion); }
                        nUbic++;
                    } else {
                        a.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
                    }
                    if (escribir) muestraRepository.save(a);
                    nAlic++;
                }
            }
        }

        if (escribir && !errores.isEmpty()) {
            // El rollback lo fuerza la excepción; convertimos los errores en una.
            throw new imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException(
                    "La carga tiene " + errores.size() + " error(es); no se escribió nada. Primero corrija el archivo.");
        }
        return new ResultadoCargaProtocolo(nProc, nPadres, nAlic, nLotes, nUbic, errores, avisos);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private record Cols(int folio, int tubo, int lote, int volumen, int unidad, int caja, int posicion, int fecha) {}

    private Cols colIndices(List<String> enc, List<String> errores) {
        int folio = -1, tubo = -1, lote = -1, vol = -1, uni = -1, caja = -1, posc = -1, fecha = -1;
        for (int i = 0; i < enc.size(); i++) {
            String h = enc.get(i) == null ? "" : enc.get(i).trim().toLowerCase();
            if (h.equals("folio")) folio = i;
            else if (h.startsWith("tubo")) tubo = i;
            else if (h.startsWith("lote")) lote = i;
            else if (h.startsWith("volumen") || h.equals("vol")) vol = i;
            else if (h.startsWith("unidad")) uni = i;
            else if (h.startsWith("caja") || h.equals("codigocaja") || h.equals("codigo caja")) caja = i;
            else if (h.startsWith("posicion") || h.startsWith("posición")) posc = i;
            else if (h.startsWith("fecha")) fecha = i;
        }
        if (folio < 0) errores.add("Falta la columna obligatoria «folio».");
        if (vol < 0) errores.add("Falta la columna obligatoria «volumen».");
        return (folio >= 0 && vol >= 0) ? new Cols(folio, tubo, lote, vol, uni, caja, posc, fecha) : null;
    }

    private void inferirTubos(List<FilaAlic> filas, List<TuboProtocolo> tubosAlic, List<String> errores) {
        boolean faltaAlguno = filas.stream().anyMatch(f -> f.tuboT == null);
        if (!faltaAlguno || tubosAlic.isEmpty()) return;
        // Reparto por orden: llena el cupo configurado de cada tubo ALICUOTAR.
        int idx = 0, usados = 0;
        for (FilaAlic f : filas) {
            if (f.tuboT != null) continue;
            if (idx >= tubosAlic.size()) { break; }
            TuboProtocolo t = tubosAlic.get(idx);
            int cupo = t.getNumeroAlicuotas() != null ? t.getNumeroAlicuotas() : 0;
            f.tuboT = t.getOrden();
            usados++;
            if (usados >= cupo) { idx++; usados = 0; }
        }
    }

    private boolean tieneErrorDeFolio(List<FilaAlic> filas, Map<Integer, TuboProtocolo> tuboPorOrden,
                                      List<String> errores, String folio) {
        boolean error = false;
        for (FilaAlic f : filas) {
            if (f.tuboT == null || tuboPorOrden.get(f.tuboT) == null) {
                errores.add("Folio " + folio + ", fila " + f.numFila + ": no se pudo determinar el tubo (T).");
                error = true;
            } else if (tuboPorOrden.get(f.tuboT).getAccion() != AccionTubo.ALICUOTAR) {
                errores.add("Folio " + folio + ", fila " + f.numFila + ": el tubo T" + f.tuboT
                        + " no está configurado para alicuotar.");
                error = true;
            }
        }
        return error;
    }

    private Paciente resolverPaciente(String folio) {
        Long idInst = institucionContextService.getIdInstitucionActual();
        Paciente p = pacienteRepository.findByFolioAndInstitucion_Id(folio, idInst).orElse(null);
        if (p == null) {
            String padded = folio.chars().allMatch(Character::isDigit) ? String.format("%06d", Integer.parseInt(folio)) : folio;
            if (!padded.equals(folio)) p = pacienteRepository.findByFolioAndInstitucion_Id(padded, idInst).orElse(null);
        }
        return p;
    }

    private PosicionCaja resolverPosicion(FilaAlic f, Institucion inst, List<String> errores) {
        if (f.codigoCaja == null || f.codigoCaja.isBlank() || f.posTexto == null || f.posTexto.isBlank()) return null;
        CajaCriogenica caja = cajaRepository.findByCodigoCajaAndInstitucion_Id(f.codigoCaja.trim(), inst.getId()).orElse(null);
        if (caja == null) { errores.add("Fila " + f.numFila + ": caja «" + f.codigoCaja + "» no existe."); return null; }
        EtiquetaPosicionCaja.Coordenada c;
        try { c = EtiquetaPosicionCaja.parsear(f.posTexto.trim()); }
        catch (RuntimeException ex) { errores.add("Fila " + f.numFila + ": posición «" + f.posTexto + "» inválida."); return null; }
        PosicionCaja pos = posicionCajaRepository.findByCaja_IdAndFilaAndColumna(caja.getId(), c.fila(), c.columna()).orElse(null);
        if (pos == null) { errores.add("Fila " + f.numFila + ": la posición " + f.posTexto + " no existe en la caja."); return null; }
        if (Boolean.TRUE.equals(pos.getOcupada())) { errores.add("Fila " + f.numFila + ": la posición " + f.codigoCaja + " " + f.posTexto + " ya está ocupada."); return null; }
        return pos;
    }

    private static String celda(List<String> fila, int idx) {
        if (idx < 0 || idx >= fila.size()) return null;
        String v = fila.get(idx);
        return v == null ? null : v.trim();
    }

    private static Double parseDouble(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Double.parseDouble(s.trim().replace(",", ".")); } catch (NumberFormatException e) { return null; }
    }

    private static Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return null; }
    }

    private static final class FilaAlic {
        int numFila;
        String folio;
        Integer tuboT;
        int loteL;
        Double volumen;
        String unidad;
        String codigoCaja;
        String posTexto;
    }
}
