package imss.gob.mx.cohorte.controllers.cuestionarios.dto;

import imss.gob.mx.cohorte.modules.cuestionarios.Cuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.discrepancias.DiscrepanciaLectura;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.cuestionarios.CriterioAplicabilidad;

import java.util.List;
import java.util.Objects;

/**
 * Pasa las entidades a las formas que viaja al navegador.
 *
 * <p>Estático y sin estado: no decide nada, solo copia. Cualquier regla que pareciera
 * caber aquí —qué se muestra, qué se oculta— pertenece al servicio, porque si vive en el
 * mapeador solo se aplica cuando el dato sale por esta puerta.</p>
 */
public final class CuestionarioMapper {

    private CuestionarioMapper() {}

    public static CuestionarioDTOs.TipoResponse tipo(
            TipoCuestionario t, long variables, long sinDocumentar, long bloques, long contestados) {
        return new CuestionarioDTOs.TipoResponse(
                t.getId(), t.getClave(), t.getNombre(), t.getDescripcion(), t.getVersion(),
                t.getColumnaEtiqueta(), t.getActivo(), t.getFechaCreacion(),
                variables, sinDocumentar, bloques, contestados,
                t.getDirigidoA() == null ? null : t.getDirigidoA().name(),
                t.getEdadMinima(), t.getEdadMaxima(), t.getPermiteAutollenado(),
                CriterioAplicabilidad.publicoComoTexto(t));
    }

    public static CuestionarioDTOs.SeccionResponse seccion(SeccionCuestionario s, long variables) {
        return new CuestionarioDTOs.SeccionResponse(
                s.getId(), s.getNombre(), s.getTitulo(), s.getDescripcion(),
                s.getPagina(), s.getOrden(), variables,
                s.getPresentacion() == null ? null : s.getPresentacion().name());
    }

    public static CuestionarioDTOs.VariableResponse variable(VariableCuestionario v) {
        return new CuestionarioDTOs.VariableResponse(
                v.getId(), v.getCodigo(), v.getColumnaVolcado(), v.getPregunta(),
                v.getTipo() == null ? null : v.getTipo().name(),
                v.getSeccion() == null ? null : v.getSeccion().getId(),
                v.getSeccion() == null ? null : v.getSeccion().getNombre(),
                v.getOrden(), v.getPagina(), v.getGrupoMultiple(),
                v.getDependeDe(), v.getValorHabilitante(),
                v.getOperadorDependencia() == null ? null : v.getOperadorDependencia().name(),
                v.getFilaMatriz(), v.getColumnaMatriz(),
                v.getObligatoria(), v.getDocumentada(), v.getActiva(),
                v.getValorMin(), v.getValorMax(), v.getLongitudMax(), v.getUnidad(),
                v.getObservaciones(), v.getInstruccion(), v.getFigura(),
                v.getOpciones() == null ? List.of() : v.getOpciones().stream()
                        .map(CuestionarioMapper::opcion).toList());
    }

    public static CuestionarioDTOs.OpcionResponse opcion(OpcionVariable o) {
        return new CuestionarioDTOs.OpcionResponse(
                o.getId(), o.getCodigo(), o.getEtiqueta(), o.getOrden(), o.getEsEscape());
    }

    public static CuestionarioDTOs.CuestionarioResponse cuestionario(Cuestionario c) {
        var paciente = c.getPaciente();
        var tipo = c.getTipoCuestionario();
        return new CuestionarioDTOs.CuestionarioResponse(
                c.getId(), tipo.getId(), tipo.getClave(), tipo.getNombre(),
                paciente.getUuid(), paciente.getFolio(), nombreDe(paciente.getPersona()),
                c.getEtiqueta(),
                c.getEstado() == null ? null : c.getEstado().name(),
                c.getOrigen() == null ? null : c.getOrigen().name(),
                c.getFechaAplicacion(), c.getFechaRegistro(),
                usuario(c.getUsuarioCaptura()), usuario(c.getUsuarioValida()),
                c.getFechaValidacion(), c.getArchivoOrigen(),
                c.getRespuestasPendientes(), c.getObservaciones(),
                c.getFueraDeCriterio(), c.getMotivoFueraDeCriterio());
    }

    public static CuestionarioDTOs.RespuestaResponse respuesta(RespuestaCuestionario r) {
        return new CuestionarioDTOs.RespuestaResponse(
                r.getId(), r.getVariable().getId(), r.getVariable().getCodigo(),
                r.getOrdenRespuesta(), r.getValorCodigo(), r.getValorNumerico(),
                r.getValorTexto(), r.getValorFecha(),
                r.getMarcaAusencia() == null ? null : r.getMarcaAusencia().name(),
                r.getValorCrudo(), r.getCeldaOrigen());
    }

    public static CuestionarioDTOs.DiscrepanciaResponse discrepancia(DiscrepanciaLectura d) {
        return new CuestionarioDTOs.DiscrepanciaResponse(
                d.getId(),
                d.getVariable() == null ? null : d.getVariable().getId(),
                d.getVariable() == null ? null : d.getVariable().getCodigo(),
                d.getColumnaVolcado(), d.getCeldaOrigen(), d.getValorEnFirme(),
                d.getLecturasAlternas(), d.getResuelta(), d.getValorAdjudicado(),
                usuario(d.getUsuarioResuelve()), d.getFechaResolucion(), d.getNota());
    }

    private static String nombreDe(Persona persona) {
        if (persona == null) return null;
        return String.join(" ",
                        Objects.toString(persona.getNombre(), ""),
                        Objects.toString(persona.getApellidoPaterno(), ""),
                        Objects.toString(persona.getApellidoMaterno(), ""))
                .replaceAll("\\s+", " ").trim();
    }

    private static String usuario(BeanUser usuario) {
        return usuario == null ? null : usuario.getUsername();
    }
}
