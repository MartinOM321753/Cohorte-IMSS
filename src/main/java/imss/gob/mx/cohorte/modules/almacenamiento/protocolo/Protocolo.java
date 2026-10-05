package imss.gob.mx.cohorte.modules.almacenamiento.protocolo;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Plantilla maestra de procesamiento por tipo de muestra <b>origen</b>.
 *
 * <p>Un protocolo abstrae lo que el laboratorio le hace a un material recolectado
 * (p. ej. "Sangre total"): define los tubos primarios que entran y, por cada uno,
 * qué se le hace —guardar, estudiar o alicuotar—. Es la configuración; al procesar
 * un participante se instancia y se generan las salidas (padres, estudios y lotes
 * de alícuotas).</p>
 *
 * <p>Sustituye, para el flujo nuevo, al par {@code TipoMuestra → TuboMuestra}: el
 * {@code TipoMuestra} queda como catálogo puro (sirve de origen y de resultante) y
 * los tubos cuelgan del protocolo, no del tipo.</p>
 */
@Entity
@Table(name = "Protocolo",
       uniqueConstraints = @UniqueConstraint(
               name = "uk_protocolo_nombre_inst",
               columnNames = {"nombre", "id_institucion"}))
@Getter
@Setter
@NoArgsConstructor
public class Protocolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_protocolo")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /**
     * Tipo de muestra que se recolecta y procesa con este protocolo (el origen).
     * Los tubos primarios y la muestra padre que se generen nacen con este tipo.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_origen", nullable = false)
    private TipoMuestra tipoOrigen;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @OneToMany(mappedBy = "protocolo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orden ASC")
    private List<TuboProtocolo> tubos = new ArrayList<>();
}
