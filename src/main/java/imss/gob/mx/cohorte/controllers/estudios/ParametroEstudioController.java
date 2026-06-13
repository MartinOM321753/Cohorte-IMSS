package imss.gob.mx.cohorte.controllers.estudios;

import imss.gob.mx.cohorte.application.GestionEstudiosApplicationService;
import imss.gob.mx.cohorte.controllers.estudios.dto.*;
import imss.gob.mx.cohorte.modules.estudios.parametros.OpcionParametro;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/estudios/parametros")
@AllArgsConstructor
@Validated
@Tag(name = "Parámetros de Estudio", description = "Gestión de parámetros y sus opciones para tipos de estudio médico")
@SecurityRequirement(name = "bearerAuth")
public class ParametroEstudioController {

    private final GestionEstudiosApplicationService gestionEstudiosApplicationService;

    @PostMapping
    @Operation(summary = "Crear parámetro de estudio", description = "Registra un nuevo parámetro. Si el tipo es TEXTO_OPCIONES, incluir el campo 'opciones'.")
    public ResponseEntity<APIResponse> createParametro(@Valid @RequestBody ParametroEstudioRequestDTO dto) {
        ParametroEstudio parametro = new ParametroEstudio();
        TipoEstudio tipoEstudio = new TipoEstudio();
        tipoEstudio.setId(dto.getIdTipoEstudio());
        parametro.setTipoEstudio(tipoEstudio);
        parametro.setNombre(dto.getNombre());
        parametro.setUnidad(dto.getUnidad());
        parametro.setTipo(dto.getTipo());
        parametro.setValorMinimo(dto.getValorMinimo());
        parametro.setValorMaximo(dto.getValorMaximo());
        ParametroEstudio creado = gestionEstudiosApplicationService.createParametro(parametro, dto.getOpciones());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(EstudioMapper.toParametroDTO(creado), "Parámetro creado correctamente", HttpStatus.CREATED, false));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar parámetro de estudio", description = "Actualiza el parámetro. Si el tipo es TEXTO_OPCIONES, el campo 'opciones' reemplaza las existentes.")
    public ResponseEntity<APIResponse> updateParametro(
        @Parameter(description = "Identificador único del parámetro de estudio a actualizar", required = true)
        @PathVariable Long id,
        @Valid @RequestBody ParametroEstudioRequestDTO dto) {
        ParametroEstudio parametro = new ParametroEstudio();
        parametro.setId(id);
        TipoEstudio tipoEstudio = new TipoEstudio();
        tipoEstudio.setId(dto.getIdTipoEstudio());
        parametro.setTipoEstudio(tipoEstudio);
        parametro.setNombre(dto.getNombre());
        parametro.setUnidad(dto.getUnidad());
        parametro.setTipo(dto.getTipo());
        parametro.setValorMinimo(dto.getValorMinimo());
        parametro.setValorMaximo(dto.getValorMaximo());
        ParametroEstudio actualizado = gestionEstudiosApplicationService.updateParametro(parametro, dto.getOpciones());
        return ResponseEntity.ok(new APIResponse(EstudioMapper.toParametroDTO(actualizado), "Parámetro actualizado correctamente", HttpStatus.OK, false));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar parámetro de estudio")
    public ResponseEntity<APIResponse> deleteParametro(
        @Parameter(description = "Identificador único del parámetro de estudio a eliminar", required = true)
        @PathVariable Long id) {
        gestionEstudiosApplicationService.deleteParametro(id);
        return ResponseEntity.ok(new APIResponse("Parámetro eliminado correctamente", HttpStatus.OK, false));
    }

    // ─── Opciones para TEXTO_OPCIONES ────────────────────────────────────────

    @PostMapping("/{id}/opciones")
    @Operation(summary = "Agregar opción a parámetro TEXTO_OPCIONES")
    public ResponseEntity<APIResponse> addOpcion(
        @PathVariable Long id,
        @Valid @RequestBody OpcionParametroRequestDTO dto) {
        OpcionParametro opcion = gestionEstudiosApplicationService.addOpcion(id, dto.getValor());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new APIResponse(opcion.getValor(), "Opción agregada correctamente", HttpStatus.CREATED, false));
    }

    @DeleteMapping("/opciones/{opcionId}")
    @Operation(summary = "Eliminar opción de parámetro TEXTO_OPCIONES")
    public ResponseEntity<APIResponse> deleteOpcion(@PathVariable Long opcionId) {
        gestionEstudiosApplicationService.deleteOpcion(opcionId);
        return ResponseEntity.ok(new APIResponse("Opción eliminada correctamente", HttpStatus.OK, false));
    }
}
