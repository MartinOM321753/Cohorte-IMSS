package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.adjuntos.EstudioAdjunto;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.estudios.EstudioService;
import imss.gob.mx.cohorte.services.estudios.ParametroEstudioService;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@AllArgsConstructor
public class EstudiosApplicationService {

    private static final String ROOT_GROUP_CODE = "ROOT";
    private static final int ROOT_ORDER = 0;

    private final EstudioService estudioService;
    private final TipoService tipoEstudioService;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final ParametroEstudioService parametroService;

    @Transactional(readOnly = true)
    public List<EstudioMedico> getAllEstudios() {
        return estudioService.getAll();
    }

    @Transactional(readOnly = true)
    public EstudioMedico getEstudio(Long id) {
        return estudioService.getOne(id);
    }

    @Transactional
    public EstudioMedico createEstudio(EstudioMedico estudioMedico) {
        resolveRelaciones(estudioMedico);
        estudioMedico.setFechaRegistro(LocalDateTime.now());
        return estudioService.create(estudioMedico);
    }

    @Transactional
    public EstudioMedico updateEstudio(Long id, EstudioMedico estudioMedico) {
        resolveRelaciones(estudioMedico);

        EstudioMedico existente = estudioService.getOne(id);
        existente.setPaciente(estudioMedico.getPaciente());
        existente.setUsuarioRealiza(estudioMedico.getUsuarioRealiza());
        existente.setTipoEstudio(estudioMedico.getTipoEstudio());
        existente.setFechaEstudio(estudioMedico.getFechaEstudio());
        existente.setObservaciones(estudioMedico.getObservaciones());

        replaceResultados(existente, estudioMedico.getResultadoEstudio());
        replaceAdjuntos(existente, estudioMedico.getAdjuntos());

        return estudioService.update(existente);
    }

    private void resolveRelaciones(EstudioMedico estudioMedico) {
        if (estudioMedico.getPaciente() == null || estudioMedico.getPaciente().getUuid() == null) {
            throw new ObjNotFoundException("Falta informacion de paciente");
        }
        if (estudioMedico.getUsuarioRealiza() == null || estudioMedico.getUsuarioRealiza().getUUID() == null) {
            throw new ObjNotFoundException("Falta informacion de usuario");
        }
        if (estudioMedico.getTipoEstudio() == null || estudioMedico.getTipoEstudio().getId() == null) {
            throw new ObjNotFoundException("Falta informacion de tipo de estudio");
        }

        Paciente paciente = pacienteService.getByUUID(estudioMedico.getPaciente().getUuid());
        BeanUser usuario = userService.getByUUID(estudioMedico.getUsuarioRealiza().getUUID());
        TipoEstudio tipoEstudio = tipoEstudioService.getOne(estudioMedico.getTipoEstudio().getId());

        resolveResultados(estudioMedico, tipoEstudio);
        resolveAdjuntos(estudioMedico);

        estudioMedico.setPaciente(paciente);
        estudioMedico.setUsuarioRealiza(usuario);
        estudioMedico.setTipoEstudio(tipoEstudio);
    }

    private void resolveResultados(EstudioMedico estudioMedico, TipoEstudio tipoEstudio) {
        if (estudioMedico.getResultadoEstudio() == null || estudioMedico.getResultadoEstudio().isEmpty()) {
            estudioMedico.setResultadoEstudio(new ArrayList<>());
            return;
        }

        Set<String> claves = new HashSet<>();
        for (ResultadoEstudio resultado : estudioMedico.getResultadoEstudio()) {
            if (resultado.getParametro() == null || resultado.getParametro().getId() == null) {
                throw new ObjNotFoundException("Falta informacion del parametro en un resultado");
            }
            if (resultado.getValorNumerico() == null
                    && resultado.getValorTexto() == null
                    && resultado.getValorBooleano() == null) {
                throw new ObjConflictException("Cada resultado debe incluir al menos un valor");
            }

            ParametroEstudio parametro = parametroService.getOne(resultado.getParametro().getId());
            if (parametro.getTipoEstudio() == null) {
                throw new ObjNotFoundException("El parametro no tiene tipo de estudio asociado");
            }
            if (!Objects.equals(parametro.getTipoEstudio().getId(), tipoEstudio.getId())) {
                throw new ObjConflictException("El tipo de estudio del parametro no coincide con el del estudio");
            }

            normalizeResultado(resultado);
            String clave = buildResultadoKey(parametro.getId(), resultado.getGrupoCodigo(), resultado.getOrdenResultado());
            if (!claves.add(clave)) {
                throw new ObjConflictException("Hay resultados duplicados para el mismo parametro, grupo y orden");
            }

            resultado.setParametro(parametro);
            resultado.setEstudio(estudioMedico);
        }
    }

    private void resolveAdjuntos(EstudioMedico estudioMedico) {
        if (estudioMedico.getAdjuntos() == null || estudioMedico.getAdjuntos().isEmpty()) {
            estudioMedico.setAdjuntos(new ArrayList<>());
            return;
        }

        Set<Integer> ordenes = new HashSet<>();
        for (EstudioAdjunto adjunto : estudioMedico.getAdjuntos()) {
            if (!ordenes.add(adjunto.getOrden())) {
                throw new ObjConflictException("No se permiten adjuntos con el mismo orden");
            }
            adjunto.setTipo(adjunto.getTipo().trim());
            adjunto.setNombreOriginal(adjunto.getNombreOriginal().trim());
            adjunto.setMimeType(adjunto.getMimeType().trim());
            adjunto.setRutaUrl(adjunto.getRutaUrl().trim());
            adjunto.setDescripcion(adjunto.getDescripcion() == null ? null : adjunto.getDescripcion().trim());
            adjunto.setEstudio(estudioMedico);
        }
    }

    private void replaceResultados(EstudioMedico estudio, List<ResultadoEstudio> nuevosResultados) {
        if (estudio.getResultadoEstudio() == null) {
            estudio.setResultadoEstudio(new ArrayList<>());
        }
        estudio.getResultadoEstudio().clear();
        if (nuevosResultados == null) {
            return;
        }
        for (ResultadoEstudio resultado : nuevosResultados) {
            resultado.setId(null);
            resultado.setEstudio(estudio);
            estudio.getResultadoEstudio().add(resultado);
        }
    }

    private void replaceAdjuntos(EstudioMedico estudio, List<EstudioAdjunto> nuevosAdjuntos) {
        if (estudio.getAdjuntos() == null) {
            estudio.setAdjuntos(new ArrayList<>());
        }
        estudio.getAdjuntos().clear();
        if (nuevosAdjuntos == null) {
            return;
        }
        for (EstudioAdjunto adjunto : nuevosAdjuntos) {
            adjunto.setId(null);
            adjunto.setEstudio(estudio);
            estudio.getAdjuntos().add(adjunto);
        }
    }

    private void normalizeResultado(ResultadoEstudio resultado) {
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

    private String buildResultadoKey(Long parametroId, String grupoCodigo, Integer ordenResultado) {
        return parametroId + "|" + grupoCodigo + "|" + ordenResultado;
    }
}
