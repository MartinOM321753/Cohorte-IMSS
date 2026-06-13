package imss.gob.mx.cohorte.modules.almacenamiento.caja;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CajaCriogenicaRepository extends JpaRepository<CajaCriogenica, Long> {
    Optional<CajaCriogenica> findByCodigoCaja(String codigoCaja);
    List<CajaCriogenica> findAllByActivo(Boolean activo);
    List<CajaCriogenica> findAllByPosicionPiso_Id(Long posicionPisoId);
    List<CajaCriogenica> findAllByInstitucion_Id(Long idInstitucion);
    List<CajaCriogenica> findAllByActivoAndInstitucion_Id(Boolean activo, Long idInstitucion);

    @Query("SELECT MAX(c.codigoCaja) FROM CajaCriogenica c WHERE c.codigoCaja LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxCodigoCajaByPrefix(@Param("prefix") String prefix);

    @Query("SELECT MAX(c.codigoCaja) FROM CajaCriogenica c WHERE c.codigoCaja LIKE CONCAT(:prefix, '%') AND c.institucion.id = :idInst")
    Optional<String> findMaxCodigoCajaByPrefixAndInstitucion(@Param("prefix") String prefix, @Param("idInst") Long idInstitucion);

    Optional<CajaCriogenica> findByCodigoCajaAndInstitucion_Id(String codigoCaja, Long idInstitucion);
}
