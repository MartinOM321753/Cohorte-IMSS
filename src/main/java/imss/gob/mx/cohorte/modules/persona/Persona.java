package imss.gob.mx.cohorte.modules.persona;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "persona")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
public class Persona {



    public enum Sexo { M, F }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_persona")
    private Long Id;
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "segundo_nombre", length = 100)
    private String segundoNombre;

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

    @Column(name = "curp", length = 18, unique = true)
    private String curp;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    public Persona(String nombre, String segundoNombre, String apellidoPaterno, String apellidoMaterno, LocalDate fechaNacimiento, Sexo sexo, String curp, String telefono, String email, LocalDateTime fechaRegistro, LocalDateTime fechaActualizacion) {
        this.nombre = nombre;
        this.segundoNombre = segundoNombre;
        this.apellidoPaterno = apellidoPaterno;
        this.apellidoMaterno = apellidoMaterno;
        this.fechaNacimiento = fechaNacimiento;
        this.sexo = sexo;
        this.curp = curp;
        this.telefono = telefono;
        this.email = email;
        this.fechaRegistro = fechaRegistro;
        this.fechaActualizacion = fechaActualizacion;
    }
}