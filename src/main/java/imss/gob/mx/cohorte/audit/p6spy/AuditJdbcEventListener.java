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
import java.util.regex.Pattern;

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

    private static final Pattern STRING_LITERAL = Pattern.compile("'(?:[^'\\\\]|\\\\.)*'");

    /**
     * Enmascara los literales de cadena en la sentencia SQL capturada para evitar
     * persistir datos personales (nombres, correos, folios, resultados clínicos en texto libre)
     * en {@code BitacoraAcciones.sentenciaSql}, conservando la estructura de la sentencia
     * (tablas, columnas, operación, valores numéricos) para fines de auditoría.
     */
    private String redactarValoresSensibles(String sql) {
        if (sql == null) return null;
        return STRING_LITERAL.matcher(sql).replaceAll("'***'");
    }

    private void capturar(String sql) {
        if (sql == null || sql.isBlank()) return;
        AuditContextHolder.addSql(redactarValoresSensibles(sql.trim()));
    }
}
