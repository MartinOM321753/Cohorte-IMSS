package imss.gob.mx.cohorte.modules.almacenamiento.traslado;

import com.fasterxml.jackson.annotation.JsonIgnore;
import imss.gob.mx.cohorte.modules.almacenamiento.almacen.Almacen;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "Traslado_Muestra")
@Getter
@Setter
@NoArgsConstructor
public class TrasladoMuestra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_traslado")
    private Long id;

    @ManyToOne
    @JsonIgnore
    @JoinColumn(name = "id_muestra", nullable = false)
    private Muestra muestra;

    @ManyToOne
    @JoinColumn(name = "id_almacen", nullable = false)
    private Almacen almacen;

    @ManyToOne
    @JoinColumn(name = "id_usuario_autoriza", nullable = false)
    private BeanUser autorizadoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoTraslado estado = EstadoTraslado.TRASLADADA;

    @Column(name = "fecha_traslado", nullable = false)
    private LocalDateTime fechaTraslado;

    @Column(name = "fecha_retorno")
    private LocalDateTime fechaRetorno;

    @Column(name = "motivo", nullable = false, length = 300)
    private String motivo;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;
}
