package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.*;
import imss.gob.mx.cohorte.modules.cuestionarios.discrepancias.DiscrepanciaLectura;
import imss.gob.mx.cohorte.modules.cuestionarios.discrepancias.DiscrepanciaLecturaRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacion;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacionRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.Severidad;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** Los cuestionarios contestados: alta, llenado por bloques y cierre. */
@Service
@AllArgsConstructor
public class CuestionarioService {

    private final CuestionarioRepository cuestionarioRepository;
    private final RespuestaCuestionarioRepository respuestaRepository;
    private final VariableCuestionarioRepository variableRepository;
    private final DiscrepanciaLecturaRepository discrepanciaRepository;
    private final PacienteRepository pacienteRepository;
    private final ReglaValidacionRepository reglaRepository;
    private final TipoCuestionarioService tipoService;
    private final InstitucionContextService contexto;

    // ── Consulta ─────────────────────────────────────────────────────────────

    public Cuestionario uno(Long id) {
        return cuestionarioRepository.findByIdAndInstitucion_Id(id, contexto.getIdInstitucionActual())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el cuestionario solicitado"));
    }

    public Page<Cuestionario> listar(Long idTipo, Pageable pagina) {
        Long inst = contexto.getIdInstitucionActual();
        return idTipo == null
                ? cuestionarioRepository.findAllByInstitucion_IdOrderByFechaRegistroDesc(inst, pagina)
                : cuestionarioRepository
                    .findAllByInstitucion_IdAndTipoCuestionario_IdOrderByFechaRegistroDesc(inst, idTipo, pagina);
    }

    public List<Cuestionario> deParticipante(String uuid) {
        return cuestionarioRepository.findAllByPaciente_UuidOrderByFechaRegistroDesc(uuid);
    }

    /** Los que tienen lecturas que una persona debe resolver. */
    public Page<Cuestionario> conPendientes(Pageable pagina) {
        return cuestionarioRepository.findConPendientes(contexto.getIdInstitucionActual(), pagina);
    }

    public List<RespuestaCuestionario> pendientesDeLectura(Long idCuestionario) {
        uno(idCuestionario);
        return respuestaRepository.findPendientesDeLectura(idCuestionario);
    }

    public List<DiscrepanciaLectura> discrepancias(Long idCuestionario, boolean soloSinResolver) {
        uno(idCuestionario);
        return soloSinResolver
                ? discrepanciaRepository.findAllByCuestionario_IdAndResueltaFalseOrderByIdAsc(idCuestionario)
                : discrepanciaRepository.findAllByCuestionario_IdOrderByIdAsc(idCuestionario);
    }

    /**
     * Un bloque del formulario: sus preguntas, lo contestado y qué está habilitado.
     *
     * <p>Se sirve por bloque y no completo porque el instrumento principal pasa de dos
     * mil preguntas: una sola respuesta con todo sería de megabytes y el navegador
     * tendría que pintar dos mil campos para mostrar cuarenta.</p>
     *
     * <p>Las dependencias se evalúan contra <b>todas</b> las respuestas del
     * cuestionario, no solo las del bloque. La llave de una pregunta suele estar en un
     * bloque anterior —el filtro «¿fuma?» abre páginas enteras— y evaluarla solo con
     * lo que hay a la vista daría por deshabilitado lo que sí se preguntó.</p>
     */
    public BloqueLleno bloque(Long idCuestionario, Long idSeccion) {
        Cuestionario cuestionario = uno(idCuestionario);
        SeccionCuestionario seccion = tipoService.seccion(idSeccion);
        if (!seccion.getTipoCuestionario().getId().equals(cuestionario.getTipoCuestionario().getId())) {
            throw new ObjConflictException("El bloque no pertenece al cuestionario indicado.");
        }

        List<VariableCuestionario> variables = variableRepository.findDeSeccionConOpciones(idSeccion);
        List<RespuestaCuestionario> respuestas = respuestaRepository.findDeSeccion(idCuestionario, idSeccion);
        Map<String, String> codigosContestados = codigosContestados(idCuestionario);

        Map<String, VariableCuestionario> llaves =
                llavesDe(seccion.getTipoCuestionario().getId());

        Set<Long> habilitadas = new HashSet<>();
        for (VariableCuestionario v : variables) {
            if (EvaluadorDependencias.habilitada(v, codigosContestados, llaves)) {
                habilitadas.add(v.getId());
            }
        }

        return new BloqueLleno(seccion, variables, respuestas, habilitadas);
    }

