package imss.gob.mx.cohorte.services.importacion;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lleva la columna de fecha de un archivo de instrumento a {@link LocalDateTime}.
 *
 * <h3>Por que no se convierte de zona horaria</h3>
 *
 * <p>La captura manual manda la fecha como hora de pared sin zona
 * ({@code 2026-08-07T16:00}) y se guarda tal cual. Si la carga masiva convirtiera
 * a UTC o a la zona de la institucion, la misma hora escrita en un aparato y
 * escrita a mano quedarian desplazadas entre si, y nadie entenderia por que.</p>
 *
 * <p>La unica excepcion es un texto que traiga desplazamiento explicito
 * ({@code ...Z} o {@code +05:00}): ahi si hay que convertir a la zona del sistema,
 * porque el aparato ya dijo a que instante se refiere.</p>
 *
 * <h3>El orden dia/mes se decide por archivo, no por fila</h3>
 *
 * <p>{@code 03/04/2026} es 3 de abril o 4 de marzo y ninguna heuristica lo resuelve
 * mirando esa fila sola. Se infiere del archivo completo: basta que una fila traiga
 * un componente mayor que 12 para fijar la posicion de todas. Cuando ninguna lo
 * trae, el archivo es ambiguo y hay que preguntarle al usuario — adivinar aqui
 * mueve resultados de mes sin que nadie lo note.</p>
 */
public final class NormalizadorFecha {

    private NormalizadorFecha() {}

    /** Como se leen las fechas separadas por barras o guiones en ESTE archivo. */
    public enum Orden { DIA_MES, MES_DIA }

    /**
     * Lo que se dedujo del archivo.
     *
     * @param orden    el que se aplicara a todas las filas
     * @param ambiguo  true si ninguna fila permitio deducirlo; la pantalla debe
     *                 preguntar antes de continuar
     */
    public record Interpretacion(Orden orden, boolean ambiguo) {}

    /** Numerica con barras o guiones: 3/4/2026, 03-04-2026. */
    private static final Pattern NUMERICA = Pattern.compile(
            "^\\s*(\\d{1,4})[/-](\\d{1,2})[/-](\\d{1,4})(?:[ T](\\d{1,2}):(\\d{2})(?::(\\d{2}))?)?\\s*$");

    private static final DateTimeFormatter[] CON_NOMBRE_DE_MES = {
            DateTimeFormatter.ofPattern("d-MMM-yyyy", new java.util.Locale("es")),
            DateTimeFormatter.ofPattern("d-MMM-yyyy", java.util.Locale.ENGLISH),
            DateTimeFormatter.ofPattern("d MMM yyyy", new java.util.Locale("es")),
            DateTimeFormatter.ofPattern("d MMM yyyy", java.util.Locale.ENGLISH),
            DateTimeFormatter.ofPattern("MMM d, yyyy", java.util.Locale.ENGLISH),
    };

    // ── Inferencia del orden ─────────────────────────────────────────────────

    /**
     * Decide como leer las fechas numericas de este archivo.
     *
     * @throws ArchivoInvalidoException si unas filas exigen dia/mes y otras mes/dia:
     *         el archivo se contradice y cualquier eleccion corrompe la mitad
     */
    public static Interpretacion inferirOrden(List<String> valores) {
        boolean exigeDiaPrimero = false;   // alguna fila tiene primer componente > 12
        boolean exigeMesPrimero = false;   // alguna fila tiene segundo componente > 12

        for (String valor : valores) {
            if (valor == null || valor.isBlank()) continue;
            Matcher m = NUMERICA.matcher(valor);
            if (!m.matches()) continue;

            int primero = Integer.parseInt(m.group(1));
            int segundo = Integer.parseInt(m.group(2));
            int tercero = Integer.parseInt(m.group(3));

            // Formato con el ano delante (2026-08-07): no aporta nada al dilema.
            if (String.valueOf(primero).length() == 4 || tercero < 32 && primero > 31) continue;

            if (primero > 12) exigeDiaPrimero = true;
            if (segundo > 12) exigeMesPrimero = true;
        }

        if (exigeDiaPrimero && exigeMesPrimero) {
            throw new ArchivoInvalidoException(
                    "Las fechas del archivo no son consistentes: unas solo tienen sentido como "
                            + "dia/mes y otras como mes/dia. Revisa la columna de fecha y vuelve a exportarla.");
        }
        if (exigeDiaPrimero) return new Interpretacion(Orden.DIA_MES, false);
        if (exigeMesPrimero) return new Interpretacion(Orden.MES_DIA, false);

        // Ninguna fila desempata. Se propone dia/mes por ser la convencion local,
        // pero marcado como ambiguo para que la pantalla lo confirme.
        return new Interpretacion(Orden.DIA_MES, true);
    }

