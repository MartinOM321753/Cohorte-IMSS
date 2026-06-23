package imss.gob.mx.cohorte.modules.usuarios.user;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name ="usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BeanUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long Id;

    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password", nullable = false, length = 255)
    private String password;

    @Column(name = "uuid", nullable = false, unique = true)
    private String UUID;

    @Column(name = "activo")
    private Boolean activo = true;

    /** true cuando la contraseña fue generada por el sistema y el usuario aún no la ha cambiado. */
    @Column(name = "debe_cambiar_password", nullable = false)
    private Boolean debeResetear = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToOne
    @JoinColumn(name = "id_persona", nullable = false, unique = true)
    private Persona persona;

    @ManyToOne
    @JoinColumn(name = "id_rol", nullable = false)
    @JsonIgnore
    private Role rol;

    /**
     * Institución a la que pertenece el usuario — define el alcance de
     * aislamiento de datos (cada institución administra sus propios
     * pacientes/estudios/exámenes/etc., salvo lo compartido vía permisos
     * de módulo). Obligatoria: todo usuario pertenece a una institución.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @PrePersist
    public void prePersist() {
        this.UUID = java.util.UUID.randomUUID().toString();
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = LocalDateTime.now();
    }




}
