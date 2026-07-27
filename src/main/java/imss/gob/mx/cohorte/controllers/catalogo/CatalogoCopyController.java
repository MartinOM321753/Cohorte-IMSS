package imss.gob.mx.cohorte.controllers.catalogo;

import imss.gob.mx.cohorte.application.catalogo.CatalogoCopyApplicationService;
import imss.gob.mx.cohorte.controllers.catalogo.dto.CopiarCatalogosRequestDTO;
import imss.gob.mx.cohorte.controllers.catalogo.dto.CopiarCatalogosResponseDTO;
import imss.gob.mx.cohorte.controllers.catalogo.dto.PreviewCopiaResponseDTO;
import imss.gob.mx.cohorte.utils.APIResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/catalogos/copiar")
@RequiredArgsConstructor
public class CatalogoCopyController {

    private final CatalogoCopyApplicationService catalogoCopyApplicationService;

    @PostMapping
    public ResponseEntity<APIResponse> copiar(@Valid @RequestBody CopiarCatalogosRequestDTO request) {
        CopiarCatalogosResponseDTO resultado = catalogoCopyApplicationService.copiar(request);
        return new ResponseEntity<>(
                new APIResponse("Copia de catálogos completada", resultado, false, HttpStatus.OK),
                HttpStatus.OK);
    }

    @PostMapping("/preview")
    public ResponseEntity<APIResponse> preview(@Valid @RequestBody CopiarCatalogosRequestDTO request) {
        PreviewCopiaResponseDTO preview = catalogoCopyApplicationService.preview(request);
        return new ResponseEntity<>(
                new APIResponse("Preview de copia generado", preview, false, HttpStatus.OK),
                HttpStatus.OK);
    }
}
