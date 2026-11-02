package imss.gob.mx.cohorte.modules.analisis;

import imss.gob.mx.cohorte.modules.persona.Persona;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proyección ligera de un resultado para la agregación. Trae solo lo que el
 * motor necesita: el valor (en sus tres formas tipadas), el sexo y la fecha de
 * nacimiento del participante (para edad a la fecha de medición), la fecha del
 * resultado y la institución. Jamás viaja al cliente — solo alimenta el cálculo
 * agregado en el servicio.
 *
 * <p>Una sola proyección sirve a métricas numéricas y categóricas: el motor usa
 * {@code valorNumerico} o {@code valorTexto}/{@code valorBooleano} según el tipo
 * de la métrica.</p>
 */
public record ResultadoProyeccion(
        Double        valorNumerico,
        String        valorTexto,
        Boolean       valorBooleano,
        Persona.Sexo  sexo,
        LocalDate     fechaNacimiento,
        LocalDateTime fecha,
        Long          institucionId
) {}
