package imss.gob.mx.cohorte.modules.impresion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "configuracion_etiqueta")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionEtiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_configuracion_etiqueta")
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "predeterminada", nullable = false)
    private Boolean predeterminada = false;

    /**
     * Soporte físico. Decide qué campos de acomodo tienen sentido: la hoja usa
     * paso y márgenes de página, el rollo usa carriles y desplazamiento de origen.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_medio", nullable = false, length = 20,
            columnDefinition = "VARCHAR(20) NOT NULL DEFAULT 'HOJA_AVERY'")
    private TipoMedio tipoMedio = TipoMedio.HOJA_AVERY;

    @Enumerated(EnumType.STRING)
    @Column(name = "tamano_hoja", nullable = false, length = 10,
            columnDefinition = "VARCHAR(10) NOT NULL DEFAULT 'CARTA'")
    private TamanoHoja tamanoHoja = TamanoHoja.CARTA;

    @Column(name = "ancho_mm", nullable = false)
    private Double anchoMm = 33.0;

    @Column(name = "alto_mm", nullable = false)
    private Double altoMm = 22.0;

    @Column(name = "dpi", nullable = false)
    private Integer dpi = 203;

    @Column(name = "etiquetas_por_fila", nullable = false)
    private Integer etiquetasPorFila = 3;

    @Column(name = "margen_izquierdo_mm", nullable = false)
    private Double margenIzquierdoMm = 2.5;

    @Column(name = "margen_superior_mm", nullable = false)
    private Double margenSuperiorMm = 2.0;

    /**
     * Márgenes internos derecho e inferior de la etiqueta. Antes no existían: se
     * suponía que el área útil era simétrica al margen izquierdo y que por abajo
     * no hacía falta reservar nada, de modo que el contenido podía salirse por el
     * borde inferior sin que ningún cálculo lo notara.
     *
     * Se dejan en cero por omisión y {@link #getMargenDerechoMmEfectivo()} los
     * resuelve, para que las configuraciones ya guardadas conserven su medida.
     */
    @Column(name = "margen_derecho_mm", nullable = false,
            columnDefinition = "DOUBLE NOT NULL DEFAULT 0")
    private Double margenDerechoMm = 0.0;

    @Column(name = "margen_inferior_mm", nullable = false,
            columnDefinition = "DOUBLE NOT NULL DEFAULT 0")
    private Double margenInferiorMm = 0.0;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_codigo", nullable = false, length = 20)
    private TipoCodigo tipoCodigo = TipoCodigo.DATAMATRIX;

    @Column(name = "modulo_codigo", nullable = false)
    private Integer moduloCodigo = 6;

    /**
     * Ancho de la barra angosta, en dots. Solo aplica a los codigos lineales
     * (Code 128), donde se emite como {@code ^BY}. En DataMatrix y QR el tamano
     * del modulo lo lleva {@link #moduloCodigo}. El valor 2 es el que la Zebra
     * usaba por omision antes de que este campo existiera.
     */
    @Column(name = "ancho_barra_codigo", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 2")
    private Integer anchoBarraCodigo = 2;

    @Column(name = "tamano_fuente_nombre", nullable = false)
    private Integer tamanoFuenteNombre = 16;

    @Column(name = "tamano_fuente_etiqueta", nullable = false)
    private Integer tamanoFuenteEtiqueta = 16;

    @Column(name = "espaciado_nombre", nullable = false)
    private Integer espaciadoNombre = 4;

    @Column(name = "espaciado_codigo", nullable = false)
    private Integer espaciadoCodigo = 10;

    @Column(name = "espaciado_etiqueta", nullable = false)
    private Integer espaciadoEtiqueta = 4;

    @Column(name = "mostrar_nombre", nullable = false)
    private Boolean mostrarNombre = true;

    @Column(name = "mostrar_codigo", nullable = false)
    private Boolean mostrarCodigo = true;

    @Column(name = "mostrar_etiqueta", nullable = false)
    private Boolean mostrarEtiqueta = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "disposicion", nullable = false, length = 40)
    private DisposicionEtiqueta disposicion = DisposicionEtiqueta.NOMBRE_CODIGO_ETIQUETA;

    @Column(name = "filas_por_pagina", nullable = false)
    private Integer filasPorPagina = 10;

    /**
     * Separación entre etiquetas. Se conserva porque es lo que el usuario tiene
     * capturado, pero ya no es la fuente de verdad del acomodo: lo es el paso.
     * Véase {@link #getPasoVerticalMmEfectivo()}.
     */
    @Column(name = "espacio_horizontal_mm", nullable = false)
    private Double espacioHorizontalMm = 3.0;

    @Column(name = "espacio_vertical_mm", nullable = false)
    private Double espacioVerticalMm = 2.0;

    /**
     * Paso: distancia de un borde de etiqueta al mismo borde de la siguiente.
     *
     * Es el dato que traen los catálogos de hoja, y el único que posiciona bien
     * una cuadrícula. Deducirlo de alto + separación acumulaba el error de ambos
     * campos fila tras fila: con medio milímetro de diferencia, la décima fila
     * salía cinco milímetros abajo de su recuadro.
     *
     * Cero significa "no capturado todavía"; los accesores efectivos caen
     * entonces al cálculo antiguo, de modo que nada cambia hasta reconfigurar.
     */
    @Column(name = "paso_horizontal_mm", nullable = false,
            columnDefinition = "DOUBLE NOT NULL DEFAULT 0")
    private Double pasoHorizontalMm = 0.0;

    @Column(name = "paso_vertical_mm", nullable = false,
            columnDefinition = "DOUBLE NOT NULL DEFAULT 0")
    private Double pasoVerticalMm = 0.0;

    @Column(name = "margen_pagina_superior_mm", nullable = false)
    private Double margenPaginaSuperiorMm = 12.7;

    @Column(name = "margen_pagina_izquierdo_mm", nullable = false)
    private Double margenPaginaIzquierdoMm = 4.8;

    /**
     * Corrección de calibración de la impresora, aplicada a la hoja completa.
     *
     * El controlador de cada impresora desplaza la página unos milímetros por su
     * área no imprimible, y ese desplazamiento no se puede leer desde el
     * navegador. Se mide una vez con la hoja de calibración y se guarda aquí.
     */
    @Column(name = "ajuste_x_mm", nullable = false,
            columnDefinition = "DOUBLE NOT NULL DEFAULT 0")
    private Double ajusteXMm = 0.0;

    @Column(name = "ajuste_y_mm", nullable = false,
            columnDefinition = "DOUBLE NOT NULL DEFAULT 0")
    private Double ajusteYMm = 0.0;

    // ── Rollo Zebra ─────────────────────────────────────────────────────────

    /**
     * Etiquetas a lo ancho del rollo. La Zebra avanza el papel por filas
     * completas, así que este número decide cuántas etiquetas se consumen en
     * cada avance, sin importar cuántas se hayan mandado a imprimir.
     *
     * Cero significa "no capturado"; se cae entonces a {@code etiquetasPorFila},
     * que es como se venía interpretando el campo compartido.
     */
    @Column(name = "carriles_rollo", nullable = false,
            columnDefinition = "INT NOT NULL DEFAULT 0")
    private Integer carrilesRollo = 0;

    /**
     * Ancho imprimible del cabezal. Sirve para acotar {@code ^PW}: pedirle a la
     * Zebra un ancho mayor al del cabezal hace que ella lo recorte por su cuenta
     * y corra el origen, que es una de las causas de que el margen izquierdo no
     * se respete.
     */
    @Column(name = "ancho_cabezal_mm", nullable = false,
            columnDefinition = "DOUBLE NOT NULL DEFAULT 104.0")
    private Double anchoCabezalMm = 104.0;

    /** Origen del formato ZPL ({@code ^LH}), en dots. Calibra el rollo. */
    @Column(name = "offset_lh_x_dots", nullable = false,
            columnDefinition = "INT NOT NULL DEFAULT 0")
    private Integer offsetLhXDots = 0;

    @Column(name = "offset_lh_y_dots", nullable = false,
            columnDefinition = "INT NOT NULL DEFAULT 0")
    private Integer offsetLhYDots = 0;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public int getAnchoDots() {
        return (int) (anchoMm / 25.4 * dpi);
    }

    public int getAltoDots() {
        return (int) (altoMm / 25.4 * dpi);
    }

    public int getMargenIzquierdoDots() {
        return (int) (margenIzquierdoMm / 25.4 * dpi);
    }

    public int getMargenSuperiorDots() {
        return (int) (margenSuperiorMm / 25.4 * dpi);
    }

    public int getMargenDerechoDots() {
        return (int) (getMargenDerechoMmEfectivo() / 25.4 * dpi);
    }

    public int getMargenInferiorDots() {
        return (int) (getMargenInferiorMmEfectivo() / 25.4 * dpi);
    }

    // ── Valores efectivos ───────────────────────────────────────────────────
    //
    // Los campos de acomodo nuevos nacen en cero para las configuraciones que ya
    // existían. Estos accesores deciden qué medida se usa realmente, de forma que
    // una configuración sin migrar siga imprimiendo exactamente igual que antes y
    // solo cambie cuando alguien capture el paso real de su hoja.

    /** Paso horizontal capturado, o el que se deducía de ancho + separación. */
    public double getPasoHorizontalMmEfectivo() {
        if (pasoHorizontalMm != null && pasoHorizontalMm > 0) return pasoHorizontalMm;
        return anchoMm + (espacioHorizontalMm != null ? espacioHorizontalMm : 0.0);
    }

    /** Paso vertical capturado, o el que se deducía de alto + separación. */
    public double getPasoVerticalMmEfectivo() {
        if (pasoVerticalMm != null && pasoVerticalMm > 0) return pasoVerticalMm;
        return altoMm + (espacioVerticalMm != null ? espacioVerticalMm : 0.0);
    }

    /** Sin margen derecho propio, el área útil es simétrica como lo era antes. */
    public double getMargenDerechoMmEfectivo() {
        if (margenDerechoMm != null && margenDerechoMm > 0) return margenDerechoMm;
        return margenIzquierdoMm != null ? margenIzquierdoMm : 0.0;
    }

    /**
     * Sin margen inferior propio se usa cero, no el superior: es lo que el
     * generador venía haciendo, y suponer un margen que nadie capturó reduciría
     * el espacio disponible y encogería etiquetas que hoy caben.
     */
    public double getMargenInferiorMmEfectivo() {
        return margenInferiorMm != null ? margenInferiorMm : 0.0;
    }

    /** Carriles del rollo, o las etiquetas por fila con que se interpretaba. */
    public int getCarrilesRolloEfectivo() {
        if (carrilesRollo != null && carrilesRollo > 0) return carrilesRollo;
        return etiquetasPorFila != null && etiquetasPorFila > 0 ? etiquetasPorFila : 1;
    }

    public double getHojaAnchoMm() {
        return (tamanoHoja != null ? tamanoHoja : TamanoHoja.CARTA).getAnchoMm();
    }

    public double getHojaAltoMm() {
        return (tamanoHoja != null ? tamanoHoja : TamanoHoja.CARTA).getAltoMm();
    }
}
