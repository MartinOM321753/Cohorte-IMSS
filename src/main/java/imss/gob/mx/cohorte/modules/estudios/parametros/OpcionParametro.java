package imss.gob.mx.cohorte.modules.estudios.parametros;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

/**
 * Opción predefinida para parámetros de tipo TEXTO_OPCIONES.
 * Permite definir un conjunto de valores válidos que el usuario
 * debe seleccionar (evita errores de captura en texto libre).
 */
@Entity
@Table(
        name = "Opcion_Parametro",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_opcion_parametro_valor",
                columnNames = {"id_parametro", "valor"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OpcionParametro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_opcion")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_parametro", nullable = false)
    private ParametroEstudio parametro;

    @Column(name = "valor", nullable = false, length = 100)
    private String valor;

    @Column(name = "orden", nullable = false)
    private Integer orden;
}
