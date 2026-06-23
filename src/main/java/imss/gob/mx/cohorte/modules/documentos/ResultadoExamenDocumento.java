package imss.gob.mx.cohorte.modules.documentos;

import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Resultado_Examen_Documento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_resultado_examen_documento",
                columnNames = {"id_resultado", "id_documento"}
        ))
@Getter
@Setter
@NoArgsConstructor
public class ResultadoExamenDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_resultado_examen_documento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_resultado", nullable = false)
    private ResultadoExamen resultadoExamen;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;
}
