package imss.gob.mx.cohorte.audit.aspect;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import imss.gob.mx.cohorte.audit.context.AuditContext;
import imss.gob.mx.cohorte.audit.context.AuditContextHolder;
import imss.gob.mx.cohorte.audit.events.AccionAuditEvent;
import imss.gob.mx.cohorte.audit.model.TipoAccion;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Aspecto de auditoría que intercepta todos los métodos mutantes de los
 * ApplicationService. Se ejecuta con orden 1 para envolver la transacción
 * (@Transactional tiene mayor order por defecto), garantizando que el SQL
 * capturado por p6spy ya esté en el ThreadLocal cuando se publica el evento.
 *
 * <p>Detecta automáticamente el tipo de acción por convención de nombre de método
 * y el nombre de entidad por convención de nombre de clase.</p>
 */
@Aspect
@Component
@Order(1)
public class ApplicationServiceAuditAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ApplicationServiceAuditAspect(ApplicationEventPublisher eventPublisher,
                                          UserRepository userRepository) {
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    }

    /**
     * Intercepta métodos mutantes en todos los ApplicationService.
     * Se detectan por convención de nombre (save, update, delete, toggle, etc.).
     */
    @Around("execution(* imss.gob.mx.cohorte.application..*(..))")
    public Object auditarAccion(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String methodName = method.getName().toLowerCase();

        // Solo auditar métodos mutantes
        TipoAccion tipoAccion = resolverTipoAccion(methodName);
        if (tipoAccion == null) {
            return pjp.proceed(); // lectura o método no reconocido → no auditar
        }

        // Sin usuario autenticado → no auditar (ej. schedulers, inicialización)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return pjp.proceed();
        }

        String uuid = auth.getName();
        String entityName = resolverEntidad(pjp.getTarget().getClass().getSimpleName());
        String ip = resolverIp();
        String endpoint = resolverEndpoint();
        String metodoHttp = resolverMetodoHttp();

        // Obtener datos del usuario (una sola consulta, sin @Lob lazy issues)
        BeanUser user = userRepository.findByUUID(uuid).orElse(null);
        String username = user != null ? user.getUsername() : uuid;
        String nombreCompleto = user != null ? buildNombre(user) : uuid;
        String rol = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("DESCONOCIDO");

        // Capturar argumentos como "valores anteriores" para UPDATE/ELIMINAR
        String valoresAnteriores = null;
        if (tipoAccion == TipoAccion.ACTUALIZAR || tipoAccion == TipoAccion.ELIMINAR) {
            valoresAnteriores = serializarArgs(pjp.getArgs());
        }

        // Establecer contexto de auditoría para p6spy
        AuditContext ctx = new AuditContext();
        ctx.setUsuarioUuid(uuid);
        ctx.setUsername(username);
        ctx.setNombreCompleto(nombreCompleto);
        ctx.setRol(rol);
        ctx.setIp(ip);
        ctx.setEntityName(entityName);
        ctx.setActionType(tipoAccion.name());
        AuditContextHolder.set(ctx);

        long inicio = System.currentTimeMillis();
        Object resultado = null;
        boolean exitoso = true;
        String mensajeError = null;

        try {
            resultado = pjp.proceed();

            // Capturar valores resultantes (para CREAR/ACTUALIZAR)
            if (tipoAccion != TipoAccion.ELIMINAR && resultado != null) {
                ctx.setValoresNuevos(serializarObjeto(resultado));
            }
            return resultado;

        } catch (Throwable ex) {
            exitoso = false;
            mensajeError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            throw ex;
        } finally {
            long duracionMs = System.currentTimeMillis() - inicio;
            String sqlConsolidado = ctx.getSqlConsolidado();
            String valoresNuevos = ctx.getValoresNuevos();

            // Limpiar ANTES de publicar para no corromper contexto si el listener es síncrono
            AuditContextHolder.clear();

            // Solo publicar si hubo SQL (la operación llegó a DB) o fue exitosa
            if (!sqlConsolidado.isBlank() || exitoso) {
                eventPublisher.publishEvent(new AccionAuditEvent(
                        this,
                        uuid, username, nombreCompleto, rol, ip,
                        endpoint, metodoHttp, tipoAccion, entityName,
                        valoresAnteriores, valoresNuevos, sqlConsolidado,
                        duracionMs, exitoso, mensajeError
                ));
            }
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private TipoAccion resolverTipoAccion(String methodName) {
        if (containsAny(methodName, "save", "create", "register", "add", "insertar", "crear")) {
            return TipoAccion.CREAR;
        }
        if (containsAny(methodName, "update", "edit", "modify", "change", "toggle",
                "setactivo", "cambiar", "actualizar", "modificar",
                "confirmar", "iniciar")) {
            return TipoAccion.ACTUALIZAR;
        }
        if (containsAny(methodName, "delete", "remove", "eliminar", "borrar")) {
            return TipoAccion.ELIMINAR;
        }
        return null; // lectura o método desconocido
    }

    private boolean containsAny(String str, String... tokens) {
        for (String t : tokens) {
            if (str.contains(t)) return true;
        }
        return false;
    }

    /** Extrae nombre de entidad de la clase: "PacienteApplicationService" → "Paciente". */
    private String resolverEntidad(String className) {
        return className.replace("ApplicationService", "").replace("Impl", "").trim();
    }

    /** Extrae la IP real del cliente (considera X-Forwarded-For de proxies). */
    private String resolverIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "SISTEMA";
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            String addr = request.getRemoteAddr();
            // Normalizar loopback IPv6 (::1) a IPv4 para consistencia en logs
            return "0:0:0:0:0:0:0:1".equals(addr) ? "127.0.0.1" : addr;
        } catch (Exception e) {
            return "DESCONOCIDO";
        }
    }

    private String resolverEndpoint() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "";
            return attrs.getRequest().getRequestURI();
        } catch (Exception e) {
            return "";
        }
    }

    private String resolverMetodoHttp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "";
            return attrs.getRequest().getMethod();
        } catch (Exception e) {
            return "";
        }
    }

    private String buildNombre(BeanUser user) {
        if (user.getPersona() == null) return user.getUsername();
        StringBuilder sb = new StringBuilder(user.getPersona().getNombre());
        sb.append(" ").append(user.getPersona().getApellidoPaterno());
        if (user.getPersona().getApellidoMaterno() != null &&
                !user.getPersona().getApellidoMaterno().isBlank()) {
            sb.append(" ").append(user.getPersona().getApellidoMaterno());
        }
        return sb.toString().trim();
    }

    private String serializarObjeto(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"no serializable\"}";
        }
    }

    private String serializarArgs(Object[] args) {
        if (args == null || args.length == 0) return null;
        // Si solo hay un arg, serializarlo directamente
        Object target = args.length == 1 ? args[0] : args;
        return serializarObjeto(target);
    }
}
