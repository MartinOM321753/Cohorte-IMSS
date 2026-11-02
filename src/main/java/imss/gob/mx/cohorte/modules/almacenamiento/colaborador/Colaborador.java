package imss.gob.mx.cohorte.modules.almacenamiento.colaborador;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.persona.Persona;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Persona física a la que el biobanco entrega o presta muestras para análisis
 * u otros usos: un colaborador interno o externo que NO necesita cuenta en el
 * sistema, pero cuyos datos deben quedar registrados para saber quién recibió
 * cada muestra.
 *
 * <p>Es una capa fina sobre {@link Persona} (identidad pura), igual que
 * {@code BeanUser}. Un colaborador y un usuario pueden compartir la misma
 * {@link Persona}: «otorgarle permisos» en el futuro es crearle un
 * {@code BeanUser} sobre esa misma persona, sin perder su historial de
 * entregas.</p>
 *
 * <p>Pertenece a la institución que lo registró ({@link #institucion}), que es
 * la que lo ve y le entrega muestras — el mismo aislamiento por institución que
 * siguen muestras, usuarios y pacientes.</p>
 */
@Entity
@Table(name = "Colaborador")
@Getter
@Setter
@NoArgsConstructor
public class Colaborador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_colaborador")
    private Long id;

    @Version
    @Column(name = "version")
    private Long version;

    /** Identidad del colaborador. Reutilizable para promoverlo a usuario del sistema después. */
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    /** Institución que registró al colaborador y le entrega muestras (aislamiento de datos). */
    @ManyToOne(optional = false)
    @JsonIgnore
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    /** Cargo o rol del colaborador (p. ej. «Investigador», «Técnico»). Libre, opcional. */
    @Column(name = "cargo", length = 150)
    private String cargo;

    /**
     * Dependencia o institución de adscripción, como texto libre: muchas veces
     * es una institución externa que NO está en el catálogo del sistema, así que
     * no se modela como FK a {@link Institucion}.
     */
    @Column(name = "adscripcion", length = 200)
    private String adscripcion;

    /** true = colaborador ajeno a la red de la cohorte (institución externa). */
    @Column(name = "es_externo")
    private Boolean esExterno = false;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
