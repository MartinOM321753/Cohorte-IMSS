package imss.gob.mx.cohorte.controllers.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import imss.gob.mx.cohorte.controllers.DTO.PersonaResponseDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {
    private Long id;
    private String username;
    @JsonProperty("UUID")
    private String UUID;
    private Boolean activo;
    private Boolean debeResetear;
    private RolDTO rol;
    private LocalDateTime fechaCreacion;
    private PersonaResponseDTO persona;
    private InstitucionResumenDTO institucion;

    /** Representación mínima del rol para el frontend (sin exponer PK numérica). */
    public record RolDTO(String uuid, String nombre) {}

    /** Representación mínima de la institución para el frontend. */
    public record InstitucionResumenDTO(Long id, String uuid, String nombre) {}
}
