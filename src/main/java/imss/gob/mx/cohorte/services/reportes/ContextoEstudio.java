package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.persona.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;

import static imss.gob.mx.cohorte.services.reportes.CatalogoCamposReporte.*;

/**
 * Resuelve los campos de un reporte de estudio contra los datos reales.
 *
 * <p>Un campo que no se sepa resolver devuelve cadena vacía, nunca la clave ni un
 * error. En un documento clínico es preferible un hueco a un texto como
 * «{{participante.curp}}» impreso en medio de la hoja, y desde luego preferible a
 * que la emisión falle entera por un campo suelto.</p>
 */
public class ContextoEstudio {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final EstudioMedico estudio;
    private final Totales totales;

    /** Conteos del participante. Se piden aparte porque no cuelgan del estudio. */
    public record Totales(long estudios, long examenes, long muestras) {
        public static Totales sinCalcular() { return new Totales(-1, -1, -1); }
    }

    public ContextoEstudio(EstudioMedico estudio, Totales totales) {
        this.estudio = estudio;
        this.totales = totales != null ? totales : Totales.sinCalcular();
    }

    public EstudioMedico estudio() {
        return estudio;
    }

    public Persona persona() {
        Paciente p = estudio.getPaciente();
        return p != null ? p.getPersona() : null;
    }

    /** El valor de un campo, ya listo para imprimirse. */
    public String valorDe(String clave) {
        if (clave == null) return "";
        return switch (clave) {
            case PARTICIPANTE_NOMBRE     -> nombreCompleto();
            case PARTICIPANTE_FOLIO      -> estudio.getPaciente() != null
                                            ? texto(estudio.getPaciente().getFolio()) : "";
            case PARTICIPANTE_EDAD       -> edad();
            case PARTICIPANTE_SEXO       -> sexo();
            case PARTICIPANTE_CURP       -> persona() != null ? texto(persona().getCurp()) : "";
            case PARTICIPANTE_NACIMIENTO -> persona() != null && persona().getFechaNacimiento() != null
                                            ? persona().getFechaNacimiento().format(FECHA) : "";

            case ESTUDIO_TIPO            -> estudio.getTipoEstudio() != null
                                            ? texto(estudio.getTipoEstudio().getNombre()) : "";
            case ESTUDIO_FECHA           -> fechaHora(estudio.getFechaEstudio());
            case ESTUDIO_REALIZO         -> quienRealizo();
            case ESTUDIO_OBSERVACIONES   -> texto(estudio.getObservaciones());

            case INSTITUCION_NOMBRE      -> estudio.getInstitucion() != null
                                            ? texto(estudio.getInstitucion().getNombre()) : "";
            case EMISION_FECHA           -> fechaHora(LocalDateTime.now());

            case TOTAL_ESTUDIOS          -> conteo(totales.estudios());
            case TOTAL_EXAMENES          -> conteo(totales.examenes());
            case TOTAL_MUESTRAS          -> conteo(totales.muestras());

            // Una clave desconocida es una plantilla hecha con una versión que
            // ofrecía algo que ya no existe. Se deja el hueco y el resto del
            // documento sale igual.
            default -> "";
        };
    }

    // ── Resolución de cada cosa ──────────────────────────────────────────────

    private String nombreCompleto() {
        Persona p = persona();
        if (p == null) return "";
        StringBuilder sb = new StringBuilder();
        anexar(sb, p.getNombre());
        anexar(sb, p.getSegundoNombre());
        anexar(sb, p.getApellidoPaterno());
        anexar(sb, p.getApellidoMaterno());
        return sb.toString();
    }

    private void anexar(StringBuilder sb, String parte) {
        if (parte == null || parte.isBlank()) return;
        if (sb.length() > 0) sb.append(' ');
        sb.append(parte.trim());
    }

    /**
     * La edad no se guarda: se calcula desde la fecha de nacimiento, y por eso
     * depende de cuándo se emita el documento. Un reporte del año pasado reimpreso
     * hoy diría una edad distinta — es inevitable sin archivar el documento.
     */
    private String edad() {
        Persona p = persona();
        if (p == null || p.getFechaNacimiento() == null) return "";
        return Period.between(p.getFechaNacimiento(), LocalDate.now()).getYears() + " años";
    }

    private String sexo() {
        Persona p = persona();
        if (p == null || p.getSexo() == null) return "";
        return p.getSexo() == Persona.Sexo.F ? "Mujer" : "Hombre";
    }

    private String quienRealizo() {
        if (estudio.getUsuarioRealiza() == null) return "";
        Persona p = estudio.getUsuarioRealiza().getPersona();
        if (p == null) return "";
        StringBuilder sb = new StringBuilder();
        anexar(sb, p.getNombre());
        anexar(sb, p.getApellidoPaterno());
        anexar(sb, p.getApellidoMaterno());
        return sb.toString();
    }

    /** -1 significa que el conteo no se pidió; se deja en blanco, no en cero. */
    private String conteo(long valor) {
        return valor < 0 ? "" : String.valueOf(valor);
    }

    private String fechaHora(LocalDateTime f) {
        return f == null ? "" : f.format(FECHA_HORA);
    }

    private String texto(String s) {
        return s == null ? "" : s;
    }
}
