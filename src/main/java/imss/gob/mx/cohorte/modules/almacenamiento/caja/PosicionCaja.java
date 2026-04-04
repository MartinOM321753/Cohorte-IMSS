package imss.gob.mx.cohorte.modules.almacenamiento.caja;


import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Posicion_Caja",
        uniqueConstraints = @UniqueConstraint(name = "uk_caja_coordenadas", columnNames = {"id_caja", "fila", "columna"})
)
@Getter
@Setter
@NoArgsConstructor
public class PosicionCaja {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_posicion_caja")
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "id_caja", nullable = false)
    private CajaCriogenica caja;

    @Column(name = "fila", nullable = false)
    private Integer fila;

    @Column(name = "columna", nullable = false)
    private Integer columna;

    @Column(name = "ocupada", nullable = false)
    private Boolean ocupada = false;


}
