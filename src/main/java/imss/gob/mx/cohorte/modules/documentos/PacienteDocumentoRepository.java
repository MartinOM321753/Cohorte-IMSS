package imss.gob.mx.cohorte.modules.documentos;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PacienteDocumentoRepository extends JpaRepository<PacienteDocumento, Long> {

    List<PacienteDocumento> findByPaciente_UuidOrderByDocumento_FechaSubidaDesc(String uuid);

    List<PacienteDocumento> findByPaciente_UuidAndTipoDocOrderByDocumento_FechaSubidaDesc(
            String uuid, TipoDocumentoPaciente tipoDoc);

    void deleteByDocumento_Id(Long documentoId);

    long countByPaciente_Institucion_Id(Long idInstitucion);
}
