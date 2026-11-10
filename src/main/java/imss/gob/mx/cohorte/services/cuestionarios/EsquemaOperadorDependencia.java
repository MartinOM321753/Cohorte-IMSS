package imss.gob.mx.cohorte.services.cuestionarios;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.OptionalInt;

/**
 * Pregunta a la base de datos cuánto mide hoy la columna
 * {@code variable_cuestionario.operador_dependencia}.
 *
 * <p>Existe porque {@code ddl-auto=update} crea columnas pero no las ensancha: la
 * entidad dice {@code VARCHAR(20)} y una base que ya existía sigue con
 * {@code VARCHAR(10)}. Los operadores {@code SIN_RESPUESTA} y {@code CON_RESPUESTA}
 * miden 13, así que cargar un catálogo que los use contra una columna angosta
 * trunca el valor a «SIN_RESPUE» (modo permisivo de MySQL) y el cuestionario revienta
 * al abrirse, o falla a media carga (modo estricto). Ver
 * {@code db/manual/2026-11-06_operador_dependencia_ancho.sql}.</p>
 *
 * <p>Esta clase solo <b>mide</b>; quien decide si bloquear es la carga del catálogo.
 * Si no se puede medir (otro motor, otro esquema, sin permiso de lectura) responde
 * vacío y la carga sigue: una guarda que tumbara la carga por no poder mirar sería
 * peor que la omisión que quiere evitar.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EsquemaOperadorDependencia {

    private static final String TABLA = "variable_cuestionario";
    private static final String COLUMNA = "operador_dependencia";

    private final DataSource dataSource;

    /** Caracteres que admite la columna, o vacío si no se pudo averiguar. */
    public OptionalInt anchoActual() {
        try (Connection c = dataSource.getConnection()) {
            DatabaseMetaData meta = c.getMetaData();
            // MySQL guarda el nombre tal cual; H2 y otros lo suben a mayúsculas.
            for (String tabla : new String[]{TABLA, TABLA.toUpperCase()}) {
                for (String columna : new String[]{COLUMNA, COLUMNA.toUpperCase()}) {
                    try (ResultSet rs = meta.getColumns(c.getCatalog(), null, tabla, columna)) {
                        if (rs.next()) {
                            int ancho = rs.getInt("COLUMN_SIZE");
                            return rs.wasNull() ? OptionalInt.empty() : OptionalInt.of(ancho);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("No se pudo medir {}.{}; la carga sigue sin esa comprobacion: {}",
                    TABLA, COLUMNA, e.getMessage());
        }
        return OptionalInt.empty();
    }
}
