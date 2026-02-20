package imss.gob.mx.cohorte.utils;

import imss.gob.mx.cohorte.modules.usuarios.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.services.usuarios.PersonaService;

import lombok.AllArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Configuration
@AllArgsConstructor
public class InitialConfig implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PersonaService personaService;


    @Override
    public void run(String... args) throws Exception {

        try {

            Role rolAdmin = roleRepository.findByRole("ADMIN");
            if (rolAdmin == null) {
                rolAdmin = new Role();
                rolAdmin.setRole("ADMIN");
                roleRepository.saveAndFlush(rolAdmin);
            }

            Role rolUser = roleRepository.findByRole("USER");
            if (rolUser == null) {
                rolUser = new Role();
                rolUser.setRole("USER");
                roleRepository.saveAndFlush(rolUser);
            }
            LocalDate newDate = LocalDate.now();
            LocalDateTime newDateTime = LocalDateTime.now();

            Role adminrol = roleRepository.findByRole("ADMIN");

            Persona persona = new Persona( "Admin", "Admin", "Admin", newDate, Persona.Sexo.M, "7772589476", "adlajsdajl@gmail.com", newDateTime, newDateTime);
            Persona newPerson = personaService.createPerson(persona);

            if (newPerson != null && adminrol != null) {

                BeanUser admin = new BeanUser();
                Optional<BeanUser> userAdmin = userRepository.findByUsername("admin");

                admin.setUsername("admin");
                admin.setUUID(UUID.randomUUID().toString());
                admin.setPassword(passwordEncoder.encode("password123"));
                admin.setActivo(true);
                admin.setRol(adminrol);
                admin.setPersona(persona);
                admin.setFechaCreacion(LocalDateTime.now());

                BeanUser respAdmin = userRepository.saveAndFlush(admin);
                System.out.println(" Usuario creado: admin - " + respAdmin);

            }else{ System.out.println("Algo salio mal al crear a la persona o usuario"); }

        }catch (Exception e){ System.out.println("Algo salio mal con la configuracion Inicial"); }

    }

}
