package imss.gob.mx.cohorte.modules.catalogo;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Unidad_Medida",
    uniqueConstraints = @UniqueConstraint(name = "uk_unidad_nombre", columnNames = {"nombre"})
)
@Getter @Setter
@NoArgsConstructor
public class UnidadMedida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_unidad")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 30)
    private String nombre;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
