package imss.gob.mx.cohorte.services.formulas;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Que el catálogo de unidades reconozca lo que hay escrito en la base.
 *
 * <p>El campo de unidad de un parámetro es texto libre, y en el catálogo del sistema
 * conviven «cm» con «milímetros», «LPM» con «Latidos por minuto LPM», y entradas que
 * no son unidades en absoluto. Estas pruebas fijan qué se reconoce, qué no, y qué
 * pasa con lo que no se reconoce — que es seguir funcionando.</p>
 */
class UnidadTest {

    @Test
    @DisplayName("las unidades del catálogo se reconocen aunque estén escritas de otra forma")
    void sinonimosDelCatalogo() {
        assertThat(Unidad.de("cm").dimension()).isEqualTo(Dimension.LONGITUD);
        assertThat(Unidad.de("metros").dimension()).isEqualTo(Dimension.LONGITUD);
        assertThat(Unidad.de("milímetros").dimension()).isEqualTo(Dimension.LONGITUD);
        assertThat(Unidad.de("MILIMETROS").dimension()).isEqualTo(Dimension.LONGITUD);

        // Las dos formas en que el catálogo escribe la frecuencia cardiaca.
        assertThat(Unidad.de("LPM")).isEqualTo(Unidad.de("Latidos por minuto LPM"));

        // El índice de masa corporal, con el cuadrado escrito de las dos maneras.
        assertThat(Unidad.de("kg/m^2")).isEqualTo(Unidad.de("kg/m²"));
    }

    @Test
    @DisplayName("el signo de micro tiene dos codificaciones y las dos valen")
    void elMicroSeUnifica() {
        // «µ» (signo de micro) y «μ» (mu griega) se dibujan igual y son caracteres
        // distintos. El catálogo usa uno; aquí se escribe el otro.
        assertThat(Unidad.de("10³ / μL").dimension())
                .isEqualTo(Dimension.CONTEO_POR_VOLUMEN);
        assertThat(Unidad.de("10³/µL").dimension())
                .isEqualTo(Dimension.CONTEO_POR_VOLUMEN);
    }

    @Test
    @DisplayName("lo que no es una unidad se deja pasar con su nombre intacto")
    void loQueNoEsUnidad() {
        // Entradas reales del catálogo que no miden nada.
        for (String texto : new String[]{"Según equipo", "Sexo de nacimiento", "A la entrega", "Cuál"}) {
            Unidad u = Unidad.de(texto);
            assertThat(u.dimension()).isEqualTo(Dimension.DESCONOCIDA);
            assertThat(u.nombre())
                    .as("el nombre se conserva para poder imprimirlo igual que siempre")
                    .isEqualTo(texto);
        }
    }

    @Test
    @DisplayName("sin unidad declarada no es lo mismo que unidad desconocida")
    void ningunaNoEsDesconocida() {
        assertThat(Unidad.de(null)).isEqualTo(Unidad.NINGUNA);
        assertThat(Unidad.de("  ")).isEqualTo(Unidad.NINGUNA);
        assertThat(Unidad.NINGUNA.dimension()).isNotEqualTo(Dimension.DESCONOCIDA);
    }

    @Test
    @DisplayName("solo se ofrece cambiar entre unidades de la misma dimensión")
    void seOfreceLoQueMideLoMismo() {
        assertThat(Unidad.de("cm").intercambiables())
                .extracting(Unidad::nombre)
                .containsExactly("mm", "cm", "m");

        assertThat(Unidad.de("mmHg").intercambiables())
                .extracting(Unidad::nombre)
                .containsExactly("mmHg", "kPa");
    }

    @Test
    @DisplayName("una unidad desconocida no tiene con qué intercambiarse")
    void desconocidaSeQuedaSola() {
        Unidad rara = Unidad.de("Según equipo");
        assertThat(rara.intercambiables()).containsExactly(rara);
        assertThat(rara.convertibleA(Unidad.de("kg"))).isFalse();

        // Ni siquiera entre dos desconocidas: coincidir en el texto no da un factor.
        assertThat(Unidad.de("Cuál").convertibleA(Unidad.de("Mano"))).isFalse();
    }

    @Test
    @DisplayName("las adimensionales no se convierten entre sí")
    void porcentajeYPuntosNoSeMezclan() {
        // Comparten el no llevar unidad física y no se parecen en nada más: pasar de
        // «%» a «Puntos» sería ofrecer un disparate.
        assertThat(Unidad.de("%").convertibleA(Unidad.de("Puntos"))).isFalse();
        assertThat(Unidad.de("%").intercambiables()).hasSize(1);

        // Consigo misma sí, porque no hay nada que convertir.
        assertThat(Unidad.de("%").convertibleA(Unidad.de("%"))).isTrue();
    }

    @Test
    @DisplayName("la albúmina puede pasar de mg/dL a g/dL")
    void concentracionesDelLaboratorio() {
        // El catálogo declara la albúmina en mg/dL y el reporte la imprime en g/dL.
        // Están las dos justamente para que ese cambio se pueda ofrecer.
        assertThat(Unidad.de("mg/dL").convertibleA(Unidad.de("g/dL"))).isTrue();
    }
}
