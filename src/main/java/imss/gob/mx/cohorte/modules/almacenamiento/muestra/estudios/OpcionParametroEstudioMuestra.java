package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "Opcion_Parametro_Estudio_Muestra",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_opcion_param_estudio_muestra_valor",
                columnNames = {"id_parametro", "valor"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpcionParametroEstudioMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_opcion")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_parametro", nullable = false)
    private ParametroEstudioMuestra parametro;

    @Column(name = "valor", nullable = false, length = 100)
    private String valor;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
