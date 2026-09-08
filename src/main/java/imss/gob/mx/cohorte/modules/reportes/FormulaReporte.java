package imss.gob.mx.cohorte.modules.reportes;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Un cálculo con nombre, guardado por institución.
 *
 * <p>Se da de alta una vez y se usa en los reportes que haga falta. No hay expresiones
 * sueltas dentro de una celda: fue una decisión explícita, y la razón es que una
 * fórmula del catálogo se revisa, se reutiliza y se corrige en un solo lugar, mientras
 * que las sueltas se multiplican con variantes que nadie vuelve a mirar.</p>
 *
 * <p>La fórmula no guarda datos de nadie. Es el molde; los números se resuelven al
 * emitir, contra el participante que toque.</p>
 */
@Entity
@Table(name = "formula_reporte",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_formula_reporte_nombre_institucion",
                columnNames = {"nombre", "id_institucion"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormulaReporte {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_formula")
    private Long id;

    /** Como la ve quien arma el reporte: «Índice de masa corporal». */
    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "descripcion", length = 500)
    private String descripcion;

    /** Lo que se escribió: {@code peso / estatura²}. */
    @Column(name = "expresion", nullable = false, length = 2000)
    private String expresion;

    /**
     * Las variables, con su clave del catálogo y la unidad elegida.
     *
     * <p>Va en JSON por la misma razón que el diseño de una plantilla: es una lista
     * corta que solo consume el motor, y repartirla en una tabla aparte no habilitaría
     * ninguna consulta que alguien vaya a hacer. Su forma se valida al entrar.</p>
     */
    @Lob
    @Column(name = "variables", nullable = false, columnDefinition = "LONGTEXT")
    private String variables;

    /**
     * Los límites de la referencia, cuando dependen del participante.
     *
     * <p>El reporte de salud imprime el peso deseable como «18.5 × talla² — 24.9 ×
     * talla²»: no es un rango fijo, son dos cuentas que dan un número distinto para
     * cada persona. Se escriben igual que la expresión principal, con las mismas
     * variables, y se validan con el mismo validador.</p>
     *
     * <p>Son opcionales y van por separado porque hay filas con un solo lado —«&lt;200
     * mg/dL» no tiene mínimo, «≥50» no tiene máximo— y obligar a poner los dos forzaría
     * a inventar un límite que el documento no dice.</p>
     */
    @Column(name = "expresion_minimo", length = 2000)
    private String expresionMinimo;

    @Column(name = "expresion_maximo", length = 2000)
    private String expresionMaximo;

    /**
     * En qué sale el resultado, tal como quien la escribió lo declaró.
     *
     * <p>Se guarda como texto y no como referencia al catálogo de unidades porque hay
     * resultados que no tienen unidad en ningún catálogo —kg/m² es el caso obvio— y
     * obligar a que exista antes convertiría declarar una fórmula en dar de alta una
     * unidad.</p>
     */
    @Column(name = "unidad_salida", length = 40)
    private String unidadSalida;

    /** Con cuántos decimales se imprime. Nulo: los que traiga el resultado. */
    @Column(name = "decimales")
    private Integer decimales;

    /**
     * Sube cada vez que cambia el cálculo.
     *
     * <p>No es contabilidad: en un estudio de cohorte van a preguntar, años después,
     * cómo se calculó un valor concreto de un participante concreto. El reporte emitido
     * guarda con qué versión salió y {@link FormulaReporteHistorial} conserva qué decía
     * esa versión. Sin las dos cosas, la pregunta no tiene respuesta.</p>
     */
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    /**
     * Una fórmula retirada deja de ofrecerse pero no se borra: los reportes que la
     * usaron ya se entregaron.
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
        if (version == null) version = 1;
        if (activo == null) activo = true;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }
}
