package imss.gob.mx.cohorte.modules.cuestionarios.tipos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TipoCuestionarioRepository extends JpaRepository<TipoCuestionario, Long> {

    List<TipoCuestionario> findAllByInstitucion_IdOrderByClaveAsc(Long idInstitucion);

    List<TipoCuestionario> findAllByActivoAndInstitucion_IdOrderByClaveAsc(
            Boolean activo, Long idInstitucion);

    Optional<TipoCuestionario> findByIdAndInstitucion_Id(Long id, Long idInstitucion);

    Optional<TipoCuestionario> findByNombreIgnoreCaseAndVersionAndInstitucion_Id(
            String nombre, String version, Long idInstitucion);

    /**
     * Resuelve el prefijo de una etiqueta al instrumento que le corresponde.
     *
     * <p>Es lo que convierte {@code C1/008601/F4} en «este archivo es del
     * cuestionario general». Devuelve lista y no un solo resultado porque la misma
     * clave puede existir en varias versiones; quien importa elige, y que haya más
     * de una es justamente lo que hay que poder detectar para preguntar.</p>
     */
    List<TipoCuestionario> findAllByClaveIgnoreCaseAndActivoAndInstitucion_Id(
            String clave, Boolean activo, Long idInstitucion);

    boolean existsByClaveIgnoreCaseAndVersionAndInstitucion_Id(
            String clave, String version, Long idInstitucion);
}
