package imss.gob.mx.cohorte.modules.escalonPrueba;


import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Entity
@Table(name = "Prueba_Escalon")
@Getter
@Setter
@NoArgsConstructor
public class PruebaEscalon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prueba_escalon")
    private Long Id;

    @Column(name = "fecha_estudio", nullable = false)
    private LocalDate fechaEstudio;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @ManyToOne
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "id_usuario_realiza", nullable = false)
    private BeanUser usuarioRealiza;

    @OneToMany(mappedBy = "pruebaEscalon")
    private List<PruebaEscalonEtapa> etapas;


}
