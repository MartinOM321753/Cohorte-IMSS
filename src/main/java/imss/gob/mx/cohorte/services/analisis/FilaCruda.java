package imss.gob.mx.cohorte.services.analisis;

/**
 * Fila cruda ya normalizada para exportar (nivel participante). Es el formato
 * común al que se traducen las cuatro fuentes antes de escribir CSV o XLSX.
 */
public record FilaCruda(
        String  folio,
        String  institucion,
        String  sexo,
        Integer edad,
        String  fecha,
        Double  valorNumerico,
        String  valorTexto
) {}
