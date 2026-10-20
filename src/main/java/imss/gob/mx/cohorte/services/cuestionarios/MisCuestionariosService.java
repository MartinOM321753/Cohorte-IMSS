package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.Cuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.CuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.EstadoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.OrigenCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacion;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacionRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.Severidad;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.institucion.InstitucionModuloService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Lo que un participante puede hacer con sus propios cuestionarios desde su cuenta.
 *
 * <h3>Por qué es un servicio aparte y no un permiso más en el de siempre</h3>
 *
 * <p>Porque el ámbito de acceso es otro. {@code CuestionarioService} resuelve todo por
 * institución —es el modelo del personal: «lo que mi sede puede ver»—. Un participante no
 * pertenece a una sede, pertenece a un expediente, y lo único que le toca es <em>lo
 * suyo</em>. Colgar su acceso del filtro por institución habría abierto los cuestionarios
 * de todos los participantes de su sede con solo equivocarse en un id.</p>
 *
 * <p>Aquí el camino de autorización es: usuario autenticado → su {@code Persona} → su
 * {@code Paciente} → sus cuestionarios. Nunca se recibe un id de participante de fuera.</p>
 *
 * <h3>La institución se saca del participante, no del usuario</h3>
 *
 * <p>El módulo por institución se comprueba contra la sede del <em>expediente</em>. Una
 * cuenta de participante puede no tener institución asignada —no es personal de ninguna—,
 * y colgar la comprobación de ahí produciría un 403 que el participante no puede entender
 * ni resolver. La sede del expediente, en cambio, siempre existe.</p>
 */
@Service
@AllArgsConstructor
public class MisCuestionariosService {

    private final CuestionarioRepository cuestionarioRepository;
    private final RespuestaCuestionarioRepository respuestaRepository;
    private final VariableCuestionarioRepository variableRepository;
    private final SeccionCuestionarioRepository seccionRepository;
    private final PacienteRepository pacienteRepository;
    private final imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionarioRepository tipoRepository;
    private final ReglaValidacionRepository reglaRepository;
    private final InstitucionModuloService moduloService;
    private final InstitucionContextService contexto;

    // ── Identidad ────────────────────────────────────────────────────────────

    /** El expediente de quien está autenticado. Es el único participante al que puede llegar. */
    private Paciente miExpediente() {
        var usuario = contexto.getUsuarioActual();
        if (usuario.getPersona() == null) {
            throw new AccessDeniedException("Esta cuenta no esta vinculada a un expediente de participante");
        }
        Paciente paciente = pacienteRepository.findByPersona_Id(usuario.getPersona().getId())
                .orElseThrow(() -> new ObjNotFoundException(
                        "No se encontro un expediente de participante vinculado a esta cuenta"));

        if (!moduloService.tieneAccesoHabilitado(
                paciente.getInstitucion().getId(), ModuloSistema.CUESTIONARIOS)) {
            throw new AccessDeniedException("El modulo de cuestionarios no esta habilitado para su sede");
        }
        return paciente;
    }

    /**
     * Uno de mis cuestionarios, listo para contestar.
     *
     * <p>Las tres condiciones no son celo: cada una tapa una forma distinta de corromper
     * datos. Que sea mío evita leer el de otro; que sea de origen {@code APLICACION} evita
     * que un participante sobrescriba lo que ya se escaneó de su hoja de papel —esos
     * valores son la lectura del instrumento, no algo que él deba poder cambiar—; y que
     * esté en borrador evita tocar uno que alguien ya revisó y firmó.</p>
     */
    private Cuestionario mioParaContestar(Long id) {
        Cuestionario c = mio(id);
        if (c.getOrigen() != OrigenCuestionario.APLICACION) {
            throw new ObjConflictException("Este cuestionario se capturo a partir de su hoja en papel, "
                    + "asi que no se contesta en linea. Si hay algo que corregir, acuda con el personal.");
        }
        if (c.getEstado() != EstadoCuestionario.BORRADOR) {
            throw new ObjConflictException("Este cuestionario ya se envio y no admite cambios.");
        }
        return c;
    }

