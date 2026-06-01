package imss.gob.mx.cohorte.modules.documentos;

import imss.gob.mx.cohorte.modules.paciente.Paciente;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Tabla de relación entre un Paciente y sus Documentos (consentimientos, expediente, etc.). */
@Entity
@Table(name = "Paciente_Documento",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_paciente_documento",
                columnNames = {"id_paciente", "id_documento"}
        ))
@Getter
@Setter
@NoArgsConstructor
public class PacienteDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_paciente_documento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_paciente", nullable = false)
    private Paciente paciente;

    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "id_documento", nullable = false)
    private Documento documento;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_doc", length = 50, nullable = false)
    private TipoDocumentoPaciente tipoDoc;
}
