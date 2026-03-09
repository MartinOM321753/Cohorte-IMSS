package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;


import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "Piso_Refrigerador",
        uniqueConstraints = @UniqueConstraint(name = "uk_refrigerador_piso", columnNames = {"id_refrigerador", "numero_piso"})
)
@Getter @Setter
@NoArgsConstructor
public class PisoRefrigerador {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_piso")
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "id_refrigerador", nullable = false)
    private Refrigerador refrigerador;

    @Column(name = "numero_piso", nullable = false)
    private Integer numeroPiso;

    @Column(name = "filas", nullable = false)
    private Integer filas;

    @Column(name = "columnas", nullable = false)
    private Integer columnas;

    @Column(name = "altura", nullable = false)
    private Integer altura;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;
}
