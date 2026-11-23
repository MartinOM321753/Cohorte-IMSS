package imss.gob.mx.cohorte.controllers.solicitud.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter @Setter
public class SolicitudAccesoDatosRequestDTO {
    @NotBlank @Size(min=2,max=140) private String fullName;
    @NotBlank @Size(min=3,max=180) private String organization;
    @NotBlank @Email @Size(max=180) private String email;
    @NotBlank @Pattern(regexp="[0-9]{10}") private String phone;
    @NotBlank @Size(max=30) private String purpose;
    @Size(max=180) private String purposeOther;
    @NotBlank @Size(min=30,max=2400) private String project;
    @NotBlank @Size(min=20,max=1800) private String requestedInformation;
    @NotEmpty @Size(max=6) private List<@NotBlank @Size(max=20) String> products;
    @Size(max=180) private String productOther;
    @Size(max=200) private String website;
    @NotBlank @Pattern(regexp="[A-Za-z0-9_-]{43}") private String emailVerificationToken;
    @NotNull @AssertTrue private Boolean locationConsent;
    @NotNull @DecimalMin("-90") @DecimalMax("90") private Double latitude;
    @NotNull @DecimalMin("-180") @DecimalMax("180") private Double longitude;
    @NotNull @Positive @Max(100000) private Double locationAccuracy;
    @NotNull private Long locationCapturedAt;
    @NotBlank @Size(max=2048) private String captchaToken;
}