    private Cuestionario mio(Long id) {
        Paciente paciente = miExpediente();
        Cuestionario c = cuestionarioRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el cuestionario solicitado"));
        if (!c.getPaciente().getId().equals(paciente.getId())) {
            // Mismo mensaje que si no existiera: decir «es de otra persona» confirmaria
            // que ese id existe, que es justo lo que no tiene por que saber.
            throw new ObjNotFoundException("No se encontro el cuestionario solicitado");
        }
        return c;
    }

    // ── Consulta ─────────────────────────────────────────────────────────────

    /** Mis cuestionarios, el pendiente de contestar primero. */
    public List<Cuestionario> mios() {
        Paciente paciente = miExpediente();
        return cuestionarioRepository.findAllByPaciente_UuidOrderByFechaRegistroDesc(paciente.getUuid())
                .stream()
                .filter(c -> c.getEstado() != EstadoCuestionario.ANULADO)
                .sorted(Comparator
                        .comparing((Cuestionario c) -> c.getEstado() != EstadoCuestionario.BORRADOR)
                        .thenComparing(Cuestionario::getFechaRegistro, Comparator.reverseOrder()))
                .toList();
    }

    /**
     * Los instrumentos que el participante puede empezar por su cuenta ahora mismo.
     *
     * <p>Tres condiciones: que el instrumento permita autollenado, que le corresponda
     * segun su sexo y su edad, y que no lo tenga ya. La segunda reutiliza el mismo
     * criterio que usa el personal, asi que un participante no puede empezar uno que no
     * le toca ni escribiendo la peticion a mano.</p>
     */
    public List<DisponibleParaIniciar> disponibles() {
        Paciente paciente = miExpediente();

        List<DisponibleParaIniciar> salida = new ArrayList<>();
        for (TipoCuestionario tipo : tipoRepository.findAllByActivoAndInstitucion_IdOrderByClaveAsc(
                true, paciente.getInstitucion().getId())) {

            if (!Boolean.TRUE.equals(tipo.getPermiteAutollenado())) continue;
            if (cuestionarioRepository.existsByPaciente_IdAndTipoCuestionario_Id(
                    paciente.getId(), tipo.getId())) continue;

            var veredicto = CriterioAplicabilidad.evaluar(tipo, paciente, null);
            // Indeterminado se ofrece igual, como al personal: el hueco es del expediente,
            // no del participante, y quedara marcado al crearlo.
            if (!veredicto.aplica() && !veredicto.indeterminado()) continue;

            salida.add(new DisponibleParaIniciar(tipo, veredicto.indeterminado()));
        }
        return salida;
    }

    public record DisponibleParaIniciar(TipoCuestionario tipo, boolean indeterminado) {}

    /**
     * El participante empieza un cuestionario por su cuenta.
     *
     * <p>Recibe el instrumento, nunca el participante: ese sale de la sesion. Y vuelve a
     * comprobar las tres condiciones en lugar de confiar en que la pantalla solo ofrecio
     * los validos — la pantalla es solo una sugerencia, la regla vive aqui.</p>
     */
    @Transactional
    public Cuestionario iniciar(Long idTipo) {
        Paciente paciente = miExpediente();
        TipoCuestionario tipo = tipoRepository.findById(idTipo)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el cuestionario solicitado"));

        if (!tipo.getInstitucion().getId().equals(paciente.getInstitucion().getId())
                || !Boolean.TRUE.equals(tipo.getActivo())) {
            throw new ObjNotFoundException("No se encontro el cuestionario solicitado");
        }
        if (!Boolean.TRUE.equals(tipo.getPermiteAutollenado())) {
            throw new ObjConflictException("Este cuestionario lo aplica el personal; "
                    + "no se contesta por cuenta propia.");
        }
        if (cuestionarioRepository.existsByPaciente_IdAndTipoCuestionario_Id(
                paciente.getId(), tipo.getId())) {
            throw new ObjConflictException("Ya tiene este cuestionario registrado.");
        }

        var veredicto = CriterioAplicabilidad.evaluar(tipo, paciente, null);
        if (!veredicto.aplica() && !veredicto.indeterminado()) {
            throw new ObjConflictException("Este cuestionario no le corresponde: "
                    + veredicto.motivo() + ".");
        }

        Cuestionario c = new Cuestionario();
        c.setPaciente(paciente);
        c.setTipoCuestionario(tipo);
        c.setEtiqueta(tipo.getClave() + "/" + paciente.getFolio() + "/" + EtiquetaCuestionario.LITERAL);
        c.setEstado(EstadoCuestionario.BORRADOR);
        c.setOrigen(OrigenCuestionario.APLICACION);
        c.setInstitucion(paciente.getInstitucion());
        c.setUsuarioCaptura(contexto.getUsuarioActual());
        if (veredicto.indeterminado()) {
            c.setFueraDeCriterio(true);
            c.setMotivoFueraDeCriterio(veredicto.motivo());
        }
        return cuestionarioRepository.save(c);
    }

