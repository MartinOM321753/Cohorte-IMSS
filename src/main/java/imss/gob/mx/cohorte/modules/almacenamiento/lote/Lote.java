package imss.gob.mx.cohorte.modules.almacenamiento.lote;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.protocolo.Protocolo;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;

/**
 * Conjunto de alícuotas de un mismo <b>tipo resultante</b> para un participante,
 * numeradas 1…N y alimentadas por uno o varios tubos primarios.
 *
 * <p>Es lo que el laboratorio llama «lote»: los 12 crioviales de suero que salen
 * de dos tubos de sangre total procesados juntos. Resuelve el requisito de no
 * tener separadas las 6 alícuotas de un tubo de las 6 del otro: ambas cuelgan del
 * mismo {@code Lote} con numeración continua.</p>
 *
 * <p>No duplica la custodia ni la contabilidad de volumen —eso sigue en cada
 * {@code Muestra} (padre y alícuota)—: el {@code Lote} solo agrupa y numera.</p>
 */
@Entity
@Table(name = "Lote",
       indexes = @Index(name = "idx_lote_paciente", columnList = "id_paciente, id_institucion"))
@Getter
@Setter
@NoArgsConstructor
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_lote")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    /** Tipo de muestra de las alícuotas de este lote (el resultante del proceso). */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_resultante")
    private TipoMuestra tipoResultante;

    /** Protocolo que generó este lote. Null si se creó fuera de un protocolo. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_protocolo")
    private Protocolo protocolo;

    /** Número del lote dentro del participante (para rotularlo «Lote 1», «Lote 2»…). */
    @Column(name = "numero_lote", nullable = false)
    private Integer numeroLote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_procesa")
    private BeanUser usuarioProcesa;

    @Column(name = "fecha_creacion", nullable = false)
    private Timestamp fechaCreacion;
}
