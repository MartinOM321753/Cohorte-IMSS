package imss.gob.mx.cohorte.modules.institucion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Decisión de una institución sobre si quiere seguir viendo los participantes de
 * una hija concreta.
 *
 * <p>Hasta ahora ver a las descendientes era una constante del sistema. Esta
 * tabla la convierte en una decisión, pero solo por excepción: si no hay fila
 * para una hija, manda el valor por defecto de la institución padre
 * ({@code Institucion.verParticipantesHijas}). Guardar únicamente las
 * excepciones evita tener que crear filas cada vez que nace una sede y evita que
 * el sistema dependa de un mantenimiento que nadie va a hacer.</p>
 *
 * <p>Lo que aquí se apaga es la vista de <em>participantes</em>, no la
 * administración: el padre sigue gestionando usuarios, catálogos y módulos de esa
 * hija. Si no fuera así, ocultar una hija sería un viaje sin retorno — dejaría de
 * verla también en la pantalla desde la que se vuelve a mostrar.</p>
 */
@Entity
@Table(name = "visibilidad_institucion_hija",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_visibilidad_padre_hija",
        columnNames = {"id_institucion_padre", "id_institucion_hija"}
    ))
@Getter
@Setter
@NoArgsConstructor
public class VisibilidadInstitucionHija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_visibilidad")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_padre", nullable = false)
    private Institucion institucionPadre;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion_hija", nullable = false)
    private Institucion institucionHija;

    @Column(name = "ver_participantes", nullable = false)
    private Boolean verParticipantes = true;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Timestamp fechaActualizacion;

    /** Quién tomó la decisión — la pantalla de instituciones lo muestra. */
    @Column(name = "usuario_uuid", length = 36)
    private String usuarioUuid;

    @PrePersist
    @PreUpdate
    public void marcarFecha() {
        this.fechaActualizacion = new Timestamp(System.currentTimeMillis());
    }
}
