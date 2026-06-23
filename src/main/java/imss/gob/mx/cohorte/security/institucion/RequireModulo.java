package imss.gob.mx.cohorte.security.institucion;

import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Exige que la institución del usuario autenticado tenga habilitado el
 * {@link ModuloSistema} indicado (ver {@code InstitucionModulo}) para poder
 * ejecutar el método anotado.
 *
 * <p>Úsese sobre métodos públicos de ApplicationService o Controller que
 * representen operaciones de un módulo cuyo acceso puede otorgarse/revocarse
 * por institución (BIOBANCO, EXAMENES, ESTUDIOS_MEDICOS, CITAS, SOMATOMETRIA,
 * DOCUMENTOS). El aspecto {@link ModuloAccessAspect} intercepta estos métodos
 * y lanza {@code AccessDeniedException} (HTTP 403) si el módulo no está
 * habilitado para la institución del usuario.</p>
 *
 * <p>Ejemplo:</p>
 * <pre>
 *   {@code @RequireModulo(ModuloSistema.EXAMENES)}
 *   public Examen create(Examen examen) { ... }
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface RequireModulo {
    ModuloSistema value();
}
