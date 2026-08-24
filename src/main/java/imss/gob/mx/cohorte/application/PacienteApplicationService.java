package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.controllers.reclutamiento.dto.ReclutamientoParticipanteRequestDTO;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.permisos.UsuarioRol;
import imss.gob.mx.cohorte.modules.permisos.UsuarioRolRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.reclutamiento.ReclutamientoParticipante;
import imss.gob.mx.cohorte.modules.usuarios.role.Role;
import imss.gob.mx.cohorte.modules.usuarios.role.RoleRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.Personas.PersonaService;
import imss.gob.mx.cohorte.services.auth.PasswordResetService;
import imss.gob.mx.cohorte.services.pacientes.ImportacionParticipantesAsyncService;
import imss.gob.mx.cohorte.services.institucion.InstitucionJerarquiaService;
import imss.gob.mx.cohorte.services.institucion.InstitucionRegistroService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;
import imss.gob.mx.cohorte.services.pacientes.ParticipanteTitularidadService;
import imss.gob.mx.cohorte.services.reclutamiento.ReclutamientoParticipanteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.CredentialGenerator;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import org.springframework.web.multipart.MultipartFile;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.PARTICIPANTES)
public class PacienteApplicationService {

    private final PacienteService pacienteService;
    private final PersonaService personaService;
    private final ReclutamientoParticipanteService reclutamientoService;
    private final ImportacionParticipantesAsyncService importacionParticipantesAsyncService;
    private final InstitucionContextService institucionContextService;
    private final InstitucionJerarquiaService institucionJerarquiaService;
    private final InstitucionRegistroService institucionRegistroService;
    private final ParticipanteTitularidadService participanteTitularidadService;
    private final ParticipanteAccesoService participanteAccesoService;
    private final UserRepository userRepository;
    private final UserService userService;
    private final RoleRepository roleRepository;
    private final UsuarioRolRepository usuarioRolRepository;
    private final PasswordResetService passwordResetService;

