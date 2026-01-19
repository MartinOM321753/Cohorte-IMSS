package imss.gob.mx.cohorte.modules.escalonPrueba.medicion;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.escalonPrueba.etapa.PruebaEscalonEtapa;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Prueba_Escalon_Medicion",
        uniqueConstraints = @UniqueConstraint(name = "uk_etapa_parametro", columnNames = {"id_etapa", "parametro"})
)
@Getter @Setter
@NoArgsConstructor
public class PruebaEscalonMedicion {
    public enum Parametro { PULSO, TA_SISTOLICA, TA_DIASTOLICA, FCM }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_medicion")
    private Long Id;

    @OneToOne
    @JoinColumn(name = "id_etapa", nullable = false)
    @JsonIgnore
    private PruebaEscalonEtapa etapa;

    @Enumerated(EnumType.STRING)
    @Column(name = "parametro", nullable = false, columnDefinition = "ENUM('PULSO','TA_SISTOLICA','TA_DIASTOLICA','FCM')")
    private Parametro parametro;

    @Column(name = "valor", nullable = false)
    private Double valor;

    @Column(name = "unidad", length = 20)
    private String unidad;
}
