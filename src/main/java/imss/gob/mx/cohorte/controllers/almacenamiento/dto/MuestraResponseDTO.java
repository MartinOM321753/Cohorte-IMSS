package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import imss.gob.mx.cohorte.controllers.DTO.PacienteResumenDTO;
import imss.gob.mx.cohorte.controllers.DTO.UsuarioResumenDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MuestraResponseDTO {
    private Long id;
    private String etiqueta;
    private Double valor;
    private String unidad;
    private LocalDateTime fechaRecoleccion;
    private String observaciones;
    private PacienteResumenDTO paciente;
    private UsuarioResumenDTO usuarioRecolecta;
    private UbicacionMuestraDTO ubicacion;

    // Stream C — TipoMuestra
    private TipoMuestraResumenDTO tipoMuestra;
    private TuboMuestraResumenDTO tuboMuestra;
    /** ID de la muestra padre si es alícuota, null si es primaria. */
    private Long idMuestraPadre;
    private Integer numeroAlicuota;
    private Integer totalAlicuotas;
    /**
     * Número de alícuotas generadas automáticamente al crear esta muestra primaria.
     * Solo se popula en la respuesta de creación; null en lecturas normales.
     */
    private Integer alicuotasGeneradas;
    /** Estado actual en el biobanco (SIN_POSICION, EN_BIOBANCO, PRESTADA, BAJA). */
    private EstadoMuestra estadoMuestra;

    /** ID de la institución propietaria original (inmutable). */
    private Long idInstitucion;
    private String nombreInstitucion;
    /** ID de la institución que actualmente tiene la muestra. */
    private Long idInstitucionActual;
    private String nombreInstitucionActual;

    // ── Contabilidad de volumen ──────────────────────────────────────────────

    /** Volumen reservado para alícuotas creadas y aún sin ubicar (solo padres). */
    private Double valorComprometido;
    /** {@code valor − valorComprometido}: lo que la muestra puede comprometer todavía. */
    private Double valorDisponible;
    /** Cuántas alícuotas de esta padre están creadas pero sin ubicar. */
    private Integer alicuotasPendientes;
    /** Solo alícuotas: si ya ocupó posición y descontó de su padre. */
    private Boolean materializada;
    private LocalDateTime fechaMaterializacion;
    /**
     * Sin volumen por consumo normal. Distinto de {@code estadoMuestra = BAJA},
     * que es una decisión (contaminación, pérdida, retiro de consentimiento).
     */
    private Boolean agotada;
    private LocalDateTime fechaAgotamiento;
    /**
     * Tiene volumen pero todo prometido a alícuotas sin ubicar: no admite
     * estudios ni lotes nuevos aunque {@code valor} sea mayor que cero.
     */
    private Boolean sinDisponible;
}
