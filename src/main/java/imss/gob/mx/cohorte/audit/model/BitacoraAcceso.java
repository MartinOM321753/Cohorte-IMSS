package imss.gob.mx.cohorte.audit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro de accesos al sistema: login, logout y login fallido.
 * Se crea en cuanto ocurre el evento; no depende de ninguna transacción de negocio.
 */
@Entity
@Table(name = "bitacora_acceso", indexes = {
    @Index(name = "idx_ba_usuario_uuid", columnList = "usuario_uuid"),
    @Index(name = "idx_ba_timestamp",    columnList = "timestamp"),
    @Index(name = "idx_ba_tipo_evento",  columnList = "tipo_evento")
})
@Getter
@Setter
@NoArgsConstructor
public class BitacoraAcceso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** UUID del usuario autenticado (null si login fallido y usuario no existe). */
    @Column(name = "usuario_uuid", length = 36)
    private String usuarioUuid;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "nombre_completo", length = 200)
    private String nombreCompleto;

    @Column(name = "rol", length = 50)
    private String rol;

    /** IP del cliente (IPv4 o IPv6, máx. 45 chars). */
    @Column(name = "ip", length = 45)
    private String ip;

    /** Latitud GPS capturada en el navegador (puede ser null si el usuario rechazó permisos). */
    @Column(name = "latitud")
    private Double latitud;

    /** Longitud GPS capturada en el navegador (puede ser null si el usuario rechazó permisos). */
    @Column(name = "longitud")
    private Double longitud;

    /**
     * Margen de error de la lectura GPS en metros, tal como lo reporta el navegador
     * (GeolocationCoordinates.accuracy). Valores típicos:
     *   ≤ 10 m  → GPS hardware (móvil con señal)
     *   ≤ 100 m → Triangulación WiFi
     *   ≤ 2000 m → Red móvil (4G/LTE)
     *   > 2000 m → Geolocalización por IP del proveedor de internet
     */
    @Column(name = "precision_m")
    private Integer precisionM;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evento", nullable = false, length = 20)
    private TipoEventoAcceso tipoEvento;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /** Duración de la sesión en segundos. Solo se llena en eventos LOGOUT. */
    @Column(name = "duracion_sesion_seg")
    private Integer duracionSesionSeg;

    /** Identificador usado al intentar el login (username o correo). Solo para LOGIN_FALLIDO. */
    @Column(name = "identificador_intento", length = 255)
    private String identificadorIntento;
}
