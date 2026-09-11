package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.services.formulas.Magnitud;
import imss.gob.mx.cohorte.services.formulas.Unidad;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
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

    /**
     * Con qué resolver las fórmulas del catálogo.
     *
     * <p>Van por inyección de propiedad y no por constructor porque el resolvedor
     * también se construye a mano —en las pruebas del maquetador, sin base de datos
     * detrás—, y ahí no hay fórmulas que resolver ni tiene por qué haberlas. Sin ellas
     * una clave {@code formula.…} devuelve cadena vacía, que es exactamente lo que hace
     * con cualquier otra clave que no sepa resolver.</p>
     */
    private FormulaReporteService formulaService;
    private CalculadoraFormulas calculadora;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void conFormulas(FormulaReporteService formulaService, CalculadoraFormulas calculadora) {
        this.formulaService = formulaService;
        this.calculadora = calculadora;
    }

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

        ClaveCampo.CampoExamen examen = ClaveCampo.comoCampoExamen(clave);
        if (examen != null) return valorDeExamen(examen, ctx);

        Long idFormula = ClaveCampo.comoFormula(clave);
        if (idFormula != null) return valorDeFormula(idFormula, ctx);

        ClaveCampo.ParteFormula parte = ClaveCampo.comoParteDeFormula(clave);
        if (parte != null) return valorDeParteDeFormula(parte, ctx);

        String resumen = ClaveCampo.comoResumen(clave);
        if (resumen != null) return conteoDelResumen(resumen, ctx);

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
     * Cuántas mediciones de laboratorio caen en cada situación.
     *
     * <p>Se cuenta aquí y no en el bloque de la lista porque estos números encabezan
     * el reporte, arriba del todo, y la lista puede ir tres páginas más abajo o no
     * estar. Que el conteo dependiera de haber dibujado la lista sería la clase de
     * atadura que un día deja el encabezado en cero sin motivo aparente.</p>
     */
    private String conteoDelResumen(String parte, ContextoReporte ctx) {
        imss.gob.mx.cohorte.modules.persona.Persona.Sexo sexo =
                ctx.persona() != null ? ctx.persona().getSexo() : null;

        int total = 0, enRango = 0, ligeramente = 0, revisar = 0, sinDato = 0;
        for (var r : ctx.examenesOrdenados()) {
            if (r.getExamen() == null) continue;
            total++;
            switch (BloqueExamenes.estadoDe(r, sexo)) {
                case EN_RANGO -> enRango++;
                case LIGERAMENTE_FUERA -> ligeramente++;
                case REVISAR -> revisar++;
                case SIN_DATO -> sinDato++;
            }
        }

        return String.valueOf(switch (parte) {
            case "total" -> total;
            case "enRango" -> enRango;
            case "ligeramenteFuera" -> ligeramente;
            case "revisar" -> revisar;
            case "sinDato" -> sinDato;
            default -> 0;
        });
    }

    /**
     * El resultado de una fórmula del catálogo, ya listo para imprimir.
     *
     * <p>Una fórmula que se borró, o que pertenece a otra institución, deja hueco. Es
     * la misma regla que con cualquier otro campo: una plantilla puede sobrevivir a que
     * le quiten de debajo algo que mencionaba, y un documento con un hueco vale más que
     * una emisión que falla entera.</p>
     */
    private String valorDeFormula(Long idFormula, ContextoReporte ctx) {
        if (formulaService == null || calculadora == null) return "";

        return formulaService.buscar(idFormula)
                .map(f -> calculadora.texto(f, ctx, clave -> magnitudDe(clave, ctx)))
                .orElse("");
    }

    /**
     * Los límites de una fórmula, su referencia escrita o si el valor cae dentro.
     *
     * <p>Van como claves propias para que quien diseña pueda ponerlas donde quiera: la
     * referencia en su columna, el estado en otra, o ninguna de las dos. El documento no
     * impone una forma de tabla, así que el sistema tampoco.</p>
     */
    private String valorDeParteDeFormula(ClaveCampo.ParteFormula parte, ContextoReporte ctx) {
        if (formulaService == null || calculadora == null) return "";

        return formulaService.buscar(parte.idFormula())
                .map(f -> {
                    java.util.function.Function<String, Magnitud> porClave =
                            clave -> magnitudDe(clave, ctx);
                    return switch (parte.parte()) {
                        case "minimo"     -> calculadora.minimo(f, ctx, porClave).texto(f.getDecimales());
                        case "maximo"     -> calculadora.maximo(f, ctx, porClave).texto(f.getDecimales());
                        case "referencia" -> calculadora.textoReferencia(f, ctx, porClave);
                        case "estado"     -> calculadora.estado(f, ctx, porClave);
                        default -> "";
                    };
                })
                .orElse("");
    }

    // ── Acceso numérico, para las fórmulas ───────────────────────────────────

    /**
     * El mismo dato que {@link #valorDe}, pero como número con su unidad.
     *
     * <p>Una fórmula no puede multiplicar «165 cm». Necesita el 165 y necesita saber
     * que son centímetros, porque el mismo dato en metros da un resultado diez mil
     * veces distinto en cuanto se eleva al cuadrado.</p>
     *
     * <p>Solo resuelven las claves que llevan un número detrás: los parámetros
     * numéricos de un estudio, el valor de un examen de laboratorio y la edad. Todo
     * lo demás —un nombre, una fecha, un parámetro de texto o de opciones— devuelve
     * <b>ausente</b>, igual que un dato que no se capturó: no hay número que dar, y
     * el cero no es una respuesta.</p>
     *
     * <p><b>Las claves {@code formula.…} tampoco resuelven aquí, y es a propósito.</b>
     * Una fórmula se calcula con esta misma función, así que dejar que una variable
     * apunte a otra fórmula abriría la puerta a que dos se llamaran entre sí y el
     * cálculo no terminara nunca. Una fórmula usa datos capturados, no resultados de
     * otras fórmulas.</p>
     */
    public Magnitud magnitudDe(String clave, ContextoReporte ctx) {
        if (clave == null || clave.isBlank() || ctx == null) return Magnitud.sinDato();

        ClaveCampo.Parametro param = ClaveCampo.comoParametro(clave);
        if (param != null) return magnitudDeParametro(param, ctx);

        ClaveCampo.CampoExamen examen = ClaveCampo.comoCampoExamen(clave);
        if (examen != null && "valor".equals(examen.campo())) {
            return ctx.examenDe(examen.idExamen())
                    .map(r -> Magnitud.de(r.getValorObtenido(),
                            r.getExamen() != null ? r.getExamen().getUnidad() : null))
                    .orElseGet(Magnitud::sinDato);
        }

        if (PARTICIPANTE_EDAD.equals(clave)) {
            Integer anios = ctx.edadEnAnios();
            return anios == null
                    ? Magnitud.sinDato()
                    : Magnitud.de(BigDecimal.valueOf(anios), Unidad.de("años"));
        }

        if (PARTICIPANTE_SEXO.equals(clave)) return sexoComoNumero(ctx);

        return Magnitud.sinDato();
    }

    /**
     * El sexo como número, para las fórmulas que se ramifican con él.
     *
     * <p>Muchas ecuaciones de referencia traen dos versiones, una por sexo, y se
     * escriben en una sola fórmula con {@code si(sexo = 1, …, …)}. Para eso hace falta
     * algo que se pueda comparar, y «Mujer» no lo es.</p>
     *
     * <p><b>1 es mujer y 0 es hombre.</b> La elección es arbitraria y lo que importa es
     * que sea siempre la misma: se muestra escrita en el selector de variables, para
     * que quien arma la fórmula no tenga que adivinarla ni recordarla.</p>
     *
     * <p>Sin sexo registrado el resultado es ausente, no cero. Cero sería «hombre», y
     * ramificaría hacia una de las dos ecuaciones sin que nadie lo hubiera decidido.</p>
     */
    private Magnitud sexoComoNumero(ContextoReporte ctx) {
        if (ctx.persona() == null || ctx.persona().getSexo() == null) return Magnitud.sinDato();
        boolean mujer = ctx.persona().getSexo() == imss.gob.mx.cohorte.modules.persona.Persona.Sexo.F;
        return Magnitud.de(mujer ? BigDecimal.ONE : BigDecimal.ZERO, Unidad.NINGUNA);
    }

    /**
     * Un parámetro de estudio como número.
     *
     * <p>La unidad sale del catálogo del parámetro, no del resultado: es el parámetro
     * el que declara en qué se mide, y todos sus resultados comparten esa unidad.</p>
     */
    private Magnitud magnitudDeParametro(ClaveCampo.Parametro clave, ContextoReporte ctx) {
        Optional<EstudioMedico> estudio = ctx.estudioDeTipo(clave.idTipo());
        if (estudio.isEmpty()) return Magnitud.sinDato();

        return ctx.resultadoDe(estudio.get(), clave.idParametro())
                .map(this::magnitudDelResultado)
                .orElseGet(Magnitud::sinDato);
    }

    /**
     * El resultado como cantidad, según en qué columna lo dejó su tipo.
     *
     * <p>Los de opciones y los de sí/no llegan como texto porque eso es lo que son. No
     * se pueden sumar, y el evaluador lo impide, pero sí comparar: es lo que permite
     * escribir {@code si(manoDominante = 'Derecha', …, …)}, que en esta cohorte es un
     * caso real —la dinamometría se reporta de la mano dominante—.</p>
     */
    private Magnitud magnitudDelResultado(ResultadoEstudio r) {
        if (r.getValorNumerico() != null) {
            return Magnitud.de(r.getValorNumerico(),
                    r.getParametro() != null ? r.getParametro().getUnidad() : null);
        }
        if (r.getValorBooleano() != null) return Magnitud.deTexto(r.getValorBooleano() ? "Sí" : "No");
        return Magnitud.deTexto(r.getValorTexto());
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

    /**
     * Un analito de laboratorio del participante.
     *
     * <p>Un examen es un analito suelto, no un panel: cada uno tiene su propio
     * resultado y su propia fecha. Se toma el más reciente, igual que con los
     * estudios.</p>
     */
    private String valorDeExamen(ClaveCampo.CampoExamen clave, ContextoReporte ctx) {
        var resultado = ctx.examenDe(clave.idExamen());
        if (resultado.isEmpty()) return "";
        var r = resultado.get();

        return switch (clave.campo()) {
            case "valor" -> numero(r.getValorObtenido());
            case "fecha" -> ctx.fechaHora(r.getFechaResultado());
            case "unidad" -> r.getExamen() != null && r.getExamen().getUnidad() != null
                             ? r.getExamen().getUnidad() : "";
            case "referencia" -> referenciaDe(r, ctx);
            default -> "";
        };
    }

    /** El rango del analito según el sexo del participante, como texto. */
    private String referenciaDe(imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen r,
                                ContextoReporte ctx) {
        if (r.getExamen() == null || ctx.persona() == null || ctx.persona().getSexo() == null) return "";
        boolean mujer = ctx.persona().getSexo() == imss.gob.mx.cohorte.modules.persona.Persona.Sexo.F;
        Double min = mujer ? r.getExamen().getValorMinMujeres() : r.getExamen().getValorMinHombres();
        Double max = mujer ? r.getExamen().getValorMaxMujeres() : r.getExamen().getValorMaxHombres();
        return RangoReferencia.texto(min == null && max == null ? null
                : new RangoReferencia.Rango(min, max));
    }

    /** Quita el «.0» de los enteros: «120» se lee mejor que «120.0» en un reporte. */
    public String numero(Double d) {
        if (d == null) return "";
        return d == Math.floor(d) && !d.isInfinite()
                ? String.valueOf(d.longValue()) : String.valueOf(d);
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
