package imss.gob.mx.cohorte.application.almacenamiento;

import imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega.DestinatarioEntregaDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.Colaborador;
import imss.gob.mx.cohorte.modules.almacenamiento.entrega.EntregaMuestra;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.almacenamiento.colaborador.ColaboradorService;
import imss.gob.mx.cohorte.services.almacenamiento.entrega.EntregaMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.MuestraService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@RequireModulo(ModuloSistema.BIOBANCO)
public class EntregaMuestraApplicationService {

    private final EntregaMuestraService entregaService;
    private final MuestraService muestraService;
    private final ColaboradorService colaboradorService;
    private final UserService userService;
    private final InstitucionContextService institucionContextService;

    /**
     * Destinatarios posibles de una entrega en mi institución: colaboradores
     * activos y usuarios del sistema (que también pueden necesitar muestras).
     * Unifica ambas fuentes para que el formulario las ofrezca en un solo
     * selector sin exigir el permiso de administración de usuarios.
     */
    @Transactional(readOnly = true)
    public List<DestinatarioEntregaDTO> getDestinatarios() {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        List<DestinatarioEntregaDTO> out = new ArrayList<>();

        for (Colaborador c : colaboradorService.getActivosByInstitucion(idInstitucion)) {
            out.add(DestinatarioEntregaDTO.builder()
                    .tipo("COLABORADOR")
                    .id(c.getId())
                    .nombreCompleto(nombrePersona(c.getPersona()))
                    .detalle(c.getCargo())
                    .esExterno(Boolean.TRUE.equals(c.getEsExterno()))
                    .build());
        }
        // Usuarios activos de la institución, excluyendo cuentas de participante.
        for (BeanUser u : userService.getAllByInstitucionConInvitacionesPendientes(idInstitucion)) {
            if (!Boolean.TRUE.equals(u.getActivo())) continue;
            out.add(DestinatarioEntregaDTO.builder()
                    .tipo("USUARIO")
                    .id(u.getId())
                    .nombreCompleto(nombrePersona(u.getPersona()))
                    .detalle(u.getUsername())
                    .esExterno(false)
                    .build());
        }
        return out;
    }

    private static String nombrePersona(Persona p) {
        if (p == null) return null;
        return (p.getNombre()
                + (p.getSegundoNombre() != null ? " " + p.getSegundoNombre() : "")
                + " " + p.getApellidoPaterno()
                + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "")).trim();
    }

    /** Una entrega solo la gestiona la institución que la hizo. */
    private EntregaMuestra getEntregaConAcceso(Long id) {
        EntregaMuestra entrega = entregaService.getById(id);
        Long mia = institucionContextService.getIdInstitucionActual();
        if (!mia.equals(entrega.getInstitucion().getId())) {
            throw new AccessDeniedException("Esta entrega pertenece a otra institución");
        }
        return entrega;
    }

    @Transactional(readOnly = true)
    public EntregaMuestra getEntrega(Long id) {
        return getEntregaConAcceso(id);
    }

    @Transactional(readOnly = true)
    public List<EntregaMuestra> getActivasDeMiInstitucion() {
        return entregaService.getActivasByInstitucion(institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public Page<EntregaMuestra> getHistorialPaginado(int page, int size) {
        return entregaService.getAllByInstitucionPaginado(
                institucionContextService.getIdInstitucionActual(), page, size);
    }

    @Transactional(readOnly = true)
    public List<EntregaMuestra> getByGrupo(String grupoEntrega) {
        List<EntregaMuestra> grupo = entregaService.getByGrupo(grupoEntrega);
        if (!grupo.isEmpty()) {
            getEntregaConAcceso(grupo.get(0).getId());
        }
        return grupo;
    }

    @Transactional(readOnly = true)
    public List<EntregaMuestra> getHistorialByMuestra(Long idMuestra) {
        // Reusa el control de acceso por muestra que ya aplican estudios/traslados.
        muestraService.getByIdConAccesoHistorico(idMuestra);
        return entregaService.getHistorialByMuestra(idMuestra);
    }

    @Transactional
    public List<EntregaMuestra> entregar(List<Long> idsMuestras, Long idColaborador, Long idUsuario,
                                         String uuidEntrega, String motivo,
                                         String observaciones, LocalDateTime fechaLimite) {
        return entregaService.entregar(idsMuestras, idColaborador, idUsuario,
                institucionContextService.getIdInstitucionActual(),
                uuidEntrega, motivo, observaciones, fechaLimite);
    }

    @Transactional
    public EntregaMuestra devolver(Long idEntrega, String uuidCierra, String observaciones) {
        getEntregaConAcceso(idEntrega);
        return entregaService.devolver(idEntrega, uuidCierra, observaciones);
    }

    @Transactional
    public EntregaMuestra marcarConsumida(Long idEntrega, String uuidCierra, String observaciones) {
        getEntregaConAcceso(idEntrega);
        return entregaService.marcarConsumida(idEntrega, uuidCierra, observaciones);
    }

    @Transactional
    public EntregaMuestra cancelar(Long idEntrega, String uuidUsuario, String motivo) {
        getEntregaConAcceso(idEntrega);
        return entregaService.cancelar(idEntrega, uuidUsuario, motivo);
    }
}
