package imss.gob.mx.cohorte.audit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Registro de acciones de escritura realizadas en el sistema.
 * Captura quién, qué, cuándo, en qué endpoint, qué cambió y la sentencia SQL real.
 */
@Entity
@Table(name = "bitacora_acciones", indexes = {
    @Index(name = "idx_bac_usuario_uuid",  columnList = "usuario_uuid"),
    @Index(name = "idx_bac_timestamp",     columnList = "timestamp"),
    @Index(name = "idx_bac_tipo_accion",   columnList = "tipo_accion"),
    @Index(name = "idx_bac_entidad",       columnList = "entidad_afectada")
})
@Getter
@Setter
@NoArgsConstructor
public class BitacoraAcciones {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "usuario_uuid", length = 36)
    private String usuarioUuid;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "nombre_completo", length = 200)
    private String nombreCompleto;

    @Column(name = "rol", length = 50)
    private String rol;

    @Column(name = "ip", length = 45)
    private String ip;

    /** Ruta HTTP llamada, ej. /api/pacientes/12. */
    @Column(name = "endpoint", length = 500)
    private String endpoint;

    /** Método HTTP: GET, POST, PUT, PATCH, DELETE. */
    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_accion", length = 20)
    private TipoAccion tipoAccion;

    /** Nombre de la entidad o tabla afectada, ej. "Paciente", "Usuario". */
    @Column(name = "entidad_afectada", length = 100)
    private String entidadAfectada;

    /**
     * Estado del objeto ANTES de la modificación (JSON).
     * Null para operaciones CREAR.
     */
    @Lob
    @Column(name = "valores_anteriores", columnDefinition = "LONGTEXT")
    private String valoresAnteriores;

    /**
     * Estado del objeto DESPUÉS de la modificación (JSON).
     * Para ELIMINAR contiene el objeto antes de ser eliminado.
     */
    @Lob
    @Column(name = "valores_nuevos", columnDefinition = "LONGTEXT")
    private String valoresNuevos;

    /**
     * Sentencias SQL completas (con valores reales) capturadas por p6spy
     * durante la ejecución del método de servicio.
     * Incluye SELECT previos (estado anterior) y DML (INSERT/UPDATE/DELETE).
     */
    @Lob
    @Column(name = "sentencia_sql", columnDefinition = "LONGTEXT")
    private String sentenciaSql;

    @Column(name = "http_status")
    private Integer httpStatus;

    /** Duración de la llamada al ApplicationService en milisegundos. */
    @Column(name = "duracion_ms")
    private Long duracionMs;

    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;

    /** true si la operación terminó sin excepción. */
    @Column(name = "exitoso")
    private Boolean exitoso = true;

    @Column(name = "mensaje_error", columnDefinition = "TEXT")
    private String mensajeError;
}
