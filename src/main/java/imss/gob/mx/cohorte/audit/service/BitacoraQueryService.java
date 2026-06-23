package imss.gob.mx.cohorte.audit.service;

import imss.gob.mx.cohorte.audit.dto.BitacoraAccesoResponseDTO;
import imss.gob.mx.cohorte.audit.dto.BitacoraAccionResponseDTO;
import imss.gob.mx.cohorte.audit.dto.UsuarioBitacoraDTO;
import imss.gob.mx.cohorte.audit.model.TipoAccion;
import imss.gob.mx.cohorte.audit.model.TipoEventoAcceso;
import imss.gob.mx.cohorte.audit.repository.BitacoraAccesoRepository;
import imss.gob.mx.cohorte.audit.repository.BitacoraAccionesRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.modules.usuarios.user.UserRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@AllArgsConstructor
public class BitacoraQueryService {

    private final BitacoraAccesoRepository  accesoRepo;
    private final BitacoraAccionesRepository accionesRepo;
    private final UserRepository userRepository;
    private final InstitucionContextService institucionCtx;

    @Transactional(readOnly = true)
    public Page<BitacoraAccesoResponseDTO> consultarAccesos(
            LocalDate desdeDate,
            LocalDate hastaDate,
            String    usuarioUuid,
            String    tipoEventoStr,
            int       page,
            int       size) {

        LocalDateTime desde = desdeDate != null ? desdeDate.atStartOfDay() : null;
        LocalDateTime hasta = hastaDate != null ? hastaDate.atTime(LocalTime.MAX) : null;
        TipoEventoAcceso tipoEvento = parseTipoEventoAcceso(tipoEventoStr);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        return accesoRepo
                .buscarConFiltros(desde, hasta,
                        blankToNull(usuarioUuid), tipoEvento, pageable)
                .map(BitacoraAccesoResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public Page<BitacoraAccionResponseDTO> consultarAcciones(
            LocalDate desdeDate,
            LocalDate hastaDate,
            String    usuarioUuid,
            String    tipoAccionStr,
            String    entidad,
            int       page,
            int       size) {

        LocalDateTime desde = desdeDate != null ? desdeDate.atStartOfDay() : null;
        LocalDateTime hasta = hastaDate != null ? hastaDate.atTime(LocalTime.MAX) : null;
        TipoAccion tipoAccion = parseTipoAccion(tipoAccionStr);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        return accionesRepo
                .buscarConFiltros(desde, hasta,
                        blankToNull(usuarioUuid), tipoAccion, blankToNull(entidad), pageable)
                .map(BitacoraAccionResponseDTO::from);
    }

    @Transactional(readOnly = true)
    public List<UsuarioBitacoraDTO> usuariosConAccesos() {
        Long idInst = institucionCtx.getIdInstitucionActual();
        return userRepository.findUsuariosConAccesos(idInst).stream()
                .map(this::toUsuarioBitacoraDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UsuarioBitacoraDTO> usuariosConAcciones() {
        Long idInst = institucionCtx.getIdInstitucionActual();
        return userRepository.findUsuariosConAcciones(idInst).stream()
                .map(this::toUsuarioBitacoraDTO)
                .toList();
    }

    private UsuarioBitacoraDTO toUsuarioBitacoraDTO(BeanUser u) {
        return new UsuarioBitacoraDTO(
                u.getUUID(),
                u.getPersona().getNombre(),
                u.getPersona().getApellidoPaterno(),
                u.getRol().getRole()
        );
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private TipoEventoAcceso parseTipoEventoAcceso(String s) {
        if (s == null || s.isBlank()) return null;
        try { return TipoEventoAcceso.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }

    private TipoAccion parseTipoAccion(String s) {
        if (s == null || s.isBlank()) return null;
        try { return TipoAccion.valueOf(s.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