    /** Los bloques del cuestionario con cuántas preguntas lleva contestadas de cada uno. */
    public List<AvanceBloque> avance(Long idCuestionario) {
        Cuestionario c = mio(idCuestionario);
        List<SeccionCuestionario> secciones = seccionRepository
                .findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(c.getTipoCuestionario().getId());

        Map<String, String> contestados = codigosContestados(idCuestionario);
        Map<String, VariableCuestionario> llaves = llavesDe(c.getTipoCuestionario().getId());
        Set<Long> conValor = new HashSet<>();
        for (RespuestaCuestionario r : respuestaRepository.findAllByCuestionario_Id(idCuestionario)) {
            // Una casilla sin marcar cuenta como contestada: es un «no», no un hueco.
            if (r.cuentaComoContestada()) conValor.add(r.getVariable().getId());
        }

        List<AvanceBloque> salida = new ArrayList<>();
        for (SeccionCuestionario s : secciones) {
            int aplicables = 0;
            int respondidas = 0;
            for (VariableCuestionario v : variableRepository.findDeSeccionConOpciones(s.getId())) {
                if (!v.esContestable()) continue;
                // Las que la pregunta llave dejo fuera no cuentan para el avance: contarlas
                // mostraria un bloque «incompleto» que el participante no puede completar.
                if (!EvaluadorDependencias.habilitada(v, contestados, llaves)) continue;
                aplicables++;
                if (conValor.contains(v.getId())) respondidas++;
            }
            salida.add(new AvanceBloque(s, aplicables, respondidas));
        }
        return salida;
    }

    /**
     * Las llaves del instrumento, para poder seguir las cadenas de saltos.
     *
     * <p>Solo las preguntas que dependen de alguna: una llave que no esté aquí es que
     * no depende de nadie, y ahí se acaba la cadena. Son unas seiscientas de dos mil,
     * mucho más barato que traer el catálogo entero en cada pantalla.</p>
     */
    private Map<String, VariableCuestionario> llavesDe(Long idTipo) {
        Map<String, VariableCuestionario> mapa = new HashMap<>();
        for (VariableCuestionario v : variableRepository
                .findAllByTipoCuestionario_IdAndDependeDeIsNotNull(idTipo)) {
            mapa.put(v.getCodigo(), v);
        }
        return mapa;
    }

    public record AvanceBloque(SeccionCuestionario seccion, int aplicables, int respondidas) {}

    /** Un bloque con sus preguntas y lo que ya contestó. */
    public BloqueParaContestar bloque(Long idCuestionario, Long idSeccion) {
        Cuestionario c = mio(idCuestionario);
        SeccionCuestionario seccion = seccionRepository.findById(idSeccion)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el bloque solicitado"));
        if (!seccion.getTipoCuestionario().getId().equals(c.getTipoCuestionario().getId())) {
            throw new ObjConflictException("El bloque no pertenece a este cuestionario.");
        }

        List<VariableCuestionario> variables = variableRepository.findDeSeccionConOpciones(idSeccion);
        List<RespuestaCuestionario> respuestas = respuestaRepository.findDeSeccion(idCuestionario, idSeccion);
        Map<String, String> contestados = codigosContestados(idCuestionario);

        Map<String, VariableCuestionario> llaves =
                llavesDe(seccion.getTipoCuestionario().getId());

        Set<Long> habilitadas = new HashSet<>();
        for (VariableCuestionario v : variables) {
            if (EvaluadorDependencias.habilitada(v, contestados, llaves)) {
                habilitadas.add(v.getId());
            }
        }
        return new BloqueParaContestar(seccion, variables, respuestas, habilitadas);
    }

