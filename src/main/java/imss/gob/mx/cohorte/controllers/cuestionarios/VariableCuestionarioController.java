package imss.gob.mx.cohorte.controllers.cuestionarios;

import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;

import imss.gob.mx.cohorte.controllers.cuestionarios.dto.CuestionarioDTOs.*;
import imss.gob.mx.cohorte.controllers.cuestionarios.dto.CuestionarioMapper;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OperadorDependencia;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.services.cuestionarios.VariableCuestionarioService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequireModulo(ModuloSistema.CUESTIONARIOS)
@RequestMapping("/api/cuestionarios")
@AllArgsConstructor
@Validated
@Tag(name = "Cuestionarios - preguntas", description = "El catalogo de preguntas de cada instrumento")
@SecurityRequirement(name = "bearerAuth")
public class VariableCuestionarioController {

    /**
     * Tope de preguntas por pagina.
     *
     * <p>Existe porque el instrumento principal pasa de dos mil: sin tope, pedir «las
     * preguntas» devolvería un documento de megabytes que ninguna pantalla aprovecha.
     * Quien necesita el catálogo completo lo pide bloque por bloque.</p>
     */
    private static final int MAX_POR_PAGINA = 200;

    private final VariableCuestionarioService service;

    @GetMapping("/tipos/{idTipo}/preguntas")
    @Operation(summary = "Buscar preguntas del instrumento",
            description = "Paginado; `buscar` filtra por codigo o por enunciado")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_CATALOGO_ACCEDER', 'CUESTIONARIOS_LLENADO_ACCEDER')")
    public ResponseEntity<APIResponse> buscar(
            @PathVariable Long idTipo,
            @RequestParam(required = false) String buscar,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "50") int tamano) {

        Page<VariableCuestionario> resultado = service.buscar(idTipo, buscar,
                PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamano), MAX_POR_PAGINA)));

        return ResponseEntity.ok(new APIResponse(Map.of(
                "contenido", resultado.getContent().stream().map(CuestionarioMapper::variable).toList(),
                "pagina", resultado.getNumber(),
                "tamano", resultado.getSize(),
                "total", resultado.getTotalElements(),
                "totalPaginas", resultado.getTotalPages()
        ), "Preguntas obtenidas correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/bloques/{idBloque}/preguntas")
    @Operation(summary = "Las preguntas de un bloque, con sus opciones")
    @PreAuthorize("hasAnyAuthority('CUESTIONARIOS_CATALOGO_ACCEDER', 'CUESTIONARIOS_LLENADO_ACCEDER')")
    public ResponseEntity<APIResponse> deBloque(@PathVariable Long idBloque) {
        return ResponseEntity.ok(new APIResponse(
                service.deSeccion(idBloque).stream().map(CuestionarioMapper::variable).toList(),
                "Preguntas obtenidas correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/tipos/{idTipo}/preguntas/sin-documentar")
    @Operation(summary = "Preguntas que el volcado produce y ningun catalogo documenta",
            description = "Se importan igual; esta lista es el trabajo pendiente de redactarlas")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_CATALOGO_ACCEDER')")
    public ResponseEntity<APIResponse> sinDocumentar(@PathVariable Long idTipo) {
        return ResponseEntity.ok(new APIResponse(
                service.sinDocumentar(idTipo).stream().map(CuestionarioMapper::variable).toList(),
                "Preguntas sin documentar obtenidas correctamente", HttpStatus.OK, false));
    }

    @PostMapping("/tipos/{idTipo}/preguntas")
    @Operation(summary = "Crear pregunta")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> crear(@PathVariable Long idTipo,
                                             @Valid @RequestBody VariableRequest dto) {
        VariableCuestionario creada = service.crear(idTipo, desde(dto), dto.getIdSeccion(),
                opcionesDe(dto));
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                CuestionarioMapper.variable(creada), "Pregunta creada correctamente",
                HttpStatus.CREATED, false));
    }

    @PutMapping("/preguntas/{id}")
    @Operation(summary = "Actualizar pregunta")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody VariableRequest dto) {
        VariableCuestionario actualizada = service.actualizar(id, desde(dto), dto.getIdSeccion(),
                dto.getOpciones() == null ? null : opcionesDe(dto));
        return ResponseEntity.ok(new APIResponse(CuestionarioMapper.variable(actualizada),
                "Pregunta actualizada correctamente", HttpStatus.OK, false));
    }

    @PatchMapping("/bloques/{idBloque}/preguntas/orden")
    @Operation(summary = "Reordenar las preguntas de un bloque")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> reordenar(@PathVariable Long idBloque,
                                                  @Valid @RequestBody ReordenarRequest dto) {
        service.reordenar(idBloque, dto.getIds());
        return ResponseEntity.ok(new APIResponse("Preguntas reordenadas", HttpStatus.OK, false));
    }

    @DeleteMapping("/preguntas/{id}")
    @Operation(summary = "Retirar pregunta",
            description = "Si tiene respuestas se retira de uso; si no, se elimina")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_ELIMINAR')")
    public ResponseEntity<APIResponse> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.ok(new APIResponse("Pregunta retirada", HttpStatus.OK, false));
    }

    @PatchMapping("/bloques/{idBloque}/dependencia")
    @Operation(summary = "Poner la misma dependencia a todo un bloque",
            description = "Los saltos del papel son por bloque: el «Ninguna» de la 112 apaga sus "
                    + "135 celdas. La pregunta llave se excluye sola. Sin `dependeDe` se quita "
                    + "la dependencia de todas")
    @PreAuthorize("hasAuthority('CUESTIONARIOS_TIPOS_EDITAR')")
    public ResponseEntity<APIResponse> dependenciaDeBloque(
            @PathVariable Long idBloque,
            @Valid @RequestBody DependenciaDeBloqueRequest dto) {
        int tocadas = service.dependenciaDeBloque(idBloque, dto.getDependeDe(),
                dto.getValorHabilitante(),
                dto.getOperadorDependencia() == null
                        ? OperadorDependencia.IGUAL
                        : OperadorDependencia.valueOf(dto.getOperadorDependencia()));
        return ResponseEntity.ok(new APIResponse(tocadas,
                tocadas + " pregunta(s) actualizada(s)", HttpStatus.OK, false));
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    /**
     * Pasa lo que llega del cliente a una pregunta, tal cual.
     *
     * <p>Lo que el cliente no mandó queda en null a propósito, y no se rellena aquí:
     * null significa «no lo dije», y es lo que permite que la actualización deje en paz
     * lo que no se tocó. Los valores por omisión del alta los pone el servicio, que es
     * quien sabe en qué bloque va la pregunta.</p>
     */
    private VariableCuestionario desde(VariableRequest dto) {
        VariableCuestionario v = new VariableCuestionario();
        v.setCodigo(dto.getCodigo());
        v.setColumnaVolcado(dto.getColumnaVolcado());
        v.setPregunta(dto.getPregunta());
        v.setTipo(TipoVariable.valueOf(dto.getTipo()));
        v.setOrden(dto.getOrden());
        v.setPagina(dto.getPagina());
        v.setGrupoMultiple(dto.getGrupoMultiple());
        v.setDependeDe(dto.getDependeDe());
        v.setValorHabilitante(dto.getValorHabilitante());
        v.setOperadorDependencia(dto.getOperadorDependencia() == null
                ? OperadorDependencia.IGUAL
                : OperadorDependencia.valueOf(dto.getOperadorDependencia()));
        v.setFilaMatriz(dto.getFilaMatriz());
        v.setColumnaMatriz(dto.getColumnaMatriz());
        v.setObligatoria(dto.getObligatoria());
        v.setDocumentada(dto.getDocumentada());
        v.setActiva(dto.getActiva());
        v.setValorMin(dto.getValorMin());
        v.setValorMax(dto.getValorMax());
        v.setLongitudMax(dto.getLongitudMax());
        v.setUnidad(dto.getUnidad());
        v.setObservaciones(dto.getObservaciones());
        v.setInstruccion(dto.getInstruccion());
        v.setFigura(dto.getFigura());
        return v;
    }

    private List<OpcionVariable> opcionesDe(VariableRequest dto) {
        List<OpcionVariable> opciones = new ArrayList<>();
        if (dto.getOpciones() == null) return opciones;
        for (OpcionRequest o : dto.getOpciones()) {
            OpcionVariable opcion = new OpcionVariable();
            opcion.setCodigo(o.getCodigo());
            opcion.setEtiqueta(o.getEtiqueta());
            opcion.setEsEscape(o.getEsEscape() != null && o.getEsEscape());
            opciones.add(opcion);
        }
        return opciones;
    }
}
