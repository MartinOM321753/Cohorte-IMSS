package imss.gob.mx.cohorte.utils;

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

import java.time.LocalDate;
import java.time.LocalDateTime;

@Configuration
@AllArgsConstructor
public class InitialConfig implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Role rolAdmin = ensureRole("ADMINISTRADOR");
        ensureRole("RECEPCIONISTA");
        ensureRole("MEDICO");
        ensureRole("LABORATORISTA");

        if (userRepository.findByUsername("admin").isPresent()) {
            return;
        }

        Persona persona = personaRepository.findByEmail("admin@cohorte.local")
                .orElseGet(() -> {
                    Persona nuevaPersona = new Persona();
                    nuevaPersona.setNombre("Admin");
                    nuevaPersona.setApellidoPaterno("Cohorte");
                    nuevaPersona.setApellidoMaterno("Sistema");
                    nuevaPersona.setFechaNacimiento(LocalDate.of(1990, 1, 1));
                    nuevaPersona.setSexo(Persona.Sexo.M);
                    nuevaPersona.setTelefono("7772589476");
                    nuevaPersona.setEmail("admin@cohorte.local");
                    nuevaPersona.setFechaRegistro(LocalDateTime.now());
                    nuevaPersona.setFechaActualizacion(LocalDateTime.now());
                    return personaRepository.save(nuevaPersona);
                });

        BeanUser admin = new BeanUser();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("password123"));
        admin.setActivo(true);
        admin.setRol(rolAdmin);
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
}
