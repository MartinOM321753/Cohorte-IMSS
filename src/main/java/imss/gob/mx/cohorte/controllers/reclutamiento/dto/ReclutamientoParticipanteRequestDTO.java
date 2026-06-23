package imss.gob.mx.cohorte.controllers.reclutamiento.dto;

import imss.gob.mx.cohorte.modules.reclutamiento.EstadoContacto;
import imss.gob.mx.cohorte.modules.reclutamiento.MedioContacto;
import imss.gob.mx.cohorte.modules.reclutamiento.TipoReclutamiento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Datos de clasificación de reclutamiento capturados al registrar un participante
 * (o al actualizar el resultado de un contacto). Se usa embebido en
 * `PacienteRequestDTO` al dar de alta y de forma independiente para actualizar
 * el estado de contacto de un participante ya existente.
 */
@Data
@NoArgsConstructor
public class ReclutamientoParticipanteRequestDTO {

    /** RETORNO: ya participó antes y fue recontactado · NUEVO: nunca ha participado. */
    @NotNull(message = "El tipo de reclutamiento es obligatorio")
    private TipoReclutamiento tipoReclutamiento;

    /** Resultado del contacto — típicamente aplica a participantes RETORNO. */
    private EstadoContacto estadoContacto;

    private MedioContacto medioContacto;

    /** UUID del usuario del sistema que realizó el contacto/reclutamiento. Si se omite, se usa el usuario autenticado. */
    private String uuidUsuarioRecluta;

    @Size(max = 500, message = "Las observaciones no pueden superar 500 caracteres")
    private String observaciones;

    /** Fecha (sin hora) en que se realizó el contacto. Recibida como "AAAA-MM-DD". No puede ser futura. */
    private LocalDate fechaContacto;
}
