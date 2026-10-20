package imss.gob.mx.cohorte.modules.cuestionarios.secciones;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un bloque de preguntas del instrumento. Es lo que el catálogo llama «bloque» y
 * lo que en el papel ocupa una página o un tramo de página.
 *
 * <h3>Por qué es una tabla y no un texto en la variable</h3>
 *
 * <p>Un tipo de estudio presenta sus parámetros en una sola lista porque son
 * decenas. Aquí son miles, y la pantalla de captura no puede ser una lista: hay
 * que partirla. La sección es la unidad de ese corte —lo que se carga, se muestra
 * y se guarda de una vez— y por eso necesita existir como entidad con su propio
 * orden, no como una etiqueta repetida en dos mil filas.</p>
 *
 * <p>{@code pagina} es la del papel, y se conserva aparte del orden porque sirve
 * para otra cosa: cuando alguien revisa una hoja escaneada contra lo capturado, la
 * busca por el número impreso en la esquina, no por la posición en la base.</p>
 */
@Entity
@Table(name = "seccion_cuestionario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seccion_cuestionario_nombre",
                columnNames = {"id_tipo_cuestionario", "nombre"}))
@Getter
@Setter
@NoArgsConstructor
public class SeccionCuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seccion")
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo_cuestionario", nullable = false)
    private TipoCuestionario tipoCuestionario;

    @Column(name = "nombre", nullable = false, length = 250)
    private String nombre;

    /**
     * El texto que el papel pone antes del bloque.
     *
     * <p>Casi todas las secciones del instrumento abren con un párrafo que explica
     * para qué se pregunta lo que viene —«los AGEs se forman principalmente durante
     * la cocción», «hablar de las relaciones violentas que hemos sufrido es algo
     * difícil»—. No es adorno: prepara para contestar, y en la sección de violencia
     * es lo que hace que alguien conteste con sinceridad. Al pasar a digital hay que
     * seguir mostrándolo.</p>
     */
    @Column(name = "descripcion", length = 2000)
    private String descripcion;

    /** El título impreso del bloque, cuando el papel le pone uno. */
    @Column(name = "titulo", length = 250)
    private String titulo;

    /** Página del instrumento impreso donde empieza el bloque. */
    @Column(name = "pagina")
    private Integer pagina;

    @Column(name = "orden", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer orden = 0;

    /**
     * Si el bloque se pinta como lista o como rejilla. Ver {@link PresentacionSeccion}.
     *
     * <p>El valor por omisión va escrito en la columna por el mismo motivo que en
     * {@code VariableCuestionario#operadorDependencia}: al añadir una columna
     * obligatoria, MySQL rellena las filas que ya había con el primer valor del tipo,
     * y ese orden lo decide el alfabeto. Aquí salió bien de casualidad —LISTA va antes
     * que MATRIZ—, y no conviene dejarlo a la suerte del siguiente valor que se
     * añada.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "presentacion", nullable = false,
            columnDefinition = "VARCHAR(10) DEFAULT 'LISTA'")
    private PresentacionSeccion presentacion = PresentacionSeccion.LISTA;
}
