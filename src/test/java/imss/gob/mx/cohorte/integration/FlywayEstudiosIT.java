package imss.gob.mx.cohorte.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
class FlywayEstudiosIT {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("bootstrap");

    @Test
    void migrationAddsExpectedColumnsAndAdjuntosTable() throws Exception {
        String schema = createMigratedSchema();

        try (Connection connection = openSchemaConnection(schema)) {
            assertTrue(columnExists(connection, "Resultado_Estudio", "valor_booleano"));
            assertTrue(columnExists(connection, "Resultado_Estudio", "grupo_codigo"));
            assertTrue(columnExists(connection, "Resultado_Estudio", "grupo_etiqueta"));
            assertTrue(columnExists(connection, "Resultado_Estudio", "orden_resultado"));
            assertTrue(tableExists(connection, "Estudio_Adjunto"));
        }
    }

    @Test
    void migrationAllowsParametroNamesAcrossDifferentStudyTypes() throws Exception {
        String schema = createMigratedSchema();

        try (Connection connection = openSchemaConnection(schema)) {
            try (Statement statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO Tipo_Estudio(nombre, descripcion, activo, fecha_creacion) VALUES ('Tipo A', NULL, b'1', NOW())");
                statement.executeUpdate("INSERT INTO Tipo_Estudio(nombre, descripcion, activo, fecha_creacion) VALUES ('Tipo B', NULL, b'1', NOW())");
            }

            assertDoesNotThrow(() -> {
                try (Statement statement = connection.createStatement()) {
                    statement.executeUpdate("INSERT INTO Parametro_Estudio(id_tipo_estudio, nombre, unidad) VALUES (1, 'IMC', 'kg/m2')");
                    statement.executeUpdate("INSERT INTO Parametro_Estudio(id_tipo_estudio, nombre, unidad) VALUES (2, 'IMC', 'kg/m2')");
                }
            });
        }
    }

    @Test
    void migrationEnforcesResultadoCompositeUniqueness() throws Exception {
        String schema = createMigratedSchema();

        try (Connection connection = openSchemaConnection(schema);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Tipo_Estudio(nombre, descripcion, activo, fecha_creacion) VALUES ('Tipo A', NULL, b'1', NOW())");
            statement.executeUpdate("INSERT INTO Parametro_Estudio(id_tipo_estudio, nombre, unidad) VALUES (1, 'Peso', 'kg')");
            statement.executeUpdate("INSERT INTO Estudio_Medico VALUES (1)");
            statement.executeUpdate("INSERT INTO Resultado_Estudio(id_estudio, id_parametro, valor_numerico, grupo_codigo, orden_resultado) VALUES (1, 1, 20.0, 'ROOT', 0)");
            statement.executeUpdate("INSERT INTO Resultado_Estudio(id_estudio, id_parametro, valor_numerico, grupo_codigo, orden_resultado) VALUES (1, 1, 21.0, 'ETAPA_1', 1)");

            assertThrows(SQLException.class, () ->
                    statement.executeUpdate("INSERT INTO Resultado_Estudio(id_estudio, id_parametro, valor_numerico, grupo_codigo, orden_resultado) VALUES (1, 1, 22.0, 'ROOT', 0)")
            );
        }
    }

    @Test
    void migrationCreatesAdjuntoOrderConstraintPerStudy() throws Exception {
        String schema = createMigratedSchema();

        try (Connection connection = openSchemaConnection(schema);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO Estudio_Medico VALUES (1)");
            statement.executeUpdate("INSERT INTO Estudio_Adjunto(id_estudio, tipo, nombre_original, mime_type, ruta_url, descripcion, orden_adjunto) VALUES (1, 'PDF', 'a.pdf', 'application/pdf', '/tmp/a.pdf', NULL, 0)");
            statement.executeUpdate("INSERT INTO Estudio_Adjunto(id_estudio, tipo, nombre_original, mime_type, ruta_url, descripcion, orden_adjunto) VALUES (1, 'PDF', 'b.pdf', 'application/pdf', '/tmp/b.pdf', NULL, 1)");

            assertThrows(SQLException.class, () ->
                    statement.executeUpdate("INSERT INTO Estudio_Adjunto(id_estudio, tipo, nombre_original, mime_type, ruta_url, descripcion, orden_adjunto) VALUES (1, 'PDF', 'c.pdf', 'application/pdf', '/tmp/c.pdf', NULL, 1)")
            );
        }
    }

    private String createMigratedSchema() throws Exception {
        String schema = "cohorte_it_" + UUID.randomUUID().toString().replace("-", "");
        try (Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + schema);
        }

        try (Connection connection = openSchemaConnection(schema)) {
            ScriptUtils.executeSqlScript(connection, new ClassPathResource("db/integration/base-schema.sql"));
        }

        Flyway.configure()
                .dataSource(schemaJdbcUrl(schema), MYSQL.getUsername(), MYSQL.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();

        return schema;
    }

    private Connection openSchemaConnection(String schema) throws SQLException {
        return DriverManager.getConnection(schemaJdbcUrl(schema), MYSQL.getUsername(), MYSQL.getPassword());
    }

    private String schemaJdbcUrl(String schema) {
        return "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getFirstMappedPort() + "/" + schema
                + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """)) {
            statement.setString(1, table);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean columnExists(Connection connection, String table, String column) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """)) {
            statement.setString(1, table);
            statement.setString(2, column);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }
}
