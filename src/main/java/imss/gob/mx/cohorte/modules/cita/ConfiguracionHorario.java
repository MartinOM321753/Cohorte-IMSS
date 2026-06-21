package imss.gob.mx.cohorte.modules.cita;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_horario")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionHorario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion_horario")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "hora_inicio", nullable = false)
    private Integer horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private Integer horaFin;

    @Column(name = "lunes", nullable = false)
    private Boolean lunes = true;

    @Column(name = "martes", nullable = false)
    private Boolean martes = true;

    @Column(name = "miercoles", nullable = false)
    private Boolean miercoles = true;

    @Column(name = "jueves", nullable = false)
    private Boolean jueves = true;

    @Column(name = "viernes", nullable = false)
    private Boolean viernes = true;

    @Column(name = "sabado", nullable = false)
    private Boolean sabado = false;

    @Column(name = "domingo", nullable = false)
    private Boolean domingo = false;

    @Column(name = "activa", nullable = false)
    private Boolean activa = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
