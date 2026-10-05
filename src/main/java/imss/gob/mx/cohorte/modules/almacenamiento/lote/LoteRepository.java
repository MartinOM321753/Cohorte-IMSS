package imss.gob.mx.cohorte.modules.almacenamiento.lote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findAllByPaciente_IdAndInstitucion_IdOrderByNumeroLoteAsc(Long idPaciente, Long idInstitucion);

    @Query("SELECT COALESCE(MAX(l.numeroLote), 0) FROM Lote l "
            + "WHERE l.paciente.id = :idPaciente AND l.institucion.id = :idInstitucion")
    int findMaxNumeroLoteByPaciente(@Param("idPaciente") Long idPaciente,
                                    @Param("idInstitucion") Long idInstitucion);

    /**
     * Máximo número de lote dentro de un (participante, protocolo). El consecutivo
     * de lote (L1, L2…) es por protocolo procesado: Sangre total y Heces numeran
     * por separado, y la 2ª pasada de un protocolo toma el siguiente.
     */
    @Query("SELECT COALESCE(MAX(l.numeroLote), 0) FROM Lote l "
            + "WHERE l.paciente.id = :idPaciente AND l.institucion.id = :idInstitucion "
            + "AND l.protocolo.id = :idProtocolo")
    int findMaxNumeroLoteByPacienteAndProtocolo(@Param("idPaciente") Long idPaciente,
                                                @Param("idInstitucion") Long idInstitucion,
                                                @Param("idProtocolo") Long idProtocolo);

    List<Lote> findAllByPaciente_IdAndInstitucion_IdAndProtocolo_IdOrderByNumeroLoteAsc(
            Long idPaciente, Long idInstitucion, Long idProtocolo);
}
