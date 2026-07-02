package imss.gob.mx.cohorte.services.permisos;

import imss.gob.mx.cohorte.modules.permisos.BitacoraPermisos;
import imss.gob.mx.cohorte.modules.permisos.BitacoraPermisosRepository;
import imss.gob.mx.cohorte.modules.permisos.UsuarioPermiso;
import imss.gob.mx.cohorte.modules.permisos.UsuarioPermisoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PermisoExpirationScheduler {

    private final UsuarioPermisoRepository usuarioPermisoRepository;
    private final BitacoraPermisosRepository bitacoraPermisosRepository;

    @Scheduled(fixedRate = 300_000)
    @Transactional
    public void expirarPermisosTemporales() {
        LocalDateTime ahora = LocalDateTime.now();
        List<UsuarioPermiso> expirados = usuarioPermisoRepository
                .findAllByActivoTrueAndFechaFinBeforeAndFechaFinIsNotNull(ahora);

        for (UsuarioPermiso up : expirados) {
            up.setActivo(false);

            BitacoraPermisos bitacora = new BitacoraPermisos();
            bitacora.setUsuarioAfectadoUuid(up.getUsuario().getUUID());
            bitacora.setAccion("PERMISO_EXPIRADO");
            bitacora.setDetalle(up.getTipo().name() + " de " + up.getPermiso().getCodigo() + " expirada");
            bitacora.setRealizadoPorUuid("SISTEMA");
            bitacoraPermisosRepository.save(bitacora);
        }

        if (!expirados.isEmpty()) {
            usuarioPermisoRepository.saveAll(expirados);
            log.info("PermisoExpirationScheduler: {} permisos temporales expirados.", expirados.size());
        }
    }
}
