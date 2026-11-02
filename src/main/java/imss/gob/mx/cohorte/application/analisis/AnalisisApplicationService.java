package imss.gob.mx.cohorte.application.analisis;

import imss.gob.mx.cohorte.controllers.analisis.dto.AgregadoRequestDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.CorrelacionDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.CorrelacionRequestDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.DispersionTemporalDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.DispersionTemporalRequestDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.PuntoDispersionDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.PuntoTendenciaDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.SerieDispersionDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.InstitucionAlcanceDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.PuntoCorrelacionDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.SerieAgregadaDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.VistaAnalisisDTO;
import imss.gob.mx.cohorte.controllers.analisis.dto.VistaAnalisisRequestDTO;
import imss.gob.mx.cohorte.modules.analisis.AnalisisConstantes;
import imss.gob.mx.cohorte.modules.analisis.VistaAnalisis;
import imss.gob.mx.cohorte.modules.analisis.VistaAnalisisRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ResultadoEstudioMuestraRepository;
import imss.gob.mx.cohorte.modules.analisis.CrudoNumProy;
import imss.gob.mx.cohorte.modules.analisis.CrudoProy;
import imss.gob.mx.cohorte.modules.analisis.CrudoSomatoProy;
import imss.gob.mx.cohorte.modules.analisis.FuenteMetrica;
import imss.gob.mx.cohorte.modules.analisis.Metrica;
import imss.gob.mx.cohorte.modules.analisis.NumericoProy;
import imss.gob.mx.cohorte.modules.analisis.ResultadoProyeccion;
import imss.gob.mx.cohorte.modules.analisis.SomatometriaProy;
import imss.gob.mx.cohorte.modules.analisis.TipoMetrica;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudioRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.somatometria.SomatometriaRepository;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.analisis.AgregacionService;
import imss.gob.mx.cohorte.services.analisis.AnalisisCrudoExportService;
import imss.gob.mx.cohorte.services.analisis.AnalisisExportService;
import imss.gob.mx.cohorte.services.analisis.AnalisisScopeService;
import imss.gob.mx.cohorte.services.analisis.CatalogoMetricasService;
import imss.gob.mx.cohorte.services.analisis.FilaCruda;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Fachada del módulo de análisis. Orquesta el alcance supra-institucional
 * ({@link AnalisisScopeService}), el catálogo de métricas
 * ({@link CatalogoMetricasService}) y el motor de agregación
 * ({@link AgregacionService}).
 *
 * <p>Anotada con {@link RequireModulo}: la institución del usuario debe tener
 * habilitado el módulo {@code ANALISIS} (lo concede la raíz). El permiso de
 * acción ({@code ANALISIS_ACCEDER}) se valida además en el controlador.</p>
 */
@Service
@RequiredArgsConstructor
@RequireModulo(ModuloSistema.ANALISIS)
public class AnalisisApplicationService {

    private final AnalisisScopeService             scopeService;
    private final CatalogoMetricasService          catalogoMetricasService;
    private final AgregacionService                agregacionService;
    private final ResultadoEstudioRepository       resultadoEstudioRepository;
    private final ResultadoExamenRepository        resultadoExamenRepository;
    private final SomatometriaRepository           somatometriaRepository;
    private final ResultadoEstudioMuestraRepository resultadoEstudioMuestraRepository;
    private final InstitucionRepository            institucionRepository;
    private final AnalisisExportService            analisisExportService;
    private final AnalisisCrudoExportService       analisisCrudoExportService;
    private final VistaAnalisisRepository          vistaAnalisisRepository;

    /** Catálogo de variables analizables dentro del alcance del usuario. */
    @Transactional(readOnly = true)
    public List<Metrica> catalogoMetricas() {
        return catalogoMetricasService.construir(scopeService.institucionesEnAlcance());
    }

