package imss.gob.mx.cohorte.modules.examenes;


import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "Examen")
@Getter @Setter
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
}
