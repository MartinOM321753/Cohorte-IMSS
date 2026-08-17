package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.institucion.PermisoAccesoPacientes;
import imss.gob.mx.cohorte.modules.institucion.PermisoAccesoPacientesRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
// La del proyecto, no la de jakarta: el GlobalExceptionHandler solo mapea esta.
// Con la de jakarta el usuario recibía «Error interno del servidor» en vez del motivo.
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class InstitucionJerarquiaService {

    private final InstitucionRepository institucionRepository;
    private final PermisoAccesoPacientesRepository permisoRepository;
    private final InstitucionRegistroService institucionRegistroService;
    private final InstitucionArbolService arbol;
    private final InstitucionContextService institucionContextService;

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

    /**
     * Instituciones que el usuario puede elegir en el selector de sedes.
     *
     * <p>Es deliberadamente más amplio que {@link #getInstitucionesVisibles}: suma
     * el subárbol administrativo completo, incluidas las hijas cuyos participantes
     * la institución decidió no ver. Una cosa es no querer ver su padrón y otra
     * dejar de administrarlas — si se cayeran del selector, ocultar una hija sería
     * irreversible porque desaparecería la pantalla desde la que se vuelve atrás.</p>
     */
    @Transactional(readOnly = true)
    public List<Long> getInstitucionesParaSelector(Long idInstitucionActual) {
        Set<Long> ids = new LinkedHashSet<>(arbol.subarbol(idInstitucionActual));
        ids.addAll(getInstitucionesVisibles(idInstitucionActual));
        return new ArrayList<>(ids);
    }

    /**
     * Abre el padrón de una institución a otra.
     *
     * <p>Antes solo una ancestra podía otorgar. Eso dejaba fuera el caso natural de
     * dos sedes hermanas que quieren colaborar y tenían que pedírselo al padre.
     * Ahora cualquier institución puede abrir <em>su propio</em> padrón a otra, y
     * una colaboración mutua son sencillamente dos permisos: cada una revoca el
     * suyo cuando quiera, sin discutir quién manda sobre el acuerdo.</p>
     *
     * <p>El permiso cubre solo a la sede otorgante, no a su subárbol, y no es
     * transitivo: si A abre a B y B abre a C, C no ve a A. Encadenarlos convertiría
     * dos acuerdos bilaterales en una red que nadie autorizó.</p>
     *
     * <p><b>Nota de seguridad.</b> Lo único que impedía otorgar a nombre de una
     * institución ajena era, por accidente, la comprobación de ancestría que había
     * aquí: el endpoint toma la otorgante de la URL y solo exige el permiso
     * INSTITUCIONES_EDITAR. Al relajar la regla ese freno desaparece, así que la
     * autorización pasa a ser explícita — hay que actuar por la propia institución
     * o por una de la que se es ancestra.</p>
     */
    @Transactional
    public PermisoAccesoPacientes otorgarPermiso(Long idInstitucionOtorga, Long idInstitucionRecibe) {
        Institucion otorga = institucionRepository.findById(idInstitucionOtorga)
                .orElseThrow(() -> new ObjNotFoundException("Institución otorgante no encontrada"));
        Institucion recibe = institucionRepository.findById(idInstitucionRecibe)
                .orElseThrow(() -> new ObjNotFoundException("Institución receptora no encontrada"));

        verificarPuedeDisponerDelPadron(idInstitucionOtorga);

        if (idInstitucionOtorga.equals(idInstitucionRecibe)) {
            throw new ValidationException("Una institución no necesita permiso sobre su propio padrón");
        }
        if (!Boolean.TRUE.equals(recibe.getActivo())) {
            throw new ValidationException("No se puede otorgar acceso a una institución inactiva");
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
        verificarPuedeDisponerDelPadron(idInstitucionOtorga);
        PermisoAccesoPacientes permiso = permisoRepository
                .findByInstitucionOtorga_IdAndInstitucionRecibe_Id(idInstitucionOtorga, idInstitucionRecibe)
                .orElseThrow(() -> new ObjNotFoundException("Permiso no encontrado"));
        permiso.setHabilitado(false);
        return permisoRepository.save(permiso);
    }

    /**
     * Quién puede decidir sobre el padrón de una institución: ella misma o una
     * ancestra suya. Sin esto, cualquiera con INSTITUCIONES_EDITAR podría regalar
     * —o cortar— el padrón de una sede con la que no tiene ninguna relación,
     * cambiando un número en la URL.
     */
    private void verificarPuedeDisponerDelPadron(Long idInstitucionOtorga) {
        Long idActual = institucionContextService.getIdInstitucionActual();
        if (idActual.equals(idInstitucionOtorga)) return;
        if (arbol.esAncestra(idActual, idInstitucionOtorga)) return;
        throw new AccessDeniedException(
                "Solo la propia institución o una superior puede decidir sobre su padrón");
    }

    @Transactional(readOnly = true)
    public List<PermisoAccesoPacientes> listarPermisosOtorgados(Long idInstitucionOtorga) {
        return permisoRepository.findAllByInstitucionOtorga_Id(idInstitucionOtorga);
    }

    @Transactional(readOnly = true)
    public List<PermisoAccesoPacientes> listarPermisosRecibidos(Long idInstitucionRecibe) {
        return permisoRepository.findAllByInstitucionRecibe_IdAndHabilitadoTrue(idInstitucionRecibe);
    }

}
