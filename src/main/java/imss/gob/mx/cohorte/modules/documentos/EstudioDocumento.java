package imss.gob.mx.cohorte.modules.documentos;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tabla de relación entre un EstudioMedico y sus Documentos adjuntos. */
@Entity
@Table(name = "Estudio_Documento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_estudio_documento",
                columnNames = {"id_estudio", "id_documento"}
        ))
@Getter
@Setter
@NoArgsConstructor
public class EstudioDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estudio_documento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estudio", nullable = false)
    private EstudioMedico estudio;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;

    /** Orden de presentación dentro del estudio (0-based). */
    @Column(name = "orden", nullable = false)
    private Integer orden = 0;
}
