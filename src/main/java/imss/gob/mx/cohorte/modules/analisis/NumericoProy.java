package imss.gob.mx.cohorte.modules.analisis;

import imss.gob.mx.cohorte.modules.persona.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proyección numérica simple (exámenes de laboratorio): un valor con la
 * demografía del participante. Se convierte a {@link ResultadoProyeccion} en el
 * servicio para reutilizar el mismo motor de agregación.
 */
public record NumericoProy(
        Double        valor,
        Persona.Sexo  sexo,
        LocalDate     fechaNacimiento,
        LocalDateTime fecha,
        Long          institucionId
) {}
