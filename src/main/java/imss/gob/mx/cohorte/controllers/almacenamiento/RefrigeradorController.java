package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.PisoRefrigeradorApplicationService;
import imss.gob.mx.cohorte.application.almacenamiento.RefrieradorApplicationService;
import imss.gob.mx.cohorte.controllers.DTO.PisosDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.RefrigeradorMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.RefrigeradorRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.RefrigeradorResponseDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PisoRefrigerador;
import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.Refrigerador;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/almacenamiento/refrigeradores")
@AllArgsConstructor
@Tag(name = "Refrigeradores", description = "Gestión de refrigeradores criogénicos")
@SecurityRequirement(name = "bearerAuth")
public class RefrigeradorController {

    private final RefrieradorApplicationService refrieradorApplicationService;
    private final PisoRefrigeradorApplicationService pisoRefrigeradorApplicationService;

    @GetMapping
    @Operation(summary = "Listar todos los refrigeradores")
    public ResponseEntity<APIResponse> getAll() {
        List<Refrigerador> list = refrieradorApplicationService.getAllRefrigeradores();
        return ResponseEntity.ok(new APIResponse("Refrigeradores encontrados", RefrigeradorMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener refrigerador por ID")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        Refrigerador ref = refrieradorApplicationService.getRefrigerador(id);
        return ResponseEntity.ok(new APIResponse("Refrigerador encontrado", RefrigeradorMapper.toResponseDTO(ref), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Crear refrigerador")
    public ResponseEntity<APIResponse> create(@Validated @RequestBody RefrigeradorRequestDTO dto) {
        Refrigerador entity = RefrigeradorMapper.toEntity(dto);
        Refrigerador saved = refrieradorApplicationService.createRefrigerador(entity);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Refrigerador creado exitosamente", RefrigeradorMapper.toResponseDTO(saved), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar refrigerador")
    public ResponseEntity<APIResponse> update(@PathVariable Long id, @Validated @RequestBody RefrigeradorRequestDTO dto) {
        Refrigerador entity = RefrigeradorMapper.toEntity(dto);
        Refrigerador updated = refrieradorApplicationService.updateRefrigerador(id, entity);
        return ResponseEntity.ok(new APIResponse("Refrigerador actualizado", RefrigeradorMapper.toResponseDTO(updated), false, HttpStatus.OK));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar refrigerador")
    public ResponseEntity<APIResponse> delete(@PathVariable Long id) {
        refrieradorApplicationService.deleteRefrigerador(id);
        return ResponseEntity.ok(new APIResponse("Refrigerador eliminado", null, false, HttpStatus.OK));
    }

    @GetMapping("/{id}/pisos")
    @Operation(summary = "Listar pisos del refrigerador")
    public ResponseEntity<APIResponse> getPisos(@PathVariable Long id) {
        List<PisoRefrigerador> pisos = pisoRefrigeradorApplicationService.getAllPisos(id);
        return ResponseEntity.ok(new APIResponse("Pisos encontrados", pisos, false, HttpStatus.OK));
    }

    @PostMapping("/pisos")
    @Operation(summary = "Crear piso(s) con generación automática de posiciones")
    public ResponseEntity<APIResponse> createPisos(@Validated @RequestBody PisosDTO pisosDTO) {
        List<PisoRefrigerador> pisos = pisoRefrigeradorApplicationService.createPisos(pisosDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse("Pisos creados exitosamente", pisos, false, HttpStatus.CREATED));
    }
}
