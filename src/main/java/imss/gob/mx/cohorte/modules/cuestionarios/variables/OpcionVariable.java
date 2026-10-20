package imss.gob.mx.cohorte.modules.cuestionarios.variables;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una opción de respuesta: el código que se guarda y la etiqueta que se lee.
 *
 * <h3>El código es parte del dato, no un detalle de presentación</h3>
 *
 * <p>{@code OpcionParametro}, en estudios, solo guarda el texto de la opción: ahí
 * lo que se almacena como resultado es ese mismo texto. Aquí no. El volcado trae
 * un dígito —{@code 7}— y lo que significa depende del catálogo de la variable.
 * Si el código no se guardara, la respuesta {@code 7} de {@code P6} no tendría
 * forma de convertirse en «Normal Superior Completa», y el catálogo que los
 * investigadores van a usar para pedir información está escrito precisamente en
 * esos códigos.</p>
 *
 * <p>De ahí que el código sea único dentro de la variable y no se reasigne nunca:
 * cambiarlo reinterpretaría todas las respuestas ya guardadas, sin dejar rastro.
 * Para corregir un significado se cambia la etiqueta; para cambiar el conjunto de
 * opciones se versiona el instrumento.</p>
 */
@Entity
@Table(name = "opcion_variable",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_opcion_variable_codigo",
                columnNames = {"id_variable", "codigo"}))
@Getter
@Setter
@NoArgsConstructor
public class OpcionVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_opcion")
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_variable", nullable = false)
    private VariableCuestionario variable;

    /** Lo que trae el volcado y lo que se guarda en la respuesta. */
    @Column(name = "codigo", nullable = false, length = 20)
    private String codigo;

    /** Lo que se le muestra a la persona. */
    @Column(name = "etiqueta", nullable = false, length = 500)
    private String etiqueta;

    /**
     * Posición impresa de la opción.
     *
     * <p>Importa más de lo que parece: el papel reparte las opciones en varias
     * columnas y las numera de arriba abajo y después de izquierda a derecha, no
     * leyéndolas como un párrafo. Un orden equivocado aquí es exactamente el error
     * que tenía el catálogo en {@code P6}.</p>
     */
    @Column(name = "orden", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer orden = 0;

    /**
     * Si esta opción es un escape y no una respuesta sustantiva: «no sabe»,
     * «no contesta», «no aplica».
     *
     * <p>Se marca porque en los conteos no se puede tratar como las demás, y porque
     * en una variable {@link TipoVariable#NUMERO_CON_ESCAPE} es lo único que
     * distingue el código de escape de una cantidad.</p>
     */
    @Column(name = "es_escape", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean esEscape = false;
}
