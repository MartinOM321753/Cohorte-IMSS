package imss.gob.mx.cohorte.audit.context;

import java.util.ArrayList;
import java.util.List;

/**
 * Contexto de auditoría para el hilo actual.
 * Almacena información del usuario y las sentencias SQL capturadas por p6spy
 * durante la ejecución de un método de ApplicationService.
 */
public class AuditContext {

    private String usuarioUuid;
    private String username;
    private String nombreCompleto;
    private String rol;
    private String ip;
    private String entityName;
    private String actionType;   // CREAR | ACTUALIZAR | ELIMINAR
    private String valoresNuevos;

    private final List<String> sqlStatements = new ArrayList<>();

    // ── Getters / Setters ────────────────────────────────────────────────────

    public String getUsuarioUuid()               { return usuarioUuid; }
    public void   setUsuarioUuid(String v)        { this.usuarioUuid = v; }

    public String getUsername()                  { return username; }
    public void   setUsername(String v)           { this.username = v; }

    public String getNombreCompleto()            { return nombreCompleto; }
    public void   setNombreCompleto(String v)     { this.nombreCompleto = v; }

    public String getRol()                       { return rol; }
    public void   setRol(String v)               { this.rol = v; }

    public String getIp()                        { return ip; }
    public void   setIp(String v)                { this.ip = v; }

    public String getEntityName()               { return entityName; }
    public void   setEntityName(String v)        { this.entityName = v; }

    public String getActionType()               { return actionType; }
    public void   setActionType(String v)        { this.actionType = v; }

    public String getValoresNuevos()            { return valoresNuevos; }
    public void   setValoresNuevos(String v)     { this.valoresNuevos = v; }

    public List<String> getSqlStatements()      { return sqlStatements; }

    public void addSql(String sql) {
        if (sql != null && !sql.isBlank()) {
            sqlStatements.add(sql);
        }
    }

    /** Devuelve todas las sentencias concatenadas con salto de línea. */
    public String getSqlConsolidado() {
        return String.join("\n", sqlStatements);
    }
}
