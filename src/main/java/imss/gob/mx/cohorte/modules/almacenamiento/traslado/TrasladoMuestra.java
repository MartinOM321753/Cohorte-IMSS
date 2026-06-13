package imss.gob.mx.cohorte.modules.almacenamiento.traslado;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
 * Registro de un préstamo de muestra entre instituciones.
 *
 * <p>Flujo de estados:
 * <pre>
 *   ENVIADA → RECIBIDA → EN_DEVOLUCION → DEVUELTA
 * </pre>
 *
 * <p>Cadena de custodia: una muestra puede encadenarse (A→B→C…) sin
 * necesidad de volver al origen. Cada salto crea un nuevo TrasladoMuestra.
 * Solo {@code institucionOrigen} (tenedor actual) puede crear el siguiente eslabón.
 *
 * <p>Lotes: cuando se presta un padre + alícuotas juntos, todos comparten
 * el mismo {@code grupoTraslado} (UUID). Null para préstamos individuales.
 */
@Entity
@Table(name = "Traslado_Muestra")
@Getter
@Setter
@NoArgsConstructor
public class TrasladoMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_traslado")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_muestra", nullable = false)
    private Muestra muestra;

    /** Institución que envía la muestra (= Muestra.institucionActual en el momento del préstamo). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_origen", nullable = false)
    private Institucion institucionOrigen;

    /** Institución que recibe la muestra en préstamo. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_destino", nullable = false)
    private Institucion institucionDestino;

    /** Usuario de la institución origen que autoriza el envío. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_usuario_autoriza", nullable = false)
    private BeanUser autorizadoPor;

    /**
     * Usuario de la institución destino que confirma la recepción física.
     * Null hasta que el destino ejecute {@code confirmarRecepcion}.
     */
    @ManyToOne
    @JoinColumn(name = "id_usuario_recibe")
    private BeanUser recibidoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTraslado estado = EstadoTraslado.ENVIADA;

    @Column(name = "fecha_traslado", nullable = false)
    private LocalDateTime fechaTraslado;

    /** Fecha en que se completa la devolución (DEVUELTA). Null hasta entonces. */
    @Column(name = "fecha_retorno")
    private LocalDateTime fechaRetorno;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    /**
     * UUID compartido entre todos los TrasladoMuestra de un mismo lote
     * (padre + alícuotas enviados juntos). Null para préstamos individuales.
     */
    @Column(name = "grupo_traslado", length = 36)
    private String grupoTraslado;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;

    /**
     * ID de la PosicionCaja que tenía la muestra antes de iniciar el préstamo.
     * Null si la muestra no tenía posición asignada al momento del traslado.
     * Se usa para restaurar la posición al cancelar el préstamo (estado CANCELADO).
     */
    @Column(name = "id_posicion_caja_anterior")
    private Long idPosicionCajaAnterior;
}
