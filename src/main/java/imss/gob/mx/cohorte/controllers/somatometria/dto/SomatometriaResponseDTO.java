package imss.gob.mx.cohorte.controllers.somatometria.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SomatometriaResponseDTO {

    private Long id;
    private String pacienteUUID;
    private String pacienteNombre;
    private LocalDate fechaMedicion;
    private BigDecimal pesoKg;
    private BigDecimal tallaM;

    /** IMC calculado en backend (peso / talla^2) */
    private BigDecimal imc;

    private Integer presionSistolica;
    private Integer presionDiastolica;
    private BigDecimal circunferenciaAbdominalCm;
    private Integer frecuenciaCardiacaReposo;
    private String observaciones;
    private String usuarioRegistraNombre;
    private LocalDateTime fechaRegistro;
    private String institucionUuid;
    private String institucionNombre;
}
