package imss.gob.mx.cohorte.audit.dto;

import imss.gob.mx.cohorte.audit.model.BitacoraAcceso;
import imss.gob.mx.cohorte.audit.model.TipoEventoAcceso;

import java.time.LocalDateTime;

public record BitacoraAccesoResponseDTO(
        Long            id,
        String          usuarioUuid,
        String          username,
        String          nombreCompleto,
        String          rol,
        String          ip,
        Double          latitud,
        Double          longitud,
        Integer         precisionM,
        TipoEventoAcceso tipoEvento,
        LocalDateTime   timestamp,
        String          userAgent,
        Integer         duracionSesionSeg,
        String          identificadorIntento
) {
    public static BitacoraAccesoResponseDTO from(BitacoraAcceso e) {
        return new BitacoraAccesoResponseDTO(
                e.getId(),
                e.getUsuarioUuid(),
                e.getUsername(),
                e.getNombreCompleto(),
                e.getRol(),
                e.getIp(),
                e.getLatitud(),
                e.getLongitud(),
                e.getPrecisionM(),
                e.getTipoEvento(),
                e.getTimestamp(),
                e.getUserAgent(),
                e.getDuracionSesionSeg(),
                e.getIdentificadorIntento()
        );
    }
}
