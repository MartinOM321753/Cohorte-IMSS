package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResultadoEstudioMuestraRepository extends JpaRepository<ResultadoEstudioMuestra, Long> {

    boolean existsByParametro_Id(Long parametroId);
}