    /**
     * Comprueba el bloque contra las reglas de la plantilla antes de guardarlo.
     *
     * <p>Aqui si se impide guardar lo que incumple con severidad ERROR, al reves que
     * al importar: hay una persona delante que puede corregirlo en el momento, y
     * dejarlo pasar significaria guardar a sabiendas algo que ya sabemos que esta
     * mal. Lo de severidad REVISION no impide nada.</p>
     *
     * <p>Se revisa con TODO lo contestado del cuestionario, no solo con el bloque:
     * media regla mira preguntas de otras paginas —el sexo esta en la 3 y la seccion
     * que depende de el empieza en la 209—.</p>
     */
    private void revisarReglas(Cuestionario cuestionario) {
        Long idTipo = cuestionario.getTipoCuestionario().getId();
        List<ReglaValidacion> reglas =
                reglaRepository.findAllByTipoCuestionario_IdAndActivaTrue(idTipo);
        if (reglas.isEmpty()) return;

        Map<String, VariableCuestionario> porCodigo = new HashMap<>();
        for (VariableCuestionario v : variableRepository
                .findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(idTipo)) {
            porCodigo.put(v.getCodigo(), v);
        }

        List<String> bloqueantes = new ArrayList<>();
        for (MotorValidaciones.Aviso aviso : MotorValidaciones.revisar(
                reglas, codigosContestados(cuestionario.getId()), porCodigo,
                edadDe(cuestionario))) {
            if (aviso.severidad() == Severidad.ERROR) {
                bloqueantes.add(aviso.codigo() + ": " + aviso.mensaje());
            }
        }
        if (!bloqueantes.isEmpty()) {
            throw new ObjConflictException(String.join(" ", primeras(bloqueantes, 3))
                    + (bloqueantes.size() > 3
                       ? " Y " + (bloqueantes.size() - 3) + " mas." : ""));
        }
    }

    /** Anios del participante el dia de la aplicacion, o null si falta el dato. */
    private Integer edadDe(Cuestionario cuestionario) {
        return CriterioAplicabilidad.evaluar(cuestionario.getTipoCuestionario(),
                cuestionario.getPaciente(), cuestionario.getFechaAplicacion()).edad();
    }

    /**
     * Las llaves del instrumento, para poder seguir las cadenas de saltos.
     *
     * <p>Solo las preguntas que dependen de alguna: una llave que no esté aquí es que
     * no depende de nadie, y ahí se acaba la cadena. Son unas seiscientas de dos mil,
     * así que sale mucho más barato que traer el catálogo entero.</p>
     */
    private Map<String, VariableCuestionario> llavesDe(Long idTipo) {
        Map<String, VariableCuestionario> mapa = new HashMap<>();
        for (VariableCuestionario v : variableRepository
                .findAllByTipoCuestionario_IdAndDependeDeIsNotNull(idTipo)) {
            mapa.put(v.getCodigo(), v);
        }
        return mapa;
    }

    /** Lo que se le entrega a la pantalla para pintar un bloque. */
    public record BloqueLleno(
            SeccionCuestionario seccion,
            List<VariableCuestionario> variables,
            List<RespuestaCuestionario> respuestas,
            Set<Long> variablesHabilitadas
    ) {}

    /**
     * Qué contestó, por código de variable. Es lo que necesita el evaluador de
     * dependencias.
     *
     * <p>Una sola consulta y un mapa en memoria. La alternativa —preguntar por la
     * llave de cada variable al evaluarla— serían cuarenta consultas por bloque, y la
     * mayoría repetidas: la misma llave abre muchas preguntas.</p>
     */
    private Map<String, String> codigosContestados(Long idCuestionario) {
        Map<String, String> mapa = new HashMap<>();
        for (RespuestaCuestionario r : respuestaRepository.findAllByCuestionario_Id(idCuestionario)) {
            if (r.getMarcaAusencia() != MarcaAusencia.NINGUNA) continue;
            // En una variable de varias respuestas basta con que exista una marca para
            // que la llave cuente como contestada; se queda la primera.
            mapa.putIfAbsent(r.getVariable().getCodigo(), r.getValorCodigo());
        }
        return mapa;
    }

