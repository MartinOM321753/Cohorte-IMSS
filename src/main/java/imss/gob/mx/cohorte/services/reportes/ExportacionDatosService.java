package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Baja los datos de varios participantes, con las fórmulas ya aplicadas.
 *
 * <p>Es el segundo consumidor del motor, y la razón de que el motor no viva dentro del
 * maquetador. Lo que se descarga sale del <b>mismo cálculo</b> que lo que se imprime:
 * una segunda implementación acabaría dando números distintos para el mismo
 * participante, y nadie sabría cuál creer.</p>
 *
 * <p>Las columnas se piden por su clave —{@code estudio.7.param.85},
 * {@code formula.3}, {@code participante.folio}— exactamente igual que en una
 * plantilla. Quien exporta elige qué columnas quiere de la misma lista de la que elige
 * qué imprimir.</p>
 */
@Service
@AllArgsConstructor
public class ExportacionDatosService {

    /**
     * Cuántos participantes admite una descarga.
     *
     * <p>Cada uno cuesta traer sus estudios y sus resultados de laboratorio, así que una
     * cohorte entera de golpe tarda y ocupa memoria. El tope hace que la petición falle
     * con una explicación en lugar de quedarse colgada, que es lo que se ve desde el
     * navegador cuando no hay límite.</p>
     */
    public static final int MAXIMO_PARTICIPANTES = 2000;

    /** Marca de orden de bytes: sin ella Excel abre el archivo con los acentos rotos. */
    private static final String BOM = "﻿";

    private final ResolvedorCampos resolvedor;
    private final CatalogoCamposReporte catalogo;

    /**
     * @param claves      las columnas, por su clave del catálogo
     * @param separador   coma o punto y coma; ver {@link #normalizarSeparador(String)}
     * @param contextos   cómo obtener los datos de cada participante, ya filtrados por
     *                    quien tiene derecho a verlos
     */
    public byte[] aCsv(List<String> claves, String separador,
                       List<String> uuids,
                       Function<String, ContextoReporte> contextos) {
        if (claves == null || claves.isEmpty()) {
            throw new ValidationException("Hay que elegir al menos una columna.");
        }
        if (uuids == null || uuids.isEmpty()) {
            throw new ValidationException("Hay que elegir al menos un participante.");
        }
        if (uuids.size() > MAXIMO_PARTICIPANTES) {
            throw new ValidationException(
                    "La descarga admite hasta " + MAXIMO_PARTICIPANTES + " participantes y se "
                            + "pidieron " + uuids.size() + ". Conviene acotar la selección.");
        }

        String sep = normalizarSeparador(separador);
        StringBuilder csv = new StringBuilder(BOM);

        Map<String, String> rotulos = rotulos();
        csv.append(claves.stream()
                .map(c -> celda(rotulos.getOrDefault(c, c), sep))
                .collect(Collectors.joining(sep)));
        csv.append("\r\n");

        for (String uuid : uuids) {
            ContextoReporte ctx = contextos.apply(uuid);
            csv.append(claves.stream()
                    .map(clave -> celda(resolvedor.valorDe(clave, ctx), sep))
                    .collect(Collectors.joining(sep)));
            csv.append("\r\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    /** Los rótulos con los que el catálogo nombra cada clave, para la fila de títulos. */
    private Map<String, String> rotulos() {
        return catalogo.todos().stream().collect(Collectors.toMap(
                CatalogoCamposReporte.Campo::clave,
                CatalogoCamposReporte.Campo::rotulo,
                (a, b) -> a));
    }

    /**
     * Coma o punto y coma, nada más.
     *
     * <p>La coma es lo que esperan las herramientas de análisis; el punto y coma es lo
     * que espera Excel configurado en español, que con comas mete toda la fila en una
     * sola celda. Como no hay una respuesta buena para los dos, se elige al descargar.</p>
     */
    private String normalizarSeparador(String separador) {
        if (separador == null || separador.isBlank()) return ",";
        String limpio = separador.trim();
        if (!",".equals(limpio) && !";".equals(limpio)) {
            throw new ValidationException("El separador solo puede ser una coma o un punto y coma.");
        }
        return limpio;
    }

    /**
     * Una celda, entrecomillada si hace falta.
     *
     * <p>Se entrecomilla cuando el texto lleva el separador, comillas o un salto de
     * línea —las observaciones de un estudio los llevan— y las comillas de dentro se
     * duplican. Sin esto, un solo campo con una coma desplaza todas las columnas de esa
     * fila y el archivo deja de cuadrar sin que nada avise.</p>
     */
    private String celda(String valor, String separador) {
        if (valor == null || valor.isEmpty()) return "";
        boolean necesitaComillas = valor.contains(separador) || valor.contains("\"")
                || valor.contains("\n") || valor.contains("\r");
        if (!necesitaComillas) return valor;
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}
