package imss.gob.mx.cohorte.controllers.users.dto;

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
    private String UUID;
    private Boolean activo;
    private RolDTO rol;
    private LocalDateTime fechaCreacion;
    private PersonaResponseDTO persona;

    /** Representación mínima del rol para el frontend (sin exponer PK numérica). */
    public record RolDTO(String uuid, String nombre) {}
}