    /**
     * Los instrumentos del catalogo frente a un participante, cada uno con el veredicto
     * del criterio y si ya lo contesto.
     *
     * <p>Devuelve <b>todos</b> los activos y no solo los que aplican. Esconder los que no
     * aplican dejaria al capturista sin saber por que falta el que esperaba —y el motivo
     * suele ser un dato del expediente que falta, no una regla del protocolo—. La pantalla
     * los muestra separados, con su razon.</p>
     */
    public List<TipoFrenteAParticipante> tiposPara(String uuidPaciente) {
        Paciente paciente = pacienteRepository.findByUuid(uuidPaciente)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el participante indicado"));

        List<TipoFrenteAParticipante> salida = new ArrayList<>();
        for (TipoCuestionario tipo : tipoService.activos()) {
            // Sin fecha de aplicacion: se evalua con hoy, que es lo correcto para un
            // cuestionario que esta por abrirse.
            var veredicto = CriterioAplicabilidad.evaluar(tipo, paciente, null);
            boolean yaLoTiene = cuestionarioRepository
                    .existsByPaciente_IdAndTipoCuestionario_Id(paciente.getId(), tipo.getId());
            salida.add(new TipoFrenteAParticipante(tipo, veredicto, yaLoTiene));
        }
        return salida;
    }

    /** Un instrumento del catalogo evaluado contra un participante concreto. */
    public record TipoFrenteAParticipante(
            TipoCuestionario tipo,
            CriterioAplicabilidad.Resultado veredicto,
            boolean yaRegistrado
    ) {}

    // ── Alta ─────────────────────────────────────────────────────────────────

    /**
     * Abre un cuestionario para contestar en la aplicación.
     *
     * <p>No crea las respuestas vacías de antemano. Podría parecer ordenado hacerlo
     * —dos mil filas listas para llenarse— pero cada fila vacía mentiría: diría «sin
     * respuesta» de una pregunta que todavía no se ha visto, y el contador de
     * completitud arrancaría con dos mil faltantes que no significan nada. Las filas
     * aparecen cuando el bloque se guarda.</p>
     */
    @Transactional
    public Cuestionario abrir(Long idTipo, String uuidPaciente, OrigenCuestionario origen) {
        TipoCuestionario tipo = tipoService.uno(idTipo);
        Paciente paciente = pacienteRepository.findByUuid(uuidPaciente)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el participante indicado"));

        cuestionarioRepository.findByPaciente_IdAndTipoCuestionario_Id(paciente.getId(), idTipo)
                .ifPresent(c -> {
                    throw new ObjConflictException("El participante " + paciente.getFolio()
                            + " ya tiene un " + tipo.getNombre() + " registrado.");
                });

        // El criterio se comprueba aqui y no en la importacion, y la diferencia es
        // intencionada: en pantalla hay alguien delante que puede corregir el instrumento
        // o completar el expediente, asi que abrir el equivocado se impide. Un volcado,
        // en cambio, viene de una hoja que ya se contesto y ahi el dato se conserva
        // marcado. Ver Cuestionario#fueraDeCriterio.
        var veredicto = CriterioAplicabilidad.evaluar(tipo, paciente, null);
        if (!veredicto.aplica() && !veredicto.indeterminado()) {
            throw new ObjConflictException("Este cuestionario no le corresponde al participante "
                    + paciente.getFolio() + ": " + veredicto.motivo() + ".");
        }

        Cuestionario cuestionario = new Cuestionario();
        cuestionario.setPaciente(paciente);
        cuestionario.setTipoCuestionario(tipo);
        cuestionario.setEtiqueta(tipo.getClave() + "/" + paciente.getFolio()
                + "/" + EtiquetaCuestionario.LITERAL);
        cuestionario.setEstado(EstadoCuestionario.BORRADOR);
        cuestionario.setOrigen(origen == null ? OrigenCuestionario.APLICACION : origen);
        cuestionario.setUsuarioCaptura(contexto.getUsuarioActual());
        cuestionario.setInstitucion(contexto.getInstitucionActual());
        // Indeterminado no bloquea pero si se registra: el hueco es del expediente y
        // alguien tiene que poder encontrarlo despues.
        if (veredicto.indeterminado()) {
            cuestionario.setFueraDeCriterio(true);
            cuestionario.setMotivoFueraDeCriterio(veredicto.motivo());
        }
        return cuestionarioRepository.save(cuestionario);
    }

