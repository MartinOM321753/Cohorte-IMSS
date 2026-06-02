package imss.gob.mx.cohorte.controllers.somatometria.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SomatometriaRequestDTO {

    @NotBlank(message = "El UUID del paciente es requerido")
    private String pacienteUUID;

    @NotBlank(message = "El UUID del usuario que registra es requerido")
    private String usuarioRegistraUUID;

    @NotNull(message = "La fecha de medición es requerida")
    private LocalDate fechaMedicion;

    private BigDecimal pesoKg;
    private BigDecimal tallaM;

    /** Presión sistólica en mmHg */
    private Integer presionSistolica;

    /** Presión diastólica en mmHg */
    private Integer presionDiastolica;

    /** Circunferencia abdominal en cm */
    private BigDecimal circunferenciaAbdominalCm;

    /** Frecuencia cardíaca en reposo (lpm) */
    private Integer frecuenciaCardiacaReposo;

    private String observaciones;
}
