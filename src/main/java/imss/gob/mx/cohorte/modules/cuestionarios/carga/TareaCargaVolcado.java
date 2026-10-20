package imss.gob.mx.cohorte.modules.cuestionarios.carga;

import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Una carga de volcado que corre en segundo plano.
 *
 * <h3>Por qué la carga dejó de contestarse en la misma petición</h3>
 *
 * <p>Una fila de un volcado no es un renglón: es un cuestionario completo con sus dos
 * mil respuestas. Cien participantes son doscientas mil inserciones, y eso no cabe en
 * el tiempo que un navegador —o el proxy de enmedio— está dispuesto a esperar: la
 * petición muere por tiempo de espera mientras el servidor sigue trabajando, y quien
 * subió el archivo se queda sin saber si se guardó. Por eso la carga se encola, la
 * petición contesta de inmediato con el número de esta tarea, y la pantalla pregunta
 * por ella hasta que termina.</p>
 *
 * <h3>Por qué se guarda en la base y no en memoria</h3>
 *
 * <p>Un mapa en memoria sería menos código y bastaría mientras nada se reinicie. Pero
 * esto dura minutos: si el servidor se reinicia a mitad, con la tarea en la base queda
 * el registro de que una carga se quedó a medias y hasta qué fila llegó, que es
 * justamente lo que hay que saber para decidir si se repite. En memoria no quedaría
 * nada que consultar, solo cuestionarios a medio cargar sin explicación.</p>
 *
 * <h3>Las cifras van aquí, no se recalculan</h3>
 *
 * <p>Se escriben conforme avanza porque es lo único que permite enseñar el progreso:
 * contar cuestionarios en la base diría cuántos hay, no cuántos trajo este archivo.</p>
 */
@Entity
@Table(name = "tarea_carga_volcado",
        indexes = {
                @Index(name = "ix_tarea_volcado_institucion",
                        columnList = "id_institucion, fecha_inicio"),
                @Index(name = "ix_tarea_volcado_estado", columnList = "estado")
        })
@Getter
@Setter
@NoArgsConstructor
public class TareaCargaVolcado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea_carga_volcado")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_cuestionario", nullable = false)
    private TipoCuestionario tipoCuestionario;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    /** Quien subió el archivo. Es a quien hay que avisarle cómo acabó. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario")
    private BeanUser usuario;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    /** Si se pidió reemplazar los cuestionarios que ya existían. */
    @Column(name = "sobrescribir", nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean sobrescribir = false;

    /**
     * Nótese el {@code columnDefinition}: con {@code ddl-auto=update}, MySQL crea el
     * enum con los valores en el orden en que los ve y rellena las filas que ya
     * existen con el primero. Fijar el valor por omisión evita que una tarea vieja
     * aparezca mañana con un estado que nadie le puso.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 12,
            columnDefinition = "VARCHAR(12) DEFAULT 'EN_PROCESO'")
    private EstadoTareaVolcado estado = EstadoTareaVolcado.EN_PROCESO;

    /** Cuántas filas trae el archivo. Queda en cero hasta que se lee la hoja. */
    @Column(name = "total_filas", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer totalFilas = 0;

    @Column(name = "filas_procesadas", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer filasProcesadas = 0;

    @Column(name = "cuestionarios_creados", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer cuestionariosCreados = 0;

    @Column(name = "respuestas_guardadas", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer respuestasGuardadas = 0;

    @Column(name = "pendientes_de_lectura", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer pendientesDeLectura = 0;

    @Column(name = "discrepancias_guardadas", nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private Integer discrepanciasGuardadas = 0;

    /** Por qué se cortó, cuando el estado es {@link EstadoTareaVolcado#FALLIDA}. */
    @Column(name = "mensaje_error", length = 1000)
    private String mensajeError;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    /**
     * Los participantes del archivo que no se cargaron, con el motivo.
     *
     * <p>Van como filas y no como un texto con todo junto porque es lo que alguien va
     * a leer renglón por renglón para decidir qué hacer con cada uno.</p>
     */
    @OneToMany(mappedBy = "tarea", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("numeroFila ASC")
    private List<FilaOmitidaVolcado> filasOmitidas = new ArrayList<>();

    @PrePersist
    private void prePersist() {
        if (this.fechaInicio == null) this.fechaInicio = LocalDateTime.now();
    }

    /** Si ya no va a cambiar. */
    @Transient
    public boolean termino() {
        return this.estado != EstadoTareaVolcado.EN_PROCESO;
    }
}
