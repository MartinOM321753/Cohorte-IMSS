package imss.gob.mx.cohorte.controllers.pacientes.dto;

import imss.gob.mx.cohorte.controllers.reclutamiento.dto.ReclutamientoParticipanteRequestDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PacienteRequestDTO {

    /**
     * Folio del participante. Es OPCIONAL al registrar: si el participante ya
     * contaba con un folio de seguimiento previo, el usuario lo captura aquí
     * (se normaliza a MAYÚSCULAS/alfanumérico); si se omite, el sistema genera
     * uno automáticamente vía {@code FolioGeneratorService} (formato COH-AA-NNNNN).
     */
    @Size(max = 50, message = "Folio máximo 50 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "El folio solo puede contener letras, números y guiones")
    private String folio;

    @NotNull(message = "Los datos de persona son obligatorios")
    @Valid
    private PacientePersonaRequestDTO persona;

    /**
     * Institución a la que quedará asignado el participante. Es OPCIONAL: si se
     * omite se usa la del usuario autenticado, que es el caso normal.
     *
     * <p>Solo se acepta si figura en el conjunto autorizado para esa sede (ver
     * {@code InstitucionRegistroService}); un id fuera de ese conjunto se rechaza
     * aunque exista. Al actualizar se ignora — la institución de un participante
     * no cambia por edición.</p>
     */
    private Long idInstitucion;

    /**
     * Clasificación de reclutamiento (RETORNO/NUEVO, institución, medio de
     * contacto, etc.). Obligatoria al dar de alta un participante — define el
     * origen y la institución responsable del reclutamiento.
     */
    @NotNull(message = "La clasificación de reclutamiento es obligatoria")
    @Valid
    private ReclutamientoParticipanteRequestDTO reclutamiento;
}
