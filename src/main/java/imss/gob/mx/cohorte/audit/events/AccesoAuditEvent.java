package imss.gob.mx.cohorte.audit.events;

import imss.gob.mx.cohorte.audit.model.TipoEventoAcceso;
import org.springframework.context.ApplicationEvent;

/**
 * Evento publicado cuando un usuario inicia sesión, cierra sesión o falla al autenticarse.
 * Es procesado de forma asíncrona por {@link imss.gob.mx.cohorte.audit.listener.AuditEventListener}.
 */
public class AccesoAuditEvent extends ApplicationEvent {

    private final String usuarioUuid;
    private final String username;
    private final String nombreCompleto;
    private final String rol;
    private final String ip;
    private final Double latitud;
    private final Double longitud;
    /** Margen de error en metros (GeolocationCoordinates.accuracy del navegador). */
    private final Integer precisionM;
    private final TipoEventoAcceso tipoEvento;
    private final String userAgent;
    private final Integer duracionSesionSeg;
    /** Solo para LOGIN_FALLIDO: identificador que usó el usuario. */
    private final String identificadorIntento;

    public AccesoAuditEvent(Object source,
                             String usuarioUuid,
                             String username,
                             String nombreCompleto,
                             String rol,
                             String ip,
                             Double latitud,
                             Double longitud,
                             Integer precisionM,
                             TipoEventoAcceso tipoEvento,
                             String userAgent,
                             Integer duracionSesionSeg,
                             String identificadorIntento) {
        super(source);
        this.usuarioUuid          = usuarioUuid;
        this.username             = username;
        this.nombreCompleto       = nombreCompleto;
        this.rol                  = rol;
        this.ip                   = ip;
        this.latitud              = latitud;
        this.longitud             = longitud;
        this.precisionM           = precisionM;
        this.tipoEvento           = tipoEvento;
        this.userAgent            = userAgent;
        this.duracionSesionSeg    = duracionSesionSeg;
        this.identificadorIntento = identificadorIntento;
    }

    public String           getUsuarioUuid()          { return usuarioUuid; }
    public String           getUsername()             { return username; }
    public String           getNombreCompleto()       { return nombreCompleto; }
    public String           getRol()                  { return rol; }
    public String           getIp()                   { return ip; }
    public Double           getLatitud()              { return latitud; }
    public Double           getLongitud()             { return longitud; }
    public Integer          getPrecisionM()           { return precisionM; }
    public TipoEventoAcceso getTipoEvento()           { return tipoEvento; }
    public String           getUserAgent()            { return userAgent; }
    public Integer          getDuracionSesionSeg()    { return duracionSesionSeg; }
    public String           getIdentificadorIntento() { return identificadorIntento; }
}
