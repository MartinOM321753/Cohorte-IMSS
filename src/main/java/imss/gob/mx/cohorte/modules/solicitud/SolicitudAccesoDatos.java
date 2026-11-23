package imss.gob.mx.cohorte.modules.solicitud;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.Instant;

@Entity
@Table(name = "solicitud_acceso_datos")
@Getter
@Setter
public class SolicitudAccesoDatos {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre_completo", nullable = false, length = 140)
    private String nombreCompleto;

    @Column(nullable = false, length = 180)
    private String organizacion;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(nullable = false, length = 180)
    private String proposito;

    @Column(name = "proposito_otro", length = 180)
    private String propositoOtro;

    @Column(nullable = false, length = 2400)
    private String proyecto;

    @Column(name = "informacion_solicitada", nullable = false, length = 1800)
    private String informacionSolicitada;

    @Column(nullable = false, length = 500)
    private String productos;

    @Column(name = "producto_otro", length = 180)
    private String productoOtro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitudDatos estado = EstadoSolicitudDatos.PENDIENTE;

    @Column(name = "ip_solicitante", length = 45)
    private String ipSolicitante;

    @Column(length=10) private String telefono;
    @Column(name="correo_verificado_en") private Instant correoVerificadoEn;
    @Column(name="ubicacion_latitud") private Double ubicacionLatitud;
    @Column(name="ubicacion_longitud") private Double ubicacionLongitud;
    @Column(name="ubicacion_precision") private Double ubicacionPrecision;
    @Column(name="ubicacion_capturada_en") private Instant ubicacionCapturadaEn;
    @Column(name="ubicacion_consentimiento_en") private Instant ubicacionConsentimientoEn;
    @Column(name="conexion_pais",length=2) private String conexionPais;
    @Column(name="conexion_proveedor",length=80) private String conexionProveedor;
    @Column(name="conexion_revisada_en") private Instant conexionRevisadaEn;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDateTime fechaSolicitud;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaSolicitud = LocalDateTime.now();
        fechaActualizacion = fechaSolicitud;
        if (estado == null) estado = EstadoSolicitudDatos.PENDIENTE;
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
