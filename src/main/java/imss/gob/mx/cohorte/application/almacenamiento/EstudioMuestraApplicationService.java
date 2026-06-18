package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.EstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ResultadoEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.TipoEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.EstudioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.HistorialCambioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.ParametroEstudioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.TipoEstudioMuestraService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class EstudioMuestraApplicationService {

    private static final String ROOT_GROUP_CODE = "ROOT";
    private static final int ROOT_ORDER = 0;

    private final EstudioMuestraService estudioService;
    private final MuestraService muestraService;
    private final MuestraRepository muestraRepository;
    private final TipoEstudioMuestraService tipoService;
    private final ParametroEstudioMuestraService parametroService;
    private final UserService userService;
    private final HistorialCambioMuestraService historialService;
    private final imss.gob.mx.cohorte.security.institucion.InstitucionContextService institucionContextService;

    // ─── Estudios por muestra ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EstudioMuestra> getByMuestra(Long idMuestra) {
        muestraService.getByIdConAcceso(idMuestra);
        return estudioService.getByMuestra(idMuestra);
    }

    @Transactional(readOnly = true)
    public EstudioMuestra getById(Long id) {
        return estudioService.getById(id);
    }

    @Transactional
    public EstudioMuestra create(Long idMuestra, EstudioMuestra estudio) {
        Muestra muestra = muestraService.getByIdComoTenedor(idMuestra);

        if (muestra.getEstadoMuestra() == EstadoMuestra.PRESTADA) {
            throw new ObjConflictException(
                    "No se puede registrar un estudio en una muestra en tránsito hacia otra institución.");
        }

        validarConsumo(muestra, estudio.getCantidadConsumida(), estudio.getUnidadConsumida());

        estudio.setMuestra(muestra);
        resolveRelaciones(estudio);
        EstudioMuestra saved = estudioService.create(estudio);

        // Descontar cantidad consumida del valor de la muestra
        Double valorAnterior = muestra.getValor();
        muestra.setValor(valorAnterior - estudio.getCantidadConsumida());
        muestraRepository.save(muestra);

        historialService.registrarEvento(muestra, estudio.getUsuarioRealiza(),
                TipoEventoMuestra.ESTUDIO_REALIZADO,
                String.valueOf(valorAnterior), String.valueOf(muestra.getValor()),
                "Estudio: " + estudio.getTipoEstudioMuestra().getNombre()
                        + " — consumo: " + estudio.getCantidadConsumida() + " " + estudio.getUnidadConsumida(),
                null);

        return saved;
    }

    @Transactional
    public EstudioMuestra update(Long id, EstudioMuestra datos) {
        EstudioMuestra existente = estudioService.getById(id);
        Muestra muestra = existente.getMuestra();

        Long idInst = institucionContextService.getIdInstitucionActual();
        if (!existente.getUsuarioRealiza().getInstitucion().getId().equals(idInst)) {
            throw new ObjConflictException(
                    "Solo la institución que creó este estudio puede editarlo.");
        }

        if (muestra.getEstadoMuestra() == EstadoMuestra.PRESTADA) {
            throw new ObjConflictException(
                    "No se puede editar un estudio de una muestra en tránsito hacia otra institución.");
        }

        Double consumoAnterior = existente.getCantidadConsumida() != null ? existente.getCantidadConsumida() : 0.0;
        Double consumoNuevo = datos.getCantidadConsumida() != null ? datos.getCantidadConsumida() : 0.0;

        // Recalcular: valor actual + consumo anterior = valor antes del estudio
        // Luego: valor antes del estudio - consumo nuevo = nuevo valor
        Double valorRecuperado = muestra.getValor() + consumoAnterior;
        if (consumoNuevo > valorRecuperado) {
            throw new ValidationException(
                    "La cantidad consumida (" + consumoNuevo + ") excede el valor disponible de la muestra ("
                            + valorRecuperado + " " + muestra.getUnidad() + ").");
        }

        datos.setMuestra(muestra);
        resolveRelaciones(datos);

        existente.setTipoEstudioMuestra(datos.getTipoEstudioMuestra());
        existente.setUsuarioRealiza(datos.getUsuarioRealiza());
        existente.setFechaEstudio(datos.getFechaEstudio());
        existente.setObservaciones(datos.getObservaciones());
        existente.setCantidadConsumida(datos.getCantidadConsumida());
        existente.setUnidadConsumida(datos.getUnidadConsumida());
        replaceResultados(existente, datos.getResultados());
        EstudioMuestra updated = estudioService.update(existente);

        // Actualizar valor de la muestra si cambió la cantidad consumida
        if (!Objects.equals(consumoAnterior, consumoNuevo)) {
            Double valorAnterior = muestra.getValor();
            muestra.setValor(valorRecuperado - consumoNuevo);
            muestraRepository.save(muestra);

            historialService.registrar(muestra, datos.getUsuarioRealiza(), "valor",
                    String.valueOf(valorAnterior), String.valueOf(muestra.getValor()),
                    "Edición de estudio: consumo ajustado de " + consumoAnterior + " a " + consumoNuevo);
        }

        return updated;
    }

    // ─── Historial ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<HistorialCambioMuestra> getHistorial(Long idMuestra) {
        muestraService.getByIdConAcceso(idMuestra);
        return historialService.getByMuestra(idMuestra);
    }

    // ─── Privados ─────────────────────────────────────────────────────────────

    private void validarConsumo(Muestra muestra, Double cantidadConsumida, String unidadConsumida) {
        if (cantidadConsumida == null || cantidadConsumida <= 0) {
            throw new ValidationException("La cantidad consumida debe ser mayor a 0.");
        }
        if (unidadConsumida == null || !unidadConsumida.equals(muestra.getUnidad())) {
            throw new ValidationException(
                    "La unidad consumida debe coincidir con la unidad de la muestra (" + muestra.getUnidad() + ").");
        }
        if (cantidadConsumida > muestra.getValor()) {
            throw new ValidationException(
                    "La cantidad consumida (" + cantidadConsumida + ") excede el valor actual de la muestra ("
                            + muestra.getValor() + " " + muestra.getUnidad() + ").");
        }
    }

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
        if (nuevos == null || nuevos.isEmpty()) return;

        // Indexar existentes por idParametro para hacer merge
        java.util.Map<Long, ResultadoEstudioMuestra> existentesPorParametro = new java.util.HashMap<>();
        for (ResultadoEstudioMuestra r : estudio.getResultados()) {
            if (r.getParametro() != null && r.getParametro().getId() != null) {
                existentesPorParametro.put(r.getParametro().getId(), r);
            }
        }

        for (ResultadoEstudioMuestra nuevo : nuevos) {
            Long idParam = nuevo.getParametro() != null ? nuevo.getParametro().getId() : null;
            ResultadoEstudioMuestra existente = idParam != null ? existentesPorParametro.get(idParam) : null;

            if (existente != null) {
                existente.setValorNumerico(nuevo.getValorNumerico());
                existente.setValorTexto(nuevo.getValorTexto());
                existente.setValorBooleano(nuevo.getValorBooleano());
                existente.setGrupoCodigo(nuevo.getGrupoCodigo());
                existente.setGrupoEtiqueta(nuevo.getGrupoEtiqueta());
                existente.setOrdenResultado(nuevo.getOrdenResultado());
                existente.setParametro(nuevo.getParametro());
            } else {
                nuevo.setId(null);
                nuevo.setEstudio(estudio);
                estudio.getResultados().add(nuevo);
            }
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
