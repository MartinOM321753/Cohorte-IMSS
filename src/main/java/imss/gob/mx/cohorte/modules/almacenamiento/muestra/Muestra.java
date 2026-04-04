package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;
import java.time.LocalDateTime;


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


}

