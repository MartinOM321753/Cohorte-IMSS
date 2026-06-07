package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "Muestra")
@Getter
@Setter
@NoArgsConstructor
public class Muestra {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_muestra")
    private Long Id;

    @Column(name = "etiqueta", nullable = false, unique = true, length = 50)
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
    private PosicionCaja posicionCaja;
    @ManyToOne

    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "id_usuario_recolecta", nullable = false)
    private BeanUser usuarioRecolecta;

    // ── Campos Stream C — TipoMuestra (todos nullable para compat con registros previos) ──

    /** Tipo de muestra configurado en el catálogo (nullable). */
    @ManyToOne
    @JoinColumn(name = "id_tipo_muestra")
    private TipoMuestra tipoMuestra;

    /** Tubo específico de este tipo de muestra (nullable). */
    @ManyToOne
    @JoinColumn(name = "id_tubo_muestra")
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

    /** Alícuotas derivadas de este tubo/muestra primaria. */
    @OneToMany(mappedBy = "muestraPadre", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<Muestra> alicuotas = new ArrayList<>();
}

