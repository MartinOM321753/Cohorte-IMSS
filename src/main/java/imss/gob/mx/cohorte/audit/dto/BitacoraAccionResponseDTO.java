package imss.gob.mx.cohorte.audit.dto;

import imss.gob.mx.cohorte.audit.model.BitacoraAcciones;
import imss.gob.mx.cohorte.audit.model.TipoAccion;

import java.time.LocalDateTime;

public record BitacoraAccionResponseDTO(
        Long         id,
        String       usuarioUuid,
        String       username,
        String       nombreCompleto,
        String       rol,
        String       ip,
        String       endpoint,
        String       metodoHttp,
        TipoAccion   tipoAccion,
        String       entidadAfectada,
        String       valoresAnteriores,
        String       valoresNuevos,
        String       sentenciaSql,
        Long         duracionMs,
        LocalDateTime timestamp,
        Boolean      exitoso,
        String       mensajeError
) {
    public static BitacoraAccionResponseDTO from(BitacoraAcciones e) {
        return new BitacoraAccionResponseDTO(
                e.getId(),
                e.getUsuarioUuid(),
                e.getUsername(),
                e.getNombreCompleto(),
                e.getRol(),
                e.getIp(),
                e.getEndpoint(),
                e.getMetodoHttp(),
                e.getTipoAccion(),
                e.getEntidadAfectada(),
                e.getValoresAnteriores(),
                e.getValoresNuevos(),
                e.getSentenciaSql(),
                e.getDuracionMs(),
                e.getTimestamp(),
                e.getExitoso(),
                e.getMensajeError()
        );
    }
}
