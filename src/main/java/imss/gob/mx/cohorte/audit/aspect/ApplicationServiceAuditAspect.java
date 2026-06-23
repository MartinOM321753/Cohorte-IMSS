package imss.gob.mx.cohorte.audit.aspect;

import imss.gob.mx.cohorte.audit.context.AuditContext;
import imss.gob.mx.cohorte.audit.context.AuditContextHolder;
import imss.gob.mx.cohorte.audit.events.AccionAuditEvent;
import imss.gob.mx.cohorte.audit.model.TipoAccion;
import imss.gob.mx.cohorte.audit.serialization.AuditSerializer;
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

@Aspect
@Component
@Order(1)
public class ApplicationServiceAuditAspect {

    private final ApplicationEventPublisher eventPublisher;
    private final UserRepository userRepository;
    private final AuditSerializer auditSerializer;

    public ApplicationServiceAuditAspect(ApplicationEventPublisher eventPublisher,
                                          UserRepository userRepository,
                                          AuditSerializer auditSerializer) {
        this.eventPublisher = eventPublisher;
        this.userRepository = userRepository;
        this.auditSerializer = auditSerializer;
    }

    @Around("execution(* imss.gob.mx.cohorte.application..*(..))")
    public Object auditarAccion(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        String methodName = method.getName().toLowerCase();

        TipoAccion tipoAccion = resolverTipoAccion(methodName);
        if (tipoAccion == null) {
            return pjp.proceed();
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) {
            return pjp.proceed();
        }

        String uuid = auth.getName();
        String entityName = resolverEntidad(pjp.getTarget().getClass().getSimpleName());
        String ip = resolverIp();
        String endpoint = resolverEndpoint();
        String metodoHttp = resolverMetodoHttp();

        BeanUser user = userRepository.findByUUID(uuid).orElse(null);
        String username = user != null ? user.getUsername() : uuid;
        String nombreCompleto = user != null ? buildNombre(user) : uuid;
        String rol = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("DESCONOCIDO");

        String valoresAnteriores = null;
        if (tipoAccion == TipoAccion.ACTUALIZAR || tipoAccion == TipoAccion.ELIMINAR) {
            valoresAnteriores = auditSerializer.serializeArgs(pjp.getArgs());
        }

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

            if (tipoAccion != TipoAccion.ELIMINAR && resultado != null) {
                ctx.setValoresNuevos(auditSerializer.serialize(resultado));
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

            AuditContextHolder.clear();

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
        return null;
    }

    private boolean containsAny(String str, String... tokens) {
        for (String t : tokens) {
            if (str.contains(t)) return true;
        }
        return false;
    }

    private String resolverEntidad(String className) {
        return className.replace("ApplicationService", "").replace("Impl", "").trim();
    }

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
}
