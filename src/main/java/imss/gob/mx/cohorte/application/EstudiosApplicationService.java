package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.documentos.EstudioDocumentoRepository;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudioRepository;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.estudios.EstudioService;
import imss.gob.mx.cohorte.services.estudios.ParametroEstudioService;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

@Slf4j
@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.ESTUDIOS_MEDICOS)
public class EstudiosApplicationService {

    private static final String ROOT_GROUP_CODE = "ROOT";
    private static final int ROOT_ORDER = 0;

    private final EstudioService estudioService;
    private final TipoService tipoEstudioService;
    private final PacienteService pacienteService;
    private final ParticipanteAccesoService participanteAccesoService;
    private final UserService userService;
    private final ParametroEstudioService parametroService;
    private final ResultadoEstudioRepository resultadoRepository;
    private final EstudioDocumentoRepository estudioDocumentoRepository;
    private final InstitucionRepository institucionRepository;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public List<EstudioMedico> getAllEstudios() {
        return estudioService.getAll();
    }

    @Transactional(readOnly = true)
    public EstudioMedico getEstudio(Long id) {
        EstudioMedico estudio = estudioService.getOne(id);
        // Lectura: basta con alcanzar el registro o al participante. Editarlo sigue
        // reservado a la sede que lo hizo (ver updateEstudio).
        participanteAccesoService.verificarLecturaRegistro(estudio.getInstitucion(), estudio.getPaciente());
        return estudio;
    }

    @Transactional(readOnly = true)
    public List<EstudioMedico> getEstudiosByPaciente(String uuid) {
        // Si alcanzo al participante veo su historial completo; si no lo alcanzo pero
        // conservo registros suyos, veo solo los mios. Es la regla de union a nivel
        // de lista — ver ParticipanteAccesoService.resolverParaLectura.
        var acceso = participanteAccesoService.resolverParaLectura(uuid);
        return acceso.soloPropio()
                ? estudioService.getAllByPacienteUUIDDeMiInstitucion(uuid)
                : estudioService.getAllByPacienteUUID(uuid);
    }

    @Transactional(readOnly = true)
    public Page<EstudioMedico> getAllEstudiosPaginado(Pageable pageable) {
        return estudioService.getAllPaginado(pageable);
    }

    @Transactional(readOnly = true)
    public Page<EstudioMedico> getEstudiosByPacientePaginado(String uuid, Pageable pageable) {
        var acceso = participanteAccesoService.resolverParaLectura(uuid);
        return acceso.soloPropio()
                ? estudioService.getAllByPacienteUUIDDeMiInstitucionPaginado(uuid, pageable)
                : estudioService.getAllByPacienteUUIDPaginado(uuid, pageable);
    }

    @Transactional
    public EstudioMedico createEstudio(EstudioMedico estudioMedico) {
        resolveRelaciones(estudioMedico);
        exigirParametrosActivos(estudioMedico);
        return estudioService.create(estudioMedico);
    }

    @Transactional
    public void deleteEstudio(Long id) {
        estudioDocumentoRepository.deleteByEstudio_Id(id);
        estudioService.delete(id);
    }

    @Transactional
    public EstudioMedico updateEstudio(Long id, EstudioMedico estudioMedico) {
        EstudioMedico existente = estudioService.getOne(id);
        institucionContextService.verificarPertenece(existente.getInstitucion());

        estudioMedico.setInstitucion(existente.getInstitucion());
        resolveRelaciones(estudioMedico);
        exigirNoPerderResultados(existente, estudioMedico);

        existente.setPaciente(estudioMedico.getPaciente());
        existente.setUsuarioRealiza(estudioMedico.getUsuarioRealiza());
        existente.setTipoEstudio(estudioMedico.getTipoEstudio());
        existente.setFechaEstudio(estudioMedico.getFechaEstudio());
        existente.setObservaciones(estudioMedico.getObservaciones());

        replaceResultados(existente, estudioMedico.getResultadoEstudio());

        return estudioService.update(existente);
    }

    /**
     * Al registrar un estudio tienen que venir todos los parámetros en uso.
     *
     * <p>Esta regla vivía solo en los formularios, así que cualquier petición que no
     * pasara por ellos podía guardar un estudio a medias —o sin un solo resultado— y
     * el servidor lo aceptaba. Tenerla aquí es lo que permite leer la ausencia de un
     * resultado como «ese parámetro no aplicaba», que es de lo que depende mostrar
     * bien las capturas antiguas.</p>
     *
     * <p>Se exige que el parámetro aparezca al menos una vez, no una por grupo: el
     * reparto entre grupos lo decide quien captura.</p>
     */
    private void exigirParametrosActivos(EstudioMedico estudioMedico) {
        Set<Long> capturados = idsDeParametros(estudioMedico.getResultadoEstudio());

        List<String> faltantes = parametroService.getByTipoEstudio(estudioMedico.getTipoEstudio().getId())
                .stream()
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .filter(p -> !capturados.contains(p.getId()))
                .map(ParametroEstudio::getNombre)
                .toList();

        if (!faltantes.isEmpty()) {
            throw new ObjConflictException(
                    "Faltan resultados para: " + String.join(", ", faltantes));
        }
    }

