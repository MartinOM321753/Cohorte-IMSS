package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "Parametro_Estudio_Muestra",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_param_estudio_muestra_tipo_nombre",
                columnNames = {"id_tipo_estudio_muestra", "nombre"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ParametroEstudioMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_tipo_estudio_muestra", nullable = false)
    private TipoEstudioMuestra tipoEstudioMuestra;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "unidad", length = 20)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoParametro tipo;

    @Column(name = "valor_minimo")
    private Double valorMinimo;

    @Column(name = "valor_maximo")
    private Double valorMaximo;

    @OneToMany(mappedBy = "parametro", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<OpcionParametroEstudioMuestra> opciones = new ArrayList<>();
}
