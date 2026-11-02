package imss.gob.mx.cohorte.controllers.almacenamiento.dto.entrega;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.EtiquetaPosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.Colaborador;
import imss.gob.mx.cohorte.modules.almacenamiento.entrega.EntregaMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.entrega.EstadoEntrega;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.time.LocalDateTime;
import java.util.List;

public class EntregaMapper {

    private EntregaMapper() {}

    public static EntregaResponseDTO toResponseDTO(EntregaMuestra e) {
        Muestra m = e.getMuestra();
        return EntregaResponseDTO.builder()
                .id(e.getId())
                .muestra(EntregaResponseDTO.MuestraResumenDTO.builder()
                        .id(m.getId())
                        .etiqueta(m.getEtiqueta())
                        .unidad(m.getUnidad())
                        .valor(m.getValor())
                        .estadoMuestra(m.getEstadoMuestra() != null ? m.getEstadoMuestra().name() : null)
                        .esAlicuota(m.getMuestraPadre() != null)
                        .posicionLabel(buildPosicionLabel(m))
                        .build())
                .destinatario(mapDestinatario(e))
                .entregadaPor(mapUsuario(e.getEntregadaPor()))
                .cerradaPor(mapUsuario(e.getCerradaPor()))
                .estado(e.getEstado().name())
                .fechaEntrega(e.getFechaEntrega())
                .fechaCierre(e.getFechaCierre())
                .fechaLimite(e.getFechaLimite())
                .vencido(isVencido(e))
                .motivo(e.getMotivo())
                .observaciones(e.getObservaciones())
                .grupoEntrega(e.getGrupoEntrega())
                .build();
    }

    public static List<EntregaResponseDTO> toResponseDTOList(List<EntregaMuestra> list) {
        return list.stream().map(EntregaMapper::toResponseDTO).toList();
    }

    private static EntregaResponseDTO.DestinatarioResumenDTO mapDestinatario(EntregaMuestra e) {
        Colaborador c = e.getColaborador();
        if (c != null) {
            return EntregaResponseDTO.DestinatarioResumenDTO.builder()
                    .tipo("COLABORADOR")
                    .id(c.getId())
                    .nombreCompleto(nombreCompleto(c.getPersona()))
                    .cargo(c.getCargo())
                    .adscripcion(c.getAdscripcion())
                    .esExterno(Boolean.TRUE.equals(c.getEsExterno()))
                    .build();
        }
        BeanUser u = e.getUsuario();
        if (u != null) {
            return EntregaResponseDTO.DestinatarioResumenDTO.builder()
                    .tipo("USUARIO")
                    .id(u.getId())
                    .nombreCompleto(nombreCompleto(u.getPersona()))
                    .cargo(u.getUsername())
                    .esExterno(false)
                    .build();
        }
        return null;
    }

    private static EntregaResponseDTO.UsuarioResumenDTO mapUsuario(BeanUser u) {
        if (u == null) return null;
        return EntregaResponseDTO.UsuarioResumenDTO.builder()
                .id(u.getId())
                .uuid(u.getUUID())
                .username(u.getUsername())
                .nombreCompleto(nombreCompleto(u.getPersona()))
                .build();
    }

    private static String buildPosicionLabel(Muestra muestra) {
        PosicionCaja pos = muestra.getPosicionCaja();
        if (pos == null) return null;
        CajaCriogenica caja = pos.getCaja();
        String cajaCode = caja != null ? caja.getCodigoCaja() : "?";
        return cajaCode + " [" + EtiquetaPosicionCaja.etiqueta(pos.getFila(), pos.getColumna()) + "]";
    }

    private static boolean isVencido(EntregaMuestra e) {
        if (e.getFechaLimite() == null) return false;
        if (e.getEstado() != EstadoEntrega.ENTREGADA) return false;
        return LocalDateTime.now().isAfter(e.getFechaLimite());
    }

    private static String nombreCompleto(Persona p) {
        if (p == null) return null;
        return (p.getNombre()
                + (p.getSegundoNombre() != null ? " " + p.getSegundoNombre() : "")
                + " " + p.getApellidoPaterno()
                + (p.getApellidoMaterno() != null ? " " + p.getApellidoMaterno() : "")).trim();
    }
}