    // ── Llenado ──────────────────────────────────────────────────────────────

    /** Lo que llega de la pantalla por cada pregunta contestada. */
    public record RespuestaEntrante(Long idVariable, Integer ordenRespuesta, String valor) {}

    /**
     * Guarda lo contestado en un bloque.
     *
     * <p>Se guarda un bloque completo de una vez, y lo que no venga en la lista se
     * borra. Es la única forma de que desmarcar una casilla tenga efecto: con un
     * guardado incremental, quitar una marca no manda nada y la respuesta anterior se
     * quedaría ahí. Que el alcance sea el bloque y no el cuestionario entero es lo que
     * hace esto seguro: solo se puede borrar lo que estaba a la vista.</p>
     *
     * <p>Lo que trae marca de ausencia no se descarta: se guarda con su marca, porque
     * «el escáner no pudo leer esto» es información que hay que conservar incluso si
     * después alguien la teclea a mano.</p>
     */
    @Transactional
    public BloqueLleno guardarBloque(Long idCuestionario, Long idSeccion,
                                     List<RespuestaEntrante> entrantes) {
        Cuestionario cuestionario = uno(idCuestionario);
        if (cuestionario.getEstado() == EstadoCuestionario.ANULADO) {
            throw new ObjConflictException("El cuestionario esta anulado; no admite cambios.");
        }
        if (cuestionario.getEstado() == EstadoCuestionario.VALIDADO) {
            throw new ObjConflictException(
                    "El cuestionario ya fue validado. Reabrelo antes de modificarlo.");
        }
        tipoService.seccion(idSeccion);

        List<VariableCuestionario> variables = variableRepository.findDeSeccionConOpciones(idSeccion);
        Map<Long, VariableCuestionario> porId = new HashMap<>();
        for (VariableCuestionario v : variables) porId.put(v.getId(), v);

        List<RespuestaCuestionario> previas =
                respuestaRepository.findDeSeccion(idCuestionario, idSeccion);
        Map<String, RespuestaCuestionario> previasPorLlave = new HashMap<>();
        for (RespuestaCuestionario r : previas) {
            previasPorLlave.put(llave(r.getVariable().getId(), r.getOrdenRespuesta()), r);
        }

        List<RespuestaCuestionario> aGuardar = new ArrayList<>();
        Set<String> llavesVistas = new HashSet<>();

        for (RespuestaEntrante entrante : entrantes == null ? List.<RespuestaEntrante>of() : entrantes) {
            VariableCuestionario variable = porId.get(entrante.idVariable());
            if (variable == null) {
                throw new ObjConflictException("La pregunta " + entrante.idVariable()
                        + " no pertenece a este bloque.");
            }
            if (variable.getTipo() == TipoVariable.GRUPO) {
                throw new ObjConflictException(variable.getCodigo()
                        + " es un encabezado de grupo y no se contesta.");
            }
            int orden = entrante.ordenRespuesta() == null ? 0 : entrante.ordenRespuesta();
            if (orden != 0 && !variable.admiteVarias()) {
                throw new ObjConflictException(variable.getCodigo()
                        + " admite una sola respuesta y se recibieron varias.");
            }

            String llave = llave(variable.getId(), orden);
            if (!llavesVistas.add(llave)) {
                throw new ObjConflictException("Se recibieron dos respuestas para "
                        + variable.getCodigo() + " en la misma posicion.");
            }

            RespuestaCuestionario fila = previasPorLlave.get(llave);
            if (fila == null) {
                fila = new RespuestaCuestionario();
                fila.setCuestionario(cuestionario);
                fila.setVariable(variable);
                fila.setOrdenRespuesta(orden);
            }
            aplicar(fila, variable, entrante.valor());
            aGuardar.add(fila);
        }

        // Lo que estaba y ya no viene: el usuario lo desmarco.
        List<RespuestaCuestionario> aBorrar = previas.stream()
                .filter(r -> !llavesVistas.contains(llave(r.getVariable().getId(), r.getOrdenRespuesta())))
                .toList();

        respuestaRepository.deleteAll(aBorrar);
        respuestaRepository.saveAll(aGuardar);
        respuestaRepository.flush();
        revisarReglas(cuestionario);

        cuestionario.setRespuestasPendientes(
                (int) respuestaRepository.contarPendientesDeLectura(idCuestionario));
        cuestionarioRepository.save(cuestionario);

        return bloque(idCuestionario, idSeccion);
    }

