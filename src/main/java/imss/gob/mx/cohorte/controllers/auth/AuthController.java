package imss.gob.mx.cohorte.controllers.auth;


import imss.gob.mx.cohorte.application.AuthApplicationService;
import imss.gob.mx.cohorte.controllers.auth.dto.LoginRequestDTO;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@AllArgsConstructor
@Tag(name = "Controlador de Autenticación", description = "Operaciones relacionadas con autenticación y registro de usuarios")
public class AuthController {
    private final AuthApplicationService authService;


    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Permite a un usuario iniciar sesión en el sistema")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Inicio de sesión exitoso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Usuario o contraseña incorrectos",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            )
    })

    public ResponseEntity<APIResponse> login(@jakarta.validation.Valid @RequestBody LoginRequestDTO payload) {

        String token = authService.login(payload);

        APIResponse response = new APIResponse(
                "Inicio de sesión exitoso",
                token,
                false,
                HttpStatus.OK
        );

        return ResponseEntity.ok(response);
    }



  /*  @GetMapping("/forgotPassword")
    public ResponseEntity<APIResponse> forgotPassword(@RequestParam String email) {
        APIResponse response = authService.forwardPassword(email);
        return new ResponseEntity<>(response, response.getStatus());
    }*/
}