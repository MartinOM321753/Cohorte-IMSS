package imss.gob.mx.cohorte.controllers.almacenamiento.dto.estudiomuestra;

import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import imss.gob.mx.cohorte.controllers.users.dto.UserMapper;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.*;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.HistorialCambioMuestra;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class EstudioMuestraMapper {

    private static final String ROOT = "ROOT";
    private static final int ROOT_ORDER = 0;

    // ─── EstudioMuestra ──────────────────────────────────────────────────────

    public static EstudioMuestra toEntity(EstudioMuestraRequestDTO dto) {
        EstudioMuestra estudio = new EstudioMuestra();

        BeanUser usuario = new BeanUser();
        usuario.setUUID(dto.getUsuarioRealizaUUID());
        estudio.setUsuarioRealiza(usuario);

        TipoEstudioMuestra tipo = new TipoEstudioMuestra();
        tipo.setId(dto.getIdTipoEstudioMuestra());
        estudio.setTipoEstudioMuestra(tipo);

        estudio.setFechaEstudio(dto.getFechaEstudio());
        estudio.setObservaciones(dto.getObservaciones());
        estudio.setCantidadConsumida(dto.getCantidadConsumida());
        estudio.setUnidadConsumida(dto.getUnidadConsumida());

        if (dto.getResultados() != null && !dto.getResultados().isEmpty()) {
            List<ResultadoEstudioMuestra> resultados = dto.getResultados().stream()
                    .map(r -> {
                        ResultadoEstudioMuestra resultado = new ResultadoEstudioMuestra();
                        ParametroEstudioMuestra parametro = new ParametroEstudioMuestra();
                        parametro.setId(r.getIdParametro());
                        resultado.setParametro(parametro);
                        resultado.setValorNumerico(r.getValorNumerico());
                        resultado.setValorTexto(r.getValorTexto());
                        resultado.setValorBooleano(r.getValorBooleano());
                        resultado.setGrupoCodigo(r.getGrupoCodigo());
                        resultado.setGrupoEtiqueta(r.getGrupoEtiqueta());
                        resultado.setOrdenResultado(r.getOrden());
                        return resultado;
                    })
                    .collect(Collectors.toList());
            estudio.setResultados(resultados);
        } else {
            estudio.setResultados(new ArrayList<>());
        }

        return estudio;
    }

    public static EstudioMuestraResponseDTO toResponseDTO(EstudioMuestra e) {
        UsuarioResumenDTO usuarioDTO = e.getUsuarioRealiza() != null
                ? UserMapper.toResumenDTO(e.getUsuarioRealiza()) : null;

        TipoEstudioMuestraResponseDTO tipoDTO = null;
        if (e.getTipoEstudioMuestra() != null) {
            tipoDTO = toTipoResponseDTO(e.getTipoEstudioMuestra(), true);
        }

        List<ResultadoEstudioMuestraResponseDTO> resultadosDTO = null;
        if (e.getResultados() != null) {
            resultadosDTO = e.getResultados().stream()
                    .map(r -> ResultadoEstudioMuestraResponseDTO.builder()
                            .id(r.getId())
                            .idParametro(r.getParametro() != null ? r.getParametro().getId() : null)
                            .parametro(r.getParametro() != null ? r.getParametro().getNombre() : null)
                            .valorNumerico(r.getValorNumerico())
                            .valorTexto(r.getValorTexto())
                            .valorBooleano(r.getValorBooleano())
                            .grupoCodigo(ROOT.equals(r.getGrupoCodigo()) ? null : r.getGrupoCodigo())
                            .grupoEtiqueta(ROOT.equals(r.getGrupoCodigo()) ? null : r.getGrupoEtiqueta())
                            .orden(ROOT.equals(r.getGrupoCodigo()) && r.getOrdenResultado() == ROOT_ORDER
                                    ? null : r.getOrdenResultado())
                            .build())
                    .collect(Collectors.toList());
        }

        return EstudioMuestraResponseDTO.builder()
                .id(e.getId())
                .idMuestra(e.getMuestra() != null ? e.getMuestra().getId() : null)
                .etiquetaMuestra(e.getMuestra() != null ? e.getMuestra().getEtiqueta() : null)
                .tipoEstudioMuestra(tipoDTO)
                .usuarioRealiza(usuarioDTO)
                .fechaEstudio(e.getFechaEstudio())
                .fechaRegistro(e.getFechaRegistro())
                .observaciones(e.getObservaciones())
                .cantidadConsumida(e.getCantidadConsumida())
                .unidadConsumida(e.getUnidadConsumida())
                .resultados(resultadosDTO)
                .cantidadResultados(resultadosDTO != null ? resultadosDTO.size() : 0)
                .idInstitucionCreadora(e.getUsuarioRealiza() != null && e.getUsuarioRealiza().getInstitucion() != null
                        ? e.getUsuarioRealiza().getInstitucion().getId() : null)
                .build();
    }

    // ─── Tipo ────────────────────────────────────────────────────────────────

    public static TipoEstudioMuestraResponseDTO toTipoResponseDTO(TipoEstudioMuestra t, boolean includeParametros) {
        List<ParametroEstudioMuestraResponseDTO> parametrosDTO = null;
        if (includeParametros && t.getParametros() != null) {
            parametrosDTO = t.getParametros().stream()
                    .map(EstudioMuestraMapper::toParametroDTO)
                    .collect(Collectors.toList());
        }
        return TipoEstudioMuestraResponseDTO.builder()
                .id(t.getId())
                .nombre(t.getNombre())
                .descripcion(t.getDescripcion())
                .activo(t.getActivo())
                .fechaCreacion(t.getFechaCreacion())
                .parametros(parametrosDTO)
                .build();
    }

    // ─── Parámetro ───────────────────────────────────────────────────────────

    public static ParametroEstudioMuestraResponseDTO toParametroDTO(ParametroEstudioMuestra p) {
        List<String> opciones = (p.getTipo() == TipoParametro.TEXTO_OPCIONES && p.getOpciones() != null)
                ? p.getOpciones().stream().map(OpcionParametroEstudioMuestra::getValor).collect(Collectors.toList())
                : null;
        return ParametroEstudioMuestraResponseDTO.builder()
                .id(p.getId())
                .idTipoEstudioMuestra(p.getTipoEstudioMuestra() != null ? p.getTipoEstudioMuestra().getId() : null)
                .nombreTipoEstudioMuestra(p.getTipoEstudioMuestra() != null ? p.getTipoEstudioMuestra().getNombre() : null)
                .nombre(p.getNombre())
                .unidad(p.getUnidad())
                .tipo(p.getTipo())
                .valorMinimo(p.getValorMinimo())
                .valorMaximo(p.getValorMaximo())
                .opciones(opciones)
                .build();
    }

    // ─── Historial ───────────────────────────────────────────────────────────

    public static HistorialCambioMuestraResponseDTO toHistorialDTO(HistorialCambioMuestra h) {
        String nombreUsuario = h.getUsuario() != null && h.getUsuario().getPersona() != null
                ? h.getUsuario().getPersona().getNombre() + " "
                + h.getUsuario().getPersona().getApellidoPaterno()
                : (h.getUsuario() != null ? h.getUsuario().getUsername() : null);

        return HistorialCambioMuestraResponseDTO.builder()
                .id(h.getId())
                .campo(h.getCampo())
                .valorAnterior(h.getValorAnterior())
                .valorNuevo(h.getValorNuevo())
                .usuario(nombreUsuario)
                .fechaCambio(h.getFechaCambio())
                .motivo(h.getMotivo())
                .build();
    }
}
