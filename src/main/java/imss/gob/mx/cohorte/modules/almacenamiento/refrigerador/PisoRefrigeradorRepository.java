package imss.gob.mx.cohorte.modules.almacenamiento.refrigerador;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PisoRefrigeradorRepository extends JpaRepository<PisoRefrigerador, Long> {
    boolean existsByNumeroPiso(String numeroPiso);

    Optional<PisoRefrigerador> findByNumeroPiso(String numeroPiso);

    List<PisoRefrigerador> findAllByRefrigerador_Id(Long refrigeradorId);

    @Query("SELECT MAX(p.numeroPiso) FROM PisoRefrigerador p WHERE p.numeroPiso LIKE CONCAT(:prefix, '%')")
    Optional<String> findMaxNumeroPisoByPrefix(@Param("prefix") String prefix);

    @Query("SELECT MAX(p.numeroPiso) FROM PisoRefrigerador p WHERE p.numeroPiso LIKE CONCAT(:prefix, '%') AND p.refrigerador.id = :idRef")
    Optional<String> findMaxNumeroPisoByPrefixAndRefrigerador(@Param("prefix") String prefix, @Param("idRef") Long idRefrigerador);

    Optional<PisoRefrigerador> findByNumeroPisoAndRefrigerador_Id(String numeroPiso, Long idRefrigerador);
}
