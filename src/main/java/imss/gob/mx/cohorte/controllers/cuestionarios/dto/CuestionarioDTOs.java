package imss.gob.mx.cohorte.controllers.cuestionarios.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Los objetos que viajan entre el navegador y el módulo de cuestionarios.
 *
 * <p>Van juntos en un archivo, al contrario que en estudios, y es a propósito: son
 * catorce formas pequeñas que solo tienen sentido leídas en conjunto —qué pide un
 * bloque, qué devuelve, cómo se declara una pregunta—. Repartidas en catorce archivos
 * de quince líneas, entender el contrato obliga a abrirlos todos.</p>
 *
 * <p>Son records y no clases con setters porque nada los modifica después de
 * construirlos; los de petición son clases porque la validación de Jakarta y el
 * deserializador de Jackson trabajan mejor con un constructor vacío y setters.</p>
 */
public final class CuestionarioDTOs {

    private CuestionarioDTOs() {}

    // ── Instrumentos ─────────────────────────────────────────────────────────

    @Data
    public static class TipoRequest {
        /** Prefijo de la etiqueta del papel: C1, C2… */
        @NotBlank
        @Pattern(regexp = "[A-Za-z]{1,2}\\d{0,2}",
                message = "La clave es una o dos letras seguidas de hasta dos digitos, como C1")
        private String clave;

        @NotBlank
        @Size(max = 150)
        private String nombre;

        @Size(max = 1000)
        private String descripcion;

        @NotBlank
        @Size(max = 50)
        private String version;

        /** Nombre de la columna del volcado que trae la etiqueta. */
        @Size(max = 100)
        private String columnaEtiqueta;

        /** A quien va dirigido por sexo: AMBOS, HOMBRES o MUJERES. */
        @Pattern(regexp = "AMBOS|HOMBRES|MUJERES",
                message = "dirigidoA debe ser AMBOS, HOMBRES o MUJERES")
        private String dirigidoA;

        /**
         * Edad minima y maxima en anos cumplidos. Nulo es sin limite por ese lado; una
         * edad exacta se expresa con las dos iguales.
         */
        @Min(0) @Max(130)
        private Integer edadMinima;

        @Min(0) @Max(130)
        private Integer edadMaxima;

        /**
         * Si el participante puede empezarlo por su cuenta. Apagado por omision: una
         * prueba cognitiva autoaplicada parece valida y no lo es.
         */
        private Boolean permiteAutollenado;

        private Boolean activo;
    }

    public record TipoResponse(
            Long id, String clave, String nombre, String descripcion, String version,
            String columnaEtiqueta, Boolean activo, LocalDateTime fechaCreacion,
            long totalVariables, long variablesSinDocumentar, long totalBloques,
            long cuestionariosContestados,
            String dirigidoA, Integer edadMinima, Integer edadMaxima,
            Boolean permiteAutollenado,
            /** El publico del instrumento en una frase, ya armada: «Mujeres de 18 a 49 anos». */
            String publico
    ) {}

    /**
     * Un instrumento del catalogo evaluado contra un participante concreto.
     *
     * @param aplica        si le corresponde
     * @param indeterminado si falta un dato del expediente para poder decidirlo. No es lo
     *                      mismo que no aplicar: el hueco es nuestro, no del participante.
     * @param motivo        en que no coincide, o que dato falta
     * @param edad          anos cumplidos hoy, cuando se pudieron calcular
     * @param yaRegistrado  si el participante ya tiene este cuestionario
     */
    public record TipoParaParticipanteResponse(
            Long idTipoCuestionario,
            String clave,
            String nombre,
            String version,
            /** El publico del instrumento en una frase: «Mujeres de 18 a 49 anos». */
            String publico,
            boolean aplica,
            boolean indeterminado,
            String motivo,
            Integer edad,
            boolean yaRegistrado
    ) {}

    // ── Bloques ──────────────────────────────────────────────────────────────

    @Data
    public static class SeccionRequest {
        @NotBlank
        @Size(max = 250)
        private String nombre;

        /** El titulo impreso del bloque, si el papel le pone uno. */
        @Size(max = 250)
        private String titulo;

        @Size(max = 2000)
        private String descripcion;

        private Integer pagina;
        private Integer orden;

        /** Cómo se pinta el bloque: {@code LISTA}, {@code MATRIZ} o {@code ESCALA}. */
        @Pattern(regexp = "LISTA|MATRIZ|ESCALA")
        private String presentacion;
    }

    public record SeccionResponse(
            Long id, String nombre, String titulo, String descripcion,
            Integer pagina, Integer orden, long totalVariables, String presentacion
    ) {}

    /**
     * Pone la misma dependencia a todas las preguntas de un bloque.
     *
     * <h3>Por qué existe este atajo</h3>
     *
     * <p>Los saltos del papel no son uno por pregunta: son uno por bloque. El
     * «Ninguna» de la 112 apaga <b>ciento treinta y cinco</b> preguntas, y
     * capturándolo pregunta por pregunta nadie lo haría nunca —y quien lo intentara
     * se equivocaría en alguna sin que se notara—.</p>
     *
     * <p>La pregunta llave se excluye sola: no puede depender de sí misma.</p>
     */
    @Data
    public static class DependenciaDeBloqueRequest {
        /** Código de la pregunta llave. Vacío quita la dependencia a todo el bloque. */
        @Size(max = 60)
        private String dependeDe;

        /** Con {@code EN}, varios valores separados por {@code |}. */
        @Size(max = 60)
        private String valorHabilitante;

        @Pattern(regexp = "IGUAL|DISTINTO|EN")
        private String operadorDependencia;
    }

