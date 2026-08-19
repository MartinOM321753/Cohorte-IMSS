package imss.gob.mx.cohorte.modules.examenes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Nombre con el que un instrumento titula la columna de este examen en el
 * archivo que exporta.
 *
 * <p>Equivale a {@code AliasParametroEstudio}, pero el ámbito de unicidad es
 * distinto: en exámenes no hay plantilla que agrupe: cada {@link Examen} es un
 * analito suelto que pertenece a una institución. Así que dos columnas no pueden
 * llamarse igual dentro de la misma institución, y la copia que se guarda aquí
 * es la de la institución, no la de un tipo.</p>
 */
@Entity
@Table(
        name = "alias_examen",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_alias_examen_institucion",
                columnNames = {"id_institucion", "alias_normalizado"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class AliasExamen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alias")
    private Long id;

    @ManyToOne(optional = false)
    @JsonIgnore
    @JoinColumn(name = "id_examen", nullable = false)
    private Examen examen;

    /** Copia de la institución del examen, para que el índice único sea posible. */
    @Column(name = "id_institucion", nullable = false)
    private Long idInstitucion;

    @Column(name = "alias", nullable = false, length = 150)
    private String alias;

    @Column(name = "alias_normalizado", nullable = false, length = 150)
    private String aliasNormalizado;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @PrePersist
    @PreUpdate
    private void derivarNormalizado() {
        this.aliasNormalizado = NormalizadorAlias.normalizar(this.alias);
        if (this.examen != null && this.examen.getInstitucion() != null) {
            this.idInstitucion = this.examen.getInstitucion().getId();
        }
    }
}
