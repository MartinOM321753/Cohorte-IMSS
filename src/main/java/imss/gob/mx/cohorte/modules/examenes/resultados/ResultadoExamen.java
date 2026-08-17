package imss.gob.mx.cohorte.modules.examenes.resultados;


import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Resultado_Examen")
@Getter
@Setter
@NoArgsConstructor
public class ResultadoExamen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado")
    private Long Id;
    @ManyToOne
    @JoinColumn(name = "id_usuario_registro", nullable = false)
    private BeanUser usuarioRegistro;

    @Column(name = "valor_obtenido", nullable = false)
    private Double valorObtenido;

    @Column(name = "observaciones", length = 1000)
    private String observaciones;

    @Column(name = "fecha_resultado", nullable = false)
    private LocalDateTime fechaResultado;

    @Column(name = "fecha_registro", nullable = false)
    private Timestamp fechaRegistro;

    @ManyToOne
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "id_examen", nullable = false)
    private Examen examen;

    /**
     * Sede que capturo el resultado. Era la unica entidad clinica sin institucion
     * propia: heredaba la del paciente, asi que al mover un participante de sede sus
     * resultados cambiaban de dueno en silencio, y una institucion no podia alcanzar
     * los resultados que ella misma capturo a un participante que ya no gestiona.
     *
     * <p>El DEFAULT 1 es una ayuda de despliegue, no una regla de negocio: permite
     * agregar la columna sobre tablas con datos. El codigo siempre la asigna desde
     * el contexto del usuario.</p>
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false,
            columnDefinition = "BIGINT NOT NULL DEFAULT 1")
    private Institucion institucion;




}