    /** Interpreta el valor y lo deja en la fila, con su marca de ausencia si la hay. */
    private void aplicar(RespuestaCuestionario fila, VariableCuestionario variable, String valor) {
        InterpreteCelda.Interpretacion i = InterpreteCelda.interpretar(variable, valor);
        fila.setMarcaAusencia(i.marca());
        fila.setValorCodigo(i.codigo());
        fila.setValorNumerico(i.numero());
        fila.setValorTexto(i.texto());
        fila.setValorFecha(i.fecha());
        fila.setValorCrudo(i.tieneProblema() ? i.crudo() : null);

        if (i.tieneProblema()) {
            // Al capturar en pantalla un valor que no encaja si se rechaza: a
            // diferencia del volcado, aqui hay alguien delante que puede corregirlo
            // en el momento, y aceptarlo solo trasladaria el problema al analisis.
            throw new ObjConflictException(variable.getCodigo() + ": " + i.problema());
        }
    }

    private String llave(Long idVariable, Integer orden) {
        return idVariable + "#" + (orden == null ? 0 : orden);
    }

    // ── Cierre ───────────────────────────────────────────────────────────────

    /**
     * Qué falta para poder cerrar el cuestionario.
     *
     * <p>Solo se exigen las obligatorias <em>habilitadas</em>. Pedir una pregunta que
     * la llave dejó fuera sería imposible de satisfacer: no está en la hoja de ese
     * participante.</p>
     */
    public List<String> faltantes(Long idCuestionario) {
        Cuestionario cuestionario = uno(idCuestionario);
        Map<String, String> contestados = codigosContestados(idCuestionario);

        Set<Long> conRespuesta = new HashSet<>();
        for (RespuestaCuestionario r : respuestaRepository.findAllByCuestionario_Id(idCuestionario)) {
            // Ver RespuestaCuestionario#cuentaComoContestada: la casilla sin marcar es
            // la respuesta «no», y exigirla dejaria el cuestionario sin poder cerrarse.
            if (r.cuentaComoContestada()) conRespuesta.add(r.getVariable().getId());
        }

        List<String> faltan = new ArrayList<>();
        Long idTipo = cuestionario.getTipoCuestionario().getId();
        Map<String, VariableCuestionario> llaves = llavesDe(idTipo);
        for (VariableCuestionario v : variableRepository
                .findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(idTipo)) {
            if (!Boolean.TRUE.equals(v.getActiva())) continue;
            if (!Boolean.TRUE.equals(v.getObligatoria())) continue;
            if (!v.esContestable()) continue;
            if (conRespuesta.contains(v.getId())) continue;
            if (!EvaluadorDependencias.habilitada(v, contestados, llaves)) continue;
            faltan.add(v.getCodigo());
        }
        return faltan;
    }

    @Transactional
    public Cuestionario cerrar(Long idCuestionario) {
        Cuestionario cuestionario = uno(idCuestionario);
        List<String> faltan = faltantes(idCuestionario);
        if (!faltan.isEmpty()) {
            throw new ObjConflictException("Faltan " + faltan.size()
                    + " respuesta(s) obligatoria(s): " + String.join(", ", primeras(faltan, 10))
                    + (faltan.size() > 10 ? "…" : ""));
        }
        cuestionario.setEstado(EstadoCuestionario.COMPLETO);
        return cuestionarioRepository.save(cuestionario);
    }

