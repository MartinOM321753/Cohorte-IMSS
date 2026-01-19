package imss.gob.mx.cohorte.modules.estudios.resultados;


import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Resultado_Estudio",
        uniqueConstraints = @UniqueConstraint(name = "uk_estudio_parametro", columnNames = {"id_estudio", "id_parametro"})
)
@Getter @Setter
@NoArgsConstructor
public class ResultadoEstudio {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Long Id;

    @Column(name = "valor_numerico")
    private Double valorNumerico;

    @Column(name = "valor_texto", length = 255)
    private String valorTexto;

    @ManyToOne
    @JoinColumn(name = "id_estudio", nullable = false)
    private EstudioMedico estudio;

    @ManyToOne
    @JoinColumn(name = "id_parametro", nullable = false)
    private ParametroEstudio parametro;
}
