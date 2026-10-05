package imss.gob.mx.cohorte.modules.almacenamiento.protocolo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuración individual de cada tubo primario de un {@link Protocolo}.
 *
 * <p>Cada tubo se configura por separado: qué se le hace ({@link #accion}) y, si
 * se alicuota, con qué receta y qué tipo resulta. El tipo resultante puede diferir
 * del tipo origen del protocolo —sangre total centrifugada da suero—, por eso la
 * receta lleva su propio {@link #tipoResultante}.</p>
 *
 * <p>Evoluciona de {@code TuboMuestra} (la receta de salida del modelo anterior)
 * añadiendo {@link #accion}, {@link #tipoResultante} y {@link #agruparEnLote}.</p>
 */
@Entity
@Table(name = "Tubo_Protocolo")
@Getter
@Setter
@NoArgsConstructor
public class TuboProtocolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tubo_protocolo")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_protocolo", nullable = false)
    private Protocolo protocolo;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    /** Prefijo para generar el código de la alícuota. Ej: "S", "EDTA", "H". */
    @Column(name = "prefijo_codigo", length = 20)
    private String prefijoCodigo;

    @Column(name = "orden", nullable = false)
    private Integer orden = 0;

    /** Qué se le hace a este tubo por omisión. */
    @Enumerated(EnumType.STRING)
    @Column(name = "accion", nullable = false, length = 20)
    private AccionTubo accion = AccionTubo.GUARDAR;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    // ── Receta de alicuotado (solo relevante si accion == ALICUOTAR) ───────────

    /**
     * Tipo de muestra que resulta del proceso aplicado a este tubo. Puede ser
     * distinto del tipo origen del protocolo (sangre total → suero). Obligatorio
     * cuando la acción es ALICUOTAR; ignorado en los demás casos.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_tipo_resultante")
    private TipoMuestra tipoResultante;

    /** Número de alícuotas que genera este tubo. 0 = no alicuota. */
    @Column(name = "numero_alicuotas", nullable = false)
    private Integer numeroAlicuotas = 0;

    /** Volumen general por alícuota; semilla y respaldo de {@link #volumenesAlicuota}. */
    @Column(name = "volumen_alicuota")
    private Double volumenAlicuota;

    /**
     * Volumen configurado de cada alícuota, en orden. Vacía = tubo uniforme y cada
     * slot vale {@link #volumenAlicuota}. EAGER a propósito, igual que en
     * {@code TuboMuestra}: el mapper y el planificador la leen fuera de la
     * transacción que cargó el tubo.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "Tubo_Protocolo_Volumen_Alicuota",
            joinColumns = @JoinColumn(name = "id_tubo_protocolo"))
    @OrderColumn(name = "posicion")
    @Column(name = "volumen", nullable = false)
    private List<Double> volumenesAlicuota = new ArrayList<>();

    /** Unidad del volumen: "mL", "mg", "g", "µL". */
    @Column(name = "unidad_volumen", length = 20)
    private String unidadVolumen;

    /** Destino sugerido para las alícuotas de este tubo. Ej: "INMEGEN", "INSP". */
    @Column(name = "destino_sugerido", length = 100)
    private String destinoSugerido;

    /**
     * Si las alícuotas de este tubo se unen con las de otros tubos del mismo
     * protocolo marcados a agrupar, en un solo lote con numeración continua 1…N.
     * Los tubos se agrupan por {@link #tipoResultante}: solo se juntan alícuotas
     * del mismo tipo. Vacío/false = este tubo forma su propio lote.
     */
    @Column(name = "agrupar_en_lote")
    private Boolean agruparEnLote = Boolean.FALSE;

    /** Valor por omisión de la generación automática de alícuotas al procesar. */
    @Column(name = "generacion_automatica")
    private Boolean generacionAutomatica = Boolean.TRUE;

    /** Si se admite cerrar el lote con una alícuota incompleta. */
    @Column(name = "permite_alicuota_parcial")
    private Boolean permiteAlicuotaParcial = Boolean.TRUE;

    @Transient
    @JsonIgnore
    public boolean esAlicuotar() {
        return accion == AccionTubo.ALICUOTAR;
    }

    @Transient
    @JsonIgnore
    public boolean esAgruparEnLote() {
        return Boolean.TRUE.equals(agruparEnLote);
    }

    @Transient
    @JsonIgnore
    public boolean esGeneracionAutomatica() {
        return !Boolean.FALSE.equals(generacionAutomatica);
    }

    @Transient
    @JsonIgnore
    public boolean admiteAlicuotaParcial() {
        return !Boolean.FALSE.equals(permiteAlicuotaParcial);
    }
}
