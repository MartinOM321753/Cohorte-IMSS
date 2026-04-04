package imss.gob.mx.cohorte.controllers.estudios;

import imss.gob.mx.cohorte.application.EstudiosApplicationService;
import imss.gob.mx.cohorte.application.GestionEstudiosApplicationService;
import imss.gob.mx.cohorte.controllers.estudios.dto.*;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/estudios")
@AllArgsConstructor
@Validated
@Tag(name = "Estudios Médicos")
@SecurityRequirement(name = "bearerAuth")
public class EstudioMedicoController {

    private final EstudiosApplicationService estudiosApplicationService;
    private final GestionEstudiosApplicationService gestionEstudiosApplicationService;

    @GetMapping
    @Operation(summary = "Listar todos los estudios médicos")
    public ResponseEntity<APIResponse> getAll() {
        List<EstudioMedico> estudios = estudiosApplicationService.getAllEstudios();
        List<EstudioMedicoResponseDTO> dtos = EstudioMapper.toResponseDTOList(estudios);
        return ResponseEntity.ok(new APIResponse(dtos, "Estudios obtenidos correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener estudio médico por ID")
    public ResponseEntity<APIResponse> getById(@PathVariable Long id) {
        EstudioMedico estudio = estudiosApplicationService.getEstudio(id);
        EstudioMedicoResponseDTO dto = EstudioMapper.toResponseDTO(estudio);
        return ResponseEntity.ok(new APIResponse(dto, "Estudio obtenido correctamente", HttpStatus.OK, false));
    }

    @PostMapping
    @Operation(summary = "Crear estudio médico con resultados")
    public ResponseEntity<APIResponse> create(@Valid @RequestBody EstudioMedicoRequestDTO dto) {
        EstudioMedico entity = EstudioMapper.toEntity(dto);
        entity.setFechaRegistro(LocalDateTime.now());
        EstudioMedico creado = estudiosApplicationService.createEstudio(entity);
        EstudioMedicoResponseDTO responseDTO = EstudioMapper.toResponseDTO(creado);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(responseDTO, "Estudio creado correctamente", HttpStatus.CREATED, false));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar estudio médico")
    public ResponseEntity<APIResponse> update(@PathVariable Long id, @Valid @RequestBody EstudioMedicoRequestDTO dto) {
        EstudioMedico entity = EstudioMapper.toEntity(dto);
        EstudioMedico actualizado = estudiosApplicationService.updateEstudio(id, entity);
        EstudioMedicoResponseDTO responseDTO = EstudioMapper.toResponseDTO(actualizado);
        return ResponseEntity.ok(new APIResponse(responseDTO, "Estudio actualizado correctamente", HttpStatus.OK, false));
    }

    @GetMapping("/tipos")
    @Operation(summary = "Listar tipos de estudio activos")
    public ResponseEntity<APIResponse> getTiposActivos() {
        List<TipoEstudio> tipos = gestionEstudiosApplicationService.getAllByEstatus();
        List<TipoEstudioResponseDTO> dtos = tipos.stream()
            .map(t -> TipoEstudioResponseDTO.builder()
                .id(t.getId())
                .nombre(t.getNombre())
                .descripcion(t.getDescripcion())
                .activo(t.getActivo())
                .build())
            .collect(Collectors.toList());
        return ResponseEntity.ok(new APIResponse(dtos, "Tipos de estudio obtenidos correctamente", HttpStatus.OK, false));
    }

    @PostMapping("/tipos")
    @Operation(summary = "Crear tipo de estudio")
    public ResponseEntity<APIResponse> createTipo(@Valid @RequestBody TipoEstudioRequestDTO dto) {
        TipoEstudio tipo = new TipoEstudio();
        tipo.setNombre(dto.getNombre());
        tipo.setDescripcion(dto.getDescripcion());
        tipo.setActivo(true);
        tipo.setFechaCreacion(LocalDateTime.now());
        TipoEstudio creado = gestionEstudiosApplicationService.createTipoService(tipo);
        TipoEstudioResponseDTO responseDTO = TipoEstudioResponseDTO.builder()
            .id(creado.getId())
            .nombre(creado.getNombre())
            .descripcion(creado.getDescripcion())
            .activo(creado.getActivo())
            .build();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(responseDTO, "Tipo de estudio creado correctamente", HttpStatus.CREATED, false));
    }

    @PutMapping("/tipos/{id}")
    @Operation(summary = "Actualizar tipo de estudio")
    public ResponseEntity<APIResponse> updateTipo(@PathVariable Long id, @Valid @RequestBody TipoEstudioRequestDTO dto) {
        TipoEstudio existente = gestionEstudiosApplicationService.getOne(id);
        existente.setNombre(dto.getNombre());
        existente.setDescripcion(dto.getDescripcion());
        TipoEstudio actualizado = gestionEstudiosApplicationService.update(existente);
        TipoEstudioResponseDTO responseDTO = TipoEstudioResponseDTO.builder()
            .id(actualizado.getId())
            .nombre(actualizado.getNombre())
            .descripcion(actualizado.getDescripcion())
            .activo(actualizado.getActivo())
            .build();
        return ResponseEntity.ok(new APIResponse(responseDTO, "Tipo de estudio actualizado correctamente", HttpStatus.OK, false));
    }

    @PutMapping("/tipos/{id}/toggle")
    @Operation(summary = "Activar o desactivar tipo de estudio")
    public ResponseEntity<APIResponse> toggleTipo(@PathVariable Long id) {
        Boolean activo = gestionEstudiosApplicationService.Active(id);
        return ResponseEntity.ok(new APIResponse(activo, "Estado del tipo de estudio actualizado correctamente", HttpStatus.OK, false));
    }

    @PostMapping("/parametros")
    @Operation(summary = "Crear parámetro de estudio")
    public ResponseEntity<APIResponse> createParametro(@Valid @RequestBody ParametroEstudioRequestDTO dto) {
        ParametroEstudio parametro = new ParametroEstudio();
        TipoEstudio tipoEstudio = new TipoEstudio();
        tipoEstudio.setId(dto.getIdTipoEstudio());
        parametro.setTipoEstudio(tipoEstudio);
        parametro.setNombre(dto.getNombre());
        parametro.setUnidad(dto.getUnidad());
        ParametroEstudio creado = gestionEstudiosApplicationService.createParametro(parametro);
        ParametroEstudioResponseDTO responseDTO = ParametroEstudioResponseDTO.builder()
            .id(creado.getId())
            .nombre(creado.getNombre())
            .unidad(creado.getUnidad())
            .tipoEstudio(creado.getTipoEstudio() != null ? creado.getTipoEstudio().getNombre() : null)
            .build();
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(responseDTO, "Parámetro creado correctamente", HttpStatus.CREATED, false));
    }

