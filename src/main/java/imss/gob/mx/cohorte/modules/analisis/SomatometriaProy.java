package imss.gob.mx.cohorte.modules.analisis;

import imss.gob.mx.cohorte.modules.persona.Persona;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Proyección de un registro de somatometría con todas sus columnas numéricas.
 * El servicio escoge la columna según la clave de la métrica
 * ({@code somato.imc}, {@code somato.pesoKg}, …) y la convierte a
 * {@link ResultadoProyeccion}.
 */
public record SomatometriaProy(
        BigDecimal    pesoKg,
        BigDecimal    tallaM,
        BigDecimal    imc,
        Integer       presionSistolica,
        Integer       presionDiastolica,
        BigDecimal    circunferenciaAbdominalCm,
        Integer       frecuenciaCardiacaReposo,
        Persona.Sexo  sexo,
        LocalDate     fechaNacimiento,
        LocalDateTime fecha,
        Long          institucionId
) {
    /** Valor de la columna indicada por el nombre de campo de la métrica. */
    public Double valorDe(String campo) {
        return switch (campo) {
            case "pesoKg"                    -> aDouble(pesoKg);
            case "tallaM"                    -> aDouble(tallaM);
            case "imc"                       -> aDouble(imc);
            case "presionSistolica"          -> presionSistolica != null ? presionSistolica.doubleValue() : null;
            case "presionDiastolica"         -> presionDiastolica != null ? presionDiastolica.doubleValue() : null;
            case "circunferenciaAbdominalCm" -> aDouble(circunferenciaAbdominalCm);
            case "frecuenciaCardiacaReposo"  -> frecuenciaCardiacaReposo != null ? frecuenciaCardiacaReposo.doubleValue() : null;
            default -> null;
        };
    }

    private static Double aDouble(BigDecimal b) {
        return b != null ? b.doubleValue() : null;
    }
}
