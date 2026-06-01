package imss.gob.mx.cohorte.audit.repository;

import imss.gob.mx.cohorte.audit.model.BitacoraAcciones;
import imss.gob.mx.cohorte.audit.model.TipoAccion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BitacoraAccionesRepository extends JpaRepository<BitacoraAcciones, Long> {

    @Query("""
        SELECT b FROM BitacoraAcciones b
        WHERE (:desde IS NULL OR b.timestamp >= :desde)
          AND (:hasta         IS NULL OR b.timestamp       <= :hasta)
          AND (:usuarioUuid   IS NULL OR b.usuarioUuid      = :usuarioUuid)
          AND (:tipoAccion    IS NULL OR b.tipoAccion        = :tipoAccion)
          AND (:entidad       IS NULL OR b.entidadAfectada   = :entidad)
        ORDER BY b.timestamp DESC
        """)
    Page<BitacoraAcciones> buscarConFiltros(
            @Param("desde")       LocalDateTime desde,
            @Param("hasta")       LocalDateTime hasta,
            @Param("usuarioUuid") String usuarioUuid,
            @Param("tipoAccion")  TipoAccion tipoAccion,
            @Param("entidad")     String entidad,
            Pageable pageable
    );
}
