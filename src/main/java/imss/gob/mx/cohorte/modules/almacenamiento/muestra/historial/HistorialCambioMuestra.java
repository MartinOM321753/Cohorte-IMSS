package imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Registro de auditoría de cambios sobre los campos de una Muestra.
 * Se crea automáticamente cada vez que se edita un valor significativo
 * (valor, unidad, observaciones, posición, tipo, etc.).
 */
@Entity
@Table(name = "Historial_Cambio_Muestra")
@Getter
@Setter
@NoArgsConstructor
public class HistorialCambioMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_historial")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_muestra", nullable = false)
    private Muestra muestra;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_usuario", nullable = false)
    private BeanUser usuario;

    /** Nombre del campo que cambió: "valor", "unidad", "observaciones", "posicionCaja", "tipoMuestra", "tuboMuestra" */
    @Column(name = "campo", nullable = false, length = 50)
    private String campo;

    /** Representación string del valor anterior (puede ser null si no había valor). */
    @Column(name = "valor_anterior", length = 500)
    private String valorAnterior;

    /** Representación string del nuevo valor. */
    @Column(name = "valor_nuevo", length = 500)
    private String valorNuevo;

    @Column(name = "fecha_cambio", nullable = false)
    private LocalDateTime fechaCambio;

    @Column(name = "motivo", length = 200)
    private String motivo;
}
