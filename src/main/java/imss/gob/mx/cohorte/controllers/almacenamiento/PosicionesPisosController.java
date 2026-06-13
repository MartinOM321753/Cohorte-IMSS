package imss.gob.mx.cohorte.controllers.almacenamiento;

import imss.gob.mx.cohorte.application.almacenamiento.PisoRefrigeradorApplicationService;
import imss.gob.mx.cohorte.application.almacenamiento.RefrigeradorApplicationService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/almacenamiento/refrigeradores")
@AllArgsConstructor
@Tag(name = "Refrigeradores", description = "Gestión de refrigeradores criogénicos")
@SecurityRequirement(name = "bearerAuth")
public class PosicionesPisosController {


    private final RefrigeradorApplicationService refrigeradorApplicationService;
    private final PisoRefrigeradorApplicationService pisoRefrigeradorApplicationService;



}
