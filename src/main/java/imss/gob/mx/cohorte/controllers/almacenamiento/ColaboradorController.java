package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.ColaboradorApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.colaborador.ColaboradorMapper;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.colaborador.ColaboradorRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.colaborador.ColaboradorResponseDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.colaborador.Colaborador;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/almacenamiento/colaboradores")
@RequiredArgsConstructor
@Tag(name = "Colaboradores", description = "Personas físicas (internas o externas) que reciben muestras del biobanco en entrega/préstamo")
@SecurityRequirement(name = "bearerAuth")
public class ColaboradorController {

    private final ColaboradorApplicationService colaboradorApplicationService;

    @GetMapping
    @Operation(summary = "Lista de colaboradores de mi institución")
    public ResponseEntity<APIResponse> getAll() {
        List<Colaborador> list = colaboradorApplicationService.getDeMiInstitucion();
        return ResponseEntity.ok(new APIResponse("Colaboradores",
                ColaboradorMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/activos")
    @Operation(summary = "Colaboradores activos (para el selector de entrega)")
    public ResponseEntity<APIResponse> getActivos() {
        List<Colaborador> list = colaboradorApplicationService.getActivosDeMiInstitucion();
        return ResponseEntity.ok(new APIResponse("Colaboradores activos",
                ColaboradorMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @GetMapping("/paginado")
    @Operation(summary = "Colaboradores paginados con búsqueda")
    public ResponseEntity<APIResponse> getPaginado(
            @RequestParam(defaultValue = "") String buscar,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<Colaborador> pageResult = colaboradorApplicationService.buscarPaginado(buscar, page, size);
        Page<ColaboradorResponseDTO> dtoPage = pageResult.map(ColaboradorMapper::toResponseDTO);
        return ResponseEntity.ok(new APIResponse("Colaboradores", dtoPage, false, HttpStatus.OK));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalle de un colaborador")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        Colaborador c = colaboradorApplicationService.getById(id);
        return ResponseEntity.ok(new APIResponse("Colaborador",
                ColaboradorMapper.toResponseDTO(c), false, HttpStatus.OK));
    }

    @PostMapping
    @Operation(summary = "Registrar colaborador")
    public ResponseEntity<APIResponse> crear(@Validated @RequestBody ColaboradorRequestDTO dto) {
        Colaborador creado = colaboradorApplicationService.crear(
                ColaboradorMapper.toPersona(dto), dto.getCargo(), dto.getAdscripcion(), dto.getEsExterno());
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse("Colaborador registrado",
                ColaboradorMapper.toResponseDTO(creado), false, HttpStatus.CREATED));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar colaborador")
    public ResponseEntity<APIResponse> actualizar(@PathVariable Long id,
                                                   @Validated @RequestBody ColaboradorRequestDTO dto) {
        Colaborador actualizado = colaboradorApplicationService.actualizar(
                id, ColaboradorMapper.toPersona(dto), dto.getCargo(), dto.getAdscripcion(), dto.getEsExterno());
        return ResponseEntity.ok(new APIResponse("Colaborador actualizado",
                ColaboradorMapper.toResponseDTO(actualizado), false, HttpStatus.OK));
    }

    @PatchMapping("/{id}/activo")
    @Operation(summary = "Activar/desactivar colaborador")
    public ResponseEntity<APIResponse> setActivo(@PathVariable Long id, @RequestParam Boolean activo) {
        Colaborador c = colaboradorApplicationService.setActivo(id, activo);
        return ResponseEntity.ok(new APIResponse("Colaborador actualizado",
                ColaboradorMapper.toResponseDTO(c), false, HttpStatus.OK));
    }
}
