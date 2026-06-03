package imss.gob.mx.cohorte.modules.estudios.parametros;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "Parametro_Estudio",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_parametro_tipo_nombre",
                columnNames = {"id_tipo_estudio", "nombre"}
        )
)
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

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "unidad", length = 20)
    private String unidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 10)
    private TipoParametro tipo;

    /** Valor mínimo de referencia (solo aplica a parámetros NUMERICO). */
    @Column(name = "valor_minimo")
    private Double valorMinimo;

    /** Valor máximo de referencia (solo aplica a parámetros NUMERICO). */
    @Column(name = "valor_maximo")
    private Double valorMaximo;

}
