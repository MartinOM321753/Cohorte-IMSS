package imss.gob.mx.cohorte.modules.almacenamiento.entrega;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.Colaborador;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * Registro de la entrega/préstamo de una muestra a un colaborador (persona
 * física), paralelo a {@code TrasladoMuestra} (préstamo entre instituciones).
 *
 * <p>A diferencia del traslado, la entrega es de UN solo paso: el usuario del
 * biobanco la registra y queda {@link EstadoEntrega#ENTREGADA} de inmediato (el
 * colaborador puede no tener acceso al sistema para confirmar). La muestra NO
 * cambia de {@code institucionActual} — sigue siendo de esta institución, solo
 * sale físicamente del biobanco y libera su posición.</p>
 *
 * <p><b>La entrega nunca toca el volumen.</b> Solo los estudios y la
 * materialización de alícuotas mueven {@code Muestra.valor}. Por eso al cerrar:
 * <ul>
 *   <li>{@link EstadoEntrega#DEVUELTA}: la muestra vuelve al biobanco con su
 *       volumen intacto.</li>
 *   <li>{@link EstadoEntrega#CONSUMIDA}: la muestra no regresó; su volumen se
 *       conserva para que los estudios pendientes aún puedan capturarse. El
 *       agotamiento lo sellan esos estudios, no esta operación.</li>
 * </ul>
 */
@Entity
@Table(name = "Entrega_Muestra")
@Getter
@Setter
@NoArgsConstructor
public class EntregaMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_entrega")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    @ManyToOne(optional = false)
    @JsonIgnore
    @JoinColumn(name = "id_muestra", nullable = false)
    private Muestra muestra;

    /**
     * Destinatario cuando es un colaborador (persona física sin cuenta). Null si
     * la muestra se entregó a un usuario del sistema. Exactamente uno de
     * {@link #colaborador} / {@link #usuario} está poblado.
     */
    @ManyToOne
    @JoinColumn(name = "id_colaborador")
    private Colaborador colaborador;

    /**
     * Destinatario cuando es un usuario del sistema ya registrado (que también
     * puede necesitar obtener muestras). Null si se entregó a un colaborador.
     */
    @ManyToOne
    @JoinColumn(name = "id_usuario_destino")
    private BeanUser usuario;

    /** Institución que entrega (dueña/tenedora de la muestra al momento de la entrega). */
    @ManyToOne(optional = false)
    @JsonIgnore
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    /** Usuario del biobanco que registra la entrega. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_entrega", nullable = false)
    private BeanUser entregadaPor;

    /** Usuario que cierra la entrega (devolución/consumo/cancelación). Null hasta el cierre. */
    @ManyToOne
    @JoinColumn(name = "id_usuario_cierra")
    private BeanUser cerradaPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoEntrega estado = EstadoEntrega.ENTREGADA;

    @Column(name = "fecha_entrega", nullable = false)
    private LocalDateTime fechaEntrega;

    /** Fecha en que se cerró (DEVUELTA/CONSUMIDA/CANCELADA). Null mientras siga ENTREGADA. */
    @Column(name = "fecha_cierre")
    private LocalDateTime fechaCierre;

    /** Fecha límite esperada de devolución. Null si no se establece. */
    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    /**
     * UUID compartido entre las entregas de un mismo lote (padre + alícuotas
     * entregados juntos). Null para entregas individuales.
     */
    @Column(name = "grupo_entrega", length = 36)
    private String grupoEntrega;

    /**
     * ID de la PosicionCaja que tenía la muestra antes de la entrega. Se usa para
     * restaurar la posición al CANCELAR la entrega. Null si no tenía posición.
     */
    @Column(name = "id_posicion_caja_anterior")
    private Long idPosicionCajaAnterior;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;
}
