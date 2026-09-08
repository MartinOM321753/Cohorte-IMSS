package imss.gob.mx.cohorte.controllers.reportes.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporte;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporteHistorial;
import imss.gob.mx.cohorte.services.formulas.ValidadorFormula;

import java.util.List;

public final class FormulaReporteMapper {

    private FormulaReporteMapper() {}

    public static FormulaReporte toEntity(FormulaReporteRequestDTO dto, ObjectMapper mapper) {
        FormulaReporte f = new FormulaReporte();
        f.setNombre(dto.getNombre() != null ? dto.getNombre().trim() : null);
        f.setDescripcion(dto.getDescripcion());
        f.setExpresion(dto.getExpresion());
        f.setExpresionMinimo(vacioComoNulo(dto.getExpresionMinimo()));
        f.setExpresionMaximo(vacioComoNulo(dto.getExpresionMaximo()));
        f.setUnidadSalida(dto.getUnidadSalida());
        f.setDecimales(dto.getDecimales());
        f.setVariables(escribirVariables(dto.getVariables(), mapper));
        return f;
    }

    public static FormulaReporteResponseDTO toResponse(FormulaReporte f, ObjectMapper mapper) {
        return toResponse(f, mapper, List.of());
    }

    public static FormulaReporteResponseDTO toResponse(FormulaReporte f, ObjectMapper mapper,
                                                       List<ValidadorFormula.Aviso> advertencias) {
        return FormulaReporteResponseDTO.builder()
                .id(f.getId())
                .nombre(f.getNombre())
                .descripcion(f.getDescripcion())
                .expresion(f.getExpresion())
                .variables(leerVariables(f.getVariables(), mapper))
                .expresionMinimo(f.getExpresionMinimo())
                .expresionMaximo(f.getExpresionMaximo())
                .unidadSalida(f.getUnidadSalida())
                .decimales(f.getDecimales())
                .version(f.getVersion())
                .activo(f.getActivo())
                .fechaCreacion(f.getFechaCreacion())
                .fechaActualizacion(f.getFechaActualizacion())
                .advertencias(advertencias.stream().map(FormulaReporteMapper::toAviso).toList())
                .build();
    }

    /** Lo que decía la fórmula en una versión anterior. */
    public static FormulaReporteResponseDTO toResponse(FormulaReporteHistorial h, ObjectMapper mapper) {
        return FormulaReporteResponseDTO.builder()
                .id(h.getIdFormula())
                .nombre(h.getNombre())
                .expresion(h.getExpresion())
                .variables(leerVariables(h.getVariables(), mapper))
                .unidadSalida(h.getUnidadSalida())
                .decimales(h.getDecimales())
                .version(h.getVersion())
                .fechaActualizacion(h.getFechaReemplazo())
                .advertencias(List.of())
                .build();
    }

    public static FormulaReporteResponseDTO.AvisoDTO toAviso(ValidadorFormula.Aviso aviso) {
        return FormulaReporteResponseDTO.AvisoDTO.builder()
                .nivel(aviso.nivel().name())
                .mensaje(aviso.mensaje())
                .posicion(aviso.posicion())
                .build();
    }

    /** Un campo vacío es «no hay límite», no un límite en blanco. */
    private static String vacioComoNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String escribirVariables(List<FormulaReporteRequestDTO.VariableFormulaDTO> variables,
                                            ObjectMapper mapper) {
        try {
            return mapper.writeValueAsString(variables == null ? List.of() : variables);
        } catch (Exception e) {
            // No debería ocurrir: son objetos que acaba de construir Jackson al entrar.
            throw new IllegalStateException("No se pudieron guardar las variables de la fórmula", e);
        }
    }

    private static List<FormulaReporteRequestDTO.VariableFormulaDTO> leerVariables(String json,
                                                                                   ObjectMapper mapper) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception malFormado) {
            // Una fórmula con las variables corruptas se muestra sin ellas en lugar de
            // tumbar el listado entero: así se puede entrar a arreglarla.
            return List.of();
        }
    }
}