    @PutMapping("/parametros/{id}")
    @Operation(summary = "Actualizar parámetro de estudio")
    public ResponseEntity<APIResponse> updateParametro(@PathVariable Long id, @Valid @RequestBody ParametroEstudioRequestDTO dto) {
        ParametroEstudio parametro = new ParametroEstudio();
        parametro.setId(id);
        TipoEstudio tipoEstudio = new TipoEstudio();
        tipoEstudio.setId(dto.getIdTipoEstudio());
        parametro.setTipoEstudio(tipoEstudio);
        parametro.setNombre(dto.getNombre());
        parametro.setUnidad(dto.getUnidad());
        ParametroEstudio actualizado = gestionEstudiosApplicationService.updateParametro(parametro);
        ParametroEstudioResponseDTO responseDTO = ParametroEstudioResponseDTO.builder()
            .id(actualizado.getId())
            .nombre(actualizado.getNombre())
            .unidad(actualizado.getUnidad())
            .tipoEstudio(actualizado.getTipoEstudio() != null ? actualizado.getTipoEstudio().getNombre() : null)
            .build();
        return ResponseEntity.ok(new APIResponse(responseDTO, "Parámetro actualizado correctamente", HttpStatus.OK, false));
    }

    @DeleteMapping("/parametros/{id}")
    @Operation(summary = "Eliminar parámetro de estudio")
    public ResponseEntity<APIResponse> deleteParametro(@PathVariable Long id) {
        gestionEstudiosApplicationService.deleteParametro(id);
        return ResponseEntity.ok(new APIResponse("Parámetro eliminado correctamente", HttpStatus.OK, false));
    }
}
