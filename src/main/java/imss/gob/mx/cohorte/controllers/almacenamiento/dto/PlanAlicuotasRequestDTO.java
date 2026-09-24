package imss.gob.mx.cohorte.controllers.almacenamiento.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Plan de alícuotas propuesto desde la pantalla: un volumen por alícuota, en
 * orden.
 *
 * <p>Se recibe la lista explícita en vez de un enum de estrategias —«completar»,
 * «repartir», «dejar en la padre»— porque esa lista se queda corta al tercer
 * caso: el usuario puede querer los 20 mL sobrantes en una sola alícuota, o
 * repartidos entre dos, o en tres desiguales. El servidor no necesita saber qué
 * estrategia se usó, solo comprobar que el reparto cabe.</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlanAlicuotasRequestDTO {

    /** Volumen de cada alícuota del lote. Nunca más entradas que las del tubo. */
    private List<Double> volumenes;
}
