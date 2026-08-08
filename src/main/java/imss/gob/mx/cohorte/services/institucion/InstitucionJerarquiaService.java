package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.institucion.PermisoAccesoPacientes;
import imss.gob.mx.cohorte.modules.institucion.PermisoAccesoPacientesRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class InstitucionJerarquiaService {

    private final InstitucionRepository institucionRepository;
    private final PermisoAccesoPacientesRepository permisoRepository;
    private final InstitucionRegistroService institucionRegistroService;

    /**
     * Instituciones cuyos participantes alcanza {@code idInstitucionActual}: los ve
     * en sus listados y —desde que el acceso tambien habilita atender— puede
     * registrarles estudios, muestras, citas y demas.
     *
     * <p>Parte del conjunto para registro, de modo que todo aquello a nombre de lo
     * que se puede dar de alta tambien se puede ver: registrar un participante para
     * una hermana y despues no poder abrirlo seria incoherente. A eso se suman los
     * ancestros que otorgaron acceso explicito a su padron.</p>
     */
    public List<Long> getInstitucionesVisibles(Long idInstitucionActual) {
        // Propia + descendientes + el grupo del padre cuando hay autorizacion de registro.
        Set<Long> ids = new LinkedHashSet<>(
                institucionRegistroService.getInstitucionesParaRegistro(idInstitucionActual));

        // Ancestros que abrieron su padron explicitamente.
        List<PermisoAccesoPacientes> permisos =
                permisoRepository.findAllByInstitucionRecibe_IdAndHabilitadoTrue(idInstitucionActual);
        for (PermisoAccesoPacientes permiso : permisos) {
            ids.add(permiso.getInstitucionOtorga().getId());
        }

        return new ArrayList<>(ids);
    }

    @Transactional
    public PermisoAccesoPacientes otorgarPermiso(Long idInstitucionOtorga, Long idInstitucionRecibe) {
        Institucion otorga = institucionRepository.findById(idInstitucionOtorga)
                .orElseThrow(() -> new ObjNotFoundException("Institución otorgante no encontrada"));
        Institucion recibe = institucionRepository.findById(idInstitucionRecibe)
                .orElseThrow(() -> new ObjNotFoundException("Institución receptora no encontrada"));

        if (!esAncestra(otorga, recibe)) {
            throw new ValidationException("Solo una institución padre puede otorgar acceso a pacientes a una hija");
        }

        Optional<PermisoAccesoPacientes> existente =
                permisoRepository.findByInstitucionOtorga_IdAndInstitucionRecibe_Id(idInstitucionOtorga, idInstitucionRecibe);

        if (existente.isPresent()) {
            PermisoAccesoPacientes permiso = existente.get();
            permiso.setHabilitado(true);
            return permisoRepository.save(permiso);
        }

        PermisoAccesoPacientes permiso = new PermisoAccesoPacientes();
        permiso.setInstitucionOtorga(otorga);
        permiso.setInstitucionRecibe(recibe);
        permiso.setHabilitado(true);
        return permisoRepository.save(permiso);
    }

    @Transactional
    public PermisoAccesoPacientes revocarPermiso(Long idInstitucionOtorga, Long idInstitucionRecibe) {
        PermisoAccesoPacientes permiso = permisoRepository
                .findByInstitucionOtorga_IdAndInstitucionRecibe_Id(idInstitucionOtorga, idInstitucionRecibe)
                .orElseThrow(() -> new ObjNotFoundException("Permiso no encontrado"));
        permiso.setHabilitado(false);
        return permisoRepository.save(permiso);
    }

    @Transactional(readOnly = true)
    public List<PermisoAccesoPacientes> listarPermisosOtorgados(Long idInstitucionOtorga) {
        return permisoRepository.findAllByInstitucionOtorga_Id(idInstitucionOtorga);
    }

    @Transactional(readOnly = true)
    public List<PermisoAccesoPacientes> listarPermisosRecibidos(Long idInstitucionRecibe) {
        return permisoRepository.findAllByInstitucionRecibe_IdAndHabilitadoTrue(idInstitucionRecibe);
    }

    private boolean esAncestra(Institucion posibleAncestra, Institucion objetivo) {
        Institucion actual = objetivo;
        while (actual.getInstitucionPadre() != null) {
            actual = actual.getInstitucionPadre();
            if (actual.getId().equals(posibleAncestra.getId())) {
                return true;
            }
        }
        return false;
    }
}
