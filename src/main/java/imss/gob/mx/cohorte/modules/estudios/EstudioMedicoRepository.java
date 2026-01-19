package imss.gob.mx.cohorte.modules.estudios;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EstudioMedicoRepository extends JpaRepository<EstudioMedico, Long> {
}
