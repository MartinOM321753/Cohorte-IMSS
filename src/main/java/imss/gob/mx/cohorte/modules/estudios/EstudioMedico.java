package imss.gob.mx.cohorte.modules.estudios;


import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "Estudio_Medico")
@Getter
@Setter
@NoArgsConstructor
public class EstudioMedico {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudio")
    private Long Id;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "fecha_estudio", nullable = false)
    private LocalDate fechaEstudio;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @ManyToOne
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "id_tipo_estudio", nullable = false)
    private TipoEstudio tipoEstudio;

    @OneToMany(mappedBy = "estudio")
    private List<ResultadoEstudio> resultadoEstudio;

    @ManyToOne
    @JoinColumn(name = "id_usuario_realiza", nullable = false)
    private BeanUser usuarioRealiza;

}
