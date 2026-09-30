package imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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
     * Volumen <b>general</b> por alícuota (mL, mg, g). Nullable: puede no estar
     * definido aún.
     *
     * <p>Sigue siendo la capacidad del tubo cuando todas sus alícuotas miden lo
     * mismo, y además cumple dos papeles cuando no: es la semilla con la que se
     * rellena {@link #volumenesAlicuota} al configurar, y el respaldo al que cae
     * cualquier slot que no tenga su propio volumen —de modo que un tubo
     * heredado, sin lista por slot, se comporta exactamente como antes—.</p>
     */
    @Column(name = "volumen_alicuota")
    private Double volumenAlicuota;

    /**
     * Volumen configurado de cada alícuota, en orden (el índice {@code i} es el
     * slot {@code i+1}). Cuando está vacía, el tubo es uniforme y cada slot vale
     * {@link #volumenAlicuota}.
     *
     * <p>Es la capacidad <em>y</em> el valor por omisión de cada vial: al generar
     * el lote cada alícuota nace con el volumen de su slot, y una parcial puede
     * bajar de ahí pero nunca pasarse. Se guarda como colección indexada
     * (no un {@code @OneToMany} con entidad propia) porque no tiene identidad ni
     * ciclo de vida fuera del tubo: son N números ordenados que se reemplazan en
     * bloque cada vez que se reconfigura.</p>
     *
     * <p>EAGER a propósito: el mapper a DTO y el planificador de lotes la leen
     * fuera de la transacción que cargó el tubo, y con LAZY reventaría con
     * {@code LazyInitializationException}. Es una lista corta (tantos números
     * como alícuotas), así que el coste es despreciable.</p>
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Tubo_Muestra_Volumen_Alicuota",
            joinColumns = @JoinColumn(name = "id_tubo_muestra"))
    @OrderColumn(name = "posicion")
    @Column(name = "volumen", nullable = false)
    private List<Double> volumenesAlicuota = new ArrayList<>();

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
