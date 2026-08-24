package imss.gob.mx.cohorte.modules.estudios.parametros;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nombre con el que un instrumento médico llama a este parámetro en el archivo
 * que exporta.
 *
 * <p>Es una tabla y no una columna porque un mismo parámetro puede recibir
 * archivos de aparatos distintos que lo titulan de otra forma: el tensiómetro
 * escribe {@code SYS} y la hoja del laboratorio escribe {@code Sistólica}.</p>
 *
 * <h3>Por qué el id del tipo de estudio está aquí, repetido</h3>
 *
 * <p>La restricción que de verdad importa es que dentro de un mismo tipo de
 * estudio no haya dos parámetros reclamando la misma columna. Esa unicidad cruza
 * dos tablas —el alias cuelga del parámetro, y el tipo cuelga del parámetro— y
 * SQL no sabe expresarla así. Duplicando el id del tipo aquí, la base de datos
 * puede imponerla con un índice único.</p>
 *
 * <p>Podría dejarse en manos del servicio, pero el precio de fallar es alto: dos
 * alias iguales significan que una columna del archivo puede resolverse al
 * parámetro equivocado, y el resultado clínico terminaría guardado en el sitio
 * incorrecto sin que nada avise. Una condición de carrera basta para provocarlo.
 * La redundancia se paga a cambio de que eso sea imposible.</p>
 */
@Entity
@Table(
        name = "alias_parametro_estudio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_alias_tipo_estudio",
                columnNames = {"id_tipo_estudio", "alias_normalizado"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class AliasParametroEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alias")
    private Long id;

    @ManyToOne(optional = false)
    @JsonIgnore
    @JoinColumn(name = "id_parametro", nullable = false)
    private ParametroEstudio parametro;

    /** Copia del tipo de estudio del parámetro. Ver la nota de la clase. */
    @Column(name = "id_tipo_estudio", nullable = false)
    private Long idTipoEstudio;

    /** Tal como lo escribió el usuario; es lo que se muestra en el catálogo. */
    @Column(name = "alias", nullable = false, length = 150)
    private String alias;

    /** Forma canónica contra la que se comparan los encabezados del archivo. */
    @Column(name = "alias_normalizado", nullable = false, length = 150)
    private String aliasNormalizado;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    /**
     * La forma normalizada se deriva siempre del alias, nunca se recibe de
     * fuera: si dependiera de que quien crea la fila se acuerde de calcularla,
     * bastaría un alta por otro camino para meter una que no corresponde y
     * romper el emparejamiento en silencio.
     */
    @PrePersist
    @PreUpdate
    private void derivarNormalizado() {
        this.aliasNormalizado = NormalizadorAlias.normalizar(this.alias);
        if (this.parametro != null && this.parametro.getTipoEstudio() != null) {
            this.idTipoEstudio = this.parametro.getTipoEstudio().getId();
        }
    }
}
