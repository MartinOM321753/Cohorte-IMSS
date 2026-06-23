package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionModulo;
import imss.gob.mx.cohorte.modules.institucion.InstitucionModuloRepository;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

/**
 * Administra los permisos de módulo por institución.
 *
 * <p>Regla de autorización: sólo puede otorgar/revocar módulos de una institución
 * (a) el encargado de la propia institución (auto-gestión), o bien
 * (b) el encargado de una institución ancestra directa o de nivel superior.
 * Si la institución destino aún no tiene encargado asignado, cualquier ADMINISTRADOR
 * puede configurarla (caso bootstrap).</p>
 */
@Service
@RequiredArgsConstructor
public class InstitucionModuloService {

    private final InstitucionModuloRepository institucionModuloRepository;
    private final InstitucionRepository institucionRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public List<InstitucionModulo> getByInstitucion(Long idInstitucion) {
        getInstitucion(idInstitucion);
        return institucionModuloRepository.findAllByInstitucion_Id(idInstitucion);
    }

    /** Devuelve únicamente los registros con habilitado=true — más eficiente que getByInstitucion + filtro en memoria. */
    @Transactional(readOnly = true)
    public List<InstitucionModulo> getHabilitadosByInstitucion(Long idInstitucion) {
        return institucionModuloRepository.findAllByInstitucion_IdAndHabilitadoTrue(idInstitucion);
    }

    @Transactional(readOnly = true)
    public boolean tieneAccesoHabilitado(Long idInstitucion, ModuloSistema modulo) {
        return institucionModuloRepository.existsByInstitucion_IdAndModuloAndHabilitadoTrue(idInstitucion, modulo);
    }

    /**
     * Otorga (o actualiza) el permiso de un módulo a una institución.
     *
     * @param idInstitucion   institución que recibe el permiso
     * @param modulo          módulo a habilitar/actualizar
     * @param habilitado      true para habilitar, false para revocar sin eliminar el registro (preserva auditoría)
     * @param idOtorgante     institución que realiza la acción — debe ser ancestro de la institución destino
     */
    @Transactional
    public InstitucionModulo otorgar(Long idInstitucion, ModuloSistema modulo, boolean habilitado, Long idOtorgante) {
        Institucion destino = getInstitucion(idInstitucion);
        Institucion otorgante = getInstitucion(idOtorgante);
        BeanUser usuarioActual = institucionContextService.getUsuarioActual();

        if (idInstitucion.equals(idOtorgante)) {
            // Auto-gestión: el encargado de una institución puede administrar sus propios módulos
            // (tanto instituciones raíz como instituciones hijas dentro de la jerarquía).
            // Si la institución aún no tiene encargado asignado se permite el acceso para
            // que el primer ADMINISTRADOR pueda configurarla (bootstrap).
            verificarEsEncargado(destino, usuarioActual);
        } else {
            // Gestión desde institución ancestra: el otorgante debe ser ancestro directo o de
            // nivel superior de la institución destino, y el usuario actual debe ser el encargado
            // de esa institución ancestra.
            validarEsAncestro(otorgante, destino);
            verificarEsEncargado(otorgante, usuarioActual);
        }

        InstitucionModulo registro = institucionModuloRepository
                .findByInstitucion_IdAndModulo(idInstitucion, modulo)
                .orElseGet(() -> {
                    InstitucionModulo nuevo = new InstitucionModulo();
                    nuevo.setInstitucion(destino);
                    nuevo.setModulo(modulo);
                    nuevo.setFechaOtorgamiento(Timestamp.from(Instant.now()));
                    return nuevo;
                });

        registro.setHabilitado(habilitado);
        registro.setOtorgadoPor(otorgante);
        if (registro.getId() == null) {
            registro.setFechaOtorgamiento(Timestamp.from(Instant.now()));
        }

        return institucionModuloRepository.save(registro);
    }

    @Transactional
    public InstitucionModulo revocar(Long idInstitucion, ModuloSistema modulo, Long idOtorgante) {
        return otorgar(idInstitucion, modulo, false, idOtorgante);
    }

    // ── Helpers privados ─────────────────────────────────────────────────────

    private Institucion getInstitucion(Long id) {
        return institucionRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución con id: " + id));
    }

    /** Recorre la cadena de instituciones padre de {@code destino} buscando a {@code posibleAncestro}. */
    private void validarEsAncestro(Institucion posibleAncestro, Institucion destino) {
        Institucion cursor = destino.getInstitucionPadre();
        while (cursor != null) {
            if (cursor.getId().equals(posibleAncestro.getId())) {
                return;
            }
            cursor = cursor.getInstitucionPadre();
        }
        throw new ObjConflictException(
                "La institución '" + posibleAncestro.getNombre() + "' no tiene autoridad sobre '"
                        + destino.getNombre() + "': sólo una institución ancestra en la jerarquía puede "
                        + "otorgar o revocar permisos de módulo.");
    }

    /**
     * Verifica que {@code usuarioActual} sea el encargado de {@code institucion}.
     * Si la institución aún no tiene encargado asignado se permite el acceso (caso bootstrap).
     */
    private void verificarEsEncargado(Institucion institucion, BeanUser usuarioActual) {
        if (institucion.getEncargado() == null) {
            return; // Bootstrap: sin encargado asignado todavía, cualquier ADMINISTRADOR puede configurarla
        }
        if (!institucion.getEncargado().getId().equals(usuarioActual.getId())) {
            throw new AccessDeniedException(
                    "Solo el encargado de '" + institucion.getNombre() +
                    "' puede gestionar los módulos de esta institución.");
        }
    }
}
