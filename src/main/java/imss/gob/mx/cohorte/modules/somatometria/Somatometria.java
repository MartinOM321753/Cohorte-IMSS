package imss.gob.mx.cohorte.modules.somatometria;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "somatometria")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Somatometria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_somatometria")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @Column(name = "fecha_medicion", nullable = false)
    private LocalDate fechaMedicion;

    /** Peso en kilogramos (ej. 68.5) */
    @Column(name = "peso_kg", precision = 6, scale = 2)
    private BigDecimal pesoKg;

    /** Talla en metros (ej. 1.58) */
    @Column(name = "talla_m", precision = 4, scale = 2)
    private BigDecimal tallaM;

    /**
     * IMC calculado automáticamente al persistir / actualizar.
     * Si peso y talla están presentes: IMC = peso / (talla^2)
     */
    @Column(name = "imc", precision = 5, scale = 2)
    private BigDecimal imc;

    /** Presión sistólica en mmHg */
    @Column(name = "presion_sistolica")
    private Integer presionSistolica;

    /** Presión diastólica en mmHg */
    @Column(name = "presion_diastolica")
    private Integer presionDiastolica;

    /** Circunferencia abdominal en cm */
    @Column(name = "circunferencia_abdominal_cm", precision = 5, scale = 1)
    private BigDecimal circunferenciaAbdominalCm;

    /** Frecuencia cardiaca en reposo (lpm) */
    @Column(name = "frecuencia_cardiaca_reposo")
    private Integer frecuenciaCardiacaReposo;

    @Column(name = "observaciones", columnDefinition = "TEXT")
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_registra")
    private BeanUser usuarioRegistra;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    public void prePersist() {
        this.fechaRegistro = LocalDateTime.now();
        calcularImc();
    }

    @PreUpdate
    public void preUpdate() {
        calcularImc();
    }

    private void calcularImc() {
        if (this.pesoKg != null && this.tallaM != null
                && this.tallaM.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal tallaCuadrado = this.tallaM.multiply(this.tallaM);
            this.imc = this.pesoKg.divide(tallaCuadrado, 2, RoundingMode.HALF_UP);
        }
    }
}
