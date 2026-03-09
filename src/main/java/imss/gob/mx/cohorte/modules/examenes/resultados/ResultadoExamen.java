package imss.gob.mx.cohorte.modules.examenes.resultados;


import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Resultado_Examen")
@Getter @Setter
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

}
