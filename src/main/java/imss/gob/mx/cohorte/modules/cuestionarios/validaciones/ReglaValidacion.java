package imss.gob.mx.cohorte.modules.cuestionarios.validaciones;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una comprobación del instrumento, configurada y no programada.
 *
 * <h3>Por qué vive en la base y no en el código</h3>
 *
 * <p>Hay más de un cuestionario y cada uno trae lo suyo: el general tiene secciones
 * que solo contestan las mujeres, el AGES no; el minimental tendrá reglas que ninguno
 * de los dos. Si cada regla fuera un {@code if} en un servicio, configurar un
 * instrumento nuevo exigiría tocar código y volver a desplegar, y quien conoce el
 * cuestionario —que no es quien programa— no podría hacerlo.</p>
 *
 * <p>Por eso las reglas se cargan desde la misma plantilla que trae las preguntas, en
 * la hoja {@code Validaciones}, y el motor solo sabe ejecutar los cinco
 * {@link TipoRegla} que existen.</p>
 *
 * <h3>Los campos son pocos a propósito</h3>
 *
 * <p>{@code pregunta}, {@code valor} y {@code comparaCon} se interpretan según el
 * tipo, y eso es deliberado: un modelo con un campo por cada variante de cada regla
 * sería inmenso y estaría casi todo vacío. Lo que cada tipo espera está documentado
 * en {@code MotorValidaciones}, que es quien los lee.</p>
 */
@Entity
@Table(name = "regla_validacion",
        indexes = @Index(name = "ix_regla_validacion_tipo", columnList = "id_tipo_cuestionario"))
@Getter
@Setter
@NoArgsConstructor
public class ReglaValidacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_regla")
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_cuestionario", nullable = false)
    private TipoCuestionario tipoCuestionario;

    @Enumerated(EnumType.STRING)
    @Column(name = "regla", nullable = false, length = 20)
    private TipoRegla regla;

    /**
     * A qué se aplica: el código de una pregunta o, en {@link TipoRegla#SOLO_SEXO},
     * también el nombre de un bloque entero.
     */
    @Column(name = "pregunta", nullable = false, length = 250)
    private String pregunta;

    /**
     * El literal que la regla necesita, según su tipo: el código que dispara un
     * {@code EXCLUYE} o un {@code REQUIERE_TEXTO}, o {@code HOMBRES} / {@code MUJERES}
     * en un {@code SOLO_SEXO}.
     */
    @Column(name = "valor", length = 60)
    private String valor;

    /**
     * El segundo operando: el bloque donde buscar las hermanas, la pregunta de texto
     * que hay que llenar, la pregunta llave del sexo, o {@code EDAD}.
     */
    @Column(name = "compara_con", length = 250)
    private String comparaCon;

    @Enumerated(EnumType.STRING)
    @Column(name = "severidad", nullable = false,
            columnDefinition = "VARCHAR(10) DEFAULT 'REVISION'")
    private Severidad severidad = Severidad.REVISION;

    /** Lo que se le dice a quien tiene que arreglarlo. Sale tal cual en pantalla. */
    @Column(name = "mensaje", nullable = false, length = 500)
    private String mensaje;

    /** De dónde salió la regla, para poder volver al papel cuando alguien la discuta. */
    @Column(name = "origen", length = 250)
    private String origen;

    @Column(name = "activa", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activa = true;
}
