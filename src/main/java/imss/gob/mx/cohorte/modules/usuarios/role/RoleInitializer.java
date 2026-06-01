package imss.gob.mx.cohorte.modules.usuarios.role;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Asigna UUIDs a roles que no los tengan y garantiza que el rol ENCARGADO
 * exista en la base de datos. Se ejecuta al arrancar la aplicación.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer {

    private final RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void inicializar() {
        asignarUuidsARolesSinUuid();
        garantizarRolEncargado();
    }

    private void asignarUuidsARolesSinUuid() {
        List<Role> sinUuid = roleRepository.findAll().stream()
                .filter(r -> r.getUuid() == null || r.getUuid().isBlank())
                .toList();

        if (sinUuid.isEmpty()) return;

        sinUuid.forEach(r -> r.setUuid(UUID.randomUUID().toString()));
        roleRepository.saveAll(sinUuid);
        log.info("RoleInitializer: UUID asignado a {} rol(es).", sinUuid.size());
    }

    private void garantizarRolEncargado() {
        if (roleRepository.findByRole("ENCARGADO").isPresent()) return;

        Role encargado = new Role();
        encargado.setRole("ENCARGADO");
        encargado.setUuid(UUID.randomUUID().toString());
        roleRepository.save(encargado);
        log.info("RoleInitializer: rol ENCARGADO creado.");
    }
}
