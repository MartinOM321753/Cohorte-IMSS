package imss.gob.mx.cohorte.modules.cuestionarios.respuestas;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.cuestionarios.Cuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Lo que un participante contestó a una variable.
 *
 * <h3>Se guarda el código, no la etiqueta</h3>
 *
 * <p>{@link #valorCodigo} es el dígito que vino del volcado o que eligió quien
 * contestó en pantalla. La etiqueta no se copia aquí: vive en
 * {@code OpcionVariable} y se resuelve al mostrar. Copiarla parecería cómodo
 * —ahorraría una unión— pero congelaría el texto de la opción en el momento de la
 * captura, y entonces corregir una etiqueta mal transcrita dejaría la base con dos
 * redacciones de la misma opción según la fecha en que se capturó cada
 * cuestionario. El código es el dato; la etiqueta es su presentación.</p>
 *
 * <h3>Por qué hay una fila incluso cuando no hay respuesta</h3>
 *
 * <p>Podría parecer desperdicio guardar dos mil filas por participante cuando
 * muchas están vacías. No lo es: la fila vacía es la que lleva la
 * {@link MarcaAusencia}, y sin ella «no contestó», «el escáner no pudo leerlo» y
 * «esta pregunta no se le hizo» serían el mismo hueco. La distinción es
 * precisamente lo que permite saber qué hay que recapturar, y hoy afecta a casi el
 * noventa por ciento de los campos de texto del instrumento.</p>
 *
 * <p>La excepción son las variables deshabilitadas por su llave: esas no generan
 * fila, porque no se preguntaron.</p>
 */
@Entity
@Table(name = "respuesta_cuestionario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_respuesta_cuestionario_variable",
                columnNames = {"id_cuestionario", "id_variable", "orden_respuesta"}),
        indexes = {
                @Index(name = "ix_respuesta_variable", columnList = "id_variable"),
                @Index(name = "ix_respuesta_pendiente", columnList = "id_cuestionario, marca_ausencia")
        })
@Getter
@Setter
@NoArgsConstructor
public class RespuestaCuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respuesta")
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuestionario", nullable = false)
    private Cuestionario cuestionario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_variable", nullable = false)
    private VariableCuestionario variable;

    /**
     * Posición dentro de una variable que admite varias respuestas. Cero cuando
     * admite una sola, que es el caso de casi todas.
     */
    @Column(name = "orden_respuesta", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer ordenRespuesta = 0;

    /** Código de la opción seleccionada. Ver la nota de la clase. */
    @Column(name = "valor_codigo", length = 20)
    private String valorCodigo;

    /** Cantidad, para NUMERO y NUMERO_CON_ESCAPE. */
    @Column(name = "valor_numerico")
    private Double valorNumerico;

    @Column(name = "valor_texto", length = 1000)
    private String valorTexto;

    @Column(name = "valor_fecha")
    private LocalDate valorFecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "marca_ausencia", nullable = false, length = 20)
    private MarcaAusencia marcaAusencia = MarcaAusencia.NINGUNA;

    /**
     * Lo que decía la celda, literal, cuando no se pudo interpretar.
     *
     * <p>No es para mostrarlo: es para poder recuperar el dato si mañana se
     * descubre que la interpretación estaba mal. Un volcado se carga una vez y el
     * archivo se pierde; lo que no quedó guardado no vuelve.</p>
     */
    @Column(name = "valor_crudo", length = 255)
    private String valorCrudo;

    /**
     * Referencia de celda en la hoja de origen ({@code G6}), si vino de un volcado.
     * Es como el usuario va a buscar el dato en su Excel.
     */
    @Column(name = "celda_origen", length = 20)
    private String celdaOrigen;

    /**
     * Contador que el cliente incrementa en cada cambio de esta respuesta.
     *
     * <h3>Para que sirve</h3>
     *
     * <p>El autoguardado manda cambios por su cuenta, reintenta los que fallaron y
     * recupera los que quedaron pendientes al perder la conexion. Eso significa que dos
     * peticiones sobre la misma respuesta pueden llegar en orden distinto al que se
     * produjeron: el participante escribe «5», lo borra, y si el «5» aterriza al final
     * reaparece solo. Sin algo que ordene los cambios, ese error es inevitable y
     * ademas intermitente, de los que no se reproducen cuando se buscan.</p>
     *
     * <p>Con la secuencia, el servidor descarta todo cambio cuyo contador no sea mayor
     * que el guardado. Eso hace el guardado <b>idempotente</b> —reenviar el mismo cambio
     * no hace nada— y <b>conmutativo</b>: el resultado final no depende del orden de
     * llegada. Es lo que permite que el cliente reintente sin miedo.</p>
     *
     * <p>Nulo en lo que vino de un volcado: ahi no hay cliente que lleve la cuenta.</p>
     */
    @Column(name = "secuencia_cliente")
    private Long secuenciaCliente;

    /** Cuando se escribio por ultima vez. Es lo que la pantalla muestra como «guardado». */
    @Column(name = "fecha_actualizacion")
    private java.time.LocalDateTime fechaActualizacion;

    /** Si tiene valor utilizable. */
    @Transient
    public boolean tieneValor() {
        return this.marcaAusencia == MarcaAusencia.NINGUNA;
    }

    /**
     * Si cuenta como contestada para la completitud, que no es lo mismo que tener
     * valor.
     *
     * <h3>La casilla sin marcar también es una respuesta</h3>
     *
     * <p>Quien no nada deja vacía la casilla «Nadar Lunes», y eso no es un hueco: es un
     * «no». Contarla como pendiente daría cuentas absurdas —la rejilla de la 112 tiene
     * ciento cinco casillas y un participante normal marca cuatro o cinco— y, lo que es
     * peor, haría imposible cerrar un cuestionario completo.</p>
     *
     * <p>Solo vale para las casillas. En una pregunta de opciones, «cero marcas» sí es
     * un hueco: alguien tenía que elegir una de las opciones y no hay ninguna elegida.</p>
     */
    @Transient
    public boolean cuentaComoContestada() {
        if (tieneValor()) return true;
        if (this.marcaAusencia != MarcaAusencia.CERO_MARCAS) return false;
        return this.variable != null
                && (this.variable.getTipo() == TipoVariable.CASILLA
                    || this.variable.getTipo() == TipoVariable.OPCION_MULTIPLE);
    }
}
