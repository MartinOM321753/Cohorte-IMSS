package imss.gob.mx.cohorte.modules.solicitud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CorreoDestinoSolicitudRepository extends JpaRepository<CorreoDestinoSolicitud, Long> {

    List<CorreoDestinoSolicitud> findByActivoTrue();

    boolean existsByEmailIgnoreCase(String email);
}
