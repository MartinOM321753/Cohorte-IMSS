package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import jakarta.validation.ValidationException;
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

    public Paciente cretePatient(Paciente paciente) {
        String folioCapturado = paciente.getFolio();
        String folio;

        if (folioCapturado == null || folioCapturado.isBlank()) {
            folio = folioGeneratorService.generarFolio();
        } else {
            folio = folioGeneratorService.normalizar(folioCapturado);
            if (pacienteRepository.existsByFolio(folio)) {
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

        if (paciente.getPersona() != null) {
            String curp = paciente.getPersona().getCurp();
            if (curp == null || curp.isBlank()) {
                throw new ValidationException("El CURP es obligatorio para completar el expediente");
            }
            if (paciente.getPersona().getFechaNacimiento() == null) {
                throw new ValidationException("La fecha de nacimiento es obligatoria para completar el expediente");
            }
            if (paciente.getPersona().getSexo() == null) {
                throw new ValidationException("El sexo es obligatorio para completar el expediente");
            }
        }

        String folioCapturado = paciente.getFolio();
        String folio = (folioCapturado == null || folioCapturado.isBlank())
                ? pacienteBD.getFolio()
                : folioGeneratorService.normalizar(folioCapturado);

        if (!folio.equals(pacienteBD.getFolio()) && pacienteRepository.existsByFolio(folio)) {
            throw new ObjConflictException("El folio ya existe");
        }

        pacienteBD.setFolio(folio);
        pacienteBD.setPersona(paciente.getPersona());
        pacienteBD.setFechaActualizacion(LocalDateTime.now());
        return pacienteRepository.save(pacienteBD);
    }

    // ── Variantes multi-institución (jerarquía) ──

    public List<Paciente> findAllByInstituciones(List<Long> ids) {
        return pacienteRepository.findAllByInstitucion_IdIn(ids);
    }

    public Page<Paciente> findAllPaginadoByInstituciones(List<Long> ids, Pageable pageable) {
        return pacienteRepository.findAllByInstitucion_IdIn(ids, pageable);
    }

    public Paciente getByUUID(String uuid, List<Long> idsInstituciones) {
        Paciente findPatient = pacienteRepository.findByUuidAndInstitucion_IdIn(uuid, idsInstituciones)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El participante no se encuentra activo");
        }
        return findPatient;
    }

    public Paciente getByFolio(String folio, List<Long> idsInstituciones) {
        Paciente findPatient = pacienteRepository.findByFolioAndInstitucion_IdIn(folio, idsInstituciones)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El participante no se encuentra activo");
        }
        return findPatient;
    }

    public Paciente getPatient(Long idPaciente, List<Long> idsInstituciones) {
        Paciente findPatient = pacienteRepository.findByIdAndInstitucion_IdIn(idPaciente, idsInstituciones)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el paciente"));
        if (!findPatient.getActivo()) {
            throw new ObjNotFoundException("El participante no se encuentra activo");
        }
        return findPatient;
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
