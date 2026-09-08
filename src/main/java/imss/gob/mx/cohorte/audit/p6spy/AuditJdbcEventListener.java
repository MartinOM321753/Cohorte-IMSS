package imss.gob.mx.cohorte.audit.p6spy;

import com.p6spy.engine.event.JdbcEventListener;
import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import com.p6spy.engine.spy.appender.Slf4JLogger;
import imss.gob.mx.cohorte.audit.context.AuditContextHolder;
import com.p6spy.engine.common.PreparedStatementInformation;
import com.p6spy.engine.common.StatementInformation;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Listener de p6spy que captura sentencias SQL ejecutadas dentro del contexto
 * de auditoría activo (establecido por {@link imss.gob.mx.cohorte.audit.aspect.ApplicationServiceAuditAspect}).
 *
 * <p>Solo captura cuando hay un {@link imss.gob.mx.cohorte.audit.context.AuditContext} activo
 * en el hilo actual, evitando acumular SQL de operaciones de lectura sin correlación.</p>
 */
@Component
public class AuditJdbcEventListener extends JdbcEventListener {

    /**
     * Captura DML (INSERT, UPDATE, DELETE) ejecutados con PreparedStatement.
     * p6spy llama a este método con los valores reales ya sustituidos.
     */
    @Override
    public void onAfterExecuteUpdate(PreparedStatementInformation psi,
                                     long timeElapsedNanos,
                                     int rowCount,
                                     SQLException e) {
        if (e != null || !AuditContextHolder.isActive()) return;
        capturar(psi.getSqlWithValues());
    }

    /**
     * Captura SELECT ejecutados con PreparedStatement (estado previo a mutaciones).
     */
    public void onAfterExecuteQuery(PreparedStatementInformation psi,
                                    long timeElapsedNanos,
                                    ResultSet resultSet,
                                    SQLException e) {
        if (e != null || !AuditContextHolder.isActive()) return;
        capturar(psi.getSqlWithValues());
    }

    /**
     * Captura sentencias ejecutadas como Statement plano (poco común en JPA).
     */
    public void onAfterExecute(StatementInformation si,
                               long timeElapsedNanos,
                               SQLException e) {
        if (e != null || !AuditContextHolder.isActive()) return;
        // Sólo si no es ya un PreparedStatement (evitar duplicados)
        if (!(si instanceof PreparedStatementInformation)) {
            capturar(si.getSqlWithValues());
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static final String MASCARA = "'***'";

    /**
     * Enmascara los literales de cadena de la sentencia capturada para no persistir
     * datos personales —nombres, correos, folios, resultados clínicos en texto
     * libre— en {@code BitacoraAcciones.sentenciaSql}, conservando la estructura
     * que le da sentido a la bitácora: tablas, columnas, operación y valores
     * numéricos.
     *
     * <p>Se recorre a mano en vez de con una expresión regular, y no por gusto.
     * El patrón anterior, {@code '(?:[^'\\]|\\.)*'}, envolvía una alternación en
     * un {@code *}, y el motor de Java recursa una vez por vuelta en ese caso: la
     * profundidad de pila crecía con el largo del literal. p6spy entrega la
     * sentencia con los valores ya sustituidos, así que el UPDATE de una plantilla
     * de reporte lleva el diseño entero como un solo literal — y guardar una
     * plantilla acabó devolviendo 500 con {@code StackOverflowError}. Este recorrido
     * es lineal y no usa pila.</p>
     *
     * <p>Un literal sin cerrar se enmascara igualmente hasta el final. Es SQL que no
     * debería llegar nunca, pero si llega, más vale tapar de más que dejar el resto
     * en claro.</p>
     */
    String redactarValoresSensibles(String sql) {
        if (sql == null) return null;
        if (sql.indexOf('\'') < 0) return sql;

        StringBuilder salida = new StringBuilder(sql.length());
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c != '\'') {
                salida.append(c);
                i++;
                continue;
            }
            salida.append(MASCARA);
            i = finDelLiteral(sql, i);
        }
        return salida.toString();
    }

    /**
     * Dónde acaba el literal que abre en {@code inicio}, ya pasada la comilla de
     * cierre.
     *
     * <p>Se reconocen las dos formas de meter una comilla dentro: escapada con
     * barra —lo que usa MySQL— y duplicada, que es la del estándar. Si solo se
     * tratara una, la otra cortaría el literal a la mitad y la segunda parte del
     * dato saldría sin enmascarar.</p>
     */
    private static int finDelLiteral(String sql, int inicio) {
        int i = inicio + 1;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '\\' && i + 1 < sql.length()) {
                i += 2;
            } else if (c == '\'') {
                boolean duplicada = i + 1 < sql.length() && sql.charAt(i + 1) == '\'';
                if (duplicada) {
                    i += 2;
                } else {
                    return i + 1;
                }
            } else {
                i++;
            }
        }
        // Sin comilla de cierre: se considera que el literal llega al final.
        return i;
    }

    private void capturar(String sql) {
        if (sql == null || sql.isBlank()) return;
        AuditContextHolder.addSql(redactarValoresSensibles(sql.trim()));
    }
}
