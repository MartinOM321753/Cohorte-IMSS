package imss.gob.mx.cohorte.modules.institucion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VisibilidadInstitucionHijaRepository extends JpaRepository<VisibilidadInstitucionHija, Long> {

    Optional<VisibilidadInstitucionHija> findByInstitucionPadre_IdAndInstitucionHija_Id(Long idPadre, Long idHija);

    List<VisibilidadInstitucionHija> findAllByInstitucionPadre_Id(Long idPadre);

    /**
     * Solo las hijas explícitamente ocultas. Es lo que consulta el recorrido del
     * árbol: se pregunta una vez por institución y se resuelve en memoria, en vez
     * de una consulta por cada hija visitada.
     */
    List<VisibilidadInstitucionHija> findAllByInstitucionPadre_IdAndVerParticipantesFalse(Long idPadre);
}
