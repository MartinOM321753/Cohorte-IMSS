package imss.gob.mx.cohorte.services.documentos;

import imss.gob.mx.cohorte.modules.documentos.TipoEntidadDocumento;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * ╔══════════════════════════════════════════════════════════════════════════════╗
 * ║            MATRIZ DE PERMISOS DE DOCUMENTOS — EDITA AQUÍ                   ║
 * ╠══════════════════════════════════════════════════════════════════════════════╣
 * ║  Para dar acceso a un rol:  agrega la cadena exacta del rol en el Set.      ║
 * ║  Para quitarlo:             elimínala.                                      ║
 * ║                                                                              ║
 * ║  Los strings deben coincidir con el campo "role" en la tabla "rol" del DB.  ║
 * ║  Ejemplo: si la BD guarda "MEDICO", escribe "MEDICO" (sin prefijo ROLE_).   ║
 * ╚══════════════════════════════════════════════════════════════════════════════╝
 */
@Component
public class DocumentoPermisosConfig {

    // ─── ESTUDIOS MÉDICOS ───────────────────────────────────────────────────────
    /** Roles que pueden VER/DESCARGAR documentos de estudios médicos */
    private static final Set<String> VER_ESTUDIO = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "USER"          // alias por si el rol en BD es "USER"
    );

    /** Roles que pueden SUBIR documentos a estudios médicos */
    private static final Set<String> SUBIR_ESTUDIO = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "USER"
    );

    // ─── MUESTRAS BIOLÓGICAS (BIOBANCO) ─────────────────────────────────────────
    /** Solo personal de almacén/biobanco y admin pueden ver muestras */
    private static final Set<String> VER_MUESTRA = Set.of(
            "ADMINISTRADOR",
            "LABORATORISTA"
    );

    /** Solo personal de almacén/biobanco y admin pueden subir a muestras */
    private static final Set<String> SUBIR_MUESTRA = Set.of(
            "ADMINISTRADOR",
            "LABORATORISTA"
    );

    // ─── CONSENTIMIENTOS DE PACIENTE ────────────────────────────────────────────
    /** Consentimientos: médicos y admin (datos sensibles) */
    private static final Set<String> VER_CONSENTIMIENTO = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "USER"
    );

    private static final Set<String> SUBIR_CONSENTIMIENTO = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "USER"
    );

    // ─── DOCUMENTOS GENERALES DEL PACIENTE ─────────────────────────────────────
    /** Documentos generales del expediente */
    private static final Set<String> VER_PACIENTE_GENERAL = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "USER",
            "RECEPCIONISTA"
    );

    private static final Set<String> SUBIR_PACIENTE_GENERAL = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "USER",
            "RECEPCIONISTA"
    );

    // ─── CUESTIONARIOS DEL PACIENTE ─────────────────────────────────────────────
    private static final Set<String> VER_CUESTIONARIO = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "USER",
            "RECEPCIONISTA"
    );

    private static final Set<String> SUBIR_CUESTIONARIO = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "USER",
            "RECEPCIONISTA"
    );

    // ─── RESULTADOS DE EXÁMENES ───────────────────────────────────────────────
    private static final Set<String> VER_RESULTADO_EXAMEN = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "LABORATORISTA",
            "USER"
    );

    private static final Set<String> SUBIR_RESULTADO_EXAMEN = Set.of(
            "ADMINISTRADOR",
            "MEDICO",
            "LABORATORISTA",
            "USER"
    );

    // ─── ELIMINAR — aplica a TODOS los tipos ───────────────────────────────────
    /** Solo ADMINISTRADOR puede eliminar cualquier documento */
    private static final Set<String> ELIMINAR = Set.of(
            "ADMINISTRADOR"
    );

    // ═══════════════════════════════════════════════════════════════════════════
    //  API — no necesitas tocar lo que hay debajo para cambiar permisos
    // ═══════════════════════════════════════════════════════════════════════════

    public boolean puedeVer(String role, TipoEntidadDocumento tipo) {
        return switch (tipo) {
            case ESTUDIO                  -> VER_ESTUDIO.contains(role);
            case MUESTRA                  -> VER_MUESTRA.contains(role);
            case PACIENTE_CONSENTIMIENTO  -> VER_CONSENTIMIENTO.contains(role);
            case PACIENTE_GENERAL         -> VER_PACIENTE_GENERAL.contains(role);
            case PACIENTE_CUESTIONARIO    -> VER_CUESTIONARIO.contains(role);
            case RESULTADO_EXAMEN         -> VER_RESULTADO_EXAMEN.contains(role);
        };
    }

    public boolean puedeSubir(String role, TipoEntidadDocumento tipo) {
        return switch (tipo) {
            case ESTUDIO                  -> SUBIR_ESTUDIO.contains(role);
            case MUESTRA                  -> SUBIR_MUESTRA.contains(role);
            case PACIENTE_CONSENTIMIENTO  -> SUBIR_CONSENTIMIENTO.contains(role);
            case PACIENTE_GENERAL         -> SUBIR_PACIENTE_GENERAL.contains(role);
            case PACIENTE_CUESTIONARIO    -> SUBIR_CUESTIONARIO.contains(role);
            case RESULTADO_EXAMEN         -> SUBIR_RESULTADO_EXAMEN.contains(role);
        };
    }

    public boolean puedeEliminar(String role) {
        return ELIMINAR.contains(role);
    }
}
