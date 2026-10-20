package imss.gob.mx.cohorte.modules.cuestionarios.carga;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Un participante del archivo que no se cargó, y por qué.
 *
 * <p>Se guarda en lugar de solo contarse: «tres filas no entraron» no sirve para
 * nada —hay que saber cuáles y qué les pasó— y la carga corre en segundo plano, así
 * que no hay nadie mirando la pantalla a quien decírselo en el momento.</p>
 */
@Entity
@Table(name = "fila_omitida_volcado",
        indexes = @Index(name = "ix_fila_omitida_tarea", columnList = "id_tarea_carga_volcado"))
@Getter
@Setter
@NoArgsConstructor
public class FilaOmitidaVolcado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_fila_omitida_volcado")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tarea_carga_volcado", nullable = false)
    private TareaCargaVolcado tarea;

    /** El número de fila del archivo, tal como lo ve quien lo abre en Excel. */
    @Column(name = "numero_fila", nullable = false)
    private Integer numeroFila;

    @Column(name = "etiqueta", length = 60)
    private String etiqueta;

    @Column(name = "motivo", length = 500)
    private String motivo;

    public FilaOmitidaVolcado(TareaCargaVolcado tarea, int numeroFila,
                              String etiqueta, String motivo) {
        this.tarea = tarea;
        this.numeroFila = numeroFila;
        this.etiqueta = etiqueta;
        this.motivo = motivo;
    }
}
