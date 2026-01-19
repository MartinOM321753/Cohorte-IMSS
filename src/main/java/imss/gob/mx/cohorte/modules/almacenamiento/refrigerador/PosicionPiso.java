package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Posicion_Piso",
        uniqueConstraints = @UniqueConstraint(name = "uk_piso_coordenadas", columnNames = {"id_piso", "fila", "columna", "profundidad"})
)
@Getter @Setter
@NoArgsConstructor
public class PosicionPiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_posicion_piso")
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "id_piso", nullable = false)
    private PisoRefrigerador piso;

    @Column(name = "fila", nullable = false)
    private Integer fila;

    @Column(name = "columna", nullable = false)
    private Integer columna;

    @Column(name = "profundidad", nullable = false)
    private Integer profundidad;

    @Column(name = "ocupada", nullable = false)
    private Boolean ocupada = false;


}
