package imss.gob.mx.cohorte.services.solicitud;

import imss.gob.mx.cohorte.controllers.solicitud.dto.SolicitudAccesoDatosRequestDTO;
import imss.gob.mx.cohorte.infrastructure.email.EmailService;
import imss.gob.mx.cohorte.modules.solicitud.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolicitudAccesoDatosService {

    private final SolicitudAccesoDatosRepository solicitudRepo;
    private final CorreoDestinoSolicitudRepository correoDestinoRepo;
    private final EmailService emailService;

    private static final int MAX_REQUESTS_PER_HOUR = 3;

    public boolean excedeLimiteIP(String ip) {
        LocalDateTime hace1Hora = LocalDateTime.now().minusHours(1);
        return solicitudRepo.countByIpSolicitanteAndFechaSolicitudAfter(ip, hace1Hora) >= MAX_REQUESTS_PER_HOUR;
    }

    public SolicitudAccesoDatos registrar(SolicitudAccesoDatosRequestDTO dto, String ip) {
        SolicitudAccesoDatos solicitud = new SolicitudAccesoDatos();
        solicitud.setNombreCompleto(dto.getFullName().trim());
        solicitud.setOrganizacion(dto.getOrganization().trim());
        solicitud.setEmail(dto.getEmail().trim().toLowerCase());
        solicitud.setProposito(dto.getPurpose().trim());
        solicitud.setPropositoOtro(dto.getPurposeOther() != null ? dto.getPurposeOther().trim() : null);
        solicitud.setProyecto(dto.getProject().trim());
        solicitud.setInformacionSolicitada(dto.getRequestedInformation().trim());
        solicitud.setProductos(String.join(",", dto.getProducts()));
        solicitud.setProductoOtro(dto.getProductOther() != null ? dto.getProductOther().trim() : null);
        solicitud.setIpSolicitante(ip);

        solicitud = solicitudRepo.save(solicitud);

        enviarNotificaciones(solicitud);

        return solicitud;
    }

    public List<SolicitudAccesoDatos> listarTodas() {
        return solicitudRepo.findAll();
    }

    private void enviarNotificaciones(SolicitudAccesoDatos s) {
        List<CorreoDestinoSolicitud> destinos = correoDestinoRepo.findByActivoTrue();
        if (destinos.isEmpty()) {
            log.warn("No hay correos destino configurados para solicitudes de acceso a datos");
            return;
        }

        Context ctx = new Context();
        ctx.setVariable("nombreCompleto", s.getNombreCompleto());
        ctx.setVariable("organizacion", s.getOrganizacion());
        ctx.setVariable("email", s.getEmail());
        ctx.setVariable("proposito", "other".equals(s.getProposito()) ? s.getPropositoOtro() : s.getProposito());
        ctx.setVariable("proyecto", s.getProyecto());
        ctx.setVariable("informacionSolicitada", s.getInformacionSolicitada());
        ctx.setVariable("productos", s.getProductos());
        ctx.setVariable("productoOtro", s.getProductoOtro());
        ctx.setVariable("fechaSolicitud", s.getFechaSolicitud());
        ctx.setVariable("idSolicitud", s.getId());

        for (CorreoDestinoSolicitud destino : destinos) {
            try {
                emailService.enviar(
                        destino.getEmail(),
                        "Nueva solicitud de acceso a datos — " + s.getNombreCompleto(),
                        "email/solicitud-acceso-datos",
                        ctx
                );
            } catch (Exception e) {
                log.error("Error enviando notificación a {}: {}", destino.getEmail(), e.getMessage());
            }
        }
    }

    // ── Gestión de correos destino ──

    public List<CorreoDestinoSolicitud> listarCorreosDestino() {
        return correoDestinoRepo.findAll();
    }

    public CorreoDestinoSolicitud agregarCorreoDestino(String email, String nombre) {
        if (correoDestinoRepo.existsByEmailIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("El correo ya está registrado como destino");
        }
        CorreoDestinoSolicitud c = new CorreoDestinoSolicitud();
        c.setEmail(email.trim().toLowerCase());
        c.setNombre(nombre != null ? nombre.trim() : null);
        return correoDestinoRepo.save(c);
    }

    public void eliminarCorreoDestino(Long id) {
        correoDestinoRepo.deleteById(id);
    }

    public CorreoDestinoSolicitud toggleActivoCorreoDestino(Long id) {
        CorreoDestinoSolicitud c = correoDestinoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Correo destino no encontrado"));
        c.setActivo(!c.getActivo());
        return correoDestinoRepo.save(c);
    }

    public SolicitudAccesoDatos cambiarEstado(Long id, String estado) {
        SolicitudAccesoDatos s = solicitudRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Solicitud no encontrada"));
        s.setEstado(EstadoSolicitudDatos.valueOf(estado.toUpperCase()));
        return solicitudRepo.save(s);
    }
}
