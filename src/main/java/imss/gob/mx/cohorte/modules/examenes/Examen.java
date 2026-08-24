package imss.gob.mx.cohorte.modules.examenes;


import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "Examen",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_examen_nombre_inst",
               columnNames = {"nombre_examen", "id_institucion"}))
@Getter
@Setter
@NoArgsConstructor
public class Examen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_examen")
    private Long Id;

    @Column(name = "nombre_examen", nullable = false, length = 100)
    private String parametro;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "unidad", length = 10)
    private String unidad;

    @Column(name = "valor_min_mujeres")
    private Double valorMinMujeres;

    @Column(name = "valor_max_mujeres")
    private Double valorMaxMujeres;

    @Column(name = "valor_min_hombres")
    private Double valorMinHombres;

    @Column(name = "valor_max_hombres")
    private Double valorMaxHombres;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Timestamp fechaCreacion;

    /** Institución propietaria del catálogo de examen — define el ámbito de aislamiento de datos. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;


    /**
     * Nombres con los que los instrumentos titulan la columna de este examen en
     * los archivos que exportan. EAGER como en los parametros: el catalogo y el
     * importador siempre los necesitan junto al examen.
     */
    @OneToMany(mappedBy = "examen", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private java.util.List<AliasExamen> alias = new java.util.ArrayList<>();

}
