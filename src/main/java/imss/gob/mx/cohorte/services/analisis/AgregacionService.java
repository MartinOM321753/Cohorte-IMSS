package imss.gob.mx.cohorte.services.analisis;

import imss.gob.mx.cohorte.controllers.analisis.dto.*;
import imss.gob.mx.cohorte.modules.analisis.AnalisisConstantes;
import imss.gob.mx.cohorte.modules.analisis.Metrica;
import imss.gob.mx.cohorte.modules.analisis.ResultadoProyeccion;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;

/**
 * Motor de agregación. Recibe la proyección ya acotada por SQL (alcance,
 * parámetro, fechas) y calcula las celdas por dimensión aplicando los cortes de
 * edad configurables, los estadísticos (numéricas) o las frecuencias
 * (categóricas), y la supresión k-anónima.
 *
 * <p>Que el bucketing de edad y la clasificación por sexo se hagan aquí —y no en
 * un {@code GROUP BY} fijo— es deliberado: dependen de decisiones del analista
 * (cortes de edad) y del participante (sexo). Al cliente solo le llegan
 * agregados, nunca las filas.</p>
 */
@Service
@RequiredArgsConstructor
public class AgregacionService {

    private final InstitucionRepository institucionRepository;

    /** Agrupamiento común: filas por clave de dimensiones, más el mapa de cada clave. */
    private record Agrupado(
            LinkedHashMap<String, List<ResultadoProyeccion>> grupos,
            LinkedHashMap<String, Map<String, String>> dimensionesPorGrupo,
            long nTotal
    ) {}

    // ═══════════════════════════ Numérica ═══════════════════════════
    public SerieAgregadaDTO agregarNumerica(Metrica metrica, List<ResultadoProyeccion> filas,
                                            AgregadoRequestDTO req) {
        Agrupado ag = agrupar(metrica, filas, req);

        int bins = req.getFiltros().getBins() != null && req.getFiltros().getBins() > 0
                ? req.getFiltros().getBins() : AnalisisConstantes.BINS_DEFECTO;
        double[] bordes = "DISTRIBUCION".equals(req.getAgregacion())
                ? calcularBordes(ag.grupos().values(), bins) : null;

        List<CeldaAgregadaDTO> celdas = new ArrayList<>();
        long nSuprimido = 0;
        for (var e : ag.grupos().entrySet()) {
            List<ResultadoProyeccion> puntos = e.getValue();
            if (puntos.size() < AnalisisConstantes.K_ANONIMO) { nSuprimido += puntos.size(); continue; }
            celdas.add(celdaNumerica(metrica, req.getAgregacion(),
                    ag.dimensionesPorGrupo().get(e.getKey()), puntos, bordes));
        }
        // El eje temporal debe salir en orden cronológico; las etiquetas de corte
        // ya son lexicográficamente ordenables (yyyy-MM, yyyy-Tn, yyyy).
        if (req.getDimensiones() != null && req.getDimensiones().contains("corte")) {
            celdas.sort(Comparator.comparing(c -> c.dimensiones().getOrDefault("corte", "")));
        }
        return serie(metrica, req, celdas, ag.nTotal(), nSuprimido);
    }

    // ═══════════════════════════ Categórica ═════════════════════════
    public SerieAgregadaDTO agregarCategorica(Metrica metrica, List<ResultadoProyeccion> filas,
                                              AgregadoRequestDTO req) {
        Agrupado ag = agrupar(metrica, filas, req);

        List<CeldaAgregadaDTO> celdas = new ArrayList<>();
        long nSuprimido = 0;
        for (var e : ag.grupos().entrySet()) {
            List<ResultadoProyeccion> puntos = e.getValue();
            if (puntos.size() < AnalisisConstantes.K_ANONIMO) { nSuprimido += puntos.size(); continue; }
            celdas.add(celdaCategorica(ag.dimensionesPorGrupo().get(e.getKey()), puntos));
        }
        return serie(metrica, req, celdas, ag.nTotal(), nSuprimido);
    }

