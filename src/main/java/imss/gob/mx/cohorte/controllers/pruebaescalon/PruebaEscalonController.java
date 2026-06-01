package imss.gob.mx.cohorte.controllers.pruebaescalon;

import imss.gob.mx.cohorte.application.PruebaEscalonApplicationService;
import imss.gob.mx.cohorte.controllers.pruebaescalon.dto.*;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/prueba-escalon")
@AllArgsConstructor
@Tag(name = "Prueba Escalón", description = "Gestión de pruebas escalón")
@SecurityRequirement(name = "bearerAuth")
@Deprecated
public class PruebaEscalonController {

    private final PruebaEscalonApplicationService pruebaEscalonApplicationService;

    @GetMapping
    @Operation(summary = "Listar todas las pruebas escalón", description = "Obtiene una lista completa de todas las pruebas escalón registradas en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getAll() {
        List<PruebaEscalon> pruebas = pruebaEscalonApplicationService.getAll();
        return ResponseEntity.ok(new APIResponse("Pruebas escalón encontradas", PruebaEscalonMapper.toResponseDTOList(pruebas), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener prueba escalón por ID", description = "Obtiene los detalles de una prueba escalón específica utilizando su identificador único")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> getById(
        @Parameter(description = "Identificador único de la prueba escalón", required = true)
        @PathVariable Long id) {
        PruebaEscalon prueba = pruebaEscalonApplicationService.getOne(id);
        return ResponseEntity.ok(new APIResponse("Prueba escalón encontrada", PruebaEscalonMapper.toResponseDTO(prueba), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear nueva prueba escalón", description = "Registra una nueva prueba escalón en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Prueba escalón creada exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> create(@Validated @RequestBody PruebaEscalonRequestDTO dto) {
        PruebaEscalon prueba = PruebaEscalonMapper.toEntity(dto);
        PruebaEscalon saved = pruebaEscalonApplicationService.create(prueba);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Prueba escalón registrada exitosamente", PruebaEscalonMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar prueba escalón", description = "Actualiza la información de una prueba escalón existente identificada por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> update(
        @Parameter(description = "Identificador único de la prueba escalón a actualizar", required = true)
        @PathVariable Long id,
        @Validated @RequestBody PruebaEscalonRequestDTO dto) {
        PruebaEscalon prueba = PruebaEscalonMapper.toEntity(dto);
        PruebaEscalon updated = pruebaEscalonApplicationService.update(id, prueba);
        return ResponseEntity.ok(new APIResponse("Prueba escalón actualizada", PruebaEscalonMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar prueba escalón", description = "Elimina una prueba escalón existente identificada por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> delete(
        @Parameter(description = "Identificador único de la prueba escalón a eliminar", required = true)
        @PathVariable Long id) {
        pruebaEscalonApplicationService.delete(id);
        return ResponseEntity.ok(new APIResponse("Prueba escalón eliminada", null, false, HttpStatus.OK));
    }

    @PostMapping("/etapas")
    @Operation(summary = "Agregar etapa a prueba escalón", description = "Registra una nueva etapa asociada a una prueba escalón existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Etapa agregada exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> addEtapa(@Validated @RequestBody EtapaRequestDTO dto) {
        PruebaEscalonEtapa etapa = new PruebaEscalonEtapa();
        PruebaEscalon prueba = new PruebaEscalon();
        prueba.setId(dto.getIdPruebaEscalon());
        etapa.setPruebaEscalon(prueba);
        etapa.setEtapa(PruebaEscalonEtapa.Etapa.valueOf(dto.getEtapa()));
        etapa.setObservaciones(dto.getObservaciones());
        PruebaEscalonEtapa saved = pruebaEscalonApplicationService.createEtapa(etapa);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Etapa agregada exitosamente", PruebaEscalonMapper.toEtapaResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/etapas/{id}")
    @Operation(summary = "Actualizar etapa", description = "Actualiza la información de una etapa de prueba escalón existente identificada por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> updateEtapa(
        @Parameter(description = "Identificador único de la etapa a actualizar", required = true)
        @PathVariable Long id,
        @Validated @RequestBody EtapaRequestDTO dto) {
        PruebaEscalonEtapa etapa = new PruebaEscalonEtapa();
        etapa.setId(id);
        etapa.setEtapa(PruebaEscalonEtapa.Etapa.valueOf(dto.getEtapa()));
        etapa.setObservaciones(dto.getObservaciones());
        PruebaEscalonEtapa updated = pruebaEscalonApplicationService.updateEtapa(id, etapa);
        return ResponseEntity.ok(new APIResponse("Etapa actualizada", PruebaEscalonMapper.toEtapaResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/etapas/{id}")
    @Operation(summary = "Eliminar etapa", description = "Elimina una etapa de prueba escalón existente identificada por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> deleteEtapa(
        @Parameter(description = "Identificador único de la etapa a eliminar", required = true)
        @PathVariable Long id) {
        pruebaEscalonApplicationService.deleteEtapa(id);
        return ResponseEntity.ok(new APIResponse("Etapa eliminada", null, false, HttpStatus.OK));
    }

    @PostMapping("/mediciones")
    @Operation(summary = "Registrar medición", description = "Registra una nueva medición asociada a una etapa de prueba escalón")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Medición registrada exitosamente",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> addMedicion(@Validated @RequestBody MedicionRequestDTO dto) {
        PruebaEscalonMedicion medicion = new PruebaEscalonMedicion();
        PruebaEscalonEtapa etapa = new PruebaEscalonEtapa();
        etapa.setId(dto.getIdEtapa());
        medicion.setEtapa(etapa);
        medicion.setParametro(PruebaEscalonMedicion.Parametro.valueOf(dto.getParametro()));
        medicion.setValor(dto.getValor());
        medicion.setUnidad(dto.getUnidad());
        PruebaEscalonMedicion saved = pruebaEscalonApplicationService.createMedicion(medicion);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Medición registrada exitosamente", PruebaEscalonMapper.toMedicionResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/mediciones/{id}")
    @Operation(summary = "Actualizar medición", description = "Actualiza la información de una medición existente identificada por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> updateMedicion(
        @Parameter(description = "Identificador único de la medición a actualizar", required = true)
        @PathVariable Long id,
        @Validated @RequestBody MedicionRequestDTO dto) {
        PruebaEscalonMedicion medicion = new PruebaEscalonMedicion();
        medicion.setId(id);
        medicion.setParametro(PruebaEscalonMedicion.Parametro.valueOf(dto.getParametro()));
        medicion.setValor(dto.getValor());
        medicion.setUnidad(dto.getUnidad());
        PruebaEscalonMedicion updated = pruebaEscalonApplicationService.updateMedicion(id, medicion);
        return ResponseEntity.ok(new APIResponse("Medición actualizada", PruebaEscalonMapper.toMedicionResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/mediciones/{id}")
    @Operation(summary = "Eliminar medición", description = "Elimina una medición existente identificada por su ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Éxito",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "404", description = "Recurso no encontrado",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "400", description = "Solicitud inválida",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class))),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = APIResponse.class)))
    })
    public ResponseEntity<APIResponse> deleteMedicion(
        @Parameter(description = "Identificador único de la medición a eliminar", required = true)
        @PathVariable Long id) {
        pruebaEscalonApplicationService.deleteMedicion(id);
        return ResponseEntity.ok(new APIResponse("Medición eliminada", null, false, HttpStatus.OK));
    }
}
