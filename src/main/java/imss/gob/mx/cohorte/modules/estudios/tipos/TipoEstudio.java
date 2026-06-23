package imss.gob.mx.cohorte.modules.estudios.tipos;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Tipo_Estudio")
@Getter
@Setter
@NoArgsConstructor
public class TipoEstudio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_estudio")
    private Long Id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @OneToMany(mappedBy = "tipoEstudio")
    private List<ParametroEstudio> parametros;

}
