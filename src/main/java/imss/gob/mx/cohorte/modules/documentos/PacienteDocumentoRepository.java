package imss.gob.mx.cohorte.modules.documentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PacienteDocumentoRepository extends JpaRepository<PacienteDocumento, Long> {

    List<PacienteDocumento> findByPaciente_UuidOrderByDocumento_FechaSubidaDesc(String uuid);

    List<PacienteDocumento> findByPaciente_UuidAndTipoDocOrderByDocumento_FechaSubidaDesc(
            String uuid, TipoDocumentoPaciente tipoDoc);

    /** Resuelve a qué participante cuelga un documento, para heredar de él su puerta de acceso. */
    List<PacienteDocumento> findByDocumento_Id(Long documentoId);

    void deleteByDocumento_Id(Long documentoId);

    long countByPaciente_Institucion_Id(Long idInstitucion);

    /** Sin filtro de institucion: se usa para saber si un participante ya quedo vinculado a alguna. */
    long countByPaciente_Uuid(String uuid);
}
