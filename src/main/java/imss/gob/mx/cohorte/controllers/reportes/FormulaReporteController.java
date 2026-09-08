package imss.gob.mx.cohorte.controllers.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.application.reportes.FormulaReporteApplicationService;
import imss.gob.mx.cohorte.controllers.reportes.dto.FormulaReporteMapper;
import imss.gob.mx.cohorte.controllers.reportes.dto.FormulaReporteRequestDTO;
import imss.gob.mx.cohorte.modules.reportes.FormulaReporte;
import imss.gob.mx.cohorte.services.formulas.ValidadorFormula;
import imss.gob.mx.cohorte.services.reportes.CatalogoVariablesFormula;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reportes/formulas")
@RequiredArgsConstructor
@Tag(name = "Fórmulas de reporte",
     description = "Cálculos con nombre que los reportes pueden usar en sus celdas")
@SecurityRequirement(name = "bearerAuth")
public class FormulaReporteController {

    private final FormulaReporteApplicationService applicationService;
    private final CatalogoVariablesFormula catalogoVariables;
    private final ObjectMapper objectMapper;

    @GetMapping("/variables")
    @Operation(summary = "Con qué se puede calcular",
               description = "Solo parámetros numéricos y con unidad declarada. Cada uno trae a qué "
                           + "otras unidades se puede pasar, que es lo que el editor ofrece cambiar.")
    public ResponseEntity<APIResponse> variables() {
        return ResponseEntity.ok(new APIResponse(catalogoVariables.todas(), HttpStatus.OK, false));
    }

    @GetMapping
    @Operation(summary = "Listar las fórmulas de la institución")
    public ResponseEntity<APIResponse> listar() {
        List<FormulaReporte> formulas = applicationService.listar();
        return ResponseEntity.ok(new APIResponse(
                formulas.stream().map(f -> FormulaReporteMapper.toResponse(f, objectMapper)).toList(),
                HttpStatus.OK, false));
    }

    @GetMapping("/activas")
    @Operation(summary = "Las fórmulas en uso",
               description = "Las que se ofrecen al diseñar un reporte.")
    public ResponseEntity<APIResponse> listarActivas() {
        List<FormulaReporte> formulas = applicationService.listarActivas();
        return ResponseEntity.ok(new APIResponse(
                formulas.stream().map(f -> FormulaReporteMapper.toResponse(f, objectMapper)).toList(),
                HttpStatus.OK, false));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una fórmula")
    public ResponseEntity<APIResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(
                FormulaReporteMapper.toResponse(applicationService.obtener(id), objectMapper),
                HttpStatus.OK, false));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Las versiones anteriores de una fórmula",
               description = "Con qué cálculo salieron los reportes que se emitieron antes de cada cambio.")
    public ResponseEntity<APIResponse> historial(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(
                applicationService.historial(id).stream()
                        .map(h -> FormulaReporteMapper.toResponse(h, objectMapper)).toList(),
                HttpStatus.OK, false));
    }

    @PostMapping("/revisar")
    @Operation(summary = "Revisar una fórmula sin guardarla",
               description = "Lo que consulta el editor mientras se escribe. Distingue lo que impide "
                           + "guardar de lo que solo conviene mirar.")
    public ResponseEntity<APIResponse> revisar(@RequestBody FormulaReporteRequestDTO dto) {
        ValidadorFormula.Revision revision = applicationService.revisar(dto);
        return ResponseEntity.ok(new APIResponse(
                Map.of("sePuedeGuardar", revision.sePuedeGuardar(),
                       "avisos", revision.avisos().stream().map(FormulaReporteMapper::toAviso).toList()),
                HttpStatus.OK, false));
    }

    @PostMapping("/probar/{uuidParticipante}")
    @Operation(summary = "Calcular la fórmula con un participante real, sin guardarla",
               description = "Devuelve además cuánto valió cada variable y en qué unidad entró: "
                           + "cuando el resultado sorprende, eso es lo que hay que mirar.")
    public ResponseEntity<APIResponse> probar(@PathVariable String uuidParticipante,
                                              @RequestBody FormulaReporteRequestDTO dto) {
        return ResponseEntity.ok(new APIResponse(
                applicationService.probar(dto, uuidParticipante), HttpStatus.OK, false));
    }

    @PostMapping
    @Operation(summary = "Crear una fórmula")
    public ResponseEntity<APIResponse> crear(@Valid @RequestBody FormulaReporteRequestDTO dto) {
        var guardada = applicationService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                "Fórmula creada",
                FormulaReporteMapper.toResponse(guardada.formula(), objectMapper, guardada.advertencias()),
                false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una fórmula",
               description = "Si cambia el cálculo, la versión anterior se archiva y el número sube.")
    public ResponseEntity<APIResponse> actualizar(@PathVariable Long id,
                                                  @Valid @RequestBody FormulaReporteRequestDTO dto) {
        var guardada = applicationService.actualizar(id, dto);
        return ResponseEntity.ok(new APIResponse(
                "Fórmula actualizada",
                FormulaReporteMapper.toResponse(guardada.formula(), objectMapper, guardada.advertencias()),
                false, HttpStatus.OK));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Poner o retirar de uso una fórmula",
               description = "Retirarla deja de ofrecerla al diseñar, sin perder los reportes que la usaron.")
    public ResponseEntity<APIResponse> toggle(@PathVariable Long id) {
        boolean activa = applicationService.toggleActivo(id);
        return ResponseEntity.ok(new APIResponse(
                activa ? "Fórmula puesta en uso" : "Fórmula retirada de uso",
                Map.of("activo", activa), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una fórmula")
    public ResponseEntity<APIResponse> eliminar(@PathVariable Long id) {
        applicationService.eliminar(id);
        return ResponseEntity.ok(new APIResponse("Fórmula eliminada", null, false, HttpStatus.OK));
    }
}
