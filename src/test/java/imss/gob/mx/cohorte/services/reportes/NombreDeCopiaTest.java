package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Cómo se llama la copia de una plantilla.
 *
 * <p>Parece cosmético y no lo es: el nombre es único por institución, así que una
 * propuesta que choque convierte «duplicar» en un error en la cara de quien solo
 * quería partir de un diseño ya hecho. Y la columna son 120 caracteres: sin recortar
 * la base, duplicar una plantilla de nombre largo revienta al guardar.</p>
 */
class NombreDeCopiaTest {

    private static final int MAX = 120;

    @Test
    @DisplayName("la primera copia no lleva número")
    void primeraCopiaSinNumero() {
        String nombre = PlantillaReporteService.nombreLibreParaCopia("Reporte EKG", n -> false);
        assertThat(nombre).isEqualTo("Reporte EKG (copia)");
    }

    @Test
    @DisplayName("si «(copia)» ya existe se numera desde 2")
    void seNumeraCuandoChoca() {
        Set<String> ocupados = Set.of("Reporte EKG (copia)", "Reporte EKG (copia 2)");

        String nombre = PlantillaReporteService.nombreLibreParaCopia("Reporte EKG", ocupados::contains);

        assertThat(nombre).isEqualTo("Reporte EKG (copia 3)");
    }

    @Test
    @DisplayName("un hueco intermedio se aprovecha")
    void aprovechaElHueco() {
        // Se borró la copia 2. No tiene sentido saltar a la 4.
        Set<String> ocupados = Set.of("Reporte EKG (copia)", "Reporte EKG (copia 3)");

        String nombre = PlantillaReporteService.nombreLibreParaCopia("Reporte EKG", ocupados::contains);

        assertThat(nombre).isEqualTo("Reporte EKG (copia 2)");
    }

    @Test
    @DisplayName("un nombre largo se recorta para que quepa con el sufijo")
    void recortaParaQueQuepa() {
        String largo = "N".repeat(MAX);

        String nombre = PlantillaReporteService.nombreLibreParaCopia(largo, n -> false);

        assertThat(nombre).hasSizeLessThanOrEqualTo(MAX);
        assertThat(nombre).endsWith(" (copia)");
    }

    @Test
    @DisplayName("recortado y numerado sigue cabiendo")
    void recortadoYNumeradoTambienCabe() {
        String largo = "N".repeat(MAX);

        // Todo lo que termine en «(copia)» está tomado, así que tiene que numerar
        // sobre un nombre ya recortado.
        String nombre = PlantillaReporteService.nombreLibreParaCopia(largo, n -> n.endsWith(" (copia)"));

        assertThat(nombre).hasSizeLessThanOrEqualTo(MAX);
        assertThat(nombre).endsWith(" (copia 2)");
    }

    @Test
    @DisplayName("con demasiadas copias se avisa en vez de dar vueltas")
    void demasiadasCopias() {
        assertThatThrownBy(() -> PlantillaReporteService.nombreLibreParaCopia("Reporte EKG", n -> true))
                .isInstanceOf(ObjConflictException.class)
                .hasMessageContaining("demasiadas copias");
    }
}
