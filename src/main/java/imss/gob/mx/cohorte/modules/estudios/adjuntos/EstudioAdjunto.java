package imss.gob.mx.cohorte.modules.estudios.adjuntos;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "Estudio_Adjunto")
@Getter
@Setter
@NoArgsConstructor
public class EstudioAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adjunto")
    private Long id;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "ruta_url", nullable = false, length = 500)
    private String rutaUrl;

    @Column(name = "descripcion", length = 255)
    private String descripcion;

    @Column(name = "orden_adjunto", nullable = false)
    private Integer orden;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_estudio", nullable = false)
    private EstudioMedico estudio;
}
