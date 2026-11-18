package imss.gob.mx.cohorte.controllers.solicitud.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SolicitudAccesoDatosRequestDTO {

    @NotBlank
    @Size(max = 140)
    private String fullName;

    @NotBlank
    @Size(max = 180)
    private String organization;

    @NotBlank
    @Email
    @Size(max = 180)
    private String email;

    @NotBlank
    @Size(max = 180)
    private String purpose;

    @Size(max = 180)
    private String purposeOther;

    @NotBlank
    @Size(max = 2400)
    private String project;

    @NotBlank
    @Size(max = 1800)
    private String requestedInformation;

    @NotEmpty
    private List<String> products;

    @Size(max = 180)
    private String productOther;

    /** Honeypot — must arrive empty */
    private String website;
}
