package imss.gob.mx.cohorte.modules.analisis;

import imss.gob.mx.cohorte.modules.persona.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fila cruda (nivel participante) para la exportación con permiso
 * {@code ANALISIS_EXPORTA_CRUDO}. A diferencia de las proyecciones de
 * agregación, lleva el folio del participante: por eso NO pasa por el motor
 * agregado ni por la supresión k-anónima, y vive detrás de un permiso mayor.
 * Sirve a las fuentes de clave-valor tipado (estudios médicos y de muestra).
 */
public record CrudoProy(
        String        folio,
        Long          institucionId,
        Persona.Sexo  sexo,
        LocalDate     fechaNacimiento,
        LocalDateTime fecha,
        Double        valorNumerico,
        String        valorTexto,
        Boolean       valorBooleano
) {}