    /** Instituciones del alcance del usuario (para el filtro de la UI), por nombre. */
    @Transactional(readOnly = true)
    public List<InstitucionAlcanceDTO> institucionesAlcance() {
        return institucionRepository.findAllById(scopeService.institucionesEnAlcance()).stream()
                .map(i -> new InstitucionAlcanceDTO(i.getId(), i.getNombre()))
                .sorted(java.util.Comparator.comparing(InstitucionAlcanceDTO::nombre,
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Agrega las métricas pedidas, cada una a su serie. El alcance de
     * instituciones se resuelve en servidor e intersecta con el filtro del
     * cliente: una institución fuera del subárbol nunca entra.
     */
    @Transactional(readOnly = true)
    public List<SerieAgregadaDTO> agregar(AgregadoRequestDTO req) {
        Set<Long> instituciones = scopeService.resolverFiltroInstituciones(
                req.getFiltros() != null ? req.getFiltros().getInstituciones() : null);

        LocalDateTime desde = aInicioDeDia(req.getFiltros() != null ? req.getFiltros().getFechaDesde() : null);
        LocalDateTime hasta = aFinDeDia(req.getFiltros() != null ? req.getFiltros().getFechaHasta() : null);

        List<SerieAgregadaDTO> series = new ArrayList<>();
        if (instituciones.isEmpty()) return series;

        // Eje temporal: "corte" es la primera dimensión de desglose; las demás
        // (sexo/institución/edad) se superponen como series de línea.
        if ("TEMPORAL".equals(req.getAgregacion())) {
            List<String> dims = new ArrayList<>();
            dims.add("corte");
            if (req.getDimensiones() != null) {
                for (String d : req.getDimensiones()) if (!"corte".equals(d)) dims.add(d);
            }
            req.setDimensiones(dims);
        }

        for (String clave : req.getMetricas()) {
            Metrica metrica = catalogoMetricasService.buscar(clave, instituciones).orElse(null);
            if (metrica == null) continue;

            List<ResultadoProyeccion> filas = switch (metrica.fuente()) {
                case ESTUDIO_MEDICO -> metrica.origenId() == null ? List.of()
                        : resultadoEstudioRepository.proyeccionPorParametro(
                                metrica.origenId(), instituciones, desde, hasta);
                case EXAMEN -> metrica.origenId() == null ? List.of()
                        : deNumerico(resultadoExamenRepository.proyeccionPorExamen(
                                metrica.origenId(), instituciones, desde, hasta));
                case SOMATOMETRIA -> deSomatometria(
                        metrica.clave(),
                        somatometriaRepository.proyeccionSomatometria(instituciones, desde, hasta));
                case ESTUDIO_MUESTRA -> metrica.origenId() == null ? List.of()
                        : resultadoEstudioMuestraRepository.proyeccionPorParametro(
                                metrica.origenId(), instituciones, desde, hasta);
            };

            series.add(metrica.tipo() == TipoMetrica.NUMERICO
                    ? agregacionService.agregarNumerica(metrica, filas, req)
                    : agregacionService.agregarCategorica(metrica, filas, req));
        }
        return series;
    }

    /** Convierte la proyección numérica simple (exámenes) al tipo común del motor. */
    private List<ResultadoProyeccion> deNumerico(List<NumericoProy> origen) {
        List<ResultadoProyeccion> out = new ArrayList<>(origen.size());
        for (NumericoProy n : origen) {
            if (n.valor() == null) continue;
            out.add(new ResultadoProyeccion(n.valor(), null, null, n.sexo(),
                    n.fechaNacimiento(), n.fecha(), n.institucionId()));
        }
        return out;
    }

    /** Extrae de cada registro de somatometría la columna que pide la clave {@code somato.<campo>}. */
    private List<ResultadoProyeccion> deSomatometria(String clave, List<SomatometriaProy> origen) {
        String campo = clave.substring(CatalogoMetricasService.PREFIJO_SOMATO.length());
        List<ResultadoProyeccion> out = new ArrayList<>(origen.size());
        for (SomatometriaProy s : origen) {
            Double v = s.valorDe(campo);
            if (v == null) continue;
            out.add(new ResultadoProyeccion(v, null, null, s.sexo(),
                    s.fechaNacimiento(), s.fecha(), s.institucionId()));
        }
        return out;
    }

    // ─────────────────────────── Vistas guardadas ───────────────────────────

    /** Vistas guardadas del usuario actual. */
    @Transactional(readOnly = true)
    public List<VistaAnalisisDTO> listarVistas() {
        return vistaAnalisisRepository.findAllByUsuarioUuidOrderByNombreAsc(usuarioActualUuid())
                .stream().map(this::aVistaDTO).toList();
    }

    /** Guarda una vista nueva para el usuario actual. */
    @Transactional
    public VistaAnalisisDTO guardarVista(VistaAnalisisRequestDTO req) {
        VistaAnalisis v = new VistaAnalisis();
        v.setUsuarioUuid(usuarioActualUuid());
        v.setNombre(req.getNombre());
        v.setConfiguracion(req.getConfiguracion());
        return aVistaDTO(vistaAnalisisRepository.save(v));
    }

    /** Elimina una vista del usuario actual (no puede tocar las de otros). */
    @Transactional
    public void eliminarVista(String uuid) {
        VistaAnalisis v = vistaAnalisisRepository.findByUuidAndUsuarioUuid(uuid, usuarioActualUuid())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la vista de análisis: " + uuid));
        vistaAnalisisRepository.delete(v);
    }

    private VistaAnalisisDTO aVistaDTO(VistaAnalisis v) {
        return new VistaAnalisisDTO(v.getUuid(), v.getNombre(), v.getConfiguracion(),
                v.getFechaActualizacion() != null ? v.getFechaActualizacion().toString() : null);
    }

    private String usuarioActualUuid() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            throw new AccessDeniedException("No hay un usuario autenticado en el contexto");
        }
        return auth.getName();
    }

    /** Exporta los agregados a CSV (carril de agregados; permiso ANALISIS_EXPORTAR). */
    @Transactional(readOnly = true)
    public AnalisisExportService.Descarga exportarAgregado(AgregadoRequestDTO req, String separador) {
        return analisisExportService.aCsv(agregar(req), separador);
    }

    /**
     * Exporta datos crudos a nivel participante de la primera métrica pedida, en
     * CSV o XLSX, SIN supresión k-anónima (carril crudo; permiso
     * ANALISIS_EXPORTA_CRUDO). El alcance de instituciones se resuelve en servidor.
     */
    @Transactional(readOnly = true)
    public AnalisisExportService.Descarga exportarCrudo(AgregadoRequestDTO req, String formato, String separador) {
        boolean xlsx = "xlsx".equalsIgnoreCase(formato);
        Set<Long> instituciones = scopeService.resolverFiltroInstituciones(
                req.getFiltros() != null ? req.getFiltros().getInstituciones() : null);

        if (req.getMetricas().isEmpty() || instituciones.isEmpty()) {
            return xlsx ? analisisCrudoExportService.aXlsx("", "", List.of())
                        : analisisCrudoExportService.aCsv("", "", List.of(), separador);
        }

        Metrica metrica = catalogoMetricasService.buscar(req.getMetricas().get(0), instituciones).orElse(null);
        if (metrica == null) {
            return xlsx ? analisisCrudoExportService.aXlsx("", "", List.of())
                        : analisisCrudoExportService.aCsv("", "", List.of(), separador);
        }

        LocalDateTime desde = aInicioDeDia(req.getFiltros() != null ? req.getFiltros().getFechaDesde() : null);
        LocalDateTime hasta = aFinDeDia(req.getFiltros() != null ? req.getFiltros().getFechaHasta() : null);

        Map<Long, String> nombres = new HashMap<>();
        for (Institucion i : institucionRepository.findAllById(instituciones)) nombres.put(i.getId(), i.getNombre());

        List<FilaCruda> filas = recolectarCrudo(metrica, instituciones, desde, hasta, nombres);
        return xlsx ? analisisCrudoExportService.aXlsx(metrica.etiqueta(), metrica.unidad(), filas)
                    : analisisCrudoExportService.aCsv(metrica.etiqueta(), metrica.unidad(), filas, separador);
    }

    private List<FilaCruda> recolectarCrudo(Metrica metrica, Set<Long> inst,
                                            LocalDateTime desde, LocalDateTime hasta, Map<Long, String> nombres) {
        List<FilaCruda> filas = new ArrayList<>();
        switch (metrica.fuente()) {
            case ESTUDIO_MEDICO -> {
                if (metrica.origenId() == null) break;
                for (CrudoProy c : resultadoEstudioRepository.crudoPorParametro(metrica.origenId(), inst, desde, hasta)) {
                    filas.add(filaDe(c, nombres));
                }
            }
            case ESTUDIO_MUESTRA -> {
                if (metrica.origenId() == null) break;
                for (CrudoProy c : resultadoEstudioMuestraRepository.crudoPorParametro(metrica.origenId(), inst, desde, hasta)) {
                    filas.add(filaDe(c, nombres));
                }
            }
            case EXAMEN -> {
                if (metrica.origenId() == null) break;
                for (CrudoNumProy c : resultadoExamenRepository.crudoPorExamen(metrica.origenId(), inst, desde, hasta)) {
                    filas.add(fila(c.folio(), c.institucionId(), c.sexo(), c.fechaNacimiento(), c.fecha(),
                            c.valor(), null, nombres));
                }
            }
            case SOMATOMETRIA -> {
                String campo = metrica.clave().substring(CatalogoMetricasService.PREFIJO_SOMATO.length());
                for (CrudoSomatoProy c : somatometriaRepository.crudoSomatometria(inst, desde, hasta)) {
                    Double v = c.valorDe(campo);
                    if (v == null) continue;
                    filas.add(fila(c.folio(), c.institucionId(), c.sexo(), c.fechaNacimiento(), c.fecha(),
                            v, null, nombres));
                }
            }
        }
        return filas;
    }

    private FilaCruda filaDe(CrudoProy c, Map<Long, String> nombres) {
        String texto = c.valorTexto() != null ? c.valorTexto()
                : (c.valorBooleano() != null ? (c.valorBooleano() ? "Sí" : "No") : null);
        return fila(c.folio(), c.institucionId(), c.sexo(), c.fechaNacimiento(), c.fecha(),
                c.valorNumerico(), texto, nombres);
    }

    private FilaCruda fila(String folio, Long instId, Persona.Sexo sexo, LocalDate nac,
                           LocalDateTime fecha, Double num, String texto, Map<Long, String> nombres) {
        Integer edad = (nac != null && fecha != null) ? Period.between(nac, fecha.toLocalDate()).getYears() : null;
        String sexoStr = sexo == null ? "" : (sexo == Persona.Sexo.F ? "Mujeres" : "Hombres");
        return new FilaCruda(
                folio,
                nombres.getOrDefault(instId, instId != null ? "Institución " + instId : ""),
                sexoStr,
                edad,
                fecha != null ? fecha.toLocalDate().toString() : "",
                num, texto);
    }

    // ───────────────────────────── Correlación ──────────────────────────────

    /**
     * Correlación entre dos métricas numéricas. Empareja por participante (folio)
     * el promedio de cada métrica, calcula la recta de ajuste y R², y aplica la
     * supresión k-anónima. Los puntos individuales solo viajan si el usuario tiene
     * el permiso {@code ANALISIS_EXPORTA_CRUDO}.
     */
    @Transactional(readOnly = true)
    public CorrelacionDTO correlacion(CorrelacionRequestDTO req) {
        Set<Long> instituciones = scopeService.resolverFiltroInstituciones(
                req.getFiltros() != null ? req.getFiltros().getInstituciones() : null);

        Metrica mx = catalogoMetricasService.buscar(req.getMetricaX(), instituciones).orElse(null);
        Metrica my = catalogoMetricasService.buscar(req.getMetricaY(), instituciones).orElse(null);
        if (mx == null || my == null || mx.tipo() != TipoMetrica.NUMERICO || my.tipo() != TipoMetrica.NUMERICO
                || instituciones.isEmpty()) {
            return correlacionVacia(mx, my, req.getGrupo());
        }

        LocalDateTime desde = aInicioDeDia(req.getFiltros() != null ? req.getFiltros().getFechaDesde() : null);
        LocalDateTime hasta = aFinDeDia(req.getFiltros() != null ? req.getFiltros().getFechaHasta() : null);

        Map<Long, String> nombres = new HashMap<>();
        for (Institucion i : institucionRepository.findAllById(instituciones)) nombres.put(i.getId(), i.getNombre());

        AgregadoRequestDTO.Filtros f = req.getFiltros() != null ? req.getFiltros() : new AgregadoRequestDTO.Filtros();
        List<int[]> buckets = parsearRangos(f.getRangosEdad());
        Set<String> sexosFiltro = sexosComoEtiqueta(f.getSexos());

        // Promedio por folio de cada métrica (una medición o varias) + su grupo.
        Map<String, double[]> x = new LinkedHashMap<>(); // folio -> [suma, conteo]
        Map<String, double[]> y = new HashMap<>();
        Map<String, String> grupoPorFolio = new HashMap<>();

        for (FilaCruda fila : recolectarCrudo(mx, instituciones, desde, hasta, nombres)) {
            if (!pasaFiltros(fila, sexosFiltro, f)) continue;
            if (fila.folio() == null || fila.valorNumerico() == null) continue;
            acumular(x, fila.folio(), fila.valorNumerico());
            grupoPorFolio.putIfAbsent(fila.folio(), grupoDeFila(fila, req.getGrupo(), buckets));
        }
        for (FilaCruda fila : recolectarCrudo(my, instituciones, desde, hasta, nombres)) {
            if (!pasaFiltros(fila, sexosFiltro, f)) continue;
            if (fila.folio() == null || fila.valorNumerico() == null) continue;
            acumular(y, fila.folio(), fila.valorNumerico());
        }

        // Pares (x, y) de los folios presentes en ambas métricas, por grupo.
        LinkedHashMap<String, List<double[]>> porGrupo = new LinkedHashMap<>();
        for (var e : x.entrySet()) {
            double[] yy = y.get(e.getKey());
            if (yy == null) continue;
            double vx = e.getValue()[0] / e.getValue()[1];
            double vy = yy[0] / yy[1];
            String g = grupoPorFolio.getOrDefault(e.getKey(), AnalisisConstantes.SIN_DATO);
            porGrupo.computeIfAbsent(g, k -> new ArrayList<>()).add(new double[]{vx, vy});
        }

        // Supresión k-anónima: un grupo con menos de k pares no aporta puntos.
        List<double[]> todos = new ArrayList<>();
        long nSuprimido = 0;
        boolean agrupa = req.getGrupo() != null && !req.getGrupo().isBlank();
        for (var e : porGrupo.entrySet()) {
            if (agrupa && e.getValue().size() < AnalisisConstantes.K_ANONIMO) { nSuprimido += e.getValue().size(); continue; }
            for (double[] p : e.getValue()) todos.add(new double[]{p[0], p[1]});
        }

        if (todos.size() < AnalisisConstantes.K_ANONIMO) {
            return new CorrelacionDTO(mx.clave(), mx.etiqueta(), mx.unidad(), my.clave(), my.etiqueta(), my.unidad(),
                    req.getGrupo(), 0, nSuprimido + todos.size(),
                    null, null, null, null, null, null, null, false, List.of());
        }

        double[] ajuste = regresion(todos); // [pendiente, intercepto, r2, mediaX, mediaY, minX, maxX]

        boolean puedeCrudo = tienePermiso("ANALISIS_EXPORTA_CRUDO");
        List<PuntoCorrelacionDTO> puntos = new ArrayList<>();
        if (puedeCrudo) {
            for (var e : porGrupo.entrySet()) {
                if (agrupa && e.getValue().size() < AnalisisConstantes.K_ANONIMO) continue;
                for (double[] p : e.getValue()) puntos.add(new PuntoCorrelacionDTO(p[0], p[1], e.getKey()));
            }
        }

        return new CorrelacionDTO(
                mx.clave(), mx.etiqueta(), mx.unidad(), my.clave(), my.etiqueta(), my.unidad(),
                req.getGrupo(), todos.size(), nSuprimido,
                redondear(ajuste[0]), redondear(ajuste[1]), redondear(ajuste[2]),
                redondear(ajuste[3]), redondear(ajuste[4]), redondear(ajuste[5]), redondear(ajuste[6]),
                puedeCrudo, puntos);
    }

    // ──────────────────────────── Dispersión temporal ───────────────────────────

    /**
     * Dispersión temporal: por cada corte de seguimiento (bucket de la fecha según
     * la granularidad) devuelve la media (línea de tendencia, siempre) y, con
     * permiso de crudo, los puntos individuales. Supresión k-anónima por celda
     * (grupo × corte): un corte con menos de k mediciones no se dibuja.
     */
    @Transactional(readOnly = true)
    public DispersionTemporalDTO dispersionTemporal(DispersionTemporalRequestDTO req) {
        Set<Long> instituciones = scopeService.resolverFiltroInstituciones(
                req.getFiltros() != null ? req.getFiltros().getInstituciones() : null);

        Metrica m = catalogoMetricasService.buscar(req.getMetrica(), instituciones).orElse(null);
        if (m == null || m.tipo() != TipoMetrica.NUMERICO || instituciones.isEmpty()) {
            return new DispersionTemporalDTO(
                    m != null ? m.clave() : null, m != null ? m.etiqueta() : null, m != null ? m.unidad() : null,
                    req.getGrupo(), List.of(), List.of(), 0, 0, false);
        }

        LocalDateTime desde = aInicioDeDia(req.getFiltros() != null ? req.getFiltros().getFechaDesde() : null);
        LocalDateTime hasta = aFinDeDia(req.getFiltros() != null ? req.getFiltros().getFechaHasta() : null);

        Map<Long, String> nombres = new HashMap<>();
        for (Institucion i : institucionRepository.findAllById(instituciones)) nombres.put(i.getId(), i.getNombre());

        AgregadoRequestDTO.Filtros f = req.getFiltros() != null ? req.getFiltros() : new AgregadoRequestDTO.Filtros();
        List<int[]> buckets = parsearRangos(f.getRangosEdad());
        Set<String> sexosFiltro = sexosComoEtiqueta(f.getSexos());
        String gran = req.getGranularidad() != null ? req.getGranularidad() : "MES";

        // grupo -> (corte ordenado -> valores)
        LinkedHashMap<String, Map<String, List<Double>>> datos = new LinkedHashMap<>();
        for (FilaCruda fila : recolectarCrudo(m, instituciones, desde, hasta, nombres)) {
            if (!pasaFiltros(fila, sexosFiltro, f)) continue;
            if (fila.valorNumerico() == null) continue;
            String corte = bucketCorteFecha(fila.fecha(), gran);
            String g = grupoDeFila(fila, req.getGrupo(), buckets);
            datos.computeIfAbsent(g, k -> new java.util.TreeMap<>())
                 .computeIfAbsent(corte, k -> new ArrayList<>())
                 .add(fila.valorNumerico());
        }

        boolean puedeCrudo = tienePermiso("ANALISIS_EXPORTA_CRUDO");
        java.util.TreeSet<String> cortes = new java.util.TreeSet<>();
        List<SerieDispersionDTO> series = new ArrayList<>();
        long n = 0, nSup = 0;

        for (var eGrupo : datos.entrySet()) {
            List<PuntoTendenciaDTO> tendencia = new ArrayList<>();
            List<PuntoDispersionDTO> puntos = new ArrayList<>();
            for (var eCorte : eGrupo.getValue().entrySet()) {
                List<Double> vals = eCorte.getValue();
                if (vals.size() < AnalisisConstantes.K_ANONIMO) { nSup += vals.size(); continue; }
                double suma = 0;
                for (double v : vals) suma += v;
                cortes.add(eCorte.getKey());
                tendencia.add(new PuntoTendenciaDTO(eCorte.getKey(), redondear(suma / vals.size())));
                n += vals.size();
                if (puedeCrudo) {
                    for (double v : vals) puntos.add(new PuntoDispersionDTO(eCorte.getKey(), redondear(v)));
                }
            }
            if (!tendencia.isEmpty()) series.add(new SerieDispersionDTO(eGrupo.getKey(), puntos, tendencia));
        }

        return new DispersionTemporalDTO(m.clave(), m.etiqueta(), m.unidad(), req.getGrupo(),
                new ArrayList<>(cortes), series, n, nSup, puedeCrudo);
    }

    /** Bucket de una fecha ISO (yyyy-MM-dd, como la entrega {@link FilaCruda}) en corte de seguimiento. */
    private String bucketCorteFecha(String iso, String granularidad) {
        LocalDate d = parseFecha(iso);
        if (d == null) return AnalisisConstantes.SIN_DATO;
        return switch (granularidad == null ? "MES" : granularidad) {
            case "ANIO" -> String.valueOf(d.getYear());
            case "TRIMESTRE" -> d.getYear() + "-T" + ((d.getMonthValue() - 1) / 3 + 1);
            default -> String.format("%04d-%02d", d.getYear(), d.getMonthValue());
        };
    }

    private CorrelacionDTO correlacionVacia(Metrica x, Metrica y, String grupo) {
        return new CorrelacionDTO(
                x != null ? x.clave() : null, x != null ? x.etiqueta() : null, x != null ? x.unidad() : null,
                y != null ? y.clave() : null, y != null ? y.etiqueta() : null, y != null ? y.unidad() : null,
                grupo, 0, 0, null, null, null, null, null, null, null, false, List.of());
    }

    private void acumular(Map<String, double[]> m, String folio, double valor) {
        double[] sc = m.computeIfAbsent(folio, k -> new double[2]);
        sc[0] += valor; sc[1] += 1;
    }

    /** Regresión lineal por mínimos cuadrados. Devuelve [pendiente, intercepto, r2, mediaX, mediaY, minX, maxX]. */
    private double[] regresion(List<double[]> pares) {
        int n = pares.size();
        double sx = 0, sy = 0, sxx = 0, sxy = 0, syy = 0, minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
        for (double[] p : pares) {
            sx += p[0]; sy += p[1]; sxx += p[0] * p[0]; sxy += p[0] * p[1]; syy += p[1] * p[1];
            minX = Math.min(minX, p[0]); maxX = Math.max(maxX, p[0]);
        }
        double mediaX = sx / n, mediaY = sy / n;
        double covxy = sxy - sx * sy / n;
        double varx = sxx - sx * sx / n;
        double vary = syy - sy * sy / n;
        double pendiente = varx == 0 ? 0 : covxy / varx;
        double intercepto = mediaY - pendiente * mediaX;
        double r = (varx == 0 || vary == 0) ? 0 : covxy / Math.sqrt(varx * vary);
        return new double[]{pendiente, intercepto, r * r, mediaX, mediaY, minX, maxX};
    }

    private String grupoDeFila(FilaCruda f, String grupo, List<int[]> buckets) {
        if (grupo == null || grupo.isBlank()) return "Todos";
        return switch (grupo) {
            case "sexo" -> f.sexo() == null || f.sexo().isBlank() ? AnalisisConstantes.SIN_DATO : f.sexo();
            case "institucion" -> f.institucion() == null || f.institucion().isBlank() ? AnalisisConstantes.SIN_DATO : f.institucion();
            case "rango_edad" -> {
                if (buckets.isEmpty()) yield "Todas las edades";
                if (f.edad() == null) yield AnalisisConstantes.SIN_DATO;
                for (int[] b : buckets) if (f.edad() >= b[0] && f.edad() <= b[1]) yield b[0] + "–" + b[1];
                yield "Otras edades";
            }
            default -> "Todos";
        };
    }

    private boolean pasaFiltros(FilaCruda f, Set<String> sexosFiltro, AgregadoRequestDTO.Filtros filtros) {
        if (!sexosFiltro.isEmpty() && (f.sexo() == null || !sexosFiltro.contains(f.sexo()))) return false;
        if (filtros.getEdadMin() != null && (f.edad() == null || f.edad() < filtros.getEdadMin())) return false;
        if (filtros.getEdadMax() != null && (f.edad() == null || f.edad() > filtros.getEdadMax())) return false;
        return true;
    }

    private Set<String> sexosComoEtiqueta(List<String> sexos) {
        if (sexos == null || sexos.isEmpty()) return Set.of();
        Set<String> out = new java.util.HashSet<>();
        for (String s : sexos) {
            if ("F".equalsIgnoreCase(s)) out.add("Mujeres");
            else if ("M".equalsIgnoreCase(s)) out.add("Hombres");
        }
        return out;
    }

    private List<int[]> parsearRangos(List<List<Integer>> rangos) {
        List<int[]> out = new ArrayList<>();
        if (rangos == null) return out;
        for (List<Integer> par : rangos) {
            if (par != null && par.size() == 2 && par.get(0) != null && par.get(1) != null) {
                out.add(new int[]{par.get(0), par.get(1)});
            }
        }
        return out;
    }

    private boolean tienePermiso(String authority) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> authority.equals(a.getAuthority()));
    }

    private Double redondear(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return null;
        return Math.round(v * 10000.0) / 10000.0;
    }

    private LocalDateTime aInicioDeDia(String iso) {
        LocalDate d = parseFecha(iso);
        return d != null ? d.atStartOfDay() : null;
    }

    private LocalDateTime aFinDeDia(String iso) {
        LocalDate d = parseFecha(iso);
        return d != null ? d.atTime(23, 59, 59) : null;
    }

    private LocalDate parseFecha(String iso) {
        if (iso == null || iso.isBlank()) return null;
        try {
            return LocalDate.parse(iso.trim());
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
