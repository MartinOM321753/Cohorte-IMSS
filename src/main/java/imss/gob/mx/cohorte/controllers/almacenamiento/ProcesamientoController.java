package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.ProcesamientoApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.AlicuotarTuboRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ProcesarProtocoloRequestDTO;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.ProcesarResultadoResponseDTO;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/almacenamiento/muestras")
@AllArgsConstructor
@Tag(name = "Procesamiento", description = "Instancia un protocolo sobre un participante y genera sus muestras y lotes")
@SecurityRequirement(name = "bearerAuth")
public class ProcesamientoController {

    private final ProcesamientoApplicationService procesamientoApplicationService;

    @PostMapping("/procesar")
    @Operation(summary = "Procesar un participante con un protocolo",
               description = "Genera las muestras padre (tubos primarios), los estudios y los lotes de alícuotas.")
    public ResponseEntity<APIResponse> procesar(@Validated @RequestBody ProcesarProtocoloRequestDTO dto) {
        ProcesarResultadoResponseDTO resultado = procesamientoApplicationService.procesar(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Participante procesado", resultado, false, HttpStatus.CREATED));
    }

    @PostMapping("/{id}/alicuotar-tubo")
    @Operation(summary = "Alicuotar un tubo primario (2ª pasada)",
               description = "Crea un lote nuevo con las alícuotas de un tubo primario que tiene volumen disponible.")
    public ResponseEntity<APIResponse> alicuotarTubo(
            @org.springframework.web.bind.annotation.PathVariable Long id,
            @RequestBody(required = false) AlicuotarTuboRequestDTO dto) {
        ProcesarResultadoResponseDTO resultado = procesamientoApplicationService.alicuotarTubo(
                id, dto != null ? dto.getPlanVolumenes() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new APIResponse("Tubo alicuotado", resultado, false, HttpStatus.CREATED));
    }
}
