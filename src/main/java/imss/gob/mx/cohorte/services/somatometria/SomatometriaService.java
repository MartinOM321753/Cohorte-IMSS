package imss.gob.mx.cohorte.services.somatometria;

import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;

import imss.gob.mx.cohorte.modules.somatometria.Somatometria;
import imss.gob.mx.cohorte.modules.somatometria.SomatometriaRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SomatometriaService {

    private final SomatometriaRepository repository;
    private final imss.gob.mx.cohorte.security.institucion.InstitucionContextService institucionContextService;
    private final ParticipanteAccesoService participanteAccesoService;

    @Transactional(readOnly = true)
    /** Somatometrias registradas por mi institucion, sin pasar por el participante. */
    public List<Somatometria> findAllDeMiInstitucion() {
        return repository.findAllByInstitucion_IdOrderByFechaMedicionDesc(
                institucionContextService.getIdInstitucionActual());
    }

    /**
     * Historial acotado a las instituciones alcanzables. Antes no filtraba nada:
     * bastaba el permiso SOMATOMETRIA_VER y el UUID para leer el historial de un
     * participante de otra sede.
     */
    public List<Somatometria> findByPacienteUuid(String uuid) {
        return repository.findByPacienteUuidOrderByFechaMedicionDesc(uuid);
    }

    @Transactional(readOnly = true)
    public Optional<Somatometria> findLatest(String pacienteUuid) {
        return repository.findLatestByPacienteUuid(pacienteUuid);
    }

    @Transactional(readOnly = true)
    public Somatometria findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("Registro de somatometría no encontrado con id: " + id));
    }

    @Transactional
    public Somatometria save(Somatometria somatometria) {
        return repository.save(somatometria);
    }

    @Transactional
    public Somatometria update(Long id, Somatometria incoming) {
        Somatometria existing = findById(id);
        existing.setFechaMedicion(incoming.getFechaMedicion());
        existing.setPesoKg(incoming.getPesoKg());
        existing.setTallaM(incoming.getTallaM());
        existing.setPresionSistolica(incoming.getPresionSistolica());
        existing.setPresionDiastolica(incoming.getPresionDiastolica());
        existing.setCircunferenciaAbdominalCm(incoming.getCircunferenciaAbdominalCm());
        existing.setFrecuenciaCardiacaReposo(incoming.getFrecuenciaCardiacaReposo());
        existing.setObservaciones(incoming.getObservaciones());
        return repository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        findById(id); // lanza 404 si no existe
        repository.deleteById(id);
    }
}
