package imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OpcionParametroEstudioMuestraRepository extends JpaRepository<OpcionParametroEstudioMuestra, Long> {

    List<OpcionParametroEstudioMuestra> findAllByParametro_IdOrderByOrdenAsc(Long parametroId);
}
