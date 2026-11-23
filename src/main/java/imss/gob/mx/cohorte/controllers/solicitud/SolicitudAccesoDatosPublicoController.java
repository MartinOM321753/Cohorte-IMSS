package imss.gob.mx.cohorte.controllers.solicitud;

import imss.gob.mx.cohorte.controllers.solicitud.dto.SolicitudAccesoDatosRequestDTO;
import imss.gob.mx.cohorte.modules.solicitud.SolicitudAccesoDatos;
import imss.gob.mx.cohorte.services.solicitud.SolicitudAccesoDatosService;
import imss.gob.mx.cohorte.services.solicitud.security.*;
import imss.gob.mx.cohorte.utils.APIResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping(value="/api/publico/solicitud-datos", produces=MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class SolicitudAccesoDatosPublicoController {
    private final SolicitudAccesoDatosService service;
    private final SolicitudEmailVerification emailVerification;
    private final SolicitudNetworkCheck networks;
    private final SolicitudClientIp ips;
    private final SolicitudCaptcha captcha;

    @GetMapping("/captcha-config")
    public ResponseEntity<APIResponse> captchaConfiguration(HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new APIResponse("Configuración antispam",captcha.configuration(ips.resolve(request)),false,HttpStatus.OK));
    }
    public record EmailStart(@NotBlank @Email @Size(max=180) String email, @Size(max=200) String website) {}
    public record EmailConfirm(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") String challengeId,
                               @NotBlank @Pattern(regexp="[0-9]{6}") String code,
                               @NotBlank @Email @Size(max=180) String email) {}

    @PostMapping(value="/verificacion-correo", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse> sendCode(@Valid @RequestBody EmailStart dto,HttpServletRequest request) {
        if (dto.website()!=null&&!dto.website().isBlank()) {
            byte[] random=new byte[32];new java.security.SecureRandom().nextBytes(random);
            return ResponseEntity.accepted().body(new APIResponse("Código solicitado",new SolicitudEmailVerification.Challenge(Base64.getUrlEncoder().withoutPadding().encodeToString(random),600),false,HttpStatus.ACCEPTED));
        }
        emailVerification.requireAvailable();
        String ip=ips.resolve(request);
        networks.requireAllowed(ip);
        return ResponseEntity.accepted().body(new APIResponse("Código solicitado",emailVerification.issue(dto.email(),ip),false,HttpStatus.ACCEPTED));
    }

    @PostMapping(value="/verificacion-correo/confirmar", consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse> confirmCode(@Valid @RequestBody EmailConfirm dto,HttpServletRequest request) {
        return ResponseEntity.ok(new APIResponse("Correo verificado",emailVerification.confirm(dto.challengeId(),dto.code(),dto.email(),ips.resolve(request)),false,HttpStatus.OK));
    }

    @PostMapping(consumes=MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<APIResponse> enviarSolicitud(@Valid @RequestBody SolicitudAccesoDatosRequestDTO dto,HttpServletRequest request) {
        if(dto.getWebsite()!=null&&!dto.getWebsite().isBlank()) return ResponseEntity.ok(new APIResponse("Solicitud enviada",null,false,HttpStatus.OK));
        String ip=ips.resolve(request);
        if(service.excedeLimiteIP(ip)) throw new SolicitudSecurityException(HttpStatus.TOO_MANY_REQUESTS,"RATE_LIMITED","Has enviado demasiadas solicitudes. Intenta más tarde.");
        captcha.verify(dto.getCaptchaToken(),ip);
        SolicitudNetworkCheck.Result network=networks.requireAllowed(ip);
        SolicitudAccesoDatos solicitud=service.registrar(dto,ip,network);
        return ResponseEntity.status(HttpStatus.CREATED).body(new APIResponse("Solicitud enviada",solicitud.getId(),false,HttpStatus.CREATED));
    }

    @ExceptionHandler(SolicitudSecurityException.class)
    public ResponseEntity<APIResponse> securityError(SolicitudSecurityException error) {
        return ResponseEntity.status(error.getStatus()).body(new APIResponse(error.getMessage(),Map.of("code",error.getCode()),true,error.getStatus()));
    }
}
