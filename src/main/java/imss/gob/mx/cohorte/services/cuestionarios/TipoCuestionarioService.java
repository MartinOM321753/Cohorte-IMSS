package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.CuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** El catálogo de instrumentos y sus bloques. */
@Service
@AllArgsConstructor
public class TipoCuestionarioService {

    private final TipoCuestionarioRepository tipoRepository;
    private final SeccionCuestionarioRepository seccionRepository;
    private final VariableCuestionarioRepository variableRepository;
    private final CuestionarioRepository cuestionarioRepository;
    private final InstitucionContextService contexto;

    // ── Tipos ────────────────────────────────────────────────────────────────

    public List<TipoCuestionario> todos() {
        return tipoRepository.findAllByInstitucion_IdOrderByClaveAsc(contexto.getIdInstitucionActual());
    }

    public List<TipoCuestionario> activos() {
        return tipoRepository.findAllByActivoAndInstitucion_IdOrderByClaveAsc(
                true, contexto.getIdInstitucionActual());
    }

    public TipoCuestionario uno(Long id) {
        return tipoRepository.findByIdAndInstitucion_Id(id, contexto.getIdInstitucionActual())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el cuestionario solicitado"));
    }

    /**
     * El instrumento al que pertenece una etiqueta.
     *
     * <p>Si la clave está en más de una versión activa, no elige: eso es una decisión
     * del usuario y resolverla por nuestra cuenta —la más reciente, la primera—
     * cargaría respuestas contra un catálogo que no es el del papel, y los códigos
     * entre versiones no significan lo mismo.</p>
     */
    public TipoCuestionario porClave(String clave) {
        List<TipoCuestionario> candidatos = tipoRepository
                .findAllByClaveIgnoreCaseAndActivoAndInstitucion_Id(
                        clave, true, contexto.getIdInstitucionActual());

        if (candidatos.isEmpty()) {
            throw new ObjNotFoundException("Ningun cuestionario activo tiene la clave " + clave
                    + ". Revisa el catalogo o la etiqueta del archivo.");
        }
        if (candidatos.size() > 1) {
            throw new ObjConflictException("La clave " + clave + " existe en "
                    + candidatos.size() + " versiones activas. Indica cual se va a usar.");
        }
        return candidatos.get(0);
    }

    @Transactional
    public TipoCuestionario crear(TipoCuestionario tipo) {
        Long idInstitucion = contexto.getIdInstitucionActual();
        if (tipoRepository.existsByClaveIgnoreCaseAndVersionAndInstitucion_Id(
                tipo.getClave(), tipo.getVersion(), idInstitucion)) {
            throw new ObjConflictException("Ya existe un cuestionario con la clave "
                    + tipo.getClave() + " en la version " + tipo.getVersion() + ".");
        }
        tipo.setInstitucion(contexto.getInstitucionActual());
        return tipoRepository.save(tipo);
    }

    @Transactional
    public TipoCuestionario actualizar(Long id, TipoCuestionario datos) {
        TipoCuestionario tipo = uno(id);

        // La clave y la version identifican al instrumento frente a los archivos y
        // frente al catalogo de los investigadores. Cambiarlas en un instrumento que
        // ya tiene cuestionarios contestados reinterpretaria esos datos, asi que se
        // bloquea en cuanto hay uno: para una revision del papel se crea otra version.
        boolean cambiaIdentidad = !tipo.getClave().equalsIgnoreCase(datos.getClave())
                || !tipo.getVersion().equals(datos.getVersion());
        if (cambiaIdentidad && cuestionarioRepository.countByTipoCuestionario_Id(id) > 0) {
            throw new ObjConflictException(
                    "No se puede cambiar la clave ni la version: ya hay cuestionarios contestados "
                            + "con este instrumento. Crea una version nueva.");
        }

        tipo.setClave(datos.getClave());
        tipo.setVersion(datos.getVersion());
        tipo.setNombre(datos.getNombre());
        tipo.setDescripcion(datos.getDescripcion());
        tipo.setColumnaEtiqueta(datos.getColumnaEtiqueta());
        tipo.setDirigidoA(datos.getDirigidoA());
        tipo.setEdadMinima(datos.getEdadMinima());
        tipo.setEdadMaxima(datos.getEdadMaxima());
        tipo.setPermiteAutollenado(datos.getPermiteAutollenado());
        if (datos.getActivo() != null) tipo.setActivo(datos.getActivo());
        return tipoRepository.save(tipo);
    }