    public record BloqueParaContestar(
            SeccionCuestionario seccion,
            List<VariableCuestionario> variables,
            List<RespuestaCuestionario> respuestas,
            Set<Long> variablesHabilitadas
    ) {}

    // ── Autoguardado ─────────────────────────────────────────────────────────

    /**
     * Un cambio suelto, tal como lo manda el autoguardado.
     *
     * @param secuencia contador que el cliente incrementa en cada cambio de esa respuesta.
     *                  Es lo que hace el guardado idempotente y conmutativo; ver
     *                  {@link RespuestaCuestionario#getSecuenciaCliente()}.
     * @param valor     lo contestado. Vacío o nulo <b>no se ignora</b>: significa que el
     *                  participante borró lo que había, y se guarda como respuesta ausente.
     *                  Ignorarlo dejaría imposible deshacer una respuesta.
     */
    public record Cambio(Long idVariable, String valor, Long secuencia) {}

    /**
     * @param aceptados  por id de variable, la secuencia que quedó guardada. El cliente
     *                   usa esto para sacar de su cola lo que ya está a salvo.
     * @param rechazados cambios que llegaron con una secuencia que no supera la guardada
     *                   —reintentos y llegadas fuera de orden—. No son errores.
     * @param invalidos  por id de variable, por qué no se pudo guardar. Sí son errores, y
     *                   el cliente tiene que dejar de reintentarlos.
     * @param avisos     lo que hay que advertirle de lo que lleva contestado. No impide
     *                   guardar: lo guardado está guardado. Es para llevarlo de la mano
     *                   mientras contesta, en vez de pararlo al final.
     */
    public record ResultadoAutoguardado(
            Map<Long, Long> aceptados,
            List<Long> rechazados,
            Map<Long, String> invalidos,
            List<AvisoParaParticipante> avisos,
            LocalDateTime guardadoEn
    ) {}

