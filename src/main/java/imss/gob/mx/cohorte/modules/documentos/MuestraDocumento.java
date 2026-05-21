package imss.gob.mx.cohorte.modules.documentos;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tabla de relación entre una Muestra biológica y sus Documentos asociados. */
@Entity
@Table(name = "Muestra_Documento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_muestra_documento",
                columnNames = {"id_muestra", "id_documento"}
        ))
@Getter
@Setter
@NoArgsConstructor
public class MuestraDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_muestra_documento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_muestra", nullable = false)
    private Muestra muestra;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;
}