    /**
     * Da el cuestionario por revisado contra la hoja original.
     *
     * <p>Exige que no queden lecturas pendientes ni discrepancias sin adjudicar. Eso es
     * lo que distingue este estado de «completo»: completo dice que están todas las
     * respuestas, validado dice que alguien se hizo responsable de ellas, y no se puede
     * ser responsable de una celda que el escáner nunca leyó.</p>
     */
    @Transactional
    public Cuestionario validar(Long idCuestionario, String nota) {
        Cuestionario cuestionario = uno(idCuestionario);

        long pendientes = respuestaRepository.contarPendientesDeLectura(idCuestionario);
        if (pendientes > 0) {
            throw new ObjConflictException("Quedan " + pendientes
                    + " respuesta(s) sin leer. Capturalas antes de validar.");
        }
        long discrepancias = discrepanciaRepository.countByCuestionario_IdAndResueltaFalse(idCuestionario);
        if (discrepancias > 0) {
            throw new ObjConflictException("Quedan " + discrepancias
                    + " lectura(s) en desacuerdo sin resolver.");
        }
        List<String> faltan = faltantes(idCuestionario);
        if (!faltan.isEmpty()) {
            throw new ObjConflictException("Faltan " + faltan.size() + " respuesta(s) obligatoria(s).");
        }

        cuestionario.setEstado(EstadoCuestionario.VALIDADO);
        cuestionario.setUsuarioValida(contexto.getUsuarioActual());
        cuestionario.setFechaValidacion(LocalDateTime.now());
        if (nota != null && !nota.isBlank()) cuestionario.setObservaciones(nota);
        return cuestionarioRepository.save(cuestionario);
    }

    /** Devuelve un cuestionario validado a borrador para poder corregirlo. */
    @Transactional
    public Cuestionario reabrir(Long idCuestionario) {
        Cuestionario cuestionario = uno(idCuestionario);
        if (cuestionario.getEstado() == EstadoCuestionario.ANULADO) {
            throw new ObjConflictException("El cuestionario esta anulado.");
        }
        cuestionario.setEstado(EstadoCuestionario.BORRADOR);
        cuestionario.setUsuarioValida(null);
        cuestionario.setFechaValidacion(null);
        return cuestionarioRepository.save(cuestionario);
    }

    /**
     * Retira el cuestionario del análisis conservando lo capturado.
     *
     * <p>No hay borrado. Un cuestionario representa una hoja de papel que existe y que
     * alguien contestó; borrarlo haría imposible saber por qué el participante aparece
     * sin instrumento, y la hoja seguiría en el archivero.</p>
     */
    @Transactional
    public Cuestionario anular(Long idCuestionario, String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ObjConflictException("Indica el motivo por el que se anula el cuestionario.");
        }
        Cuestionario cuestionario = uno(idCuestionario);
        cuestionario.setEstado(EstadoCuestionario.ANULADO);
        cuestionario.setObservaciones(motivo);
        return cuestionarioRepository.save(cuestionario);
    }

    // ── Discrepancias ────────────────────────────────────────────────────────

    /**
     * Adjudica una celda en desacuerdo.
     *
     * <p>Al resolverla se escribe también la respuesta, que es el sentido de resolver:
     * dejar la decisión anotada sin aplicarla dejaría la base diciendo una cosa y la
     * resolución otra.</p>
     */
    @Transactional
    public DiscrepanciaLectura resolverDiscrepancia(Long idDiscrepancia, String valorElegido, String nota) {
        DiscrepanciaLectura d = discrepanciaRepository.findById(idDiscrepancia)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la discrepancia indicada"));
        Cuestionario cuestionario = uno(d.getCuestionario().getId());

        if (d.getVariable() == null) {
            throw new ObjConflictException("La columna \"" + d.getColumnaVolcado()
                    + "\" no esta en el catalogo. Documentala antes de resolver la lectura.");
        }

        List<RespuestaCuestionario> filas = respuestaRepository
                .findAllByCuestionario_IdAndVariable_Id(cuestionario.getId(), d.getVariable().getId());
        RespuestaCuestionario fila = filas.isEmpty() ? null : filas.get(0);
        if (fila == null) {
            fila = new RespuestaCuestionario();
            fila.setCuestionario(cuestionario);
            fila.setVariable(d.getVariable());
            fila.setOrdenRespuesta(0);
        }
        aplicar(fila, d.getVariable(), valorElegido);
        fila.setCeldaOrigen(d.getCeldaOrigen());
        respuestaRepository.save(fila);

        d.setResuelta(true);
        d.setValorAdjudicado(valorElegido);
        d.setUsuarioResuelve(contexto.getUsuarioActual());
        d.setFechaResolucion(LocalDateTime.now());
        d.setNota(nota);

        cuestionario.setRespuestasPendientes(
                (int) respuestaRepository.contarPendientesDeLectura(cuestionario.getId()));
        cuestionarioRepository.save(cuestionario);

        return discrepanciaRepository.save(d);
    }

    private List<String> primeras(List<String> lista, int n) {
        return lista.size() <= n ? lista : lista.subList(0, n);
    }
}
