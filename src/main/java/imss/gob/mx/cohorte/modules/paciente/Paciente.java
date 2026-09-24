package imss.gob.mx.cohorte.modules.paciente;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
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

    /**
     * Número consecutivo del participante. Opcional: no todos lo traen, y la
     * columna admite nulos para eso.
     *
     * <p>Único como el folio, y por el mismo motivo: sirve para identificar a una
     * persona, así que dos participantes con el mismo número harían ambiguo el
     * registro que lo cite. El ámbito también es el del folio —todo el padrón, no
     * cada institución—, porque un participante se reasigna de sede sin que su
     * identificador cambie.</p>
     *
     * <p>Que sea nulo no rompe la unicidad: MySQL no compara nulos entre sí en un
     * índice único, así que pueden convivir todos los participantes sin número que
     * haga falta. Esa es también la razón de que agregar la columna a una tabla con
     * datos no necesite rellenar nada.</p>
     */
    @Column(name = "no_consecutivo", unique = true)
    private Long noConsecutivo;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToOne
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    /** Institución que registró al participante — define el ámbito de aislamiento de datos. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;


    @PrePersist
    public void prePersist() {
        this.uuid = UUID.randomUUID().toString();
        this.fechaRegistro = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }
}
