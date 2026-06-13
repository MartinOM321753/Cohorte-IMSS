package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefrigeradorRepository extends JpaRepository<Refrigerador, Long> {

    Optional<Refrigerador> findByCodigo(String codigo);

    List<Refrigerador> findAllByInstitucion_Id(Long idInstitucion);

    List<Refrigerador> findAllByActivoAndInstitucion_Id(Boolean activo, Long idInstitucion);

    @Query("SELECT MAX(r.codigo) FROM Refrigerador r WHERE r.codigo LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxCodigoByPrefix(@Param("prefix") String prefix);

    @Query("SELECT MAX(r.codigo) FROM Refrigerador r WHERE r.codigo LIKE CONCAT(:prefix, '%') AND r.institucion.id = :idInst")
    Optional<String> findMaxCodigoByPrefixAndInstitucion(@Param("prefix") String prefix, @Param("idInst") Long idInstitucion);

    Optional<Refrigerador> findByCodigoAndInstitucion_Id(String codigo, Long idInstitucion);
}
