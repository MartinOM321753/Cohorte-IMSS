package imss.gob.mx.cohorte.security.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionModuloRepository;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspecto de "puerta de módulo": intercepta los métodos anotados con
 * {@link RequireModulo} y verifica que la institución del usuario autenticado
 * tenga ese {@link ModuloSistema} habilitado (ver {@code InstitucionModulo}).
 *
 * <p>Si el módulo no está habilitado para la institución del usuario, lanza
 * {@link AccessDeniedException} (HTTP 403, manejado globalmente por
 * {@code GlobalExceptionHandler}) antes de ejecutar el método protegido —
 * complementa al aislamiento de datos realizado por
 * {@link InstitucionContextService}: primero se valida que la institución
 * tenga acceso al módulo, y luego (dentro del propio servicio) que el recurso
 * pertenezca a esa institución.</p>
 *
 * <p>Se ejecuta antes que el aspecto de auditoría
 * ({@code ApplicationServiceAuditAspect}, order = 1) para no registrar
 * intentos bloqueados como acciones ejecutadas.</p>
 */
@Aspect
@Component
@Order(0)
@RequiredArgsConstructor
public class ModuloAccessAspect {

    private final InstitucionContextService institucionContextService;
    private final InstitucionModuloRepository institucionModuloRepository;

    @Around("@annotation(imss.gob.mx.cohorte.security.institucion.RequireModulo) || @within(imss.gob.mx.cohorte.security.institucion.RequireModulo)")
    public Object validarAccesoModulo(ProceedingJoinPoint pjp) throws Throwable {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();

        RequireModulo anotacion = method.getAnnotation(RequireModulo.class);
        if (anotacion == null) {
            anotacion = method.getDeclaringClass().getAnnotation(RequireModulo.class);
        }
        if (anotacion == null) {
            // No debería ocurrir dado el pointcut, pero por seguridad dejamos pasar.
            return pjp.proceed();
        }

        ModuloSistema modulo = anotacion.value();
        Institucion institucion = institucionContextService.getInstitucionActual();

        boolean habilitado = institucionModuloRepository
                .existsByInstitucion_IdAndModuloAndHabilitadoTrue(institucion.getId(), modulo);

        if (!habilitado) {
            throw new AccessDeniedException(
                    "La institución '" + institucion.getNombre() + "' no tiene habilitado el módulo " + modulo.name());
        }

        return pjp.proceed();
    }
}
