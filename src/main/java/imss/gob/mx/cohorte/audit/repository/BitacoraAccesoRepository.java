package imss.gob.mx.cohorte.audit.repository;

import imss.gob.mx.cohorte.audit.model.BitacoraAcceso;
import imss.gob.mx.cohorte.audit.model.TipoEventoAcceso;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface BitacoraAccesoRepository extends JpaRepository<BitacoraAcceso, Long> {

    @Query("""
        SELECT b FROM BitacoraAcceso b
        WHERE (:desde IS NULL OR b.timestamp >= :desde)
          AND (:hasta  IS NULL OR b.timestamp <= :hasta)
          AND (:usuarioUuid IS NULL OR b.usuarioUuid = :usuarioUuid)
          AND (:tipoEvento  IS NULL OR b.tipoEvento  = :tipoEvento)
        ORDER BY b.timestamp DESC
        """)
    Page<BitacoraAcceso> buscarConFiltros(
            @Param("desde")       LocalDateTime desde,
            @Param("hasta")       LocalDateTime hasta,
            @Param("usuarioUuid") String usuarioUuid,
            @Param("tipoEvento")  TipoEventoAcceso tipoEvento,
            Pageable pageable
    );
}
