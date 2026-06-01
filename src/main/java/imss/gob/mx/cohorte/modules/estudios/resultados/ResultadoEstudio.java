package imss.gob.mx.cohorte.modules.estudios.resultados;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Resultado_Estudio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_estudio_parametro_grupo_orden",
                columnNames = {"id_estudio", "id_parametro", "grupo_codigo", "orden_resultado"}
        )
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

    @Column(name = "valor_booleano")
    private Boolean valorBooleano;

    @Column(name = "grupo_codigo", nullable = false, length = 50)
    private String grupoCodigo;

    @Column(name = "grupo_etiqueta", length = 100)
    private String grupoEtiqueta;

    @Column(name = "orden_resultado", nullable = false)
    private Integer ordenResultado;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_estudio", nullable = false)
    private EstudioMedico estudio;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_parametro", nullable = false)
    private ParametroEstudio parametro;
}
