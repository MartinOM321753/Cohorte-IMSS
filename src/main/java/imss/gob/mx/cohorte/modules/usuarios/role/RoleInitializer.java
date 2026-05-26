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
 * Asigna un UUID a cualquier rol que aún no tenga uno.
 * Se ejecuta una sola vez al arrancar la aplicación.
 * Después de la primera ejecución no hace nada (todos ya tienen UUID).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RoleInitializer {

    private final RoleRepository roleRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void asignarUuidsAuRolesSinUuid() {
        List<Role> sinUuid = roleRepository.findAll().stream()
                .filter(r -> r.getUuid() == null || r.getUuid().isBlank())
                .toList();

        if (sinUuid.isEmpty()) return;

        sinUuid.forEach(r -> r.setUuid(UUID.randomUUID().toString()));
        roleRepository.saveAll(sinUuid);
        log.info("RoleInitializer: UUID asignado a {} rol(es) existente(s).", sinUuid.size());
    }
}