    /**
     * Al editar no se exige la lista completa, sino no empeorar lo que había.
     *
     * <p>Exigirla dejaría atrapados los estudios antiguos a los que legítimamente les
     * falta un parámetro: los que se capturaron antes de que ese parámetro existiera.
     * Entrar a corregirles una observación obligaría a inventar un dato que nadie
     * midió. Lo que sí se impide es que una edición borre un resultado que ya estaba,
     * salvo que su parámetro haya quedado fuera de uso.</p>
     */
    private void exigirNoPerderResultados(EstudioMedico existente, EstudioMedico entrante) {
        Set<Long> entran = idsDeParametros(entrante.getResultadoEstudio());

        List<String> perdidos = existente.getResultadoEstudio() == null
                ? List.of()
                : existente.getResultadoEstudio().stream()
                        .map(ResultadoEstudio::getParametro)
                        .filter(Objects::nonNull)
                        .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                        .filter(p -> !entran.contains(p.getId()))
                        .map(ParametroEstudio::getNombre)
                        .distinct()
                        .toList();

        if (!perdidos.isEmpty()) {
            throw new ObjConflictException(
                    "La edición dejaría sin resultado a: " + String.join(", ", perdidos));
        }
    }

    private Set<Long> idsDeParametros(List<ResultadoEstudio> resultados) {
        if (resultados == null) return Set.of();
        return resultados.stream()
                .map(ResultadoEstudio::getParametro)
                .filter(Objects::nonNull)
                .map(ParametroEstudio::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private void resolveRelaciones(EstudioMedico estudioMedico) {
        if (estudioMedico.getPaciente() == null || estudioMedico.getPaciente().getUuid() == null) {
            throw new ObjNotFoundException("Falta informacion de participante");
        }
        if (estudioMedico.getUsuarioRealiza() == null || estudioMedico.getUsuarioRealiza().getUUID() == null) {
            throw new ObjNotFoundException("Falta informacion de usuario");
        }
        if (estudioMedico.getTipoEstudio() == null || estudioMedico.getTipoEstudio().getId() == null) {
            throw new ObjNotFoundException("Falta informacion de tipo de estudio");
        }
        if (estudioMedico.getInstitucion() == null || estudioMedico.getInstitucion().getId() == null) {
            throw new ObjNotFoundException("Falta informacion de institucion responsable del estudio");
        }
        System.out.println("USUARIO REALIZA= " + estudioMedico.getUsuarioRealiza().getUUID());
        Paciente paciente = participanteAccesoService.resolver(estudioMedico.getPaciente().getUuid());
        BeanUser usuario = userService.getByUUID(estudioMedico.getUsuarioRealiza().getUUID());
        TipoEstudio tipoEstudio = tipoEstudioService.getOne(estudioMedico.getTipoEstudio().getId());
        Institucion institucion = institucionRepository.findById(estudioMedico.getInstitucion().getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución con id: " + estudioMedico.getInstitucion().getId()));
        // Aislamiento por institución: no se permite registrar/editar estudios a nombre de otra institución
        institucionContextService.verificarPertenece(institucion);

        resolveResultados(estudioMedico, tipoEstudio);

        estudioMedico.setPaciente(paciente);
        estudioMedico.setUsuarioRealiza(usuario);
        estudioMedico.setTipoEstudio(tipoEstudio);
        estudioMedico.setInstitucion(institucion);
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

    private void replaceResultados(EstudioMedico estudio, List<ResultadoEstudio> nuevosResultados) {
        if (estudio.getResultadoEstudio() == null) {
            estudio.setResultadoEstudio(new ArrayList<>());
        }
        estudio.getResultadoEstudio().clear();
        resultadoRepository.flush();
        if (nuevosResultados == null) {
            return;
        }
        for (ResultadoEstudio resultado : nuevosResultados) {
            resultado.setId(null);
            resultado.setEstudio(estudio);
            estudio.getResultadoEstudio().add(resultado);
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

    /**
     * Cuantos documentos tiene cada estudio de la lista.
     *
     * <p>Los adjuntos de un estudio viven en EstudioDocumento, del modulo de
     * documentos. La tabla Estudio_Adjunto es anterior a ese modulo y hoy no la
     * escribe nadie: contar sobre ella devolvia siempre cero, aunque el estudio
     * tuviera archivos y el dialogo los mostrara.</p>
     *
     * @return id de estudio a numero de documentos; los estudios sin ninguno no
     *         aparecen en el mapa
     */
    @Transactional(readOnly = true)
    public java.util.Map<Long, Integer> contarAdjuntosPorEstudio(java.util.List<EstudioMedico> estudios) {
        if (estudios == null || estudios.isEmpty()) return java.util.Map.of();

        java.util.List<Long> ids = estudios.stream().map(EstudioMedico::getId).toList();
        java.util.Map<Long, Integer> conteo = new java.util.HashMap<>();
        for (Object[] fila : estudioDocumentoRepository.contarPorEstudio(ids)) {
            conteo.put((Long) fila[0], ((Number) fila[1]).intValue());
        }
        return conteo;
    }
}
