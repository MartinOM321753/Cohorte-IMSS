package imss.gob.mx.cohorte.services.reportes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * De dónde sale el nombre con el que una imagen aparece en la galería.
 *
 * <p>Se toma del archivo, y lo que llega ahí no siempre es un nombre: algunos
 * navegadores mandan la ruta entera, y el usuario puede subir algo llamado solo
 * «.png». La galería tiene que quedar legible en los dos casos.</p>
 */
class NombreDeImagenTest {

    @Test
    @DisplayName("se quita la extensión")
    void quitaLaExtension() {
        assertThat(ImagenReporteService.limpiarNombreDeArchivo("logo-imss.png"))
                .isEqualTo("logo-imss");
    }

    @Test
    @DisplayName("de una ruta completa se queda con el archivo")
    void soloElArchivo() {
        // Internet Explorer y algunos clientes mandan la ruta local entera.
        assertThat(ImagenReporteService.limpiarNombreDeArchivo("C:\\Users\\ana\\Escritorio\\membrete.jpg"))
                .isEqualTo("membrete");
        assertThat(ImagenReporteService.limpiarNombreDeArchivo("/home/ana/sello.png"))
                .isEqualTo("sello");
    }

    @Test
    @DisplayName("un nombre con puntos conserva todo menos la extensión")
    void nombreConPuntos() {
        assertThat(ImagenReporteService.limpiarNombreDeArchivo("logo.v2.final.png"))
                .isEqualTo("logo.v2.final");
    }

    @Test
    @DisplayName("sin nada usable queda un nombre genérico, no uno vacío")
    void nombreGenerico() {
        assertThat(ImagenReporteService.limpiarNombreDeArchivo(null)).isEqualTo("Imagen");
        assertThat(ImagenReporteService.limpiarNombreDeArchivo("")).isEqualTo("Imagen");
        assertThat(ImagenReporteService.limpiarNombreDeArchivo("   ")).isEqualTo("Imagen");
        // Un archivo que es solo extensión: sin esto quedaría una fila sin rótulo.
        assertThat(ImagenReporteService.limpiarNombreDeArchivo(".png")).isEqualTo("Imagen");
    }
}
