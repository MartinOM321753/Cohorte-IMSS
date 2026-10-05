package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.LoteApplicationService;
import imss.gob.mx.cohorte.controllers.almacenamiento.dto.CartaFolioResponseDTO;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/almacenamiento/lotes")
@AllArgsConstructor
@Tag(name = "Lotes", description = "Carta de biobanco por participante: tubos primarios y lotes de alícuotas")
@SecurityRequirement(name = "bearerAuth")
public class LoteController {

    private final LoteApplicationService loteApplicationService;

    @GetMapping("/carta/{uuid}")
    @Operation(summary = "Carta de biobanco de un participante",
               description = "Todo lo que se le hizo: tubos primarios y lotes de alícuotas numeradas 1…N.")
    public ResponseEntity<APIResponse> carta(
            @Parameter(description = "UUID del participante") @PathVariable String uuid) {
        CartaFolioResponseDTO carta = loteApplicationService.carta(uuid);
        return ResponseEntity.ok(new APIResponse("Carta encontrada", carta, false, HttpStatus.OK));
    }

    @GetMapping("/cards")
    @Operation(summary = "Cards de procesamientos",
               description = "Una card por (participante × protocolo), con tubos primarios y lotes de alícuotas.")
    public ResponseEntity<APIResponse> cards() {
        return ResponseEntity.ok(new APIResponse("Cards encontradas",
                loteApplicationService.listarCards(), false, HttpStatus.OK));
    }
}
