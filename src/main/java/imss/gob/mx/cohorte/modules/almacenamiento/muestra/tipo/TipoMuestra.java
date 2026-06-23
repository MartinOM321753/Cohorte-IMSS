package imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Tipo_Muestra",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_tipo_muestra_nombre_inst",
               columnNames = {"nombre", "id_institucion"}))
@Getter
@Setter
@NoArgsConstructor
public class TipoMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_muestra")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /**
     * Temperatura de almacenamiento sugerida: "4°C", "-80°C", "Ambiente".
     */
    @Column(name = "temperatura_almacenamiento", length = 30)
    private String temperaturaAlmacenamiento;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @OneToMany(mappedBy = "tipoMuestra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<TuboMuestra> tubos = new ArrayList<>();
}
