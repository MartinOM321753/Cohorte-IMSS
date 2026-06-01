package imss.gob.mx.cohorte.audit.context;

/**
 * Holder de hilo (ThreadLocal) para el contexto de auditoría activo.
 * El aspecto establece el contexto antes de ejecutar el método de servicio
 * y lo limpia en el bloque finally.
 * p6spy lee el contexto durante la ejecución para capturar el SQL.
 */
public final class AuditContextHolder {

    private static final ThreadLocal<AuditContext> HOLDER = new ThreadLocal<>();

    private AuditContextHolder() {}

    public static void set(AuditContext context) {
        HOLDER.set(context);
    }

    public static AuditContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static boolean isActive() {
        return HOLDER.get() != null;
    }

    /** Agrega una sentencia SQL al contexto activo (si existe). */
    public static void addSql(String sql) {
        AuditContext ctx = HOLDER.get();
        if (ctx != null) {
            ctx.addSql(sql);
        }
    }
}
