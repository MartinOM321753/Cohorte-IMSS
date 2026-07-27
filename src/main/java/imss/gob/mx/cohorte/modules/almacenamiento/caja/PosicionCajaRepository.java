package imss.gob.mx.cohorte.modules.almacenamiento.caja;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PosicionCajaRepository extends JpaRepository<PosicionCaja, Long> {
    List<PosicionCaja> findAllByCaja_Id(Long cajaId);
    List<PosicionCaja> findAllByCaja_IdAndOcupada(Long cajaId, Boolean ocupada);
    Optional<PosicionCaja> findByCaja_IdAndFilaAndColumna(Long cajaId, Integer fila, Integer columna);

    @Query("SELECT COUNT(p) FROM PosicionCaja p WHERE p.caja.id = :cajaId AND p.ocupada = true")
    long countOcupadasByCajaId(@Param("cajaId") Long cajaId);

    @Modifying
    @Query("DELETE FROM PosicionCaja p WHERE p.caja.id = :cajaId")
    void deleteAllByCajaId(@Param("cajaId") Long cajaId);
}
