package imss.gob.mx.cohorte.modules.documentos_publicos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seccion_documento_publico",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_sec_doc_pub_nombre_inst",
        columnNames = {"nombre", "id_institucion"}
    )
)
@Getter @Setter
@NoArgsConstructor
public class SeccionDocumentoPublico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_seccion")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "orden")
    private Integer orden;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;
}
