package imss.gob.mx.cohorte.modules.estudios.parametros;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Parametro_Estudio")
@Getter
@Setter
@NoArgsConstructor
public class ParametroEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Long Id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_tipo_estudio", nullable = false)
    private TipoEstudio tipoEstudio;

    @Column(name = "nombre", nullable = false, length = 100,unique = true)
    private String nombre;

    @Column(name = "unidad", length = 20)
    private String unidad;


}
