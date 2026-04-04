package imss.gob.mx.cohorte.modules.paciente;

import imss.gob.mx.cohorte.modules.persona.Persona;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "paciente")
@Getter
@Setter
@NoArgsConstructor

public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paciente")
    private Long Id;

    @Column(name = "uuid", nullable = false, unique = true )
    private String uuid;

    @Column(name = "folio", nullable = false, unique = true, length = 50)
    private String folio;

    @Column(name = "activo")
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToOne
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;


    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID().toString();
        this.fechaRegistro = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }
}
