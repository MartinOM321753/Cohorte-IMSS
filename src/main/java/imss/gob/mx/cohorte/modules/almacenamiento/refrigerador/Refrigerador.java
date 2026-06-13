package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "Refrigerador",
        uniqueConstraints = @UniqueConstraint(name = "uk_refrigerador_codigo_institucion", columnNames = {"codigo", "id_institucion"}))
@Getter @Setter
@NoArgsConstructor
public class Refrigerador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_refrigerador")
    private Long id;

    @Column(name = "codigo", nullable = false, length = 50)
    private String codigo;

    @Column(name = "nombre", length = 100)
    private String nombre;

    @Column(name = "marca", length = 50)
    private String marca;

    @Column(name = "modelo", length = 50)
    private String modelo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    /** Institución propietaria del refrigerador — define el ámbito de aislamiento de datos. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    @JsonIgnore
    private Institucion institucion;

    @OneToMany(mappedBy = "refrigerador")
    @JsonIgnore
    private List<PisoRefrigerador> pisos;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;
}
