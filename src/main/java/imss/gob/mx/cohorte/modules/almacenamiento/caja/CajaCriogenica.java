package imss.gob.mx.cohorte.modules.almacenamiento.caja;


import imss.gob.mx.cohorte.modules.almacenamiento.refrigerador.PosicionPiso;
import jakarta.persistence.*;
import lombok.*;
import java.sql.Timestamp;

@Entity
@Table(name = "Caja_Criogenica")
@Getter @Setter
@NoArgsConstructor
public class CajaCriogenica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_caja")
    private Long Id;

    @Column(name = "codigo_caja", nullable = false, unique = true, length = 50)
    private String codigoCaja;

    @Column(name = "filas", nullable = false)
    private Integer filas;

    @Column(name = "columnas", nullable = false)
    private Integer columnas;

    @Column(name = "tipo_caja", length = 50)
    private String tipoCaja;

    @Column(name = "color", length = 30)
    private String color;

    @Column(name = "observaciones", length = 500)
    private String observaciones;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private Timestamp fechaRegistro;

    @OneToOne
    @JoinColumn(name = "id_posicion_piso", unique = true)
    private PosicionPiso posicionPiso;
}
