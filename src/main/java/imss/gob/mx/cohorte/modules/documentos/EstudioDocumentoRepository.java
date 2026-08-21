package imss.gob.mx.cohorte.modules.documentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface EstudioDocumentoRepository extends JpaRepository<EstudioDocumento, Long> {

    List<EstudioDocumento> findByEstudio_IdOrderByOrdenAsc(Long estudioId);

    /** Resuelve a qué estudio cuelga un documento, para heredar de él su puerta de acceso. */
    List<EstudioDocumento> findByDocumento_Id(Long documentoId);

    /**
     * Cuantos documentos tiene cada estudio de la lista, en una sola consulta.
     *
     * <p>Se cuenta en bloque y no estudio por estudio porque estos listados traen
     * decenas de filas: una consulta por cada una convertiria abrir el expediente
     * de un participante en decenas de viajes a la base.</p>
     */
    @Query("SELECT ed.estudio.id, COUNT(ed) FROM EstudioDocumento ed "
         + "WHERE ed.estudio.id IN :ids GROUP BY ed.estudio.id")
    List<Object[]> contarPorEstudio(@Param("ids") List<Long> ids);

    void deleteByDocumento_Id(Long documentoId);

    @Modifying
    @Query("DELETE FROM EstudioDocumento ed WHERE ed.estudio.id = :estudioId")
    void deleteByEstudio_Id(@Param("estudioId") Long estudioId);
}
