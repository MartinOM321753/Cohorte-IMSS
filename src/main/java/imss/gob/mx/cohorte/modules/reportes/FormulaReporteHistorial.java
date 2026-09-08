package imss.gob.mx.cohorte.modules.reportes;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Qué decía una fórmula en cada una de sus versiones.
 *
 * <p>Se guarda la versión que <b>queda atrás</b> justo antes de sobrescribirla. Sin
 * esto, cambiar una fórmula borra para siempre cómo se calcularon los reportes que ya
 * se emitieron con ella, y en un estudio de cohorte esa pregunta se hace: «este VO₂ de
 * 2026, ¿con qué ecuación salió?».</p>
 *
 * <p>Es una tabla de solo escritura y lectura, sin actualizaciones. Una fila corregida
 * a posteriori no serviría para nada.</p>
 */
@Entity
@Table(name = "formula_reporte_historial",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_formula_historial_version",
                columnNames = {"id_formula", "version"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormulaReporteHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    /**
     * El identificador de la fórmula, sin relación gestionada.
     *
     * <p>A propósito: si algún día se borra una fórmula del catálogo, su historial tiene
     * que seguir ahí. Una relación con borrado en cascada se llevaría por delante justo
     * lo que esta tabla existe para conservar.</p>
     */
    @Column(name = "id_formula", nullable = false)
    private Long idFormula;

    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "nombre", nullable = false, length = 120)
    private String nombre;

    @Column(name = "expresion", nullable = false, length = 2000)
    private String expresion;

    @Lob
    @Column(name = "variables", nullable = false, columnDefinition = "LONGTEXT")
    private String variables;

    @Column(name = "unidad_salida", length = 40)
    private String unidadSalida;

    @Column(name = "decimales")
    private Integer decimales;

    /** Cuándo dejó de estar vigente esta versión. */
    @Column(name = "fecha_reemplazo", nullable = false, updatable = false)
    private LocalDateTime fechaReemplazo;

    @PrePersist
    void alGuardar() {
        fechaReemplazo = LocalDateTime.now();
    }

    /** Copia el estado actual de una fórmula, antes de que se sobrescriba. */
    public static FormulaReporteHistorial de(FormulaReporte formula) {
        FormulaReporteHistorial h = new FormulaReporteHistorial();
        h.setIdFormula(formula.getId());
        h.setVersion(formula.getVersion());
        h.setNombre(formula.getNombre());
        h.setExpresion(formula.getExpresion());
        h.setVariables(formula.getVariables());
        h.setUnidadSalida(formula.getUnidadSalida());
        h.setDecimales(formula.getDecimales());
        return h;
    }
}