    @Data
    public static class ReordenarRequest {
        @NotNull
        private List<Long> ids;
    }

    // ── Preguntas ────────────────────────────────────────────────────────────

    @Data
    public static class OpcionRequest {
        @NotBlank
        @Size(max = 20)
        private String codigo;

        @NotBlank
        @Size(max = 500)
        private String etiqueta;

        private Boolean esEscape;
    }

    @Data
    public static class VariableRequest {
        @NotBlank
        @Size(max = 60)
        private String codigo;

        @Size(max = 100)
        private String columnaVolcado;

        @Size(max = 2000)
        private String pregunta;

        @NotBlank
        @Pattern(regexp = "OPCION_UNICA|CASILLA|OPCION_MULTIPLE|NUMERO|NUMERO_CON_ESCAPE|TEXTO|FECHA|GRUPO")
        private String tipo;

        private Long idSeccion;
        private Integer orden;
        private Integer pagina;

        @Size(max = 60)
        private String grupoMultiple;

        /** Código de la pregunta que habilita a esta. */
        @Size(max = 60)
        private String dependeDe;

        /** Con {@code EN}, varios valores separados por {@code |}. */
        @Size(max = 60)
        private String valorHabilitante;

        /** Cómo se compara la llave: {@code IGUAL} o {@code DISTINTO}. */
        @Pattern(regexp = "IGUAL|DISTINTO|EN")
        private String operadorDependencia;

        /** Celda que ocupa cuando el bloque se pinta como rejilla. */
        @Size(max = 150)
        private String filaMatriz;

        @Size(max = 150)
        private String columnaMatriz;

        private Boolean obligatoria;
        private Boolean documentada;
        private Boolean activa;
        private Double valorMin;
        private Double valorMax;
        private Integer longitudMax;

        @Size(max = 30)
        private String unidad;

        @Size(max = 1000)
        private String observaciones;

        /** La aclaracion que el papel imprime junto a la pregunta. Se le muestra. */
        @Size(max = 1000)
        private String instruccion;

        /** Ruta de la figura sin la cual la pregunta no se puede contestar. */
        @Size(max = 200)
        private String figura;

        @Valid
        private List<OpcionRequest> opciones;
    }

    public record OpcionResponse(Long id, String codigo, String etiqueta, Integer orden, Boolean esEscape) {}

    public record VariableResponse(
            Long id, String codigo, String columnaVolcado, String pregunta, String tipo,
            Long idSeccion, String nombreSeccion, Integer orden, Integer pagina,
            String grupoMultiple, String dependeDe, String valorHabilitante,
            String operadorDependencia, String filaMatriz, String columnaMatriz,
            Boolean obligatoria, Boolean documentada, Boolean activa,
            Double valorMin, Double valorMax, Integer longitudMax, String unidad,
            String observaciones, String instruccion, String figura,
            List<OpcionResponse> opciones
    ) {}

    // ── Cuestionarios contestados ────────────────────────────────────────────

    @Data
    public static class AbrirRequest {
        @NotNull
        private Long idTipoCuestionario;

        @NotBlank
        private String uuidParticipante;

        @Pattern(regexp = "APLICACION|CAPTURA_MANUAL|PAPEL_REMARK")
        private String origen;
    }

    public record CuestionarioResponse(
            Long id, Long idTipoCuestionario, String claveCuestionario, String nombreCuestionario,
            String uuidParticipante, String folioParticipante, String nombreParticipante,
            String etiqueta, String estado, String origen,
            LocalDate fechaAplicacion, LocalDateTime fechaRegistro,
            String usuarioCaptura, String usuarioValida, LocalDateTime fechaValidacion,
            String archivoOrigen, Integer respuestasPendientes, String observaciones,
            Boolean fueraDeCriterio, String motivoFueraDeCriterio
    ) {}

    public record RespuestaResponse(
            Long id, Long idVariable, String codigoVariable, Integer ordenRespuesta,
            String valorCodigo, Double valorNumerico, String valorTexto, LocalDate valorFecha,
            String marcaAusencia, String valorCrudo, String celdaOrigen
    ) {}

    /** Un bloque del formulario: qué preguntar, qué hay contestado y qué está habilitado. */
    public record BloqueResponse(
            SeccionResponse seccion,
            List<VariableResponse> variables,
            List<RespuestaResponse> respuestas,
            List<Long> variablesHabilitadas
    ) {}

    @Data
    public static class RespuestaRequest {
        @NotNull
        private Long idVariable;

        private Integer ordenRespuesta;

        /**
         * Lo contestado, tal como se teclea. Nulo o vacío significa sin respuesta, que
         * no es lo mismo que no mandar la pregunta: lo que no viene en la lista se
         * borra.
         */
        private String valor;
    }

    @Data
    public static class GuardarBloqueRequest {
        @Valid
        private List<RespuestaRequest> respuestas;
    }

    @Data
    public static class NotaRequest {
        @Size(max = 1000)
        private String nota;
    }

    @Data
    public static class AnularRequest {
        @NotBlank(message = "Indica el motivo por el que se anula el cuestionario")
        @Size(max = 1000)
        private String motivo;
    }

    // ── Lecturas en desacuerdo ───────────────────────────────────────────────

    public record DiscrepanciaResponse(
            Long id, Long idVariable, String codigoVariable, String columnaVolcado,
            String celdaOrigen, String valorEnFirme, String lecturasAlternas,
            Boolean resuelta, String valorAdjudicado, String usuarioResuelve,
            LocalDateTime fechaResolucion, String nota
    ) {}

    @Data
    public static class ResolverRequest {
        @NotNull(message = "Indica cual de las lecturas vale")
        private String valorElegido;

        @Size(max = 500)
        private String nota;
    }
}
