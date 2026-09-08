package imss.gob.mx.cohorte.controllers.reportes;

import imss.gob.mx.cohorte.application.reportes.PlantillaReporteApplicationService;
import imss.gob.mx.cohorte.controllers.reportes.dto.DuplicarPlantillaDTO;
import imss.gob.mx.cohorte.controllers.reportes.dto.PlantillaReporteMapper;
import imss.gob.mx.cohorte.controllers.reportes.dto.RenombrarPlantillaDTO;
import imss.gob.mx.cohorte.controllers.reportes.dto.PlantillaReporteRequestDTO;
import imss.gob.mx.cohorte.modules.reportes.PlantillaReporte;
import imss.gob.mx.cohorte.modules.reportes.TipoReporte;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reportes/plantillas")
@RequiredArgsConstructor
@Tag(name = "Plantillas de reporte", description = "Diseños de reporte por institución")
@SecurityRequirement(name = "bearerAuth")
public class PlantillaReporteController {

    private final PlantillaReporteApplicationService applicationService;

    @GetMapping
    @Operation(summary = "Listar las plantillas de la institución",
               description = "Sin el diseño: los listados solo muestran nombres y un diseño puede pesar bastante.")
    public ResponseEntity<APIResponse> listar() {
        List<PlantillaReporte> plantillas = applicationService.listar();
        return ResponseEntity.ok(new APIResponse(
                plantillas.stream().map(PlantillaReporteMapper::toResumen).toList(),
                HttpStatus.OK, false));
    }

    @GetMapping("/tipo/{tipo}")
    @Operation(summary = "Plantillas en uso para un tipo de reporte")
    public ResponseEntity<APIResponse> listarPorTipo(
            @Parameter(description = "Sobre qué se emite el reporte", required = true)
            @PathVariable TipoReporte tipo) {
        List<PlantillaReporte> plantillas = applicationService.listarActivasPorTipo(tipo);
        return ResponseEntity.ok(new APIResponse(
                plantillas.stream().map(PlantillaReporteMapper::toResumen).toList(),
                HttpStatus.OK, false));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una plantilla con su diseño completo")
    public ResponseEntity<APIResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(new APIResponse(
                PlantillaReporteMapper.toResponse(applicationService.obtener(id)),
                HttpStatus.OK, false));
    }

    @GetMapping("/tipo/{tipo}/predeterminada")
    @Operation(summary = "La plantilla que se ofrece primero para ese tipo",
               description = "Devuelve null si la institución no ha marcado ninguna.")
    public ResponseEntity<APIResponse> predeterminada(@PathVariable TipoReporte tipo) {
        PlantillaReporte plantilla = applicationService.obtenerPredeterminada(tipo);
        return ResponseEntity.ok(new APIResponse(
                plantilla != null ? PlantillaReporteMapper.toResponse(plantilla) : null,
                HttpStatus.OK, false));
    }

    @PostMapping
    @Operation(summary = "Crear una plantilla")
    public ResponseEntity<APIResponse> crear(@Valid @RequestBody PlantillaReporteRequestDTO dto) {
        PlantillaReporte creada = applicationService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                "Plantilla creada", PlantillaReporteMapper.toResponse(creada), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar una plantilla")
    public ResponseEntity<APIResponse> actualizar(
            @PathVariable Long id, @Valid @RequestBody PlantillaReporteRequestDTO dto) {
        PlantillaReporte actualizada = applicationService.actualizar(id, dto);
        return ResponseEntity.ok(new APIResponse(
                "Plantilla actualizada", PlantillaReporteMapper.toResponse(actualizada), false, HttpStatus.OK));
    }

    @PutMapping("/{id}/nombre")
    @Operation(summary = "Renombrar una plantilla",
               description = "Cambia nombre y descripción sin tocar el diseño. Va aparte del "
                           + "actualizar general, que exige mandar el diseño entero y podría "
                           + "pisar el guardado bueno con una versión vieja.")
    public ResponseEntity<APIResponse> renombrar(
            @PathVariable Long id, @Valid @RequestBody RenombrarPlantillaDTO dto) {
        PlantillaReporte renombrada = applicationService.renombrar(id, dto);
        return ResponseEntity.ok(new APIResponse(
                "Plantilla renombrada", PlantillaReporteMapper.toResumen(renombrada), false, HttpStatus.OK));
    }

    @PostMapping("/{id}/duplicar")
    @Operation(summary = "Duplicar una plantilla",
               description = "Copia el diseño para partir de algo ya hecho. Sin nombre en el "
                           + "cuerpo se propone uno libre. La copia nunca nace como predeterminada.")
    public ResponseEntity<APIResponse> duplicar(
            @PathVariable Long id,
            @RequestBody(required = false) @Valid DuplicarPlantillaDTO dto) {
        PlantillaReporte copia = applicationService.duplicar(id, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                "Plantilla duplicada", PlantillaReporteMapper.toResponse(copia), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}/toggle")
    @Operation(summary = "Poner o retirar de uso una plantilla",
               description = "Retirarla la quita también de predeterminada: un formato fuera de uso "
                           + "no puede seguir siendo la primera opción al emitir.")
    public ResponseEntity<APIResponse> toggle(@PathVariable Long id) {
        boolean activa = applicationService.toggleActivo(id);
        return ResponseEntity.ok(new APIResponse(
                activa ? "Plantilla puesta en uso" : "Plantilla retirada de uso",
                activa, false, HttpStatus.OK));
    }

    @PutMapping("/{id}/predeterminada")
    @Operation(summary = "Marcarla como la predeterminada de su tipo")
    public ResponseEntity<APIResponse> establecerPredeterminada(@PathVariable Long id) {
        applicationService.establecerPredeterminada(id);
        return ResponseEntity.ok(new APIResponse("Plantilla marcada como predeterminada", HttpStatus.OK, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una plantilla")
    public ResponseEntity<APIResponse> eliminar(@PathVariable Long id) {
        applicationService.eliminar(id);
        return ResponseEntity.ok(new APIResponse("Plantilla eliminada", HttpStatus.OK, false));
    }
}
