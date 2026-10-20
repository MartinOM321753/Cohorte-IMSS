package imss.gob.mx.cohorte.modules.cuestionarios.validaciones;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReglaValidacionRepository extends JpaRepository<ReglaValidacion, Long> {

    List<ReglaValidacion> findAllByTipoCuestionario_IdAndActivaTrue(Long idTipo);

    List<ReglaValidacion> findAllByTipoCuestionario_IdOrderByIdAsc(Long idTipo);

    void deleteAllByTipoCuestionario_Id(Long idTipo);
}
