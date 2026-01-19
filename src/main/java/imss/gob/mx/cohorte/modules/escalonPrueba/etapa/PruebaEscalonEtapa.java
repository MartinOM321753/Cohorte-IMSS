package imss.gob.mx.cohorte.modules.escalonPrueba.etapa;


import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.escalonPrueba.PruebaEscalon;

import imss.gob.mx.cohorte.modules.escalonPrueba.medicion.PruebaEscalonMedicion;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "Prueba_Escalon_Etapa",
        uniqueConstraints = @UniqueConstraint(name = "uk_prueba_etapa", columnNames = {"id_prueba_escalon", "etapa"})
)
@Getter @Setter
@NoArgsConstructor
public class PruebaEscalonEtapa {
    public enum Etapa { BASAL, ETAPA_1, ETAPA_2, ETAPA_3 }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etapa")
    private Long Id;

    @ManyToOne
    @JoinColumn(name = "id_prueba_escalon")
    @JsonIgnore
    private PruebaEscalon pruebaEscalon;

    @OneToOne(mappedBy = "etapa")
    private PruebaEscalonMedicion medicion;

    @Enumerated(EnumType.STRING)
    @Column(name = "etapa", nullable = false, columnDefinition = "ENUM('BASAL','ETAPA_1','ETAPA_2','ETAPA_3')")
    private Etapa etapa;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;


}