    // ─────────────────────── Agrupamiento común ─────────────────────
    private Agrupado agrupar(Metrica metrica, List<ResultadoProyeccion> filas, AgregadoRequestDTO req) {
        AgregadoRequestDTO.Filtros f = req.getFiltros();
        List<String> dims = req.getDimensiones() != null ? req.getDimensiones() : List.of();
        boolean numerica = "NUMERICO".equals(metrica.tipo().name());

        Set<String> sexosFiltro = f.getSexos() == null ? Set.of() : new HashSet<>(f.getSexos());
        List<int[]> buckets = parsearRangosEdad(f.getRangosEdad());
        Map<Long, String> nombresInst = dims.contains("institucion") ? mapaNombresInstituciones(filas) : Map.of();
        String granularidad = req.getGranularidad() != null ? req.getGranularidad() : "MES";

        LinkedHashMap<String, List<ResultadoProyeccion>> grupos = new LinkedHashMap<>();
        LinkedHashMap<String, Map<String, String>> dimsPorGrupo = new LinkedHashMap<>();
        long nTotal = 0;

        for (ResultadoProyeccion r : filas) {
            // Debe tener un valor utilizable según el tipo.
            if (numerica) {
                if (r.valorNumerico() == null) continue;
                if (f.getValorMin() != null && r.valorNumerico() < f.getValorMin()) continue;
                if (f.getValorMax() != null && r.valorNumerico() > f.getValorMax()) continue;
            } else if (r.valorTexto() == null && r.valorBooleano() == null) {
                continue;
            }

            String sexoStr = r.sexo() != null ? r.sexo().name() : null;
            if (!sexosFiltro.isEmpty() && (sexoStr == null || !sexosFiltro.contains(sexoStr))) continue;

            Integer edad = edadEnFecha(r.fechaNacimiento(), r.fecha() != null ? r.fecha().toLocalDate() : null);
            if (f.getEdadMin() != null && (edad == null || edad < f.getEdadMin())) continue;
            if (f.getEdadMax() != null && (edad == null || edad > f.getEdadMax())) continue;

            Map<String, String> claveDims = new LinkedHashMap<>();
            for (String dim : dims) claveDims.put(dim, valorDimension(dim, r, edad, buckets, nombresInst, granularidad));
            String clave = String.join("||", claveDims.values());

            grupos.computeIfAbsent(clave, k -> new ArrayList<>()).add(r);
            dimsPorGrupo.putIfAbsent(clave, claveDims);
            nTotal++;
        }
        return new Agrupado(grupos, dimsPorGrupo, nTotal);
    }

    // ─────────────────────── Construcción de celdas ─────────────────
    private CeldaAgregadaDTO celdaNumerica(Metrica metrica, String agregacion, Map<String, String> dims,
                                           List<ResultadoProyeccion> puntos, double[] bordes) {
        double[] vals = puntos.stream().mapToDouble(ResultadoProyeccion::valorNumerico).sorted().toArray();
        long n = vals.length;
        double media = Arrays.stream(vals).average().orElse(0);

        long enRango = 0, fuera = 0, sinRef = 0;
        for (ResultadoProyeccion p : puntos) {
            Double min = p.sexo() == Persona.Sexo.F ? metrica.refMinMujeres() : metrica.refMinHombres();
            Double max = p.sexo() == Persona.Sexo.F ? metrica.refMaxMujeres() : metrica.refMaxHombres();
            if (min == null && max == null) { sinRef++; continue; }
            boolean bajo = min != null && p.valorNumerico() < min;
            boolean alto = max != null && p.valorNumerico() > max;
            if (bajo || alto) fuera++; else enRango++;
        }

        List<BinHistogramaDTO> histograma = "DISTRIBUCION".equals(agregacion) && bordes != null
                ? histograma(vals, bordes) : null;

        return new CeldaAgregadaDTO(
                dims, n,
                redondear(media), redondear(percentil(vals, 0.50)),
                redondear(percentil(vals, 0.25)), redondear(percentil(vals, 0.75)),
                redondear(vals[0]), redondear(vals[vals.length - 1]), redondear(desviacion(vals, media)),
                enRango, fuera, sinRef, histograma, null);
    }

    private CeldaAgregadaDTO celdaCategorica(Map<String, String> dims, List<ResultadoProyeccion> puntos) {
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (ResultadoProyeccion p : puntos) {
            String cat = p.valorTexto() != null ? p.valorTexto()
                    : (Boolean.TRUE.equals(p.valorBooleano()) ? "Sí" : "No");
            conteo.merge(cat, 1L, Long::sum);
        }
        long n = puntos.size();
        List<CategoriaConteoDTO> categorias = new ArrayList<>();
        conteo.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(en -> categorias.add(new CategoriaConteoDTO(
                        en.getKey(), en.getValue(), redondear((double) en.getValue() / n))));

        return new CeldaAgregadaDTO(dims, n,
                null, null, null, null, null, null, null,
                null, null, null,
                null, categorias);
    }

    private SerieAgregadaDTO serie(Metrica metrica, AgregadoRequestDTO req,
                                   List<CeldaAgregadaDTO> celdas, long nTotal, long nSuprimido) {
        return new SerieAgregadaDTO(
                metrica.clave(), metrica.etiqueta(), metrica.grupo(), metrica.unidad(),
                metrica.tipo().name(), req.getAgregacion(),
                req.getDimensiones() != null ? req.getDimensiones() : List.of(),
                celdas, nTotal, nSuprimido, AnalisisConstantes.K_ANONIMO);
    }

