package imss.gob.mx.cohorte.modules.notificaciones;

import imss.gob.mx.cohorte.modules.cita.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NotificacionCitaRepository extends JpaRepository<NotificacionCita, Long> {

    /**
     * ¿Ya se envió exitosamente este tipo de notificación por este canal para esta cita?
     * Usado por el scheduler para evitar duplicados.
     */
    boolean existsByCitaAndTipoAndCanalAndExitoso(
            Cita cita,
            TipoNotificacion tipo,
            CanalNotificacion canal,
            boolean exitoso
    );
}
