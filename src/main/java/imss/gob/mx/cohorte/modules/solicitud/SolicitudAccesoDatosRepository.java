package imss.gob.mx.cohorte.modules.solicitud;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface SolicitudAccesoDatosRepository extends JpaRepository<SolicitudAccesoDatos, Long> {

    long countByIpSolicitanteAndFechaSolicitudAfter(String ip, LocalDateTime after);
}
