package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "Resultado_Estudio_Muestra",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resultado_estudio_muestra_param_grupo_orden",
                columnNames = {"id_estudio_muestra", "id_parametro", "grupo_codigo", "orden_resultado"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class ResultadoEstudioMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_estudio_muestra", nullable = false)
    private EstudioMuestra estudio;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_parametro", nullable = false)
    private ParametroEstudioMuestra parametro;

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
}
