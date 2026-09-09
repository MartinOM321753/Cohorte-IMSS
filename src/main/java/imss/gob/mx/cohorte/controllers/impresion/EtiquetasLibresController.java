package imss.gob.mx.cohorte.controllers.impresion;

import imss.gob.mx.cohorte.controllers.impresion.dto.TablaEtiquetasDTO;
import imss.gob.mx.cohorte.services.impresion.EtiquetasLibresService;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Impresion de etiquetas a partir de un archivo externo.
 *
 * <p>Un solo endpoint y sin estado: el archivo entra, la tabla sale, y ahi
 * termina la participacion del servidor. Elegir columnas, maquetar y mandar a la
 * impresora ocurre en la pantalla, con el mismo motor y la misma configuracion de
 * hoja que las etiquetas de muestras y documentos.</p>
 *
 * <p>No hay endpoint de guardado a proposito: el lote no se persiste. Reimprimir
 * significa volver a subir el archivo, que es la unica forma de garantizar que lo
 * impreso corresponde a lo que hoy dice la hoja de calculo.</p>
 */
@RestController
@RequestMapping("/api/impresion/etiquetas-libres")
@RequiredArgsConstructor
@Tag(name = "Etiquetas desde archivo", description = "Impresion de etiquetas con datos de un archivo externo")
@SecurityRequirement(name = "bearerAuth")
public class EtiquetasLibresController {

    private final EtiquetasLibresService service;

    @PostMapping("/leer")
    @Operation(summary = "Leer un archivo tabular para imprimirlo como etiquetas",
            description = "Devuelve el contenido tal como se ve en la hoja de calculo. No guarda nada.")
    public ResponseEntity<APIResponse> leer(@RequestParam("archivo") MultipartFile archivo) {
        TablaEtiquetasDTO tabla = service.leer(archivo);

        return ResponseEntity.ok(new APIResponse(
                "El archivo se leyo correctamente: " + tabla.totalFilas() + " etiqueta(s)",
                tabla,
                false, HttpStatus.OK));
    }
}