    // ── Dimensiones ───────────────────────────────────────────────────────────
    private String valorDimension(String dim, ResultadoProyeccion r, Integer edad,
                                  List<int[]> buckets, Map<Long, String> nombresInst, String granularidad) {
        return switch (dim) {
            case "sexo" -> r.sexo() != null ? (r.sexo() == Persona.Sexo.F ? "Mujeres" : "Hombres")
                                            : AnalisisConstantes.SIN_DATO;
            case "rango_edad" -> bucketEdad(edad, buckets);
            case "institucion" -> nombresInst.getOrDefault(r.institucionId(), "Institución " + r.institucionId());
            case "corte" -> bucketCorte(r.fecha(), granularidad);
            default -> AnalisisConstantes.SIN_DATO;
        };
    }

    /**
     * "Corte de seguimiento" derivado de la fecha de la medición. Las etiquetas se
     * eligen para que ordenen lexicográficamente igual que cronológicamente:
     * {@code 2011-03} (mes), {@code 2011-T1} (trimestre), {@code 2011} (año).
     */
    private String bucketCorte(java.time.LocalDateTime fecha, String granularidad) {
        if (fecha == null) return AnalisisConstantes.SIN_DATO;
        LocalDate d = fecha.toLocalDate();
        return switch (granularidad == null ? "MES" : granularidad) {
            case "ANIO" -> String.valueOf(d.getYear());
            case "TRIMESTRE" -> d.getYear() + "-T" + ((d.getMonthValue() - 1) / 3 + 1);
            default -> String.format("%04d-%02d", d.getYear(), d.getMonthValue());
        };
    }

    private String bucketEdad(Integer edad, List<int[]> buckets) {
        if (buckets.isEmpty()) return "Todas las edades";
        if (edad == null) return AnalisisConstantes.SIN_DATO;
        for (int[] b : buckets) if (edad >= b[0] && edad <= b[1]) return b[0] + "–" + b[1];
        return "Otras edades";
    }

    private List<int[]> parsearRangosEdad(List<List<Integer>> rangos) {
        if (rangos == null) return List.of();
        List<int[]> out = new ArrayList<>();
        for (List<Integer> par : rangos) {
            if (par != null && par.size() == 2 && par.get(0) != null && par.get(1) != null) {
                out.add(new int[]{par.get(0), par.get(1)});
            }
        }
        return out;
    }

    private Map<Long, String> mapaNombresInstituciones(List<ResultadoProyeccion> filas) {
        Set<Long> ids = new HashSet<>();
        for (ResultadoProyeccion r : filas) if (r.institucionId() != null) ids.add(r.institucionId());
        Map<Long, String> mapa = new HashMap<>();
        for (Institucion i : institucionRepository.findAllById(ids)) mapa.put(i.getId(), i.getNombre());
        return mapa;
    }

    private Integer edadEnFecha(LocalDate nacimiento, LocalDate fecha) {
        if (nacimiento == null || fecha == null) return null;
        return Period.between(nacimiento, fecha).getYears();
    }

    // ── Estadística ─────────────────────────────────────────────────────────
    private double desviacion(double[] vals, double media) {
        if (vals.length < 2) return 0;
        double suma = 0;
        for (double v : vals) suma += (v - media) * (v - media);
        return Math.sqrt(suma / (vals.length - 1));
    }

    private double percentil(double[] ordenado, double p) {
        int n = ordenado.length;
        if (n == 0) return 0;
        if (n == 1) return ordenado[0];
        double idx = p * (n - 1);
        int lo = (int) Math.floor(idx), hi = (int) Math.ceil(idx);
        if (lo == hi) return ordenado[lo];
        double frac = idx - lo;
        return ordenado[lo] * (1 - frac) + ordenado[hi] * frac;
    }

    private double[] calcularBordes(Collection<List<ResultadoProyeccion>> grupos, int bins) {
        double min = Double.POSITIVE_INFINITY, max = Double.NEGATIVE_INFINITY;
        for (List<ResultadoProyeccion> g : grupos) {
            for (ResultadoProyeccion r : g) {
                if (r.valorNumerico() == null) continue;
                min = Math.min(min, r.valorNumerico());
                max = Math.max(max, r.valorNumerico());
            }
        }
        if (!Double.isFinite(min) || !Double.isFinite(max)) return new double[]{0, 1};
        if (min == max) max = min + 1;
        double[] bordes = new double[bins + 1];
        double paso = (max - min) / bins;
        for (int i = 0; i <= bins; i++) bordes[i] = min + paso * i;
        return bordes;
    }

    private List<BinHistogramaDTO> histograma(double[] valsOrdenados, double[] bordes) {
        int bins = bordes.length - 1;
        long[] conteo = new long[bins];
        for (double v : valsOrdenados) {
            int idx = (int) ((v - bordes[0]) / (bordes[bins] - bordes[0]) * bins);
            if (idx < 0) idx = 0;
            if (idx >= bins) idx = bins - 1;
            conteo[idx]++;
        }
        List<BinHistogramaDTO> out = new ArrayList<>(bins);
        for (int i = 0; i < bins; i++) out.add(new BinHistogramaDTO(redondear(bordes[i]), redondear(bordes[i + 1]), conteo[i]));
        return out;
    }

    private double redondear(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }
}
