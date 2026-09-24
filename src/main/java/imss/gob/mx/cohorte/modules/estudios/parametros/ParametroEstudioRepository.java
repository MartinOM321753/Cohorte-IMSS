package imss.gob.mx.cohorte.modules.estudios.parametros;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
@Repository
public interface ParametroEstudioRepository extends JpaRepository<ParametroEstudio, Long> {
    Optional<ParametroEstudio> findByTipoEstudio_IdAndNombreIgnoreCase(Long tipoEstudioId, String nombre);

    List<ParametroEstudio> findAllByTipoEstudio_Nombre(String tipoEstudioNombre);

    /**
     * Los parámetros del tipo en el orden configurado. Es el finder que debe usar
     * todo lo que muestre o recorra parámetros: sin ORDER BY, el orden lo decide
     * la base y cambia entre motores y entre planes de consulta.
     */
    List<ParametroEstudio> findAllByTipoEstudio_IdOrderByOrdenAscIdAsc(Long tipoEstudioId);

    List<ParametroEstudio> findAllByTipoEstudio_Id(Long tipoEstudioId);
}
