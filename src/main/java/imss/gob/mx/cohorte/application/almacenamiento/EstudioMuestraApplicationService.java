package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.EstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ResultadoEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.TipoEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestra;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.EstudioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.HistorialCambioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.ParametroEstudioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.TipoEstudioMuestraService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@AllArgsConstructor
public class EstudioMuestraApplicationService {

    private static final String ROOT_GROUP_CODE = "ROOT";
    private static final int ROOT_ORDER = 0;

    private final EstudioMuestraService estudioService;
    private final MuestraService muestraService;
    private final TipoEstudioMuestraService tipoService;
    private final ParametroEstudioMuestraService parametroService;
    private final UserService userService;
    private final HistorialCambioMuestraService historialService;

    // ─── Estudios por muestra ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EstudioMuestra> getByMuestra(Long idMuestra) {
        muestraService.getById(idMuestra); // valida que existe
        return estudioService.getByMuestra(idMuestra);
    }

    @Transactional(readOnly = true)
    public EstudioMuestra getById(Long id) {
        return estudioService.getById(id);
    }

    @Transactional
    public EstudioMuestra create(Long idMuestra, EstudioMuestra estudio) {
        Muestra muestra = muestraService.getById(idMuestra);
        estudio.setMuestra(muestra);
        resolveRelaciones(estudio);
        return estudioService.create(estudio);
    }

    @Transactional
    public EstudioMuestra update(Long id, EstudioMuestra datos) {
        EstudioMuestra existente = estudioService.getById(id);
        // No se cambia la muestra al actualizar
        datos.setMuestra(existente.getMuestra());
        resolveRelaciones(datos);

        existente.setTipoEstudioMuestra(datos.getTipoEstudioMuestra());
        existente.setUsuarioRealiza(datos.getUsuarioRealiza());
        existente.setFechaEstudio(datos.getFechaEstudio());
        existente.setObservaciones(datos.getObservaciones());
        existente.setCantidadConsumida(datos.getCantidadConsumida());
        existente.setUnidadConsumida(datos.getUnidadConsumida());
        replaceResultados(existente, datos.getResultados());
        return estudioService.update(existente);
    }

    @Transactional
    public void delete(Long id) {
        estudioService.delete(id);
    }

    // ─── Historial ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<HistorialCambioMuestra> getHistorial(Long idMuestra) {
        muestraService.getById(idMuestra); // valida que existe
        return historialService.getByMuestra(idMuestra);
    }

    // ─── Privados ─────────────────────────────────────────────────────────────

    private void resolveRelaciones(EstudioMuestra estudio) {
        if (estudio.getUsuarioRealiza() == null || estudio.getUsuarioRealiza().getUUID() == null) {
            throw new ObjNotFoundException("Falta información del usuario que realiza el estudio");
        }
        if (estudio.getTipoEstudioMuestra() == null || estudio.getTipoEstudioMuestra().getId() == null) {
            throw new ObjNotFoundException("Falta información del tipo de estudio de muestra");
        }

        BeanUser usuario = userService.getByUUID(estudio.getUsuarioRealiza().getUUID());
        TipoEstudioMuestra tipo = tipoService.getByIdActivo(estudio.getTipoEstudioMuestra().getId());

        resolveResultados(estudio, tipo);

        estudio.setUsuarioRealiza(usuario);
        estudio.setTipoEstudioMuestra(tipo);
    }

    private void resolveResultados(EstudioMuestra estudio, TipoEstudioMuestra tipo) {
        if (estudio.getResultados() == null || estudio.getResultados().isEmpty()) {
            estudio.setResultados(new ArrayList<>());
            return;
        }

        Set<String> claves = new HashSet<>();
        for (ResultadoEstudioMuestra resultado : estudio.getResultados()) {
            if (resultado.getParametro() == null || resultado.getParametro().getId() == null) {
                throw new ObjNotFoundException("Falta información del parámetro en un resultado");
            }
            if (resultado.getValorNumerico() == null
                    && resultado.getValorTexto() == null
                    && resultado.getValorBooleano() == null) {
                throw new ObjConflictException("Cada resultado debe incluir al menos un valor");
            }

            ParametroEstudioMuestra parametro = parametroService.getById(resultado.getParametro().getId());
            if (!Objects.equals(parametro.getTipoEstudioMuestra().getId(), tipo.getId())) {
                throw new ObjConflictException(
                        "El parámetro '" + parametro.getNombre() + "' no pertenece al tipo de estudio seleccionado");
            }

            normalizeResultado(resultado);
            String clave = buildKey(parametro.getId(), resultado.getGrupoCodigo(), resultado.getOrdenResultado());
            if (!claves.add(clave)) {
                throw new ObjConflictException("Hay resultados duplicados para el mismo parámetro, grupo y orden");
            }

            resultado.setParametro(parametro);
            resultado.setEstudio(estudio);
        }
    }

    private void replaceResultados(EstudioMuestra estudio, List<ResultadoEstudioMuestra> nuevos) {
        if (estudio.getResultados() == null) {
            estudio.setResultados(new ArrayList<>());
        }
        estudio.getResultados().clear();
        if (nuevos == null) return;
        for (ResultadoEstudioMuestra r : nuevos) {
            r.setId(null);
            r.setEstudio(estudio);
            estudio.getResultados().add(r);
        }
    }

    private void normalizeResultado(ResultadoEstudioMuestra resultado) {
        if (resultado.getGrupoCodigo() == null || resultado.getGrupoCodigo().isBlank()) {
            resultado.setGrupoCodigo(ROOT_GROUP_CODE);
        } else {
            resultado.setGrupoCodigo(resultado.getGrupoCodigo().trim());
        }
        if (resultado.getGrupoEtiqueta() != null && !resultado.getGrupoEtiqueta().isBlank()) {
            resultado.setGrupoEtiqueta(resultado.getGrupoEtiqueta().trim());
        } else if (ROOT_GROUP_CODE.equals(resultado.getGrupoCodigo())) {
            resultado.setGrupoEtiqueta(null);
        }
        if (resultado.getOrdenResultado() == null) {
            resultado.setOrdenResultado(ROOT_ORDER);
        }
    }

    private String buildKey(Long parametroId, String grupoCodigo, Integer orden) {
        return parametroId + "|" + grupoCodigo + "|" + orden;
    }
}
