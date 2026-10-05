package imss.gob.mx.cohorte.modules.almacenamiento.protocolo;

/**
 * Qué se le hace a un tubo primario dentro de un protocolo de procesamiento.
 *
 * <p>Los tres destinos reales de un tubo recolectado, abstraídos de la hoja de
 * procesamiento del laboratorio:</p>
 *
 * <ul>
 *   <li>{@link #GUARDAR} — se conserva entero, sin alicuotar; típicamente pasa a
 *       custodia y de ahí a una institución (los Vacutainer tapa lila a 4 °C).</li>
 *   <li>{@link #ESTUDIO} — se consume en mediciones (VCG, hematocrito, conteo de
 *       plaquetas); termina en cero y se desecha. No produce alícuotas; sus
 *       valores se guardan como Estudios de muestra.</li>
 *   <li>{@link #ALICUOTAR} — se transforma en alícuotas guardables (los 12 sueros
 *       de 500 µL). El tipo resultante puede diferir del tipo del tubo origen:
 *       sangre total → suero.</li>
 * </ul>
 *
 * <p>Es un valor por omisión del protocolo, no una regla inamovible: al procesar
 * un participante se decide qué se le hace de verdad a cada tubo, porque cualquier
 * tubo —incluso los que hoy se guardan— podría alicuotarse a futuro.</p>
 */
public enum AccionTubo {
    GUARDAR,
    ESTUDIO,
    ALICUOTAR
}
