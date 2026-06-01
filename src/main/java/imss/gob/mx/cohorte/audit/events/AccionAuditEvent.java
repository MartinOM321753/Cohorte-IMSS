package imss.gob.mx.cohorte.audit.events;

import imss.gob.mx.cohorte.audit.model.TipoAccion;
import org.springframework.context.ApplicationEvent;

/**
 * Evento publicado al completar un método mutante en un ApplicationService.
 * Contiene todo lo necesario para persistir un registro en {@code bitacora_acciones}.
 */
public class AccionAuditEvent extends ApplicationEvent {

    private final String    usuarioUuid;
    private final String    username;
    private final String    nombreCompleto;
    private final String    rol;
    private final String    ip;
    private final String    endpoint;
    private final String    metodoHttp;
    private final TipoAccion tipoAccion;
    private final String    entidadAfectada;
    private final String    valoresAnteriores;
    private final String    valoresNuevos;
    private final String    sentenciaSql;
    private final long      duracionMs;
    private final boolean   exitoso;
    private final String    mensajeError;

    public AccionAuditEvent(Object source,
                             String usuarioUuid,
                             String username,
                             String nombreCompleto,
                             String rol,
                             String ip,
                             String endpoint,
                             String metodoHttp,
                             TipoAccion tipoAccion,
                             String entidadAfectada,
                             String valoresAnteriores,
                             String valoresNuevos,
                             String sentenciaSql,
                             long duracionMs,
                             boolean exitoso,
                             String mensajeError) {
        super(source);
        this.usuarioUuid      = usuarioUuid;
        this.username         = username;
        this.nombreCompleto   = nombreCompleto;
        this.rol              = rol;
        this.ip               = ip;
        this.endpoint         = endpoint;
        this.metodoHttp       = metodoHttp;
        this.tipoAccion       = tipoAccion;
        this.entidadAfectada  = entidadAfectada;
        this.valoresAnteriores = valoresAnteriores;
        this.valoresNuevos    = valoresNuevos;
        this.sentenciaSql     = sentenciaSql;
        this.duracionMs       = duracionMs;
        this.exitoso          = exitoso;
        this.mensajeError     = mensajeError;
    }

    public String    getUsuarioUuid()      { return usuarioUuid; }
    public String    getUsername()         { return username; }
    public String    getNombreCompleto()   { return nombreCompleto; }
    public String    getRol()             { return rol; }
    public String    getIp()              { return ip; }
    public String    getEndpoint()        { return endpoint; }
    public String    getMetodoHttp()      { return metodoHttp; }
    public TipoAccion getTipoAccion()     { return tipoAccion; }
    public String    getEntidadAfectada() { return entidadAfectada; }
    public String    getValoresAnteriores() { return valoresAnteriores; }
    public String    getValoresNuevos()   { return valoresNuevos; }
    public String    getSentenciaSql()    { return sentenciaSql; }
    public long      getDuracionMs()      { return duracionMs; }
    public boolean   isExitoso()         { return exitoso; }
    public String    getMensajeError()    { return mensajeError; }
}
