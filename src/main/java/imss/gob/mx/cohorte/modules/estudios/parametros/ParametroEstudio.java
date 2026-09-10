package imss.gob.mx.cohorte.modules.estudios.parametros;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "Parametro_Estudio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_parametro_tipo_nombre",
                columnNames = {"id_tipo_estudio", "nombre"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ParametroEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Long Id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_tipo_estudio", nullable = false)
    private TipoEstudio tipoEstudio;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "unidad", length = 20)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoParametro tipo;

    /**
     * Un parámetro fuera de uso no se ofrece al capturar ni se exige, pero sus
     * resultados anteriores siguen siendo válidos y visibles. Es la alternativa al
     * borrado, que queda prohibido en cuanto existe un solo resultado.
     *
     * <p>El DEFAULT de la columna importa: Hibernate la añade a una tabla que ya
     * tiene datos, y sin él los parámetros existentes quedarían en NULL, que aquí
     * significaría desactivados de golpe.</p>
     */
    @Column(name = "activo", nullable = false, columnDefinition = "BOOLEAN DEFAULT TRUE")
    private Boolean activo = true;

    /** Rango de referencia para mujeres (solo aplica a parámetros NUMERICO). */
    @Column(name = "valor_min_mujeres")
    private Double valorMinMujeres;

    @Column(name = "valor_max_mujeres")
    private Double valorMaxMujeres;

    /** Rango de referencia para hombres (solo aplica a parámetros NUMERICO). */
    @Column(name = "valor_min_hombres")
    private Double valorMinHombres;

    @Column(name = "valor_max_hombres")
    private Double valorMaxHombres;

    /**
     * Cuánto se puede pasar del límite y seguir contando como diferencia menor, en
     * las unidades del propio parámetro.
     *
     * <p>Separa «ligeramente fuera» de «revisar con su médico» en el reporte que se
     * entrega al participante. Esa frontera depende del parámetro y no se puede
     * deducir del rango, así que se guarda; sin valor, el reporte usa una décima
     * parte de la amplitud.</p>
     *
     * <p>Un cero es una decisión, no un hueco: significa que cualquier diferencia
     * hay que revisarla.</p>
     */
    @Column(name = "margen_revision")
    private Double margenRevision;

    /**
     * Opciones válidas para parámetros de tipo TEXTO_OPCIONES.
     * Vacío para cualquier otro tipo.
     */
    @OneToMany(mappedBy = "parametro", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<OpcionParametro> opciones = new ArrayList<>();

    /**
     * Nombres con los que los instrumentos medicos titulan la columna de este
     * parametro. Aplica a cualquier tipo de parametro, no solo a los numericos.
     *
     * <p>Se carga en modo EAGER como las opciones porque el catalogo y el
     * importador siempre los necesitan junto al parametro; en LAZY, recorrer los
     * parametros de un tipo dispararia una consulta por cada uno.</p>
     */
    @OneToMany(mappedBy = "parametro", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<AliasParametroEstudio> alias = new ArrayList<>();

}