    /**
     * Guarda un puñado de cambios. <b>Nunca borra una respuesta que no venga en la lista.</b>
     *
     * <h3>Por qué esto no puede ser el guardado del personal</h3>
     *
     * <p>{@code CuestionarioService.guardarBloque} reemplaza el bloque entero: lo que no
     * llega, se borra. Eso es correcto detrás de un botón «Guardar», donde el formulario
     * manda todo lo que hay a la vista. Aplicado al autoguardado seria desastroso: el
     * cliente manda solo lo que cambió, así que la primera petición borraría el resto del
     * bloque. Son dos contratos distintos y tienen que ser dos endpoints distintos.</p>
     *
     * <h3>Un cambio malo no tumba los demás</h3>
     *
     * <p>Cada cambio se evalúa por separado y los que fallan se reportan en
     * {@code invalidos} sin afectar al resto. Rechazar el lote completo porque una celda
     * trae algo raro haría que el participante perdiera lo que sí estaba bien, y el
     * autoguardado reintentaría el lote entero para siempre.</p>
     */
    @Transactional
    public ResultadoAutoguardado autoguardar(Long idCuestionario, List<Cambio> cambios) {
        Cuestionario cuestionario = mioParaContestar(idCuestionario);

        Map<Long, Long> aceptados = new LinkedHashMap<>();
        List<Long> rechazados = new ArrayList<>();
        Map<Long, String> invalidos = new LinkedHashMap<>();

        if (cambios == null || cambios.isEmpty()) {
            return new ResultadoAutoguardado(aceptados, rechazados, invalidos,
                    List.of(), LocalDateTime.now());
        }

        // Las variables implicadas, de una consulta: con el autoguardado disparando cada
        // pocos segundos, resolverlas una por una multiplicaria los viajes a la base.
        Map<Long, VariableCuestionario> variables = new HashMap<>();
        for (VariableCuestionario v : variableRepository.findAllById(
                cambios.stream().map(Cambio::idVariable).filter(Objects::nonNull).toList())) {
            variables.put(v.getId(), v);
        }

        Map<Long, RespuestaCuestionario> previas = new HashMap<>();
        for (RespuestaCuestionario r : respuestaRepository.findAllByCuestionario_Id(idCuestionario)) {
            if (r.getOrdenRespuesta() == null || r.getOrdenRespuesta() == 0) {
                previas.put(r.getVariable().getId(), r);
            }
        }
        Map<String, String> contestados = codigosContestados(idCuestionario);

        List<RespuestaCuestionario> aGuardar = new ArrayList<>();
        Set<String> llavesTocadas = new HashSet<>();

        for (Cambio cambio : cambios) {
            VariableCuestionario variable = variables.get(cambio.idVariable());
            if (variable == null
                    || !variable.getTipoCuestionario().getId()
                        .equals(cuestionario.getTipoCuestionario().getId())) {
                invalidos.put(cambio.idVariable(), "La pregunta no pertenece a este cuestionario.");
                continue;
            }
            if (!variable.esContestable()) {
                invalidos.put(cambio.idVariable(), "Es un encabezado y no se contesta.");
                continue;
            }
            if (!Boolean.TRUE.equals(variable.getActiva())) {
                invalidos.put(cambio.idVariable(), "La pregunta ya no esta en uso.");
                continue;
            }
            if (!EvaluadorDependencias.habilitada(variable, contestados,
                    llavesDe(variable.getTipoCuestionario().getId()))) {
                invalidos.put(cambio.idVariable(),
                        "Esta pregunta no aplica segun lo que contesto antes.");
                continue;
            }

            RespuestaCuestionario fila = previas.get(cambio.idVariable());

            // El corazon del asunto: un cambio viejo no pisa uno nuevo. Ver la nota de
            // RespuestaCuestionario#secuenciaCliente.
            Long secuenciaNueva = cambio.secuencia();
            if (fila != null && fila.getSecuenciaCliente() != null && secuenciaNueva != null
                    && secuenciaNueva <= fila.getSecuenciaCliente()) {
                rechazados.add(cambio.idVariable());
                continue;
            }

            InterpreteCelda.Interpretacion i =
                    InterpreteCelda.interpretar(variable, cambio.valor());
            if (i.tieneProblema()) {
                invalidos.put(cambio.idVariable(), i.problema());
                continue;
            }

            if (fila == null) {
                fila = new RespuestaCuestionario();
                fila.setCuestionario(cuestionario);
                fila.setVariable(variable);
                fila.setOrdenRespuesta(0);
                previas.put(variable.getId(), fila);
            }
            fila.setMarcaAusencia(i.marca());
            fila.setValorCodigo(i.codigo());
            fila.setValorNumerico(i.numero());
            fila.setValorTexto(i.texto());
            fila.setValorFecha(i.fecha());
            fila.setValorCrudo(null);
            fila.setSecuenciaCliente(secuenciaNueva);
            fila.setFechaActualizacion(LocalDateTime.now());

            aGuardar.add(fila);
            aceptados.put(variable.getId(), secuenciaNueva);
            llavesTocadas.add(variable.getCodigo());
            if (i.marca() == MarcaAusencia.NINGUNA && i.codigo() != null) {
                contestados.put(variable.getCodigo(), i.codigo());
            } else {
                contestados.remove(variable.getCodigo());
            }
        }

        respuestaRepository.saveAll(aGuardar);
        limpiarDependientesDeshabilitadas(cuestionario, llavesTocadas, contestados);

        cuestionarioRepository.save(cuestionario);   // refresca fecha_actualizacion
        respuestaRepository.flush();

        // Solo las reglas de los bloques que acaba de tocar. Revisar las dos mil
        // preguntas en cada pulsación haría el autoguardado inservible.
        Set<String> bloquesTocados = new HashSet<>();
        for (RespuestaCuestionario r : aGuardar) {
            if (r.getVariable().getSeccion() != null) {
                bloquesTocados.add(r.getVariable().getSeccion().getNombre());
            }
        }

        return new ResultadoAutoguardado(aceptados, rechazados, invalidos,
                revisarReglas(cuestionario, bloquesTocados), LocalDateTime.now());
    }

