package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Tipo_Estudio_Muestra",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_tipo_estudio_muestra_nombre_inst",
               columnNames = {"nombre", "id_institucion"}))
@Getter
@Setter
@NoArgsConstructor
public class TipoEstudioMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_estudio_muestra")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @OneToMany(mappedBy = "tipoEstudioMuestra", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("nombre ASC")
    private List<ParametroEstudioMuestra> parametros = new ArrayList<>();
}
