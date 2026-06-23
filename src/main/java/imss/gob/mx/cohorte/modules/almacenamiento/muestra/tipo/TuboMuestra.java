package imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Tubo_Muestra")
@Getter
@Setter
@NoArgsConstructor
public class TuboMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tubo_muestra")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_tipo_muestra", nullable = false)
    private TipoMuestra tipoMuestra;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /**
     * Prefijo para generar el código de la alícuota. Ej: "S", "EDTA", "H".
     */
    @Column(name = "prefijo_codigo", length = 20)
    private String prefijoCodigo;

    /**
     * Número de alícuotas que genera este tubo. 0 = tubo directo sin alicuotar.
     */
    @Column(name = "numero_alicuotas", nullable = false)
    private Integer numeroAlicuotas = 0;

    /**
     * Volumen por alícuota (mL, mg, g). Nullable: puede no estar definido aún.
     */
    @Column(name = "volumen_alicuota")
    private Double volumenAlicuota;

    /**
     * Unidad del volumen: "mL", "mg", "g", "µL".
     */
    @Column(name = "unidad_volumen", length = 20)
    private String unidadVolumen;

    /**
     * Destino sugerido para las alícuotas de este tubo. Ej: "INMEGEN", "INSP", "Biobanco".
     */
    @Column(name = "destino_sugerido", length = 100)
    private String destinoSugerido;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
