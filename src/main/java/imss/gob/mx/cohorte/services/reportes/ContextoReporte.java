package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Todo lo que un reporte puede necesitar de un participante.
 *
 * <p>Un reporte no habla de un estudio, habla de una persona. Puede pedir cinco
 * parámetros de su DEXA, tres de sus signos vitales, y las evidencias solo del
 * primero. Por eso el contexto carga <b>todos</b> sus estudios, indexados por tipo,
 * en lugar de uno solo.</p>
 *
 * <p>Cuando un participante tiene varios estudios del mismo tipo se toma <b>el más
 * reciente</b>. Es lo que espera quien pide «el DEXA» sin más, y la alternativa
 * —obligar a elegir cuál en cada campo— convertiría el diseño de una plantilla en
 * un trabajo distinto por cada participante.</p>
 */
public class ContextoReporte {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Paciente paciente;
    private final List<EstudioMedico> estudios;
    private final List<ResultadoExamen> examenes;
    private final Totales totales;

    /**
     * El estudio sobre el que se emite, cuando el reporte es de uno concreto. En un
     * reporte de participante no hay ninguno y las claves sin tipo no resuelven.
     */
    private final EstudioMedico focal;

    /** El más reciente de cada tipo, calculado una vez. */
    private final Map<Long, EstudioMedico> masRecientePorTipo;

    /**
     * El resultado más reciente de cada analito de laboratorio. Misma regla que con
     * los estudios: quien pide «la glucosa» quiere la última que le tomaron.
     */
    private final Map<Long, ResultadoExamen> masRecientePorExamen;

    public record Totales(long estudios, long examenes, long muestras) {
        public static Totales sinCalcular() { return new Totales(-1, -1, -1); }
    }

    public ContextoReporte(Paciente paciente, List<EstudioMedico> estudios,
                           List<ResultadoExamen> examenes, EstudioMedico focal, Totales totales) {
        this.paciente = paciente;
        this.estudios = estudios != null ? estudios : List.of();
        this.examenes = examenes != null ? examenes : List.of();
        this.focal = focal;
        this.totales = totales != null ? totales : Totales.sinCalcular();
        this.masRecientePorTipo = indexarPorTipo(this.estudios);
        this.masRecientePorExamen = indexarExamenes(this.examenes);
    }

    /** Contexto de un solo estudio: el focal es ese, y también entra en el índice. */
    public static ContextoReporte deEstudio(EstudioMedico estudio, Totales totales) {
        return new ContextoReporte(estudio.getPaciente(), List.of(estudio), List.of(), estudio, totales);
    }

    private static Map<Long, EstudioMedico> indexarPorTipo(List<EstudioMedico> estudios) {
        Map<Long, EstudioMedico> mapa = new LinkedHashMap<>();
        for (EstudioMedico e : estudios) {
            if (e.getTipoEstudio() == null || e.getTipoEstudio().getId() == null) continue;
            Long idTipo = e.getTipoEstudio().getId();
            EstudioMedico actual = mapa.get(idTipo);
            if (actual == null || esPosterior(e, actual)) mapa.put(idTipo, e);
        }
        return mapa;
    }

    private static Map<Long, ResultadoExamen> indexarExamenes(List<ResultadoExamen> resultados) {
        Map<Long, ResultadoExamen> mapa = new LinkedHashMap<>();
        for (ResultadoExamen r : resultados) {
            if (r.getExamen() == null || r.getExamen().getId() == null) continue;
            Long id = r.getExamen().getId();
            ResultadoExamen actual = mapa.get(id);
            if (actual == null || esPosteriorExamen(r, actual)) mapa.put(id, r);
        }
        return mapa;
    }

    private static boolean esPosteriorExamen(ResultadoExamen candidato, ResultadoExamen actual) {
        LocalDateTime a = candidato.getFechaResultado();
        LocalDateTime b = actual.getFechaResultado();
        if (a == null) return false;
        if (b == null) return true;
        return a.isAfter(b);
    }

    private static boolean esPosterior(EstudioMedico candidato, EstudioMedico actual) {
        LocalDateTime a = candidato.getFechaEstudio();
        LocalDateTime b = actual.getFechaEstudio();
        if (a == null) return false;
        if (b == null) return true;
        return a.isAfter(b);
    }

