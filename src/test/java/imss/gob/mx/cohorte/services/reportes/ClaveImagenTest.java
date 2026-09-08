package imss.gob.mx.cohorte.services.reportes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cómo apunta un diseño a una imagen de la galería.
 *
 * <p>La forma exacta importa más de lo que parece: el borrado consulta qué plantillas
 * usan una imagen buscando este texto dentro del JSON del diseño. Si la clave admitiera
 * variantes, esa consulta dejaría de encontrarlas y se podría borrar un logo que
 * estaba en uso.</p>
 */
class ClaveImagenTest {

    @Test
    @DisplayName("id y clave son la misma cosa en los dos sentidos")
    void idaYVuelta() {
        assertThat(ClaveImagen.de(42L)).isEqualTo("imagen:42");
        assertThat(ClaveImagen.idDe("imagen:42")).isEqualTo(42L);
    }

    @Test
    @DisplayName("lo que no es una clave de imagen se rechaza")
    void loQueNoEsClave() {
        assertThat(ClaveImagen.idDe(null)).isNull();
        assertThat(ClaveImagen.idDe("")).isNull();
        assertThat(ClaveImagen.idDe("imagen:")).isNull();
        assertThat(ClaveImagen.idDe("imagen:abc")).isNull();
        assertThat(ClaveImagen.idDe("bloque.estudios.listado")).isNull();
    }

    @Test
    @DisplayName("una dirección escrita a mano no es una clave")
    void unaUrlNoEsClave() {
        // Es lo que impide que el maquetador descargue nada: quien pediría esa
        // dirección al imprimir sería el servidor, no el navegador de quien mira.
        assertThat(ClaveImagen.esClave("https://intranet.imss.gob.mx/logo.png")).isFalse();
        assertThat(ClaveImagen.esClave("data:image/png;base64,AAAA")).isFalse();
        assertThat(ClaveImagen.esClave("file:///C:/logo.png")).isFalse();
    }

    @Test
    @DisplayName("la 1 y la 10 no se confunden")
    void unaNoEsLaOtra() {
        // La consulta de usos compara «"imagen:1"» con comillas justamente por esto.
        assertThat(ClaveImagen.de(1L)).isNotEqualTo(ClaveImagen.de(10L));
        assertThat(ClaveImagen.idDe("imagen:10")).isEqualTo(10L);
    }

    @Test
    @DisplayName("se toleran los espacios de sobra")
    void toleraEspacios() {
        assertThat(ClaveImagen.idDe("  imagen:7  ")).isEqualTo(7L);
    }
}