    /**
     * Desactiva el instrumento. No lo borra: un cuestionario contestado sin su
     * catálogo es un montón de dígitos sin significado.
     */
    @Transactional
    public void desactivar(Long id) {
        TipoCuestionario tipo = uno(id);
        tipo.setActivo(false);
        tipoRepository.save(tipo);
    }

    /**
     * Borra el instrumento con todo su catálogo. Solo si nadie lo ha contestado.
     */
    @Transactional
    public void eliminar(Long id) {
        TipoCuestionario tipo = uno(id);
        long contestados = cuestionarioRepository.countByTipoCuestionario_Id(id);
        if (contestados > 0) {
            throw new ObjConflictException("No se puede eliminar: hay " + contestados
                    + " cuestionario(s) contestado(s) con este instrumento. Desactivalo en su lugar.");
        }
        variableRepository.deleteAllByTipoCuestionario_Id(id);
        seccionRepository.deleteAllByTipoCuestionario_Id(id);
        tipoRepository.delete(tipo);
    }

    // ── Secciones ────────────────────────────────────────────────────────────

    public List<SeccionCuestionario> secciones(Long idTipo) {
        uno(idTipo);   // comprueba que el instrumento sea de la institucion
        return seccionRepository.findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(idTipo);
    }

    public SeccionCuestionario seccion(Long idSeccion) {
        SeccionCuestionario seccion = seccionRepository.findById(idSeccion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el bloque solicitado"));
        contexto.verificarPertenece(seccion.getTipoCuestionario().getInstitucion());
        return seccion;
    }

    @Transactional
    public SeccionCuestionario crearSeccion(Long idTipo, SeccionCuestionario seccion) {
        TipoCuestionario tipo = uno(idTipo);
        seccionRepository.findByTipoCuestionario_IdAndNombreIgnoreCase(idTipo, seccion.getNombre())
                .ifPresent(s -> {
                    throw new ObjConflictException("Ya existe un bloque llamado \""
                            + seccion.getNombre() + "\" en este cuestionario.");
                });
        seccion.setTipoCuestionario(tipo);
        if (seccion.getOrden() == null || seccion.getOrden() == 0) {
            seccion.setOrden(seccionRepository
                    .findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(idTipo).size());
        }
        return seccionRepository.save(seccion);
    }

    @Transactional
    public SeccionCuestionario actualizarSeccion(Long idSeccion, SeccionCuestionario datos) {
        SeccionCuestionario seccion = seccion(idSeccion);
        seccion.setNombre(datos.getNombre());
        seccion.setTitulo(datos.getTitulo());
        seccion.setDescripcion(datos.getDescripcion());
        seccion.setPagina(datos.getPagina());
        if (datos.getPresentacion() != null) seccion.setPresentacion(datos.getPresentacion());
        if (datos.getOrden() != null) seccion.setOrden(datos.getOrden());
        return seccionRepository.save(seccion);
    }

    /**
     * Reordena los bloques en una sola pasada.
     *
     * <p>De una sola vez y no uno por uno porque el orden es único dentro del
     * instrumento: moviendo de a uno, cualquier paso intermedio deja dos bloques en la
     * misma posición, y si la petición se corta a la mitad el usuario se queda con un
     * orden que no pidió.</p>
     */
    @Transactional
    public void reordenarSecciones(Long idTipo, List<Long> idsEnOrden) {
        List<SeccionCuestionario> secciones = secciones(idTipo);
        if (idsEnOrden.size() != secciones.size()) {
            throw new ObjConflictException("La lista trae " + idsEnOrden.size()
                    + " bloques y el cuestionario tiene " + secciones.size()
                    + ". Vuelve a cargar la pantalla antes de reordenar.");
        }
        for (int i = 0; i < idsEnOrden.size(); i++) {
            Long id = idsEnOrden.get(i);
            SeccionCuestionario s = secciones.stream()
                    .filter(x -> x.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ObjConflictException(
                            "El bloque " + id + " no pertenece a este cuestionario."));
            s.setOrden(i);
        }
        seccionRepository.saveAll(secciones);
    }

    @Transactional
    public void eliminarSeccion(Long idSeccion) {
        SeccionCuestionario seccion = seccion(idSeccion);
        long variables = variableRepository
                .findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(
                        seccion.getTipoCuestionario().getId())
                .stream()
                .filter(v -> v.getSeccion() != null && v.getSeccion().getId().equals(idSeccion))
                .count();
        if (variables > 0) {
            throw new ObjConflictException("El bloque tiene " + variables
                    + " pregunta(s). Muevelas a otro bloque antes de eliminarlo.");
        }
        seccionRepository.delete(seccion);
    }
}
