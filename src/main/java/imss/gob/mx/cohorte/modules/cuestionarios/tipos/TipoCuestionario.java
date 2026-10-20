package imss.gob.mx.cohorte.modules.cuestionarios.tipos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Un instrumento: el cuestionario general, el minimental, la afluencia verbal,
 * AGES. Es el equivalente de {@code TipoEstudio}, pero a otra escala —el general
 * trae más de dos mil variables— y con dos cosas que un tipo de estudio no
 * necesita.
 *
 * <h3>La clave</h3>
 *
 * <p>{@code clave} es el prefijo con el que se etiqueta el papel: {@code C1},
 * {@code C2}… La etiqueta completa que se imprime y que después vuelve en el
 * volcado es {@code C1/008601/F4}, así que la clave es lo que permite saber a qué
 * instrumento pertenece un archivo sin preguntárselo a nadie. Por eso es única y
 * no un nombre libre.</p>
 *
 * <h3>La versión</h3>
 *
 * <p>El instrumento en papel cambia —el que está en uso lleva fecha en el propio
 * nombre del archivo—, y cuando cambia, los códigos de las preguntas se mueven.
 * Un cuestionario contestado quedaría mal interpretado si se leyera contra el
 * catálogo de otra versión, así que la versión se guarda en el tipo y cada
 * cuestionario contestado recuerda contra cuál se capturó.</p>
 */
@Entity
@Table(name = "tipo_cuestionario",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tipo_cuestionario_clave",
                        columnNames = {"clave", "version", "id_institucion"}),
                @UniqueConstraint(name = "uk_tipo_cuestionario_nombre",
                        columnNames = {"nombre", "version", "id_institucion"})
        })
@Getter
@Setter
@NoArgsConstructor
public class TipoCuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_cuestionario")
    private Long id;

    /** Prefijo de la etiqueta del papel: {@code C1}, {@code C2}, {@code C3}, {@code C4}. */
    @Column(name = "clave", nullable = false, length = 10)
    private String clave;

    @Column(name = "nombre", nullable = false, length = 150)
    private String nombre;

    @Column(name = "descripcion", length = 1000)
    private String descripcion;

    /**
     * Versión del instrumento en papel, tal como la nombran los investigadores
     * («19-08-2024»). Texto y no fecha: es un identificador que ellos escriben, y
     * forzarlo a fecha obligaría a inventar un formato cuando no lo traiga.
     */
    @Column(name = "version", nullable = false, length = 50)
    private String version = "1";

    /**
     * Nombre de la columna del volcado que trae la etiqueta del participante. Se
     * configura porque no es estable: el primer archivo que llegó la titulaba
     * {@code Id-Participante} y el siguiente {@code Folio}.
     */
    @Column(name = "columna_etiqueta", nullable = false, length = 100)
    private String columnaEtiqueta = "Folio";

    /**
     * A qué participantes va dirigido el instrumento, por sexo.
     *
     * <p>No es decoración del catálogo: es lo que permite ofrecer al capturista los
     * cuestionarios que le tocan a la persona que tiene delante, en lugar de la lista
     * completa. Con cuatro instrumentos ya importa; el día que sean diez, elegir a mano
     * el equivocado deja de ser una posibilidad remota.</p>
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dirigido_a", nullable = false, length = 10)
    private DirigidoA dirigidoA = DirigidoA.AMBOS;

    /**
     * Edad mínima y máxima a las que se aplica, en años cumplidos. Nulo significa sin
     * límite por ese lado, no cero: un instrumento «de 45 en adelante» tiene mínimo y
     * no tiene máximo.
     *
     * <p>Una edad exacta se expresa con los dos iguales. Podría haberse añadido un campo
     * «edad exacta» aparte, pero serían dos formas de decir lo mismo y habría que
     * decidir cuál manda cuando las dos vengan llenas.</p>
     */
    @Column(name = "edad_minima")
    private Integer edadMinima;

    @Column(name = "edad_maxima")
    private Integer edadMaxima;

    /**
     * Si el participante puede empezarlo por su cuenta desde la plataforma.
     *
     * <h3>Por que viene apagado por omision</h3>
     *
     * <p>No todos los instrumentos se pueden autoaplicar, y la diferencia no es de forma
     * sino de validez. El minimental y la afluencia verbal son pruebas cognitivas que un
     * entrevistador administra: mide tiempos, repite consignas y observa. Contestadas a
     * solas dejan de medir lo que miden, y el resultado <em>parece</em> valido — es el peor
     * tipo de dato malo, porque nadie lo distingue despues.</p>
     *
     * <p>De los dos valores posibles para el defecto, el seguro es este. Apagado, lo peor
     * que pasa es que alguien del personal tenga que asignar el cuestionario: friccion
     * visible, cero datos corrompidos. Encendido, lo peor que pasa es una prueba cognitiva
     * autoaplicada que se cuela en el analisis sin que nada avise.</p>
     */
    @Column(name = "permite_autollenado", nullable = false,
            columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean permiteAutollenado = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @PrePersist
    private void prePersist() {
        if (this.fechaCreacion == null) this.fechaCreacion = LocalDateTime.now();
    }
}
