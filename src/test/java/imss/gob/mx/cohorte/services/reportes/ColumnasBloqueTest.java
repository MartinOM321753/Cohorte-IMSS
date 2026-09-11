package imss.gob.mx.cohorte.services.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Las columnas que se ofrecen marcar tienen que ser las que ese bloque imprime.
 *
 * <p>Existe por un fallo concreto: el panel de propiedades ofrecía siempre
 * «Parámetro / Resultado / Unidad / Referencia», también sobre el listado de
 * estudios, que en realidad saca «Estudio / Fecha / Resultados». Se marcaban unas
 * casillas y salían otras columnas, sin que nada avisara.</p>
 */
class ColumnasBloqueTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode elemento(String clave, String columnasJson) {
        String json = "{\"clave\":\"" + clave + "\""
                + (columnasJson == null ? "" : ",\"estilo\":{\"columnas\":" + columnasJson + "}")
                + "}";
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("cada bloque declara sus propias columnas")
    void cadaBloqueTieneLasSuyas() {
        assertThat(ColumnasBloque.clavesDe("bloque.estudio.10.resultados"))
                .containsExactly("parametro", "valor", "unidad", "referencia", "estado");
        assertThat(ColumnasBloque.clavesDe(ClaveCampo.BLOQUE_LISTADO_ESTUDIOS))
                .containsExactly("estudio", "fecha", "resultados");
        assertThat(ColumnasBloque.clavesDe(ClaveCampo.BLOQUE_LISTADO_EXAMENES))
                .containsExactly("examen", "valor", "unidad", "referencia", "estado", "fecha");
    }

    @Test
    @DisplayName("las evidencias no son una tabla y no tienen columnas")
    void evidenciasSinColumnas() {
        assertThat(ColumnasBloque.clavesDe("bloque.estudio.10.evidencias")).isEmpty();
        assertThat(ColumnasBloque.clavesDe("estudio.10.param.101")).isEmpty();
        assertThat(ColumnasBloque.clavesDe(null)).isEmpty();
    }

    @Test
    @DisplayName("sin columnas guardadas se usan todas las del bloque, no una lista fija")
    void porDefectoLasDelBloque() {
        assertThat(BloqueResultados.Estilo.de(elemento(ClaveCampo.BLOQUE_LISTADO_ESTUDIOS, null)).columnas())
                .containsExactly("estudio", "fecha", "resultados");
        assertThat(BloqueResultados.Estilo.de(elemento(ClaveCampo.BLOQUE_LISTADO_EXAMENES, null)).columnas())
                .containsExactly("examen", "valor", "unidad", "referencia", "estado", "fecha");
    }

    @Test
    @DisplayName("una columna que el bloque no admite se descarta")
    void descartaLoQueNoAdmite() {
        // Es lo que queda guardado en una plantilla diseñada antes de la corrección,
        // o al cambiarle la clave a un elemento ya colocado.
        List<String> columnas = BloqueResultados.Estilo
                .de(elemento(ClaveCampo.BLOQUE_LISTADO_ESTUDIOS, "[\"parametro\",\"fecha\",\"referencia\"]"))
                .columnas();

        assertThat(columnas).containsExactly("fecha");
    }

    @Test
    @DisplayName("si no queda ninguna válida se vuelve a todas, no a una tabla vacía")
    void ningunaValidaVuelveATodas() {
        List<String> columnas = BloqueResultados.Estilo
                .de(elemento(ClaveCampo.BLOQUE_LISTADO_ESTUDIOS, "[\"parametro\",\"unidad\"]"))
                .columnas();

        assertThat(columnas).containsExactly("estudio", "fecha", "resultados");
    }

    @Test
    @DisplayName("se respeta lo elegido cuando el bloque sí lo admite")
    void respetaLoElegido() {
        List<String> columnas = BloqueResultados.Estilo
                .de(elemento("bloque.estudio.10.resultados", "[\"parametro\",\"valor\"]"))
                .columnas();

        assertThat(columnas).containsExactly("parametro", "valor");
    }
}
