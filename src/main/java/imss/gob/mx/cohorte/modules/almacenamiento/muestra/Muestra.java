package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

// ─── NOTE ──────────────────────────────────────────────────────────────────────
// institucion       = propietaria original (quién la registró, inmutable)
// institucionActual = tenedor actual       (cambia en cada préstamo/devolución)
// estadoMuestra     = estado físico desde la perspectiva de institucionActual
// ───────────────────────────────────────────────────────────────────────────────


@Entity
@Table(name = "Muestra",
        uniqueConstraints = @UniqueConstraint(name = "uk_muestra_etiqueta_institucion", columnNames = {"etiqueta", "id_institucion"}))
@Getter
@Setter
@NoArgsConstructor
public class Muestra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_muestra")
    private Long Id;

    @Column(name = "etiqueta", nullable = false, length = 100)
    private String etiqueta;

    @Column(name = "valor")
    private Double valor;

    @Column(name = "unidad", length = 50)
    private String unidad;

    @Column(name = "fecha_recoleccion")
    private LocalDateTime fechaRecoleccion;

    @Column(name = "observaciones", length = 200)
    private String observaciones;

    @Column(name = "fecha_registro", nullable = false)
    private Timestamp fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private Timestamp fechaActualizacion;

    @OneToOne
    @JoinColumn(name = "id_posicion_caja", unique = true)
    @JsonIgnore
    private PosicionCaja posicionCaja;

    @ManyToOne
    @JoinColumn(name = "id_paciente", nullable = false)
    @JsonIgnore
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "id_usuario_recolecta", nullable = false)
    @JsonIgnore
    private BeanUser usuarioRecolecta;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    @JsonIgnore
    private Institucion institucion;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_actual", nullable = false)
    @JsonIgnore
    private Institucion institucionActual;

    /** Estado físico de la muestra desde la perspectiva de {@code institucionActual}. */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_muestra", nullable = false, length = 20)
    private EstadoMuestra estadoMuestra = EstadoMuestra.SIN_POSICION;

    // ── Campos Stream C — TipoMuestra (todos nullable para compat con registros previos) ──

    @ManyToOne
    @JoinColumn(name = "id_tipo_muestra")
    @JsonIgnore
    private TipoMuestra tipoMuestra;

    @ManyToOne
    @JoinColumn(name = "id_tubo_muestra")
    @JsonIgnore
    private TuboMuestra tuboMuestra;

    /**
     * Muestra padre: null = muestra primaria / tubo directo;
     * not null = alícuota derivada de muestraPadre.
     */
    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_muestra_padre")
    private Muestra muestraPadre;

    /** Número de esta alícuota dentro del tubo (1-based). Null si es muestra primaria. */
    @Column(name = "numero_alicuota")
    private Integer numeroAlicuota;

    /** Total de alícuotas del tubo al que pertenece. Null si es muestra primaria. */
    @Column(name = "total_alicuotas")
    private Integer totalAlicuotas;

    @Column(name = "numero_lote")
    private Integer numeroLote;

    /** Alícuotas derivadas de este tubo/muestra primaria. */
    @OneToMany(mappedBy = "muestraPadre", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Muestra> alicuotas = new ArrayList<>();
}

