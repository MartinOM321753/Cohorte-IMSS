package imss.gob.mx.cohorte.modules.estudios.parametros;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
    @Column(name = "tipo", nullable = false, length = 15)
    private TipoParametro tipo;

    /** Valor mínimo de referencia (solo aplica a parámetros NUMERICO). */
    @Column(name = "valor_minimo")
    private Double valorMinimo;

    /** Valor máximo de referencia (solo aplica a parámetros NUMERICO). */
    @Column(name = "valor_maximo")
    private Double valorMaximo;

    /**
     * Opciones válidas para parámetros de tipo TEXTO_OPCIONES.
     * Vacío para cualquier otro tipo.
     */
    @OneToMany(mappedBy = "parametro", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<OpcionParametro> opciones = new ArrayList<>();

}
