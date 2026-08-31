package imss.gob.mx.cohorte.modules.reportes;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * El diseño de un reporte, guardado por institución.
 *
 * <p>Cada institución arma sus propios formatos: su membrete, sus colores, y qué datos
 * salen impresos. Una plantilla no contiene datos de nadie — es el molde—; los datos se
 * resuelven al emitir.</p>
 */
@Entity
@Table(name = "plantilla_reporte",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_plantilla_reporte_nombre_institucion",
                columnNames = {"nombre", "id_institucion"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PlantillaReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /** Sobre qué se emite: define qué datos puede insertar el diseño. */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_reporte", nullable = false, length = 20)
    private TipoReporte tipoReporte;

    /**
     * El diseño completo: tamaño de página, y las páginas con sus elementos.
     *
     * <p>Va en una sola columna JSON y no repartido en tablas porque es un árbol
     * heterogéneo —cuatro familias de elementos, cada una con sus propiedades— que
     * solo consume el maquetador. Normalizarlo costaría varias tablas y un ensamblado
     * en cada lectura sin habilitar ninguna consulta que haga falta: nadie va a buscar
     * plantillas «que tengan un rectángulo azul». La forma se valida al entrar, con el
     * DTO.</p>
     *
     * <p>LONGTEXT y no JSON nativo: el contenido se maneja siempre como texto desde
     * Java, y así no se depende de un tipo que cada motor de base de datos trata a su
     * manera.</p>
     */
    @Lob
    @Column(name = "diseno", nullable = false, columnDefinition = "LONGTEXT")
    private String diseno;

    /**
     * La que se ofrece primero al emitir este tipo de reporte. Solo puede haber una por
     * institución y tipo; de que se cumpla se encarga el servicio, no la base.
     */
    @Column(name = "predeterminada", nullable = false)
    private Boolean predeterminada = false;

    /**
     * Una plantilla retirada deja de ofrecerse pero no se borra: los reportes que se
     * emitieron con ella ya se entregaron, y saber con qué formato salieron importa.
     */
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }
}
