package imss.gob.mx.cohorte.modules.almacenamiento.caja;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "Caja_Criogenica",
        uniqueConstraints = @UniqueConstraint(name = "uk_caja_codigo_institucion", columnNames = {"codigo_caja", "id_institucion"}))
@Getter @Setter
@NoArgsConstructor
public class CajaCriogenica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    private Long Id;

    @Column(name = "codigo_caja", nullable = false, length = 50)
    private String codigoCaja;

    @Column(name = "filas", nullable = false)
    private Integer filas;

    @Column(name = "columnas", nullable = false)
    private Integer columnas;

    @Column(name = "tipo_caja", length = 50)
    private String tipoCaja;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;

    /** Institución propietaria de la caja criogénica — define el ámbito de aislamiento de datos. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    @JsonIgnore
    private Institucion institucion;

    @OneToOne
    @JoinColumn(name = "id_posicion_piso", unique = true)
    @JsonIgnore
    private PosicionPiso posicionPiso;
}
