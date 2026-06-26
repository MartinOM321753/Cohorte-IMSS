package imss.gob.mx.cohorte.modules.estudios.parametros;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OpcionParametroRepository extends JpaRepository<OpcionParametro, Long> {

    List<OpcionParametro> findAllByParametro_IdOrderByOrdenAsc(Long parametroId);
}
