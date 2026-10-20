package imss.gob.mx.cohorte.modules.cuestionarios.secciones;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeccionCuestionarioRepository extends JpaRepository<SeccionCuestionario, Long> {

    List<SeccionCuestionario> findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(Long idTipo);

    Optional<SeccionCuestionario> findByTipoCuestionario_IdAndNombreIgnoreCase(
            Long idTipo, String nombre);

    void deleteAllByTipoCuestionario_Id(Long idTipo);
}
