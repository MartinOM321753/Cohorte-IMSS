package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Posicion_Piso",
        uniqueConstraints = @UniqueConstraint(name = "uk_piso_coordenadas", columnNames = {"id_piso", "fila", "columna", "altura"})
)
@Getter @Setter
@NoArgsConstructor
public class PosicionPiso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_posicion_piso")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_piso", nullable = false)
    @JsonIgnore
    private PisoRefrigerador piso;

    @Column(name = "fila", nullable = false)
    private String fila;

    @Column(name = "columna", nullable = false)
    private String columna;

    @Column(name = "altura", nullable = false)
    private String altura;

    @Column(name = "ocupada", nullable = false)
    private Boolean ocupada = false;


}
