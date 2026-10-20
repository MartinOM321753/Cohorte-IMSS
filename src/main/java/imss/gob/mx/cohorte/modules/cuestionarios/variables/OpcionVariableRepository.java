package imss.gob.mx.cohorte.modules.cuestionarios.variables;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OpcionVariableRepository extends JpaRepository<OpcionVariable, Long> {

    List<OpcionVariable> findAllByVariable_IdOrderByOrdenAscIdAsc(Long idVariable);

    Optional<OpcionVariable> findByVariable_IdAndCodigo(Long idVariable, String codigo);

    void deleteAllByVariable_Id(Long idVariable);
}
