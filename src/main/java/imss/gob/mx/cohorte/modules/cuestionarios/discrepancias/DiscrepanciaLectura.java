package imss.gob.mx.cohorte.modules.cuestionarios.discrepancias;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.cuestionarios.Cuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Una celda del volcado sobre la que hubo más de una lectura.
 *
 * <h3>Qué es realmente la hoja «Duplicados»</h3>
 *
 * <p>Parece un registro de correcciones aplicadas —valor anterior, valor nuevo—
 * pero no lo es. En los dos volcados recibidos, <em>todas</em> las filas de esa
 * hoja tienen en «Datos Limpios» el valor <b>previo</b>, no el nuevo: cincuenta y
 * dos de cincuenta y dos en el último. Y hay celdas con tres lecturas distintas,
 * que en un esquema de «antes y después» no cabrían.</p>
 *
 * <p>Así que es un registro de desacuerdos sin resolver: alguien o algo leyó la
 * misma casilla de varias formas y nadie decidió cuál vale. Importarlo como si
 * fueran correcciones sobrescribiría datos con valores que nadie adjudicó; no
 * importarlo perdería la única señal de que esa celda es dudosa. Se guarda
 * aparte, se marca sin resolver, y la respuesta en firme sigue siendo la de «Datos
 * Limpios» hasta que una persona decida.</p>
 */
@Entity
@Table(name = "discrepancia_lectura",
        indexes = {
                @Index(name = "ix_discrepancia_cuestionario", columnList = "id_cuestionario, resuelta"),
                @Index(name = "ix_discrepancia_variable", columnList = "id_variable")
        })
@Getter
@Setter
@NoArgsConstructor
public class DiscrepanciaLectura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_discrepancia")
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cuestionario", nullable = false)
    private Cuestionario cuestionario;

    /**
     * Variable afectada. Admite nulo: la hoja puede señalar una columna que el
     * catálogo no documenta, y perder el aviso por eso sería quedarse sin la parte
     * útil del dato.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_variable")
    private VariableCuestionario variable;

    /** Columna tal como la nombra el volcado, incluso si no se resolvió a variable. */
    @Column(name = "columna_volcado", length = 100)
    private String columnaVolcado;

    /** Referencia de celda en la hoja original ({@code G6}). */
    @Column(name = "celda_origen", length = 20)
    private String celdaOrigen;

    /** La lectura que quedó en «Datos Limpios» y que por tanto se guardó como respuesta. */
    @Column(name = "valor_en_firme", length = 255)
    private String valorEnFirme;

    /**
     * Las otras lecturas de la misma celda, separadas por {@code |}.
     *
     * <p>Es una lista en un texto y no una tabla aparte a propósito: no se consulta
     * ni se agrega por ella, solo se le muestra a quien va a adjudicar la celda.
     * Una tabla más añadiría un salto a cada lectura sin comprar nada.</p>
     */
    @Column(name = "lecturas_alternas", length = 1000)
    private String lecturasAlternas;

    @Column(name = "resuelta", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean resuelta = false;

    /** El valor que una persona decidió que vale. Nulo mientras nadie decide. */
    @Column(name = "valor_adjudicado", length = 255)
    private String valorAdjudicado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_resuelve")
    private BeanUser usuarioResuelve;

    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;

    @Column(name = "nota", length = 500)
    private String nota;
}
