package imss.gob.mx.cohorte.services.almacenamiento.muestra;

/**
 * La receta de alicuotado de un tubo: cuántas alícuotas salen de él, de qué
 * tamaño y en qué unidad.
 *
 * <p>Es un calco deliberado de los campos de {@code TuboMuestra} y no la entidad
 * misma. El planificador tiene que poder ejercitarse sin base de datos ni JPA, y
 * si recibiera la entidad acabaría arrastrando su grafo —tipo de muestra,
 * institución— hasta las pruebas.</p>
 *
 * @param nombreTubo       solo para redactar mensajes que el usuario entienda
 * @param numeroAlicuotas  cuántas alícuotas produce el tubo; 0 = tubo directo
 * @param volumenAlicuota  capacidad de cada alícuota; null = tubo sin configurar
 * @param unidad           unidad del volumen; manda sobre la de la muestra padre
 * @param permiteParcial   si se admite cerrar el lote con una alícuota incompleta
 */
public record RecetaTubo(
        String nombreTubo,
        Integer numeroAlicuotas,
        Double volumenAlicuota,
        String unidad,
        boolean permiteParcial
) {
}
