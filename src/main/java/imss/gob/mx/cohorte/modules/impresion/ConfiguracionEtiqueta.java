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

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_codigo", nullable = false, length = 20)
    private TipoCodigo tipoCodigo = TipoCodigo.DATAMATRIX;

    @Column(name = "modulo_codigo", nullable = false)
    private Integer moduloCodigo = 6;

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
}
