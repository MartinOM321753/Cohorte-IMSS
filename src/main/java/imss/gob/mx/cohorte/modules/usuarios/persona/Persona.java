package imss.gob.mx.cohorte.modules.usuarios.persona;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "persona")
@Getter @Setter
@NoArgsConstructor
public class Persona {

    public enum Sexo { M, F }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    private Long Id;
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;
    
    @Column(name = "apellido_paterno", nullable = false, length = 100)
    private String apellidoPaterno;
    
    @Column(name = "apellido_materno", length = 100)
    private String apellidoMaterno;
    
    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "sexo", columnDefinition = "ENUM('M','F')")
    private Sexo sexo;

    @Column(name = "telefono", length = 10)
    private String telefono;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}