    // ── Acceso ───────────────────────────────────────────────────────────────

    public Paciente paciente() { return paciente; }

    public Persona persona() {
        return paciente != null ? paciente.getPersona() : null;
    }

    public EstudioMedico focal() { return focal; }

    public List<EstudioMedico> estudios() { return estudios; }

    /** El estudio más reciente de ese tipo, si el participante tiene alguno. */
    public Optional<EstudioMedico> estudioDeTipo(Long idTipo) {
        return Optional.ofNullable(masRecientePorTipo.get(idTipo));
    }

    /** Los estudios ordenados del más reciente al más antiguo, para el listado. */
    public List<EstudioMedico> estudiosOrdenados() {
        return estudios.stream()
                .sorted(Comparator.comparing(
                        EstudioMedico::getFechaEstudio,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    /** El resultado de un parámetro dentro de un estudio, si se capturó. */
    public Optional<ResultadoEstudio> resultadoDe(EstudioMedico estudio, Long idParametro) {
        if (estudio == null || estudio.getResultadoEstudio() == null) return Optional.empty();
        return estudio.getResultadoEstudio().stream()
                .filter(r -> r.getParametro() != null
                        && idParametro.equals(r.getParametro().getId()))
                .findFirst();
    }

    /** El resultado más reciente de ese analito, si el participante lo tiene. */
    public Optional<ResultadoExamen> examenDe(Long idExamen) {
        return Optional.ofNullable(masRecientePorExamen.get(idExamen));
    }

    /** Los resultados de laboratorio, del más reciente al más antiguo. */
    public List<ResultadoExamen> examenesOrdenados() {
        return examenes.stream()
                .sorted(Comparator.comparing(
                        ResultadoExamen::getFechaResultado,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Totales totales() { return totales; }

    // ── Datos del participante, ya listos para imprimir ──────────────────────

    public String nombreCompleto() {
        Persona p = persona();
        if (p == null) return "";
        StringBuilder sb = new StringBuilder();
        anexar(sb, p.getNombre());
        anexar(sb, p.getSegundoNombre());
        anexar(sb, p.getApellidoPaterno());
        anexar(sb, p.getApellidoMaterno());
        return sb.toString();
    }

    public String folio() {
        return paciente != null && paciente.getFolio() != null ? paciente.getFolio() : "";
    }

    /**
     * La edad se calcula desde la fecha de nacimiento porque no se guarda. Depende
     * de cuándo se emita: un reporte reimpreso el año que viene dirá una edad
     * distinta, y eso es inevitable si no se archiva el documento.
     */
    public String edad() {
        Integer anios = edadEnAnios();
        return anios == null ? "" : anios + " años";
    }

    /**
     * La edad como número, para las fórmulas que la usan como variable.
     *
     * <p>Varias ecuaciones de referencia la llevan dentro —la distancia esperada en
     * la caminata de seis minutos, por ejemplo—, y ahí no sirve el texto «45 años».
     * Es la misma cuenta que {@link #edad()}, en un solo lugar.</p>
     */
    public Integer edadEnAnios() {
        Persona p = persona();
        if (p == null || p.getFechaNacimiento() == null) return null;
        return Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears();
    }

    public String sexo() {
        Persona p = persona();
        if (p == null || p.getSexo() == null) return "";
        return p.getSexo() == Persona.Sexo.F ? "Mujer" : "Hombre";
    }

    public String curp() {
        Persona p = persona();
        return p != null && p.getCurp() != null ? p.getCurp() : "";
    }

    public String fechaNacimiento() {
        Persona p = persona();
        return p != null && p.getFechaNacimiento() != null ? p.getFechaNacimiento().format(FECHA) : "";
    }

    public String nombreDe(Persona p) {
        if (p == null) return "";
        StringBuilder sb = new StringBuilder();
        anexar(sb, p.getNombre());
        anexar(sb, p.getApellidoPaterno());
        anexar(sb, p.getApellidoMaterno());
        return sb.toString();
    }

    public String fechaHora(LocalDateTime f) {
        return f == null ? "" : f.format(FECHA_HORA);
    }

    private void anexar(StringBuilder sb, String parte) {
        if (parte == null || parte.isBlank()) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(parte.trim());
    }
}
