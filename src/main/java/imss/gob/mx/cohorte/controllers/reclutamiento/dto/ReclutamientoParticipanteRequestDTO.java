package imss.gob.mx.cohorte.controllers.reclutamiento.dto;

import imss.gob.mx.cohorte.modules.reclutamiento.EstadoContacto;
import imss.gob.mx.cohorte.modules.reclutamiento.MedioContacto;
import imss.gob.mx.cohorte.modules.reclutamiento.TipoReclutamiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
public class ReclutamientoParticipanteRequestDTO {

    @NotNull(message = "El tipo de reclutamiento es obligatorio")
    private TipoReclutamiento tipoReclutamiento;

    private EstadoContacto estadoContacto;

    private MedioContacto medioContacto;

    private String uuidUsuarioRecluta;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;

    private LocalDateTime fechaContacto;
}
