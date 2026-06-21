package imss.gob.mx.cohorte.controllers.citas.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfiguracionHorarioRequestDTO {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
    private String nombre;

    @NotNull(message = "La hora de inicio es obligatoria")
    @Min(value = 0, message = "La hora de inicio mínima es 0")
    @Max(value = 23, message = "La hora de inicio máxima es 23")
    private Integer horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @Min(value = 1, message = "La hora de fin mínima es 1")
    @Max(value = 24, message = "La hora de fin máxima es 24")
    private Integer horaFin;

    private Boolean lunes = true;
    private Boolean martes = true;
    private Boolean miercoles = true;
    private Boolean jueves = true;
    private Boolean viernes = true;
    private Boolean sabado = false;
    private Boolean domingo = false;

    private Boolean activa = false;
}
