package imss.gob.mx.cohorte.modules.analisis;

import imss.gob.mx.cohorte.modules.persona.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fila cruda numérica (exámenes de laboratorio) para la exportación a nivel participante. */
public record CrudoNumProy(
        String        folio,
        Long          institucionId,
        Persona.Sexo  sexo,
        LocalDate     fechaNacimiento,
        LocalDateTime fecha,
        Double        valor
) {}