    /**
     * Borra lo contestado en preguntas que acaban de dejar de aplicar.
     *
     * <p>Si alguien contesta «cuántos cigarros al día» y después cambia «¿fuma?» a no, esa
     * cantidad tiene que desaparecer: dejarla guardaría que fuma veinte al día alguien que
     * declaró no fumar, y ningún análisis lo detectaría. La pantalla ya no la muestra, así
     * que si no se borra aquí nadie volveria a verla.</p>
     *
     * <p>Solo mira las dependientes de las llaves que se acaban de tocar, no las dos mil
     * respuestas del cuestionario: el autoguardado dispara cada pocos segundos y recorrerlo
     * entero cada vez lo haria inservible.</p>
     */
    private void limpiarDependientesDeshabilitadas(
            Cuestionario cuestionario, Set<String> llavesTocadas, Map<String, String> contestados) {
        if (llavesTocadas.isEmpty()) return;

        List<VariableCuestionario> candidatas = variableRepository
                .findAllByTipoCuestionario_IdAndDependeDeIn(
                        cuestionario.getTipoCuestionario().getId(), llavesTocadas);
        if (candidatas.isEmpty()) return;

        List<RespuestaCuestionario> aBorrar = new ArrayList<>();
        Map<String, VariableCuestionario> llaves =
                llavesDe(cuestionario.getTipoCuestionario().getId());
        for (VariableCuestionario v : candidatas) {
            if (EvaluadorDependencias.habilitada(v, contestados, llaves)) continue;
            aBorrar.addAll(respuestaRepository
                    .findAllByCuestionario_IdAndVariable_Id(cuestionario.getId(), v.getId()));
        }
        if (!aBorrar.isEmpty()) respuestaRepository.deleteAll(aBorrar);
    }

    // ── Envio ────────────────────────────────────────────────────────────────

    /** Qué preguntas obligatorias le faltan, de las que sí le aplican. */
    public List<String> faltantes(Long idCuestionario) {
        Cuestionario c = mio(idCuestionario);
        Map<String, String> contestados = codigosContestados(idCuestionario);

        Set<Long> conValor = new HashSet<>();
        for (RespuestaCuestionario r : respuestaRepository.findAllByCuestionario_Id(idCuestionario)) {
            // Igual que en el avance: una casilla sin marcar ya esta contestada.
            if (r.cuentaComoContestada()) conValor.add(r.getVariable().getId());
        }

        Map<String, VariableCuestionario> llaves = llavesDe(c.getTipoCuestionario().getId());
        List<String> faltan = new ArrayList<>();
        for (VariableCuestionario v : variableRepository
                .findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(c.getTipoCuestionario().getId())) {
            if (!Boolean.TRUE.equals(v.getActiva())) continue;
            if (!Boolean.TRUE.equals(v.getObligatoria())) continue;
            if (v.getTipo() == TipoVariable.GRUPO) continue;
            if (conValor.contains(v.getId())) continue;
            if (!EvaluadorDependencias.habilitada(v, contestados, llaves)) continue;
            faltan.add(v.getCodigo());
        }
        return faltan;
    }

    /**
     * El participante da por terminado su cuestionario.
     *
     * <p>Lo deja en {@code COMPLETO}, nunca en {@code VALIDADO}: validar es hacerse
     * responsable de lo capturado frente a la hoja original, y eso le toca al personal. Un
     * participante no puede firmar la revisión de su propio cuestionario.</p>
     */
    @Transactional
    public Cuestionario enviar(Long idCuestionario) {
        Cuestionario c = mioParaContestar(idCuestionario);
        List<String> faltan = faltantes(idCuestionario);
        if (!faltan.isEmpty()) {
            throw new ObjConflictException("Faltan " + faltan.size()
                    + " respuesta(s) obligatoria(s) por contestar.");
        }
        // Aquí sí se para: después de enviar ya no puede corregir nada. Solo lo
        // marcado como ERROR; lo demás queda para que el personal lo mire.
        List<String> impiden = revisarReglas(c, null).stream()
                .filter(AvisoParaParticipante::impideEnviar)
                .map(AvisoParaParticipante::mensaje)
                .toList();
        if (!impiden.isEmpty()) {
            throw new ObjConflictException("Antes de enviarlo hay que revisar "
                    + impiden.size() + (impiden.size() == 1 ? " respuesta: " : " respuestas: ")
                    + String.join(" ", impiden.subList(0, Math.min(3, impiden.size()))));
        }

        c.setEstado(EstadoCuestionario.COMPLETO);
        return cuestionarioRepository.save(c);
    }


