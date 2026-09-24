package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Una página del listado de muestras y las señales para pedir la de al lado.
 *
 * <p>No lleva número de página ni total de páginas: la ventana se sitúa con los
 * cursores, que son opacos a propósito. La pantalla no necesita saber en qué
 * posición está, solo si puede seguir hacia arriba o hacia abajo.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginaMuestrasResponseDTO {

    /** Las tarjetas de esta página, de la más reciente a la más antigua. */
    private List<MuestraResponseDTO> muestras;

    /**
     * Las alícuotas de esas tarjetas, aparte.
     *
     * <p>La pantalla las dibuja plegadas dentro de su padre; van en su propia
     * lista para que el tamaño de página cuente tarjetas y no filas.</p>
     */
    private List<MuestraResponseDTO> alicuotas;

    /** Posición de la primera fila: con ella se pide hacia lo más reciente. */
    private String cursorInicio;

    /** Posición de la última fila: con ella se pide hacia lo más antiguo. */
    private String cursorFin;

    private boolean hayAnteriores;
    private boolean haySiguientes;

    /** Cuántas tarjetas cumplen los criterios, para el «N de M» del pie. */
    private long total;

    /** Cuántas alícuotas huérfanas devueltas están ocultas tras su filtro. */
    private long huerfanasDevueltas;
}