    @Transactional(readOnly = true)
    public List<Paciente> getAll() {
        return pacienteService.findAllByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public List<Paciente> getAllConJerarquia() {
        List<Long> ids = institucionJerarquiaService.getInstitucionesVisibles(
                institucionContextService.getIdInstitucionActual());
        return pacienteService.findAllByInstituciones(ids);
    }

    @Transactional(readOnly = true)
    public Page<Paciente> getAllPaginado(Pageable pageable) {
        return pacienteService.findAllPaginadoByInstitucion(institucionContextService.getIdInstitucionActual(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Paciente> getAllPaginadoConJerarquia(Pageable pageable) {
        List<Long> ids = institucionJerarquiaService.getInstitucionesVisibles(
                institucionContextService.getIdInstitucionActual());
        return pacienteService.findAllPaginadoByInstituciones(ids, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Paciente> buscarPaginado(String buscar, Boolean soloActivos, Pageable pageable) {
        return pacienteService.buscarPaginado(institucionContextService.getIdInstitucionActual(), buscar, soloActivos, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Paciente> buscarPaginadoConJerarquia(String buscar, Boolean soloActivos, Long idInstitucionFiltro, Pageable pageable) {
        List<Long> ids = institucionJerarquiaService.getInstitucionesVisibles(
                institucionContextService.getIdInstitucionActual());
        if (idInstitucionFiltro != null) {
            if (!ids.contains(idInstitucionFiltro)) {
                throw new org.springframework.security.access.AccessDeniedException(
                        "La institución solicitada no es visible para tu institución actual");
            }
            return pacienteService.buscarPaginado(idInstitucionFiltro, buscar, soloActivos, pageable);
        }
        return pacienteService.buscarPaginadoEnInstituciones(ids, buscar, soloActivos, pageable);
    }

    public Long getIdInstitucionActual() {
        return institucionContextService.getIdInstitucionActual();
    }

    @Transactional(readOnly = true)
    public List<Paciente> getActivos() {
        return pacienteService.findAllStatusByInstitucion(true, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public List<Paciente> findAll(Paciente paciente) {
        return pacienteService.findAllByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public List<Paciente> findAllActive(Paciente paciente) {
        return pacienteService.findAllStatusByInstitucion(true, institucionContextService.getIdInstitucionActual());
    }

    public List<Paciente> findAllInactive(Paciente paciente) {
        return pacienteService.findAllStatusByInstitucion(false, institucionContextService.getIdInstitucionActual());
    }

    // Estas dos abren la ficha de un participante. Van por el conjunto alcanzable y
    // no por la institución propia: los listados ya mostraban participantes de la
    // jerarquía, así que abrir uno de ellos respondía "no se encontró".
    @Transactional
    public Paciente findUser(Long id) {
        return participanteAccesoService.resolverPorId(id);
    }

    @Transactional
    public Paciente findByUUID(String uuid) {
        return participanteAccesoService.resolver(uuid);
    }

    @Transactional
    public Paciente findByFolio(String folio) {
        return pacienteService.getByFolio(folio, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public Paciente saveUser(Paciente paciente) {
        return saveUser(paciente, null);
    }

    /**
     * Registra un participante, opcionalmente a nombre de otra institución del
     * mismo grupo.
     *
     * <p>La institución nunca se toma tal cual del cliente: {@code idInstitucionSolicitada}
     * es una petición que se valida contra el conjunto que
     * {@code InstitucionRegistroService} calcula en el servidor. Si se omite, o si
     * la sede no tiene autorización para registrar fuera de sí misma, queda la del
     * usuario autenticado.</p>
     */
    @Transactional
    public Paciente saveUser(Paciente paciente, Long idInstitucionSolicitada) {
        Persona savePersona = personaService.createPerson(paciente.getPersona());
        paciente.setPersona(savePersona);
        Institucion destino = institucionRegistroService.resolverInstitucionDestino(
                institucionContextService.getIdInstitucionActual(), idInstitucionSolicitada);
        paciente.setInstitucion(destino);
        return pacienteService.cretePatient(paciente);
    }

    /**
     * Registra al participante junto con su clasificación de reclutamiento
     * (origen RETORNO/NUEVO, institución y medio de contacto). Ambas entidades
     * se persisten en la misma transacción para mantener la integridad 1:1.
     *
     * @param uuidUsuarioAutenticado UUID del usuario en sesión — se usa como reclutador
     *                               por defecto cuando el DTO no especifica uno explícito.
     */
    @Transactional
    public Paciente saveUserConReclutamiento(Paciente paciente, ReclutamientoParticipanteRequestDTO reclutamientoDto, String uuidUsuarioAutenticado) {
        return saveUserConReclutamiento(paciente, reclutamientoDto, uuidUsuarioAutenticado, null);
    }

    @Transactional
    public Paciente saveUserConReclutamiento(Paciente paciente, ReclutamientoParticipanteRequestDTO reclutamientoDto,
                                             String uuidUsuarioAutenticado, Long idInstitucionSolicitada) {
        Paciente saved = saveUser(paciente, idInstitucionSolicitada);

        String uuidRecluta = (reclutamientoDto.getUuidUsuarioRecluta() != null && !reclutamientoDto.getUuidUsuarioRecluta().isBlank())
                ? reclutamientoDto.getUuidUsuarioRecluta()
                : uuidUsuarioAutenticado;

        reclutamientoService.create(
                saved,
                reclutamientoDto.getTipoReclutamiento(),
                reclutamientoDto.getEstadoContacto(),
                reclutamientoDto.getMedioContacto(),
                institucionContextService.getIdInstitucionActual(),
                uuidRecluta,
                reclutamientoDto.getObservaciones(),
                reclutamientoDto.getFechaContacto() != null
                        ? java.sql.Timestamp.valueOf(reclutamientoDto.getFechaContacto())
                        : null
        );

        return saved;
    }

    /** Obtiene la clasificación de reclutamiento (1:1) asociada a un paciente, si existe. */
    @Transactional(readOnly = true)
    public ReclutamientoParticipante getReclutamiento(Long idPaciente) {
        // El reclutamiento es del participante, así que se lee con las mismas reglas
        // que él. Consultarlo directamente por id se saltaba esa comprobación.
        participanteAccesoService.resolverPorId(idPaciente);
        return reclutamientoService.findByPaciente(idPaciente).orElse(null);
    }

    /**
     * Resuelve el expediente del participante vinculado al usuario autenticado
     * (rol PACIENTE). Nunca acepta un UUID de entrada — siempre resuelve "el
     * propio" a partir de la Persona vinculada al BeanUser de la sesión, para
     * que un participante no pueda consultar el expediente de otro.
     */
    @Transactional(readOnly = true)
    public Paciente obtenerPacientePropio() {
        BeanUser usuario = institucionContextService.getUsuarioActual();
        if (usuario.getPersona() == null) {
            throw new ObjNotFoundException("Tu cuenta no tiene un participante vinculado");
        }
        return pacienteService.getByPersonaId(usuario.getPersona().getId());
    }

    /**
     * Reutilizada por los endpoints de expediente (paciente, citas, estudios,
     * exámenes, documentos) que un usuario con rol PACIENTE puede consultar en
     * modo lectura vía la misma página del staff (Expediente 360). Si el
     * usuario NO tiene rol PACIENTE, no hace nada (comportamiento de staff sin
     * cambios — el aislamiento por institución ya se aplica en cada servicio).
     * Si SÍ tiene rol PACIENTE, exige que el UUID solicitado sea el suyo.
     */
    @Transactional(readOnly = true)
    public void verificarAccesoPropioSiEsPaciente(String uuidSolicitado) {
        BeanUser usuario = institucionContextService.getUsuarioActual();
        boolean esPaciente = usuarioRolRepository.findAllByUsuario(usuario).stream()
                .anyMatch(ur -> "PACIENTE".equals(ur.getRol().getRole()));
        if (!esPaciente) {
            return;
        }
        Paciente propio = obtenerPacientePropio();
        if (!propio.getUuid().equals(uuidSolicitado)) {
            throw new AccessDeniedException("No tienes acceso a este expediente");
        }
    }

    /**
     * Actualiza un participante. Se busca dentro de las instituciones visibles y no
     * solo en la propia, porque con el registro cruzado un participante puede
     * pertenecer a otra sede del grupo. Su institución no se toca: cambiarla dejaría
     * su historial —estudios, muestras, citas, documentos— en la sede anterior.
     */
    @Transactional
    public Paciente updateUser(Paciente paciente) {
        List<Long> visibles = institucionJerarquiaService.getInstitucionesVisibles(
                institucionContextService.getIdInstitucionActual());
        Paciente existing = pacienteService.getPatient(paciente.getId(), visibles);
        paciente.getPersona().setId(existing.getPersona().getId());
        Persona updatePersona = personaService.update(paciente.getPersona());
        paciente.setPersona(updatePersona);
        return pacienteService.updatePatient(paciente, visibles);
    }

    // ─── Cambio de institución del participante ──────────────────────────────

    /**
     * Vínculos que impiden mover al participante. Lista vacía significa que sí se
     * puede. Se consulta antes de ofrecer el cambio en pantalla; la decisión real
     * se vuelve a tomar dentro de {@link #cambiarInstitucion}.
     */
    @Transactional(readOnly = true)
    public List<ParticipanteTitularidadService.Vinculo> vinculosQueImpidenCambio(String uuidPaciente) {
        // Resolver el participante primero valida que el usuario alcance a verlo.
        pacienteService.getByUUID(uuidPaciente, getInstitucionesVisibles());
        return participanteTitularidadService.vinculosQueImpidenCambio(uuidPaciente);
    }

    /**
     * Cambia la institución dueña de un participante.
     *
     * <p>Solo procede mientras nada lo ate a su institución actual. La comprobación
     * se repite aquí dentro y no basta con la consulta previa de la pantalla: entre
     * una y otra alguien pudo registrarle un estudio.</p>
     *
     * <p>La cuenta de acceso del participante, si existe, se mueve con él: su
     * institución la copió de aquí cuando se creó y dejarla atrás la desincronizaría.
     * El reclutamiento no se toca — registra quién lo contactó, y eso no cambia.</p>
     */
    @Transactional
    public Paciente cambiarInstitucion(String uuidPaciente, Long idInstitucionDestino) {
        Paciente paciente = pacienteService.getByUUID(uuidPaciente, getInstitucionesVisibles());
        Institucion destino = institucionRegistroService.resolverInstitucionDestino(
                institucionContextService.getIdInstitucionActual(), idInstitucionDestino);

        List<ParticipanteTitularidadService.Vinculo> vinculos =
                participanteTitularidadService.vinculosQueImpidenCambio(uuidPaciente);
        if (!vinculos.isEmpty()) {
            throw new ValidationException(
                    "No se puede cambiar la institución: el participante ya tiene "
                            + participanteTitularidadService.describir(vinculos)
                            + " registrados en su institución actual.");
        }

        return aplicarCambioInstitucion(paciente, destino);
    }

    /** Texto legible de los vínculos, para el mensaje que ve el usuario. */
    public String describirVinculos(List<ParticipanteTitularidadService.Vinculo> vinculos) {
        return participanteTitularidadService.describir(vinculos);
    }

    /** Resultado de mover un participante dentro de una reasignación en lote. */
    public record ResultadoReasignacion(String uuid, String folio, boolean movido, String motivo) {}

    /**
     * Reasignación en lote, para redistribuir lo que la importación masiva dejó
     * todo a nombre de una sola sede.
     *
     * <p>Cada participante se resuelve por separado y sin excepciones: que uno tenga
     * un estudio no debe abortar el resto ni marcar la transacción para deshacerse.
     * El destino sí se valida una sola vez al principio — es el mismo para todos, y
     * si no está autorizado no hay nada que procesar.</p>
     */
    @Transactional
    public List<ResultadoReasignacion> reasignarInstitucion(List<String> uuids, Long idInstitucionDestino) {
        Institucion destino = institucionRegistroService.resolverInstitucionDestino(
                institucionContextService.getIdInstitucionActual(), idInstitucionDestino);
        List<Long> visibles = getInstitucionesVisibles();

        List<ResultadoReasignacion> resultados = new ArrayList<>();

        for (String uuid : uuids) {
            Paciente paciente = pacienteService.buscarPorUUID(uuid, visibles).orElse(null);
            if (paciente == null) {
                resultados.add(new ResultadoReasignacion(uuid, null, false,
                        "No se encontró el participante o no pertenece a una institución que puedas ver"));
                continue;
            }

            String folio = paciente.getFolio();

            if (destino.getId().equals(paciente.getInstitucion().getId())) {
                resultados.add(new ResultadoReasignacion(uuid, folio, false,
                        "Ya pertenece a la institución destino"));
                continue;
            }

            List<ParticipanteTitularidadService.Vinculo> vinculos =
                    participanteTitularidadService.vinculosQueImpidenCambio(uuid);
            if (!vinculos.isEmpty()) {
                resultados.add(new ResultadoReasignacion(uuid, folio, false,
                        "Tiene " + participanteTitularidadService.describir(vinculos)));
                continue;
            }

            aplicarCambioInstitucion(paciente, destino);
            resultados.add(new ResultadoReasignacion(uuid, folio, true, null));
        }

        return resultados;
    }

    /**
     * Escribe el cambio. La cuenta de acceso del participante, si existe, se mueve
     * con él: su institución la copió de aquí cuando se creó y dejarla atrás la
     * desincronizaría. El reclutamiento no se toca — registra quién lo contactó, y
     * eso no cambia porque cambie de sede.
     */
    private Paciente aplicarCambioInstitucion(Paciente paciente, Institucion destino) {
        paciente.setInstitucion(destino);
        paciente.setFechaActualizacion(LocalDateTime.now());
        Paciente guardado = pacienteService.guardar(paciente);

        if (paciente.getPersona() != null) {
            userRepository.findByPersona_Id(paciente.getPersona().getId()).ifPresent(cuenta -> {
                cuenta.setInstitucion(destino);
                userRepository.save(cuenta);
            });
        }

        return guardado;
    }

    // ─── Participantes que ya no se gestionan pero conservan registros propios ──

    /**
     * Participantes fuera del alcance actual de la institución de los que, sin
     * embargo, conserva registros. Aparecen en la búsqueda marcados como «ya no los
     * gestionas»: se les puede consultar lo que esta sede registró, nada más.
     */
    @Transactional(readOnly = true)
    public List<Paciente> getParticipantesConRegistrosPropios() {
        return pacienteService.buscarConRegistrosDeInstitucion(
                institucionContextService.getIdInstitucionActual(),
                getInstitucionesVisibles());
    }

    /**
     * Resuelve un participante para consulta histórica: no está al alcance, pero
     * esta institución conserva registros suyos.
     *
     * <p>No abre el expediente. Solo confirma que hay algo propio que mostrar; los
     * registros se piden después, cada uno filtrado por la institución. Si no
     * hubiera ninguno, no habría nada que ver y se rechaza.</p>
     */
    @Transactional(readOnly = true)
    public Paciente resolverParaConsultaHistorica(String uuid) {
        return getParticipantesConRegistrosPropios().stream()
                .filter(p -> p.getUuid().equals(uuid))
                .findFirst()
                .orElseThrow(() -> new AccessDeniedException(
                        "No gestionas a este participante y no conservas registros suyos"));
    }

    /**
     * Identidad del participante para una pantalla de consulta: quién es y si ya no
     * se le gestiona.
     *
     * <p>Va aparte de {@link #findByUUID} a propósito. Aquélla abre el expediente y
     * sigue exigiendo alcance sobre el participante; ésta solo resuelve el nombre y
     * el folio con los que rotular un historial que la institución ya tiene derecho
     * a ver. Mezclarlas convertiría un registro suelto en la llave del expediente
     * completo de una sede ajena.</p>
     */
    @Transactional(readOnly = true)
    public ParticipanteAccesoService.AccesoLectura resolverParaLectura(String uuid) {
        return participanteAccesoService.resolverParaLectura(uuid);
    }

    /** Instituciones a las que el usuario actual puede asignar un participante nuevo. */
    @Transactional(readOnly = true)
    public List<Institucion> getInstitucionesParaRegistro() {
        return institucionRegistroService.getInstitucionesParaRegistroDetalle(
                institucionContextService.getIdInstitucionActual());
    }

    /** Instituciones cuyos participantes el usuario actual alcanza a ver. */
    @Transactional(readOnly = true)
    public List<Long> getInstitucionesVisibles() {
        return institucionJerarquiaService.getInstitucionesVisibles(
                institucionContextService.getIdInstitucionActual());
    }

    /** Alterna el estado activo/inactivo del paciente. Devuelve el paciente actualizado. */
    @Transactional
    public Paciente toggleActivo(String uuid) {
        return pacienteService.toggleActivo(uuid, institucionContextService.getIdInstitucionActual());
    }

    @Transactional
    public Paciente crearAccesoPaciente(String uuid) {
        Paciente paciente = participanteAccesoService.resolver(uuid);

        if (!Boolean.TRUE.equals(paciente.getActivo())) {
            throw new ValidationException("Solo se puede crear acceso para participantes activos");
        }

        Persona persona = paciente.getPersona();
        if (persona == null) {
            throw new ValidationException("El participante no tiene datos de persona asociados");
        }
        if (persona.getEmail() == null || persona.getEmail().isBlank()) {
            throw new ValidationException("El participante debe tener un correo electrónico registrado para crear acceso");
        }

        if (userRepository.existsByPersona_Id(persona.getId())) {
            throw new ObjConflictException("El participante ya cuenta con una cuenta de acceso");
        }

        Role rolPaciente = roleRepository.findByRole("PACIENTE")
                .orElseThrow(() -> new ObjNotFoundException("El rol PACIENTE no existe en el sistema"));

        String username = persona.getEmail().trim().toLowerCase();

        String rawPassword = CredentialGenerator.generarPasswordSeguro();

        BeanUser nuevaCuenta = new BeanUser();
        nuevaCuenta.setUsername(username);
        nuevaCuenta.setPersona(persona);
        nuevaCuenta.setPassword(rawPassword);
        nuevaCuenta.setInstitucion(paciente.getInstitucion());
        nuevaCuenta.setDebeResetear(true);

        BeanUser saved = userService.save(nuevaCuenta);

        UsuarioRol ur = new UsuarioRol();
        ur.setUsuario(saved);
        ur.setRol(rolPaciente);
        ur.setFechaAsignacion(LocalDateTime.now());
        usuarioRolRepository.save(ur);

        passwordResetService.enviarInvitacion(saved);

        return paciente;
    }

    /**
     * Dispara la importación masiva en segundo plano (ver
     * ImportacionParticipantesAsyncService) y regresa de inmediato. El archivo
     * se lee a memoria aquí mismo porque el MultipartFile ya no es válido una
     * vez que termina la petición HTTP. Al concluir, se notifica por correo al
     * usuario que inició la carga.
     */
    public void importarPacientesAsync(MultipartFile archivo) {
        Institucion institucionActual = institucionContextService.getInstitucionActual();
        BeanUser usuarioActual = institucionContextService.getUsuarioActual();
        Persona persona = usuarioActual.getPersona();
        String email = persona != null ? persona.getEmail() : null;
        String nombre = persona != null ? persona.getNombre() : usuarioActual.getUsername();

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer el archivo: " + e.getMessage(), e);
        }

        importacionParticipantesAsyncService.procesarYNotificar(
                contenido, archivo.getOriginalFilename(), institucionActual, email, nombre);
    }
}
