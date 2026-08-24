package imss.gob.mx.cohorte.controllers.almacenamiento.dto.ubicacion3d;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Ficha de la muestra para el panel lateral.
 *
 * <p>Solo campos que la entidad Muestra (o sus catálogos) guardan de verdad.
 * No hay ciclos de congelación, ni auditoría de revisiones, ni responsable de
 * resguardo: esos conceptos no existen en el modelo y por eso no viajan.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ubicacion3DMuestraDTO {
    private Long id;
    private String etiqueta;
    private String estadoMuestra;

    /** Muestra.valor + Muestra.unidad — el volumen registrado. */
    private Double valor;
    private String unidad;
    private LocalDateTime fechaRecoleccion;
    private String observaciones;

    /** TipoMuestra.nombre y su temperatura de almacenamiento de catálogo. */
    private String tipoMuestra;
    private String temperaturaAlmacenamiento;
    /** TuboMuestra.nombre — el contenedor. */
    private String tuboMuestra;

    /** Solo en alícuotas. */
    private Integer numeroAlicuota;
    private Integer totalAlicuotas;
    private Long idMuestraPadre;
    private String etiquetaMuestraPadre;

    private String pacienteFolio;
    private String pacienteNombre;
    /** Usuario que recolectó — es el único responsable que la muestra registra. */
    private String usuarioRecolecta;

    private String institucionPropietaria;
    private String institucionActual;
}
