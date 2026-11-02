package imss.gob.mx.cohorte.modules.analisis;

import imss.gob.mx.cohorte.modules.persona.Persona;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Fila cruda de somatometría (todas las columnas) para la exportación a nivel participante. */
public record CrudoSomatoProy(
        String        folio,
        Long          institucionId,
        Persona.Sexo  sexo,
        LocalDate     fechaNacimiento,
        LocalDateTime fecha,
        BigDecimal    pesoKg,
        BigDecimal    tallaM,
        BigDecimal    imc,
        Integer       presionSistolica,
        Integer       presionDiastolica,
        BigDecimal    circunferenciaAbdominalCm,
        Integer       frecuenciaCardiacaReposo
) {
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
