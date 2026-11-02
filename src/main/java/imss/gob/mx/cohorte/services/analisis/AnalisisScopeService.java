package imss.gob.mx.cohorte.services.analisis;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.institucion.InstitucionArbolService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Resuelve qué instituciones puede agregar el usuario en el módulo de análisis.
 *
 * <p>Es la pieza que rompe —de forma controlada— el aislamiento por institución
 * que rige al resto del sistema (ver {@link InstitucionContextService}). En vez
 * de "solo mi institución", el alcance del análisis es:</p>
 *
 * <ul>
 *   <li><b>ROOT</b> → todas las instituciones (análisis global de la cohorte).</li>
 *   <li><b>Cualquier otro</b> → su institución y todas sus descendientes
 *       (subárbol). Una sede hoja solo se ve a sí misma; nunca a sus hermanas.</li>
 * </ul>
 *
 * <p>Toda consulta de agregación debe filtrar por el conjunto que este servicio
 * devuelve. Un {@code institucionId} que llegue del cliente solo vale si está
 * dentro del alcance resuelto aquí ({@link #validarDentroDeAlcance}): así un
 * filtro manipulado no puede sacar datos de otro subárbol.</p>
 */
@Service
@RequiredArgsConstructor
public class AnalisisScopeService {

    private final InstitucionContextService institucionContextService;
    private final InstitucionArbolService   institucionArbolService;
    private final InstitucionRepository      institucionRepository;

    /** ¿El usuario actual analiza toda la cohorte (ROOT) o solo su subárbol? */
    @Transactional(readOnly = true)
    public boolean esGlobal() {
        return institucionContextService.esRootActual();
    }

    /**
     * Ids de las instituciones que el usuario puede agregar. Para ROOT, todas las
     * activas; para el resto, su subárbol.
     */
    @Transactional(readOnly = true)
    public Set<Long> institucionesEnAlcance() {
        if (esGlobal()) {
            Set<Long> todas = new LinkedHashSet<>();
            for (Institucion i : institucionRepository.findAll()) {
                todas.add(i.getId());
            }
            return todas;
        }
        return institucionArbolService.subarbol(institucionContextService.getIdInstitucionActual());
    }

    /**
     * Intersecta un filtro de instituciones pedido por el cliente con el alcance
     * real del usuario. Si el filtro viene vacío o nulo, devuelve todo el alcance.
     * Nunca devuelve una institución fuera del alcance, aunque el cliente la pida.
     */
    @Transactional(readOnly = true)
    public Set<Long> resolverFiltroInstituciones(Set<Long> solicitadas) {
        Set<Long> alcance = institucionesEnAlcance();
        if (solicitadas == null || solicitadas.isEmpty()) {
            return alcance;
        }
        Set<Long> interseccion = new LinkedHashSet<>(solicitadas);
        interseccion.retainAll(alcance);
        return interseccion;
    }

    /**
     * Garantiza que una institución concreta esté dentro del alcance del usuario.
     *
     * @throws org.springframework.security.access.AccessDeniedException si no lo está
     */
    @Transactional(readOnly = true)
    public void validarDentroDeAlcance(Long idInstitucion) {
        if (idInstitucion == null || !institucionesEnAlcance().contains(idInstitucion)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "La institución solicitada está fuera del alcance de análisis del usuario");
        }
    }
}
