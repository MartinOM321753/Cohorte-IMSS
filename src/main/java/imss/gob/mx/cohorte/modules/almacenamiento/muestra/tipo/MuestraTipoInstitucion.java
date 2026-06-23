package imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

@Entity
@Table(name = "Muestra_Tipo_Institucion",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_muestra_tipo_institucion",
                columnNames = {"id_muestra", "id_institucion"}))
@Getter
@Setter
@NoArgsConstructor
public class MuestraTipoInstitucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_muestra_tipo_institucion")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_muestra", nullable = false)
    @JsonIgnore
    private Muestra muestra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    @JsonIgnore
    private Institucion institucion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tipo_muestra", nullable = false)
    @JsonIgnore
    private TipoMuestra tipoMuestra;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tubo_muestra", nullable = false)
    @JsonIgnore
    private TuboMuestra tuboMuestra;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;
}
