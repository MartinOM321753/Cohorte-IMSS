package imss.gob.mx.cohorte.services.referencias;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.services.formulas.Magnitud;
import imss.gob.mx.cohorte.services.formulas.Unidad;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Las tablas de referencia que viajan con la aplicación, leídas de
 * {@code resources/referencias/*.json}.
 *
 * <p>Se leen una sola vez y, si alguna viene mal, la aplicación no arranca: un error
 * de captura en estos datos no debe descubrirse en el reporte de un participante.</p>
 *
 * <p>Es un singleton de clase además de componente de Spring: el resolvedor de campos
 * y el catálogo de variables también se construyen a mano en las pruebas, sin contexto,
 * y necesitan llegar a las tablas igual.</p>
 */
@Component
public class TablasReferencia {

    /** La clave con que una fórmula pide un corte: {@code referencia.dinamometria.p50}. */
    private static final Pattern CLAVE = Pattern.compile("^referencia\\.([a-z0-9_]+)\\.p(\\d+)$");

    private static TablasReferencia estandar;

    private final Map<String, TablaReferencia> porId;

    public TablasReferencia() {
        this.porId = cargar();
    }

    /** La instancia compartida, para quien no pasa por Spring. */
    public static synchronized TablasReferencia estandar() {
        if (estandar == null) estandar = new TablasReferencia();
        return estandar;
    }

    public Optional<TablaReferencia> tabla(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    public List<TablaReferencia> todas() {
        return List.copyOf(porId.values());
    }

    public static String clave(String idTabla, int percentil) {
        return "referencia." + idTabla + ".p" + percentil;
    }

    /**
     * El corte que pide la clave para esta persona.
     *
     * <p>Sin sexo, sin edad o con una edad que la tabla no cubre, el resultado es
     * <b>ausente</b>: un valor de otra edad o del otro sexo sería un dato falso con
     * aspecto de bueno.</p>
     *
     * @param mujer {@code null} si el sexo no está registrado
     * @param edad  {@code null} si no se pudo calcular
     */
    public Magnitud corte(String clave, Boolean mujer, Integer edad) {
        if (clave == null || mujer == null || edad == null) return Magnitud.sinDato();

        Matcher m = CLAVE.matcher(clave);
        if (!m.matches()) return Magnitud.sinDato();

        TablaReferencia tabla = porId.get(m.group(1));
        if (tabla == null) return Magnitud.sinDato();

        int indice = tabla.cortes().indexOf(Integer.parseInt(m.group(2)));
        if (indice < 0) return Magnitud.sinDato();

        return tabla.fila(mujer, edad)
                .map(f -> Magnitud.de(f.valores().get(indice), Unidad.de(tabla.unidad())))
                .orElseGet(Magnitud::sinDato);
    }

    private static Map<String, TablaReferencia> cargar() {
        ObjectMapper json = new ObjectMapper();
        Map<String, TablaReferencia> tablas = new LinkedHashMap<>();
        try {
            for (Resource r : new PathMatchingResourcePatternResolver()
                    .getResources("classpath*:referencias/*.json")) {
                TablaReferencia t;
                try (InputStream in = r.getInputStream()) {
                    t = json.readValue(in, TablaReferencia.class);
                }
                List<String> problemas = t.problemas();
                if (!problemas.isEmpty()) {
                    throw new IllegalStateException("La tabla de referencia «" + r.getFilename()
                            + "» está mal: " + String.join("; ", problemas));
                }
                if (tablas.putIfAbsent(t.id(), t) != null) {
                    throw new IllegalStateException("Hay dos tablas de referencia con el id «"
                            + t.id() + "»");
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("No se pudieron leer las tablas de referencia", e);
        }
        return java.util.Collections.unmodifiableMap(tablas);
    }
}
