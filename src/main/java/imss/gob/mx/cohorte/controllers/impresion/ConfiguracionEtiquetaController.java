package imss.gob.mx.cohorte.controllers.impresion;

import imss.gob.mx.cohorte.application.impresion.ConfiguracionEtiquetaApplicationService;
import imss.gob.mx.cohorte.controllers.impresion.dto.ConfiguracionEtiquetaMapper;
import imss.gob.mx.cohorte.controllers.impresion.dto.ConfiguracionEtiquetaRequestDTO;
import imss.gob.mx.cohorte.controllers.impresion.dto.ConfiguracionEtiquetaResponseDTO;
import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.DisposicionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.TipoCodigo;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/impresion/configuraciones")
@RequiredArgsConstructor
@Tag(name = "Configuración de Etiquetas", description = "Gestión de configuraciones de impresión de etiquetas por institución")
@SecurityRequirement(name = "bearerAuth")
public class ConfiguracionEtiquetaController {

    private final ConfiguracionEtiquetaApplicationService applicationService;

    @GetMapping
    @Operation(summary = "Listar configuraciones de etiquetas de la institución")
    public ResponseEntity<APIResponse> listar() {
        List<ConfiguracionEtiqueta> list = applicationService.listar();
        return ResponseEntity.ok(new APIResponse(
                "Configuraciones encontradas",
                ConfiguracionEtiquetaMapper.toResponseDTOList(list),
                false, HttpStatus.OK));
    }

    @GetMapping("/activas")
    @Operation(summary = "Listar solo las configuraciones activas")
    public ResponseEntity<APIResponse> listarActivas() {
        List<ConfiguracionEtiqueta> list = applicationService.listarActivas();
        return ResponseEntity.ok(new APIResponse(
                "Configuraciones activas encontradas",
                ConfiguracionEtiquetaMapper.toResponseDTOList(list),
                false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una configuración por ID")
    public ResponseEntity<APIResponse> obtener(@PathVariable Long id) {
        ConfiguracionEtiqueta config = applicationService.obtener(id);
        return ResponseEntity.ok(new APIResponse(
                "Configuración encontrada",
                ConfiguracionEtiquetaMapper.toResponseDTO(config),
                false, HttpStatus.OK));
    }

    @GetMapping("/predeterminada")
    @Operation(summary = "Obtener la configuración predeterminada de la institución")
    public ResponseEntity<APIResponse> obtenerPredeterminada() {
        ConfiguracionEtiqueta config = applicationService.obtenerPredeterminada();
        return ResponseEntity.ok(new APIResponse(
                config != null ? "Configuración predeterminada encontrada" : "No hay configuración predeterminada",
                config != null ? ConfiguracionEtiquetaMapper.toResponseDTO(config) : null,
                false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear nueva configuración de etiqueta")
    public ResponseEntity<APIResponse> crear(@Validated @RequestBody ConfiguracionEtiquetaRequestDTO dto) {
        ConfiguracionEtiqueta entity = ConfiguracionEtiquetaMapper.toEntity(dto);
        ConfiguracionEtiqueta saved = applicationService.crear(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                "Configuración creada",
                ConfiguracionEtiquetaMapper.toResponseDTO(saved),
                false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar configuración de etiqueta")
    public ResponseEntity<APIResponse> actualizar(@PathVariable Long id,
                                                   @Validated @RequestBody ConfiguracionEtiquetaRequestDTO dto) {
        ConfiguracionEtiqueta datos = ConfiguracionEtiquetaMapper.toEntity(dto);
        ConfiguracionEtiqueta updated = applicationService.actualizar(id, datos);
        return ResponseEntity.ok(new APIResponse(
                "Configuración actualizada",
                ConfiguracionEtiquetaMapper.toResponseDTO(updated),
                false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/toggle")
    @Operation(summary = "Activar/desactivar configuración")
    public ResponseEntity<APIResponse> toggleActivo(@PathVariable Long id) {
        applicationService.toggleActivo(id);
        return ResponseEntity.ok(new APIResponse("Estado actualizado", null, false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/predeterminada")
    @Operation(summary = "Establecer como configuración predeterminada")
    public ResponseEntity<APIResponse> establecerPredeterminada(@PathVariable Long id) {
        applicationService.establecerPredeterminada(id);
        return ResponseEntity.ok(new APIResponse("Configuración establecida como predeterminada", null, false, HttpStatus.OK));
    }

    @GetMapping("/opciones")
    @Operation(summary = "Obtener las opciones disponibles para tipo de código y disposición")
    public ResponseEntity<APIResponse> obtenerOpciones() {
        Map<String, Object> opciones = Map.of(
                "tiposCodigo", Arrays.stream(TipoCodigo.values()).map(Enum::name).toList(),
                "disposiciones", Arrays.stream(DisposicionEtiqueta.values()).map(Enum::name).toList()
        );
        return ResponseEntity.ok(new APIResponse("Opciones disponibles", opciones, false, HttpStatus.OK));
    }
}
