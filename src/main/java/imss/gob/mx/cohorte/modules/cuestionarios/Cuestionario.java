package imss.gob.mx.cohorte.modules.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Un cuestionario contestado por un participante.
 *
 * <h3>La etiqueta</h3>
 *
 * <p>{@link #etiqueta} es la cadena completa que se imprimió en la hoja y que
 * vuelve en el volcado: {@code C1/008601/F4}. Se guarda tal cual, además del
 * participante al que ya se resolvió, por una razón que no es redundancia: es la
 * única prueba de con qué hoja física se corresponde este registro. Si mañana se
 * reasigna un folio, o se descubre que una hoja se etiquetó mal, hace falta saber
 * qué decía el papel —no solo a quién creímos que pertenecía—.</p>
 *
 * <h3>Un cuestionario por participante, tipo y versión</h3>
 *
 * <p>La restricción única lo impone. Sin ella, cargar dos veces el mismo archivo
 * duplicaría el cuestionario entero, dos mil respuestas incluidas, y nada avisaría:
 * los informes empezarían a contar doble y el error se descubriría tarde. Incluir
 * la versión en la llave es lo que permite que un participante que contestó el
 * instrumento de 2024 y el de una revisión posterior tenga los dos.</p>
 */
@Entity
@Table(name = "cuestionario",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cuestionario_paciente_tipo",
                columnNames = {"id_paciente", "id_tipo_cuestionario"}),
        indexes = {
                @Index(name = "ix_cuestionario_etiqueta", columnList = "etiqueta"),
                @Index(name = "ix_cuestionario_institucion", columnList = "id_institucion, estado")
        })
@Getter
@Setter
@NoArgsConstructor
public class Cuestionario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cuestionario")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_cuestionario", nullable = false)
    private TipoCuestionario tipoCuestionario;

    /** La etiqueta completa del papel, {@code C1/008601/F4}. Ver la nota de la clase. */
    @Column(name = "etiqueta", length = 60)
    private String etiqueta;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 15)
    private EstadoCuestionario estado = EstadoCuestionario.BORRADOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 20)
    private OrigenCuestionario origen = OrigenCuestionario.PAPEL_REMARK;

    /** Cuándo lo contestó el participante, no cuándo se capturó. */
    @Column(name = "fecha_aplicacion")
    private LocalDate fechaAplicacion;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_captura")
    private BeanUser usuarioCaptura;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_valida")
    private BeanUser usuarioValida;

    @Column(name = "fecha_validacion")
    private LocalDateTime fechaValidacion;

    /** Institución responsable — define el ámbito de aislamiento de datos. */
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    /**
     * Si el participante no entraba en el publico del instrumento.
     *
     * <p>Se guarda en lugar de impedir la carga, y es deliberado: la hoja de papel existe
     * y alguien la contesto. Un minimental aplicado a una persona de 40 anos es un hecho
     * de calidad de datos que hay que poder encontrar despues —y contar—, no algo que se
     * descarte en la importacion dejando al participante sin instrumento y sin
     * explicacion.</p>
     *
     * <p>Es una columna y no un calculo al vuelo porque el criterio se evalua con la edad
     * que tenia el dia de la aplicacion: si manana alguien cambia el rango del
     * instrumento, lo que se registro aquel dia no deberia cambiar de sentido.</p>
     */
    @Column(name = "fuera_de_criterio", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean fueraDeCriterio = false;

    /** En que no coincidia, tal como se evaluo el dia de la carga. */
    @Column(name = "motivo_fuera_de_criterio", length = 500)
    private String motivoFueraDeCriterio;

    /**
     * Nombre del archivo del que se importó, si vino de un volcado.
     *
     * <p>Es lo primero que se pregunta cuando una respuesta no cuadra: de qué
     * archivo salió. Sin esto, con varios volcados cargados, la respuesta es
     * revisar todos.</p>
     */
    @Column(name = "archivo_origen", length = 255)
    private String archivoOrigen;

    /**
     * Cuántas respuestas quedaron pendientes de leer —sin autorizar o con error de
     * lectura— al importar.
     *
     * <p>Se guarda contado, en lugar de calcularse, porque es lo que ordena la
     * bandeja de pendientes: contarlo al vuelo sobre dos mil respuestas por
     * cuestionario obligaría a recorrerlas todas para pintar una lista.</p>
     */
    @Column(name = "respuestas_pendientes", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer respuestasPendientes = 0;

    @PrePersist
    private void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (this.fechaRegistro == null) this.fechaRegistro = ahora;
        this.fechaActualizacion = ahora;
    }

    @PreUpdate
    private void preUpdate() {
        this.fechaActualizacion = LocalDateTime.now();
    }
}
