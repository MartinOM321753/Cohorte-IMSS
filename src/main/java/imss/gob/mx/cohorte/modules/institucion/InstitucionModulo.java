package imss.gob.mx.cohorte.modules.institucion;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

/**
 * Permiso de acceso de una institución a un módulo del sistema (BIOBANCO,
 * EXAMENES, ESTUDIOS_MEDICOS, etc.). Sólo puede ser otorgado por una
 * institución "padre" (típicamente la raíz del árbol de instituciones) —
 * la validación de quién puede otorgar vive en el servicio, no aquí.
 */
@Entity
@Table(name = "Institucion_Modulo",
    uniqueConstraints = @UniqueConstraint(name = "uk_institucion_modulo", columnNames = {"id_institucion", "modulo"})
)
@Getter @Setter
@NoArgsConstructor
public class InstitucionModulo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_institucion_modulo")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_institucion", nullable = false)
    private Institucion institucion;

    @Enumerated(EnumType.STRING)
    @Column(name = "modulo", nullable = false, length = 30)
    private ModuloSistema modulo;

    @Column(name = "habilitado", nullable = false)
    private Boolean habilitado = true;

    /** Institución que otorgó el permiso (debe ser una institución "padre" en la jerarquía). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_otorgado_por", nullable = false)
    private Institucion otorgadoPor;

    @Column(name = "fecha_otorgamiento", nullable = false, updatable = false)
    private Timestamp fechaOtorgamiento;
}
