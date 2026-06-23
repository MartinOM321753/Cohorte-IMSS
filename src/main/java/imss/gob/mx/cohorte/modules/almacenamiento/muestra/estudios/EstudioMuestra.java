package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Estudio_Muestra")
@Getter
@Setter
@NoArgsConstructor
public class EstudioMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudio_muestra")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_muestra", nullable = false)
    private Muestra muestra;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_tipo_estudio_muestra", nullable = false)
    private TipoEstudioMuestra tipoEstudioMuestra;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_usuario_realiza", nullable = false)
    private BeanUser usuarioRealiza;

    @Column(name = "fecha_estudio", nullable = false)
    private LocalDate fechaEstudio;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    /** Volumen/masa consumida de la muestra al realizar este estudio. */
    @Column(name = "cantidad_consumida")
    private Double cantidadConsumida;

    /** Unidad del consumo: "mL", "µL", "mg", "g". */
    @Column(name = "unidad_consumida", length = 20)
    private String unidadConsumida;

    @OneToMany(mappedBy = "estudio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("grupoCodigo ASC, ordenResultado ASC, id ASC")
    private List<ResultadoEstudioMuestra> resultados = new ArrayList<>();
}
