package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@AllArgsConstructor
public class PacienteService {
    private final PacienteRepository pacienteRepository;
    private final FolioGeneratorService folioGeneratorService;

    public List<Paciente> findAllByInstitucion(Long idInstitucion) {
        return pacienteRepository.findAllByInstitucion_Id(idInstitucion);
    }

    public Page<Paciente> findAllPaginadoByInstitucion(Long idInstitucion, Pageable pageable) {
        return pacienteRepository.findAllByInstitucion_Id(idInstitucion, pageable);
    }

    public List<Paciente> findAllStatusByInstitucion(Boolean activo, Long idInstitucion) {
        return pacienteRepository.findAllByActivoAndInstitucion_Id(activo, idInstitucion);
    }

    public Paciente getPatient(Long idPaciente, Long idInstitucion) {
        Paciente findPatient = pacienteRepository.findByIdAndInstitucion_Id(idPaciente, idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El participante no se encuentra activo");
        }
        return findPatient;
    }

    /**
     * Variante sin filtro de institución — solo para uso interno controlado donde
     * el llamador YA validó/asignará la institución por otro medio. Preferir
     * siempre {@link #getByUUID(String, Long)} para resolver referencias entre
     * módulos y mantener el aislamiento de datos.
     */
    public Paciente getByUUID(String uuid) {
        Paciente findPatient = pacienteRepository.findByUuid(uuid)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El participante no se encuentra activo");
        }
        return findPatient;
    }

    public Paciente getByUUID(String uuid, Long idInstitucion) {
        Paciente findPatient = pacienteRepository.findByUuidAndInstitucion_Id(uuid, idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El participante no se encuentra activo");
        }
        return findPatient;
    }

    public Paciente getByFolio(String folio, Long idInstitucion) {
        Paciente findPatient = pacienteRepository.findByFolioAndInstitucion_Id(folio, idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El participante no se encuentra activo");
        }
        return findPatient;
    }

    public Paciente cretePatient(Paciente paciente, Long idInstitucion) {
        String folioCapturado = paciente.getFolio();
        String folio;

        if (folioCapturado == null || folioCapturado.isBlank()) {
            folio = folioGeneratorService.generarFolio(idInstitucion);
        } else {
            // El usuario capturó un folio existente (participante con seguimiento previo):
            // se normaliza a MAYÚSCULAS/alfanumérico para evitar choques de formato
            // con los folios autogenerados, y se valida su unicidad.
            folio = folioGeneratorService.normalizar(folioCapturado);
            if (pacienteRepository.findByFolio(folio).isPresent()) {
                throw new ObjConflictException("El folio ya existe");
            }
        }
        paciente.setFolio(folio);

        paciente.setFechaRegistro(LocalDateTime.now());
        paciente.setFechaActualizacion(LocalDateTime.now());
        paciente.setUuid(java.util.UUID.randomUUID().toString());
        return pacienteRepository.save(paciente);
    }

    public Paciente updatePatient(Paciente paciente, Long idInstitucion) {
        Paciente pacienteBD = pacienteRepository.findByIdAndInstitucion_Id(paciente.getId(), idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException("El participante no existe"));

        String folioCapturado = paciente.getFolio();
        String folio = (folioCapturado == null || folioCapturado.isBlank())
                ? pacienteBD.getFolio()
                : folioGeneratorService.normalizar(folioCapturado);

        if (!folio.equals(pacienteBD.getFolio()) && pacienteRepository.findByFolio(folio).isPresent()) {
            throw new ObjConflictException("El folio ya existe");
        }

        pacienteBD.setFolio(folio);
        pacienteBD.setPersona(paciente.getPersona());
        pacienteBD.setFechaActualizacion(LocalDateTime.now());
        return pacienteRepository.save(pacienteBD);
    }

    /** Alterna el campo activo del paciente (activo ↔ inactivo) sin restricciones de estado. */
    public Paciente toggleActivo(String uuid, Long idInstitucion) {
        Paciente paciente = pacienteRepository.findByUuidAndInstitucion_Id(uuid, idInstitucion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el participante con uuid: " + uuid));
        paciente.setActivo(!paciente.getActivo());
        paciente.setFechaActualizacion(LocalDateTime.now());
        return pacienteRepository.save(paciente);
    }
}