    // ── Conversion ───────────────────────────────────────────────────────────

    /**
     * Convierte un texto de fecha.
     *
     * @param orden el deducido para el archivo; solo interviene en las numericas
     *              con barras o guiones
     * @throws FechaNoReconocidaException si el texto no corresponde a ningun
     *              formato conocido o la fecha no existe
     */
    public static LocalDateTime parsear(String texto, Orden orden) {
        if (texto == null || texto.isBlank()) {
            throw new FechaNoReconocidaException("La fecha esta vacia.");
        }
        String limpio = texto.trim();

        // 1. Con desplazamiento explicito: el aparato ya dijo a que instante se
        //    refiere, asi que se traslada a la zona del sistema.
        try {
            return OffsetDateTime.parse(limpio)
                    .atZoneSameInstant(ZoneId.systemDefault())
                    .toLocalDateTime();
        } catch (DateTimeParseException ignorada) {
            // no traia zona; sigue
        }

        // 2. ISO sin zona, que es lo que entrega el lector para las celdas de fecha
        //    nativas de Excel.
        try {
            return LocalDateTime.parse(limpio);
        } catch (DateTimeParseException ignorada) {
            // sigue
        }

        try {
            return LocalDate.parse(limpio).atStartOfDay();
        } catch (DateTimeParseException ignorada) {
            // sigue
        }

        // 3. Numerica con barras o guiones.
        Matcher m = NUMERICA.matcher(limpio);
        if (m.matches()) {
            return desdeNumerica(m, orden, limpio);
        }

        // 4. Con el mes en letra, en espanol o ingles.
        String sinPreposiciones = limpio.replace(" de ", " ").replace(".", "");
        for (DateTimeFormatter f : CON_NOMBRE_DE_MES) {
            try {
                return LocalDate.parse(sinPreposiciones, f).atStartOfDay();
            } catch (DateTimeParseException ignorada) {
                // sigue
            }
        }

        throw new FechaNoReconocidaException(
                "No se reconoce \"" + limpio + "\" como fecha. Formatos admitidos: "
                        + "2026-08-07, 2026-08-07 14:30, 07/08/2026 o 7-ago-2026.");
    }

    private static LocalDateTime desdeNumerica(Matcher m, Orden orden, String original) {
        int a = Integer.parseInt(m.group(1));
        int b = Integer.parseInt(m.group(2));
        int c = Integer.parseInt(m.group(3));

        int anio, mes, dia;
        if (String.valueOf(a).length() == 4) {
            // 2026/08/07 — el ano delante no deja lugar a dudas.
            anio = a; mes = b; dia = c;
        } else if (String.valueOf(c).length() == 4) {
            anio = c;
            if (orden == Orden.DIA_MES) { dia = a; mes = b; } else { mes = a; dia = b; }
        } else {
            // Ano de dos cifras: no se adivina el siglo. Un archivo clinico con
            // "07/08/26" puede ser 2026 o 1926, y equivocarse deja el estudio en un
            // sitio donde nadie lo va a buscar.
            throw new FechaNoReconocidaException(
                    "La fecha \"" + original + "\" usa un ano de dos cifras y no se puede saber el siglo. "
                            + "Exporta el archivo con el ano completo (2026).");
        }

        try {
            LocalDate fecha = LocalDate.of(anio, mes, dia);
            int hora = m.group(4) != null ? Integer.parseInt(m.group(4)) : 0;
            int minuto = m.group(5) != null ? Integer.parseInt(m.group(5)) : 0;
            int segundo = m.group(6) != null ? Integer.parseInt(m.group(6)) : 0;
            return fecha.atTime(hora, minuto, segundo);
        } catch (java.time.DateTimeException e) {
            throw new FechaNoReconocidaException(
                    "La fecha \"" + original + "\" no existe en el calendario.");
        }
    }

    /**
     * Problema de UNA fecha, no del archivo entero.
     *
     * <p>Va aparte de {@link ArchivoInvalidoException} porque no detiene la carga:
     * la fila se marca en la previsualizacion para que el usuario la corrija.</p>
     */
    public static class FechaNoReconocidaException extends RuntimeException {
        public FechaNoReconocidaException(String mensaje) {
            super(mensaje);
        }
    }
}
