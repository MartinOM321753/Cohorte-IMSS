package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Convierte la clave de un campo en el texto que se imprime.
 *
 * <p>Una clave que no se sepa resolver devuelve cadena vacía, nunca la clave ni un
 * error. En un documento clínico es preferible un hueco a ver impreso
 * «estudio.12.param.229» en medio de la hoja, y desde luego preferible a que la
 * emisión falle entera por un campo suelto de una plantilla vieja.</p>
 */
@Service
public class ResolvedorCampos {

    // Claves del participante y generales, que no dependen de ningún estudio.
    public static final String PARTICIPANTE_NOMBRE     = "participante.nombreCompleto";
    public static final String PARTICIPANTE_FOLIO      = "participante.folio";
    public static final String PARTICIPANTE_EDAD       = "participante.edad";
    public static final String PARTICIPANTE_SEXO       = "participante.sexo";
    public static final String PARTICIPANTE_CURP       = "participante.curp";
    public static final String PARTICIPANTE_NACIMIENTO = "participante.fechaNacimiento";
    public static final String INSTITUCION_NOMBRE      = "institucion.nombre";
    public static final String EMISION_FECHA           = "emision.fecha";
    public static final String TOTAL_ESTUDIOS          = "totales.estudios";
    public static final String TOTAL_EXAMENES          = "totales.examenes";
    public static final String TOTAL_MUESTRAS          = "totales.muestras";

    public String valorDe(String clave, ContextoReporte ctx) {
        if (clave == null || clave.isBlank()) return "";

        // Primero lo que depende de un tipo de estudio, que es lo específico.
        ClaveCampo.Parametro param = ClaveCampo.comoParametro(clave);
        if (param != null) return valorDeParametro(param, ctx);

        ClaveCampo.CampoEstudio campo = ClaveCampo.comoCampoEstudio(clave);
        if (campo != null) return valorDeCampoEstudio(campo, ctx);

        return switch (clave) {
            case PARTICIPANTE_NOMBRE     -> ctx.nombreCompleto();
            case PARTICIPANTE_FOLIO      -> ctx.folio();
            case PARTICIPANTE_EDAD       -> ctx.edad();
            case PARTICIPANTE_SEXO       -> ctx.sexo();
            case PARTICIPANTE_CURP       -> ctx.curp();
            case PARTICIPANTE_NACIMIENTO -> ctx.fechaNacimiento();

            case INSTITUCION_NOMBRE      -> institucionDe(ctx);
            case EMISION_FECHA           -> ctx.fechaHora(LocalDateTime.now());

            case TOTAL_ESTUDIOS          -> conteo(ctx.totales().estudios());
            case TOTAL_EXAMENES          -> conteo(ctx.totales().examenes());
            case TOTAL_MUESTRAS          -> conteo(ctx.totales().muestras());

            default -> "";
        };
    }

    /**
     * El valor de un parámetro dentro del estudio de ese tipo.
     *
     * <p>Que el participante no tenga ese estudio, o que ese estudio no midiera ese
     * parámetro, no es un error: es un hueco. La misma plantilla se usa con gente
     * distinta y no todos tienen los mismos estudios hechos.</p>
     */
    private String valorDeParametro(ClaveCampo.Parametro clave, ContextoReporte ctx) {
        Optional<EstudioMedico> estudio = ctx.estudioDeTipo(clave.idTipo());
        if (estudio.isEmpty()) return "";

        return ctx.resultadoDe(estudio.get(), clave.idParametro())
                .map(this::textoDelValor)
                .orElse("");
    }

    private String valorDeCampoEstudio(ClaveCampo.CampoEstudio clave, ContextoReporte ctx) {
        Optional<EstudioMedico> estudio = ctx.estudioDeTipo(clave.idTipo());
        if (estudio.isEmpty()) return "";
        EstudioMedico e = estudio.get();

        return switch (clave.campo()) {
            case "fecha"         -> ctx.fechaHora(e.getFechaEstudio());
            case "realizo"       -> e.getUsuarioRealiza() != null
                                    ? ctx.nombreDe(e.getUsuarioRealiza().getPersona()) : "";
            case "observaciones" -> e.getObservaciones() != null ? e.getObservaciones() : "";
            case "nombre"        -> e.getTipoEstudio() != null ? e.getTipoEstudio().getNombre() : "";
            default -> "";
        };
    }

    /** El valor guardado, según en qué columna lo dejó el tipo del parámetro. */
    public String textoDelValor(ResultadoEstudio r) {
        if (r == null) return "";
        if (r.getValorNumerico() != null) {
            double v = r.getValorNumerico();
            return v == Math.floor(v) && !Double.isInfinite(v)
                    ? String.valueOf((long) v) : String.valueOf(v);
        }
        if (r.getValorBooleano() != null) return r.getValorBooleano() ? "Sí" : "No";
        return r.getValorTexto() != null ? r.getValorTexto() : "";
    }

    private String institucionDe(ContextoReporte ctx) {
        if (ctx.focal() != null && ctx.focal().getInstitucion() != null) {
            return ctx.focal().getInstitucion().getNombre();
        }
        if (ctx.paciente() != null && ctx.paciente().getInstitucion() != null) {
            return ctx.paciente().getInstitucion().getNombre();
        }
        return "";
    }

    /** -1 significa que el conteo no se pidió; se deja en blanco, no en cero. */
    private String conteo(long valor) {
        return valor < 0 ? "" : String.valueOf(valor);
    }
}