    // ── Lo que hay que advertirle ────────────────────────────────────────────

    /**
     * Un aviso de los que el participante tiene que ver, ya resuelto a id de pregunta.
     *
     * <p>El motor habla en códigos del catálogo —{@code P112_1}— y la pantalla trabaja
     * con ids. La traducción se hace aquí y no en el cliente: el participante no tiene
     * por qué recibir el catálogo entero para poder entender un aviso.</p>
     */
    public record AvisoParaParticipante(Long idVariable, String codigo,
                                        boolean impideEnviar, String mensaje) {}

    /**
     * Revisa las reglas del instrumento con lo que lleva contestado.
     *
     * <h3>Por qué se le avisa mientras contesta y no al final</h3>
     *
     * <p>Llevarlo de la mano es precisamente esto: si marca «Ninguna» y además una
     * actividad, enterarse en ese momento le cuesta un clic; enterarse al pulsar
     * «Terminar», después de cuarenta minutos, le obliga a volver a buscar dónde fue.
     * Y si no se entera nunca, el dato llega mal y nadie lo sabe.</p>
     *
     * @param soloBloques si no es null, solo se miran las reglas que tocan esos
     *                    bloques. Es lo que usa el autoguardado, que corre cada vez
     *                    que el participante escribe y no puede permitirse revisar
     *                    las dos mil preguntas en cada pulsación.
     */
    private List<AvisoParaParticipante> revisarReglas(Cuestionario cuestionario,
                                                      Set<String> soloBloques) {
        Long idTipo = cuestionario.getTipoCuestionario().getId();
        List<ReglaValidacion> reglas =
                reglaRepository.findAllByTipoCuestionario_IdAndActivaTrue(idTipo);
        if (reglas.isEmpty()) return List.of();

        Map<String, VariableCuestionario> porCodigo = new HashMap<>();
        for (VariableCuestionario v : variableRepository
                .findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(idTipo)) {
            porCodigo.put(v.getCodigo(), v);
        }

        if (soloBloques != null) {
            reglas = reglas.stream()
                    .filter(r -> tocaAlguno(r, soloBloques, porCodigo))
                    .toList();
            if (reglas.isEmpty()) return List.of();
        }

        List<AvisoParaParticipante> salida = new ArrayList<>();
        for (MotorValidaciones.Aviso aviso : MotorValidaciones.revisar(
                reglas, codigosContestados(cuestionario.getId()), porCodigo,
                CriterioAplicabilidad.evaluar(cuestionario.getTipoCuestionario(),
                        cuestionario.getPaciente(), null).edad())) {
            VariableCuestionario v = porCodigo.get(aviso.codigo());
            salida.add(new AvisoParaParticipante(v == null ? null : v.getId(),
                    aviso.codigo(), aviso.severidad() == Severidad.ERROR, aviso.mensaje()));
        }
        return salida;
    }

    /** Si la regla mira algo de alguno de esos bloques. */
    private boolean tocaAlguno(ReglaValidacion regla, Set<String> bloques,
                               Map<String, VariableCuestionario> porCodigo) {
        return enBloques(regla.getPregunta(), bloques, porCodigo)
                || enBloques(regla.getComparaCon(), bloques, porCodigo);
    }

    private boolean enBloques(String referencia, Set<String> bloques,
                              Map<String, VariableCuestionario> porCodigo) {
        if (referencia == null) return false;
        if (bloques.contains(referencia)) return true;   // la referencia ES un bloque
        VariableCuestionario v = porCodigo.get(referencia);
        return v != null && v.getSeccion() != null
                && bloques.contains(v.getSeccion().getNombre());
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private Map<String, String> codigosContestados(Long idCuestionario) {
        Map<String, String> mapa = new HashMap<>();
        for (RespuestaCuestionario r : respuestaRepository.findAllByCuestionario_Id(idCuestionario)) {
            if (r.getMarcaAusencia() != MarcaAusencia.NINGUNA) continue;
            mapa.putIfAbsent(r.getVariable().getCodigo(), r.getValorCodigo());
        }
        return mapa;
    }
}
