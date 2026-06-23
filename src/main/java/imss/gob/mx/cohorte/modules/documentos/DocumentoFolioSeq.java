package imss.gob.mx.cohorte.modules.documentos;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "documento_folio_seq",
        uniqueConstraints = @UniqueConstraint(columnNames = {"id_institucion", "anio"}))
@Getter
@Setter
@NoArgsConstructor
public class DocumentoFolioSeq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_institucion", nullable = false)
    private Long idInstitucion;

    @Column(name = "anio", nullable = false)
    private Integer anio;

    @Column(name = "ultimo_folio", nullable = false)
    private Integer ultimoFolio = 0;
}
