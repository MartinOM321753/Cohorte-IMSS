package imss.gob.mx.cohorte.utils;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.institucion.TipoInstitucion;
import imss.gob.mx.cohorte.modules.institucion.TipoInstitucionRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.persona.PersonaRepository;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Configuration
@AllArgsConstructor
public class InitialConfig implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final InstitucionRepository institucionRepository;
    private final TipoInstitucionRepository tipoInstitucionRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Role rolAdmin = ensureRole("ADMINISTRADOR");
        ensureRole("RECEPCIONISTA");
        ensureRole("MEDICO");
        ensureRole("LABORATORISTA");
        ensureRole("ENCARGADO");

        Institucion institucionRaiz = ensureInstitucionRaiz();

        if (userRepository.findByUsername("admin").isPresent()) {
            return;
        }

        Persona persona = personaRepository.findByEmail("garciawalit@gmail.com")
                .orElseGet(() -> {
                    Persona nuevaPersona = new Persona();
                    nuevaPersona.setNombre("Admin");
                    nuevaPersona.setApellidoPaterno("Cohorte");
                    nuevaPersona.setApellidoMaterno("Sistema");
                    nuevaPersona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
                    nuevaPersona.setSexo(Persona.Sexo.M);
                    nuevaPersona.setTelefono("7772589476");
                    nuevaPersona.setEmail("garciawalit@gmail.com");
                    nuevaPersona.setFechaRegistro(LocalDateTime.now());
                    nuevaPersona.setFechaActualizacion(LocalDateTime.now());
                    return personaRepository.save(nuevaPersona);
                });

        BeanUser admin = new BeanUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("password123"));
        admin.setActivo(true);
        admin.setRol(rolAdmin);
        admin.setInstitucion(institucionRaiz);
        admin.setPersona(persona);
        admin.setFechaCreacion(LocalDateTime.now());
        admin.setFechaActualizacion(LocalDateTime.now());

        userRepository.save(admin);
    }

    private Role ensureRole(String roleName) {
        return roleRepository.findByRole(roleName)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setRole(roleName);
                    return roleRepository.saveAndFlush(role);
                });
    }

    /**
     * Garantiza la existencia de una institución raíz (sin institución padre) —
     * es la única autorizada para otorgar permisos de módulo a otras
     * instituciones y sirve de "home" del usuario administrador inicial.
     */
    private Institucion ensureInstitucionRaiz() {
        TipoInstitucion tipoImss = tipoInstitucionRepository.findByNombreIgnoreCase("IMSS")
                .orElseGet(() -> {
                    TipoInstitucion tipo = new TipoInstitucion();
                    tipo.setNombre("IMSS");
                    tipo.setActivo(true);
                    return tipoInstitucionRepository.saveAndFlush(tipo);
                });
        tipoInstitucionRepository.findByNombreIgnoreCase("INSP").orElseGet(() -> {
            TipoInstitucion tipo = new TipoInstitucion();
            tipo.setNombre("INSP");
            tipo.setActivo(true);
            return tipoInstitucionRepository.saveAndFlush(tipo);
        });

        return institucionRepository.findByNombreIgnoreCase("IMSS Cuernavaca - Sede Central")
                .orElseGet(() -> {
                    Institucion raiz = new Institucion();
                    raiz.setUuid(UUID.randomUUID().toString());
                    raiz.setNombre("IMSS Cuernavaca - Sede Central");
                    raiz.setTipoInstitucion(tipoImss);
                    raiz.setInstitucionPadre(null);
                    raiz.setEstado("Morelos");
                    raiz.setCiudad("Cuernavaca");
                    raiz.setTieneBiobanco(true);
                    raiz.setActivo(true);
                    raiz.setFechaRegistro(Timestamp.from(Instant.now()));
                    return institucionRepository.saveAndFlush(raiz);
                });
    }
}
