package imss.gob.mx.cohorte.audit.service;

import imss.gob.mx.cohorte.audit.events.AccesoAuditEvent;
import imss.gob.mx.cohorte.audit.events.AccionAuditEvent;
import imss.gob.mx.cohorte.audit.model.BitacoraAcceso;
import imss.gob.mx.cohorte.audit.model.BitacoraAcciones;
import imss.gob.mx.cohorte.audit.repository.BitacoraAccesoRepository;
import imss.gob.mx.cohorte.audit.repository.BitacoraAccionesRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Persiste los eventos de auditoría en las tablas correspondientes.
 * Usa REQUIRES_NEW para que el registro de auditoría nunca falle por un
 * rollback de la transacción de negocio y viceversa.
 */
@Service
@AllArgsConstructor
public class AuditService {

    private final BitacoraAccesoRepository  accesoRepository;
    private final BitacoraAccionesRepository accionesRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAcceso(AccesoAuditEvent event) {
        BitacoraAcceso ba = new BitacoraAcceso();
        ba.setUsuarioUuid(event.getUsuarioUuid());
        ba.setUsername(event.getUsername());
        ba.setNombreCompleto(event.getNombreCompleto());
        ba.setRol(event.getRol());
        ba.setIp(event.getIp());
        ba.setLatitud(event.getLatitud());
        ba.setLongitud(event.getLongitud());
        ba.setPrecisionM(event.getPrecisionM());
        ba.setTipoEvento(event.getTipoEvento());
        ba.setTimestamp(LocalDateTime.now());
        ba.setUserAgent(event.getUserAgent());
        ba.setDuracionSesionSeg(event.getDuracionSesionSeg());
        ba.setIdentificadorIntento(event.getIdentificadorIntento());
        accesoRepository.save(ba);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarAccion(AccionAuditEvent event) {
        BitacoraAcciones bac = new BitacoraAcciones();
        bac.setUsuarioUuid(event.getUsuarioUuid());
        bac.setUsername(event.getUsername());
        bac.setNombreCompleto(event.getNombreCompleto());
        bac.setRol(event.getRol());
        bac.setIp(event.getIp());
        bac.setEndpoint(event.getEndpoint());
        bac.setMetodoHttp(event.getMetodoHttp());
        bac.setTipoAccion(event.getTipoAccion());
        bac.setEntidadAfectada(event.getEntidadAfectada());
        bac.setValoresAnteriores(event.getValoresAnteriores());
        bac.setValoresNuevos(event.getValoresNuevos());
        bac.setSentenciaSql(event.getSentenciaSql());
        bac.setDuracionMs(event.getDuracionMs());
        bac.setTimestamp(LocalDateTime.now());
        bac.setExitoso(event.isExitoso());
        bac.setMensajeError(event.getMensajeError());
        accionesRepository.save(bac);
    }
}
