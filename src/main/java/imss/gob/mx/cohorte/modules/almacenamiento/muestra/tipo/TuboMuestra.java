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

    /**
     * Si al registrar una muestra con este tubo las alícuotas se crean solas.
     *
     * <p>Es un valor por omisión, no una regla: quien registra puede activarlo
     * en un tubo marcado como manual o saltárselo en uno automático, y siempre
     * puede generar el lote más tarde. Existe porque no toda muestra se alicuota
     * en la unidad que la tomó —muchas se guardan y se alicuotan en otra, o
     * nunca—, y crearlas siempre imponía un caso particular a todos.</p>
     *
     * <p><b>Nullable a propósito, con {@code null} = automático.</b> MySQL
     * rellena una columna booleana NOT NULL nueva con 0 en las filas que ya
     * existen: declararla obligatoria habría dejado en manual, de golpe y sin
     * un solo error visible, todos los tubos ya configurados. Léase siempre con
     * {@link #esGeneracionAutomatica()}.</p>
     */
    @Column(name = "generacion_automatica")
    private Boolean generacionAutomatica = Boolean.TRUE;

    /**
     * Si se admite cerrar el lote con una alícuota incompleta —los 20 mL que
     * sobran metidos en un vial de 50—. Hay tipos de muestra donde un vial a
     * medias no sirve y conviene apagarlo.
     *
     * <p>Nullable con {@code null} = permitido, por el mismo motivo que
     * {@link #generacionAutomatica}. Léase con {@link #admiteAlicuotaParcial()}.</p>
     */
    @Column(name = "permite_alicuota_parcial")
    private Boolean permiteAlicuotaParcial = Boolean.TRUE;

    /** Generación automática, tratando el dato heredado (null) como activada. */
    @Transient
    @JsonIgnore
    public boolean esGeneracionAutomatica() {
        return !Boolean.FALSE.equals(generacionAutomatica);
    }

    /** Alícuotas incompletas, tratando el dato heredado (null) como permitidas. */
    @Transient
    @JsonIgnore
    public boolean admiteAlicuotaParcial() {
        return !Boolean.FALSE.equals(permiteAlicuotaParcial);
    }
}
