package imss.gob.mx.cohorte.audit.p6spy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * El enmascarado de literales antes de guardar una sentencia en la bitácora.
 *
 * <p>Existe por una caída real: guardar una plantilla de reporte devolvía 500 con
 * {@code StackOverflowError} dentro de {@code java.util.regex}. La sentencia que
 * p6spy entrega lleva los valores ya sustituidos, así que el UPDATE de una
 * plantilla contiene el diseño entero como un solo literal. El motor de regex de
 * Java recursa una vez por vuelta de bucle cuando el {@code *} envuelve una
 * alternación, de modo que un literal largo agota la pila — no es que el patrón
 * fuera lento, es que la profundidad crecía con el texto.</p>
 *
 * <p>Por eso el tamaño de la entrada es lo que más se prueba aquí.</p>
 */
class RedaccionSqlTest {

    private final AuditJdbcEventListener listener = new AuditJdbcEventListener();

    @Test
    @DisplayName("enmascara un literal corriente")
    void enmascaraLiteral() {
        String sql = "update paciente set nombre='Ana López' where id_paciente=7";

        assertThat(listener.redactarValoresSensibles(sql))
                .isEqualTo("update paciente set nombre='***' where id_paciente=7");
    }

    @Test
    @DisplayName("respeta los números, que son los que dan sentido a la bitácora")
    void conservaNumeros() {
        String sql = "update muestra set id_caja=42, alicuota='A-1' where id_muestra=99";

        assertThat(listener.redactarValoresSensibles(sql))
                .isEqualTo("update muestra set id_caja=42, alicuota='***' where id_muestra=99");
    }

    @Test
    @DisplayName("enmascara varios literales de la misma sentencia")
    void variosLiterales() {
        String sql = "insert into t (a,b,c) values ('uno', 3, 'dos')";

        assertThat(listener.redactarValoresSensibles(sql))
                .isEqualTo("insert into t (a,b,c) values ('***', 3, '***')");
    }

    @Test
    @DisplayName("una comilla escapada no cierra el literal")
    void comillaEscapada() {
        // Si se cortara aquí, la segunda mitad del dato quedaría fuera del
        // enmascarado y acabaría en la bitácora en claro.
        String sql = "update t set n='O\\'Brien y su historia' where id=1";

        assertThat(listener.redactarValoresSensibles(sql))
                .isEqualTo("update t set n='***' where id=1");
    }

    @Test
    @DisplayName("un diseño de plantilla entero no tumba la petición")
    void literalMuyLargo() {
        // Un diseño real de reporte pasa de sobra de este tamaño. Es exactamente el
        // caso que devolvía 500 al guardar.
        String diseno = "x".repeat(400_000);
        String sql = "update plantilla_reporte set diseno='" + diseno + "' where id_plantilla=1";

        String redactado = listener.redactarValoresSensibles(sql);

        assertThat(redactado).isEqualTo("update plantilla_reporte set diseno='***' where id_plantilla=1");
    }

    @Test
    @DisplayName("un literal sin cerrar no se lleva por delante la sentencia")
    void literalSinCerrar() {
        // SQL mal formado no debería llegar nunca, pero si llega, más vale enmascarar
        // de más que dejar el resto en claro.
        String sql = "update t set n='algo sin cerrar";

        assertThat(listener.redactarValoresSensibles(sql)).isEqualTo("update t set n='***'");
    }

    @Test
    @DisplayName("sin literales la sentencia queda igual")
    void sinLiterales() {
        String sql = "delete from muestra where id_muestra=3";

        assertThat(listener.redactarValoresSensibles(sql)).isEqualTo(sql);
    }

    @Test
    @DisplayName("null y vacío no revientan")
    void nullYVacio() {
        assertThat(listener.redactarValoresSensibles(null)).isNull();
        assertThat(listener.redactarValoresSensibles("")).isEmpty();
    }
}
