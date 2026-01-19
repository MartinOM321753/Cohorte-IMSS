package imss.gob.mx.cohorte.modules.estudios.parametros;


import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Parametro_Estudio")
@Getter @Setter
@NoArgsConstructor
public class ParametroEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_parametro")
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "id_tipo_estudio", nullable = false)
    private TipoEstudio tipoEstudio;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "unidad", length = 20)
    private String unidad;
}
