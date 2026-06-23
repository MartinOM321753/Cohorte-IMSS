package imss.gob.mx.cohorte.controllers.citas;

import imss.gob.mx.cohorte.application.citas.ConfiguracionHorarioApplicationService;
import imss.gob.mx.cohorte.controllers.citas.dto.ConfiguracionHorarioMapper;
import imss.gob.mx.cohorte.controllers.citas.dto.ConfiguracionHorarioRequestDTO;
import imss.gob.mx.cohorte.controllers.citas.dto.ConfiguracionHorarioResponseDTO;
import imss.gob.mx.cohorte.modules.cita.ConfiguracionHorario;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/citas/configuracion-horario")
@RequiredArgsConstructor
@Tag(name = "Configuración de Horario de Citas", description = "Gestión de horarios disponibles para citas por institución")
@SecurityRequirement(name = "bearerAuth")
public class ConfiguracionHorarioController {

    private final ConfiguracionHorarioApplicationService applicationService;

    @GetMapping
    @Operation(summary = "Listar configuraciones de horario de la institución")
    public ResponseEntity<APIResponse> listar() {
        List<ConfiguracionHorario> list = applicationService.listar();
        return ResponseEntity.ok(new APIResponse(
                "Configuraciones de horario encontradas",
                ConfiguracionHorarioMapper.toResponseDTOList(list),
                false, HttpStatus.OK));
    }

    @GetMapping("/activa")
    @Operation(summary = "Obtener la configuración de horario activa")
    public ResponseEntity<APIResponse> obtenerActiva() {
        ConfiguracionHorario config = applicationService.obtenerActiva();
        return ResponseEntity.ok(new APIResponse(
                config != null ? "Configuración activa encontrada" : "No hay configuración de horario activa",
                config != null ? ConfiguracionHorarioMapper.toResponseDTO(config) : null,
                false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una configuración de horario por ID")
    public ResponseEntity<APIResponse> obtener(@PathVariable Long id) {
        ConfiguracionHorario config = applicationService.obtener(id);
        return ResponseEntity.ok(new APIResponse(
                "Configuración encontrada",
                ConfiguracionHorarioMapper.toResponseDTO(config),
                false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear nueva configuración de horario")
    public ResponseEntity<APIResponse> crear(@Validated @RequestBody ConfiguracionHorarioRequestDTO dto) {
        ConfiguracionHorario entity = ConfiguracionHorarioMapper.toEntity(dto);
        ConfiguracionHorario saved = applicationService.crear(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse(
                "Configuración de horario creada",
                ConfiguracionHorarioMapper.toResponseDTO(saved),
                false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar configuración de horario")
    public ResponseEntity<APIResponse> actualizar(@PathVariable Long id,
                                                   @Validated @RequestBody ConfiguracionHorarioRequestDTO dto) {
        ConfiguracionHorario datos = ConfiguracionHorarioMapper.toEntity(dto);
        ConfiguracionHorario updated = applicationService.actualizar(id, datos);
        return ResponseEntity.ok(new APIResponse(
                "Configuración de horario actualizada",
                ConfiguracionHorarioMapper.toResponseDTO(updated),
                false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/activar")
    @Operation(summary = "Activar una configuración de horario (desactiva la anterior)")
    public ResponseEntity<APIResponse> activar(@PathVariable Long id) {
        applicationService.activar(id);
        return ResponseEntity.ok(new APIResponse("Configuración de horario activada", null, false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar una configuración de horario (no puede ser la activa)")
    public ResponseEntity<APIResponse> eliminar(@PathVariable Long id) {
        applicationService.eliminar(id);
        return ResponseEntity.ok(new APIResponse("Configuración de horario eliminada", null, false, HttpStatus.OK));
    }
}
