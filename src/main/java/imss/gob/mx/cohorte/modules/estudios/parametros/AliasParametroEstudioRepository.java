package imss.gob.mx.cohorte.modules.estudios.parametros;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AliasParametroEstudioRepository extends JpaRepository<AliasParametroEstudio, Long> {

    List<AliasParametroEstudio> findAllByParametro_IdOrderByOrdenAsc(Long idParametro);

    /**
     * Todos los alias de un tipo de estudio, de una sola consulta.
     *
     * <p>Es lo que carga el importador antes de recorrer el archivo: emparejar
     * columna por columna con una consulta cada vez multiplicaría los viajes a
     * la base por el número de columnas del archivo.</p>
     */
    List<AliasParametroEstudio> findAllByIdTipoEstudio(Long idTipoEstudio);

    /** Resuelve una columna del archivo dentro del tipo indicado. */
    Optional<AliasParametroEstudio> findByIdTipoEstudioAndAliasNormalizado(
            Long idTipoEstudio, String aliasNormalizado);

    void deleteAllByParametro_Id(Long idParametro);
}
