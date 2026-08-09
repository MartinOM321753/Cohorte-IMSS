package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.institucion.InstitucionJerarquiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Resuelve el participante sobre el que se va a trabajar.
 *
 * <p>Antes cada modulo clinico hacia
 * {@code pacienteService.getByUUID(uuid, getIdInstitucionActual())}, un filtro
 * duro contra la institucion propia. Con el acceso entre sedes esa cuenta ya no
 * alcanza: una institucion autorizada atiende a los participantes de otra como si
 * fueran suyos, asi que la puerta es el conjunto de instituciones visibles.</p>
 *
 * <p>La regla vive aqui y en un solo lugar. Repetirla en cada modulo era como
 * estaba antes y es como se desincroniza: basta que uno se olvide de cambiarla
 * para tener un modulo que deja pasar de mas o de menos.</p>
 *
 * <p>Ojo con lo que NO hace: no autoriza a editar registros ajenos. Cada modulo
 * sigue llamando a {@code verificarPertenece} sobre el registro al actualizar o
 * borrar, de modo que un estudio lo modifica solo la sede que lo hizo.</p>
 */
@Service
@RequiredArgsConstructor
public class ParticipanteAccesoService {

    private final PacienteService pacienteService;
    private final imss.gob.mx.cohorte.modules.paciente.PacienteRepository pacienteRepository;
    private final InstitucionJerarquiaService institucionJerarquiaService;
    private final InstitucionContextService institucionContextService;

    /** Instituciones cuyos participantes puede atender el usuario actual. */
    @Transactional(readOnly = true)
    public List<Long> institucionesAlcanzables() {
        return institucionJerarquiaService.getInstitucionesVisibles(
                institucionContextService.getIdInstitucionActual());
    }

    /**
     * Participante sobre el que puede operar el usuario actual, sea de su
     * institucion o de otra a la que alcance.
     */
    @Transactional(readOnly = true)
    public Paciente resolver(String uuidPaciente) {
        return pacienteService.getByUUID(uuidPaciente, institucionesAlcanzables());
    }

    /** Igual que {@link #resolver} pero por id numerico. */
    @Transactional(readOnly = true)
    public Paciente resolverPorId(Long idPaciente) {
        return pacienteService.getPatient(idPaciente, institucionesAlcanzables());
    }

    /**
     * Verifica que el participante indicado este al alcance del usuario actual.
     * Para modulos que ya tienen la entidad y solo necesitan la comprobacion.
     */
    @Transactional(readOnly = true)
    public void verificarAlcance(Paciente paciente) {
        if (paciente == null || paciente.getInstitucion() == null
                || !institucionesAlcanzables().contains(paciente.getInstitucion().getId())) {
            throw new AccessDeniedException("El participante pertenece a otra institución");
        }
    }

    /**
     * Puerta de LECTURA de un registro clinico. Basta con que se cumpla una de dos:
     *
     * <ol>
     *   <li>el registro lo hizo una sede a mi alcance — asi una institucion conserva
     *       lo que ella misma registro aunque despues le revoquen el acceso al
     *       participante: ese estudio es suyo y no deja de serlo;</li>
     *   <li>alcanzo al participante — asi la institucion propietaria ve su historial
     *       completo, incluido lo que le hizo otra sede.</li>
     * </ol>
     *
     * <p>Es una union, no una interseccion. Exigir las dos condiciones dejaba a la
     * propietaria sin ver lo que otra sede le hizo a su propio participante.</p>
     *
     * <p>No autoriza a modificar: actualizar y borrar siguen exigiendo que el registro
     * sea de la institucion propia.</p>
     */
    @Transactional(readOnly = true)
    public void verificarLecturaRegistro(Institucion institucionRegistro, Paciente paciente) {
        if (!puedeLeerRegistro(institucionRegistro, paciente)) {
            throw new AccessDeniedException("El registro pertenece a otra institución");
        }
    }

    /**
     * La misma regla de {@link #verificarLecturaRegistro} pero como predicado.
     *
     * <p>Existe porque un documento puede colgar de más de una entidad y hay que
     * poder preguntar por varias antes de negar: con una versión que solo lanza,
     * la primera que fallara cortaría la evaluación de las demás.</p>
     */
    @Transactional(readOnly = true)
    public boolean puedeLeerRegistro(Institucion institucionRegistro, Paciente paciente) {
        List<Long> alcanzables = institucionesAlcanzables();

        if (institucionRegistro != null && alcanzables.contains(institucionRegistro.getId())) return true;
        return paciente != null && paciente.getInstitucion() != null
                && alcanzables.contains(paciente.getInstitucion().getId());
    }

    /**
     * Resultado de abrir un participante para LEER su historial.
     *
     * <p>{@code soloPropio} distingue los dos modos: si la institucion alcanza al
     * participante ve su historial completo; si no lo alcanza pero conserva
     * registros suyos ve unicamente los que ella hizo. Es la misma regla de union
     * que se aplica registro por registro, llevada al nivel de lista.</p>
     */
    public record AccesoLectura(Paciente paciente, boolean soloPropio) {}

    /**
     * Abre un participante para consulta. Lanza si no se alcanza y ademas no hay
     * nada propio que mostrar.
     */
    @Transactional(readOnly = true)
    public AccesoLectura resolverParaLectura(String uuidPaciente) {
        Paciente alcanzable = pacienteService
                .buscarPorUUID(uuidPaciente, institucionesAlcanzables())
                .orElse(null);
        if (alcanzable != null) {
            return new AccesoLectura(alcanzable, false);
        }

        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        if (pacienteRepository.tieneRegistrosDeInstitucion(uuidPaciente, idInstitucion)) {
            Paciente p = pacienteRepository.findByUuid(uuidPaciente)
                    .orElseThrow(() -> new AccessDeniedException("No se encontró el participante"));
            return new AccesoLectura(p, true);
        }

        throw new AccessDeniedException(
                "No gestionas a este participante y no conservas registros suyos");
    }

    /** ¿Alcanzo a este participante? Sin lanzar, para decidir qué mostrar en pantalla. */
    @Transactional(readOnly = true)
    public boolean alcanza(Paciente paciente) {
        return paciente != null && paciente.getInstitucion() != null
                && institucionesAlcanzables().contains(paciente.getInstitucion().getId());
    }
}
