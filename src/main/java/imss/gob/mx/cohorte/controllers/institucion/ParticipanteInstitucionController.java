package imss.gob.mx.cohorte.controllers.institucion;

import imss.gob.mx.cohorte.application.institucion.ParticipanteInstitucionApplicationService;
import imss.gob.mx.cohorte.controllers.institucion.dto.ParticipanteInstitucionMapper;
import imss.gob.mx.cohorte.controllers.institucion.dto.ParticipanteInstitucionRequestDTO;
import imss.gob.mx.cohorte.modules.institucion.ParticipanteInstitucion;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes/{uuid}/instituciones")
@RequiredArgsConstructor
@Tag(name = "Instituciones del Participante",
        description = "Vínculo M:N entre participantes e instituciones — registra todas las instituciones " +
                "con las que un participante tiene relación activa (reclutamiento, seguimiento, traslados, etc.)")
@SecurityRequirement(name = "bearerAuth")
public class ParticipanteInstitucionController {

    private final ParticipanteInstitucionApplicationService applicationService;

    @GetMapping
    @Operation(summary = "Listar instituciones vinculadas a un participante")
    public ResponseEntity<APIResponse> getByParticipante(@PathVariable String uuid) {
        List<ParticipanteInstitucion> list = applicationService.findAllByPacienteUuid(uuid);
        return ResponseEntity.ok(new APIResponse("Instituciones vinculadas", ParticipanteInstitucionMapper.toResponseDTOList(list), false, HttpStatus.OK));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('ENCARGADO')")
    @Operation(summary = "Vincular un participante a una institución")
    public ResponseEntity<APIResponse> vincular(
            @PathVariable String uuid,
            @Validated @RequestBody ParticipanteInstitucionRequestDTO dto) {
        ParticipanteInstitucion vinculo = applicationService.vincular(uuid, dto.getIdInstitucion(), dto.getObservaciones());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Participante vinculado exitosamente", ParticipanteInstitucionMapper.toResponseDTO(vinculo), false, HttpStatus.CREATED));
    }

    @DeleteMapping("/{idInstitucion}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('ENCARGADO')")
    @Operation(summary = "Desvincular un participante de una institución",
            description = "No elimina el registro — lo marca como inactivo para preservar la auditoría histórica.")
    public ResponseEntity<APIResponse> desvincular(
            @PathVariable String uuid,
            @Parameter(description = "ID de la institución a desvincular", required = true) @PathVariable Long idInstitucion) {
        applicationService.desvincular(uuid, idInstitucion);
        return ResponseEntity.ok(new APIResponse("Participante desvinculado exitosamente", null, false, HttpStatus.OK));
    }
}
