package imss.gob.mx.cohorte.controllers.auth;


import imss.gob.mx.cohorte.controllers.auth.dto.LoginRequestDTO;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.utils.APIResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Controlador de Autenticación", description = "Operaciones relacionadas con autenticación y registro de usuarios")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("")
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
    public ResponseEntity<APIResponse> doLogin(@RequestBody LoginRequestDTO payload){
        APIResponse response = authService.doLogin(payload);
        return new ResponseEntity<>(response, response.getStatus());
    }

    @PostMapping("/register")
    @Operation(summary = "Registrar usuario", description = "Permite registrar un nuevo usuario en el sistema")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Usuario registrado exitosamente",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "El usuario ya existe",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Error interno del servidor",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = APIResponse.class))
            )
    })
    public ResponseEntity<APIResponse> register(@RequestBody BeanUser payload){
        APIResponse response = authService.register(payload);
        return new ResponseEntity<>(response, response.getStatus());
    }

  /*  @GetMapping("/forgotPassword")
    public ResponseEntity<APIResponse> forgotPassword(@RequestParam String email) {
        APIResponse response = authService.forwardPassword(email);
        return new ResponseEntity<>(response, response.getStatus());
    }*/
}