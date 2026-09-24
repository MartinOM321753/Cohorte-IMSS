package imss.gob.mx.cohorte.modules.almacenamiento.muestra;

import jakarta.persistence.Entity;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Que el JPQL del listado lo compile Hibernate de verdad.
 *
 * <p>{@code ConsultaDelListadoTest} comprueba la forma del texto; esto comprueba
 * que ese texto SIGNIFICA algo: que los alias existen, que las rutas de
 * propiedad corresponden al modelo, que las funciones son las que Hibernate
 * conoce. Un {@code m.fechaRegistro} mal escrito pasa cualquier revisión de
 * cadenas y solo revienta cuando alguien abre la pantalla, porque una consulta
 * construida en ejecución no la valida nadie al arrancar —a diferencia de las
 * anotadas con {@code @Query}, que Spring sí compila al iniciar—.</p>
 *
 * <p>Se levanta un registro de Hibernate sin base de datos: se fija el dialecto
 * a mano y se le prohíbe consultar los metadatos del controlador, de modo que
 * nunca abre una conexión. La prueba corre igual en una máquina sin MySQL.</p>
 */
class ElListadoCompilaTest {

    private static StandardServiceRegistry registro;
    private static SessionFactoryImplementor fabrica;

    @BeforeAll
    static void levantarModelo() throws IOException, URISyntaxException {
        Map<String, Object> ajustes = new HashMap<>();
        ajustes.put("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        // Sin esto Hibernate pediría una conexión para deducir el dialecto.
        ajustes.put("hibernate.boot.allow_jdbc_metadata_access", "false");
        ajustes.put("hibernate.hbm2ddl.auto", "none");
        ajustes.put("hibernate.temp.use_jdbc_metadata_defaults", "false");
        ajustes.put("hibernate.connection.provider_disables_autocommit", "true");

        registro = new StandardServiceRegistryBuilder().applySettings(ajustes).build();
        MetadataSources fuentes = new MetadataSources(registro);
        for (Class<?> entidad : entidades()) {
            fuentes.addAnnotatedClass(entidad);
        }

        fabrica = (SessionFactoryImplementor) fuentes.buildMetadata().buildSessionFactory();
    }

    @AfterAll
    static void apagar() {
        if (fabrica != null) {
            fabrica.close();
        }
        if (registro != null) {
            StandardServiceRegistryBuilder.destroy(registro);
        }
    }

    /**
     * Todas las entidades compiladas del proyecto.
     *
     * <p>Se recorre el directorio de clases en vez de enumerarlas: el modelo de
     * {@code Muestra} arrastra participante, persona, institución, usuario,
     * posición, caja, piso, refrigerador, tipo, tubo y traslado, y cada una las
     * suyas. Mantener esa lista a mano se rompería con la primera entidad nueva
     * que alguien agregue.</p>
     */
    private static List<Class<?>> entidades() throws IOException, URISyntaxException {
        URL raizUrl = ElListadoCompilaTest.class.getProtectionDomain()
                .getCodeSource().getLocation();
        File raizPruebas = new File(raizUrl.toURI());
        File raiz = new File(raizPruebas.getParentFile(), "classes");

        List<Class<?>> encontradas = new ArrayList<>();
        recorrer(raiz, raiz, encontradas);
        return encontradas;
    }

    private static void recorrer(File raiz, File actual, List<Class<?>> acumulado) {
        File[] hijos = actual.listFiles();
        if (hijos == null) {
            return;
        }
        for (File hijo : hijos) {
            if (hijo.isDirectory()) {
                recorrer(raiz, hijo, acumulado);
                continue;
            }
            if (!hijo.getName().endsWith(".class")) {
                continue;
            }
            String nombre = raiz.toPath().relativize(hijo.toPath()).toString()
                    .replace(File.separatorChar, '.')
                    .replaceAll("\\.class$", "");
            try {
                Class<?> clase = Class.forName(nombre, false,
                        ElListadoCompilaTest.class.getClassLoader());
                if (clase.isAnnotationPresent(Entity.class)) {
                    acumulado.add(clase);
                }
            } catch (Throwable ignorada) {
                // Clases que no cargan sin su contexto (configuraciones de Spring,
                // por ejemplo) no son entidades y no interesan aquí.
            }
        }
    }

    /**
     * Compila la consulta; si el JPQL no es válido, Hibernate lanza aquí.
     *
     * <p>Se traduce con el analizador de HQL directamente, sin abrir sesión ni
     * transacción: eso pediría una conexión, y el objetivo es justamente poder
     * comprobar la consulta sin base de datos. La traducción resuelve los alias
     * y cada ruta de propiedad contra el modelo, que es donde están los errores
     * que se buscan.</p>
     */
    private static void compilar(String jpql) {
        fabrica.getQueryEngine().getHqlTranslator().translate(jpql, Object.class);
    }

    private static CriteriosMuestra criterios(int mascara) {
        return CriteriosMuestra.de(
                7L,
                (mascara & 1) != 0,
                (mascara & 2) != 0,
                (mascara & 4) != 0 ? "heces" : null,
                (mascara & 8) != 0 ? LocalDate.of(2026, 1, 1) : null,
                (mascara & 16) != 0 ? LocalDate.of(2026, 12, 31) : null,
                (mascara & 32) != 0 ? List.of("Heces") : null,
                (mascara & 64) != 0 ? "F" : null,
                (mascara & 128) != 0 ? "1" : null,
                (mascara & 256) != 0 ? "500" : null);
    }

    @Test
    @DisplayName("El modelo de entidades se levantó: sin él esta prueba no probaría nada")
    void elModeloExiste() {
        assertFalse(fabrica.getMetamodel().getEntities().isEmpty());
    }

    @Test
    @DisplayName("El traductor rechaza lo que está mal: si no, aprobaría cualquier cosa")
    void elTraductorTieneDientes() {
        // Sin esta comprobación, un arnés mal montado —sin entidades cargadas o
        // traduciendo en balde— daría las demás pruebas por buenas sin mirar nada.
        assertThrows(Exception.class,
                () -> compilar("SELECT m FROM Muestra m WHERE m.fechaQueNoExiste > :x"),
                "una propiedad inventada tiene que fallar");
        assertThrows(Exception.class,
                () -> compilar("SELECT m FROM Muestra m WHERE aliasInventado.id = 1"),
                "un alias que nadie declaró tiene que fallar");
        assertThrows(Exception.class,
                () -> compilar("SELECT m FROM EntidadInventada m"),
                "una entidad que no existe tiene que fallar");
    }

    @Test
    @DisplayName("Hibernate compila la consulta de la página en todas las combinaciones")
    void laPaginaCompila() {
        for (int mascara = 0; mascara < 512; mascara++) {
            CriteriosMuestra c = criterios(mascara);
            for (boolean conCursor : new boolean[]{false, true}) {
                for (boolean haciaAtras : new boolean[]{false, true}) {
                    String jpql = MuestraCursorRepositoryImpl.jpqlPagina(c, conCursor, haciaAtras);
                    assertDoesNotThrow(() -> compilar(jpql), jpql);
                }
            }
        }
    }

    @Test
    @DisplayName("Hibernate compila los dos conteos y la consulta de alícuotas")
    void losConteosCompilan() {
        for (int mascara = 0; mascara < 512; mascara++) {
            CriteriosMuestra c = criterios(mascara);
            assertDoesNotThrow(() -> compilar(MuestraCursorRepositoryImpl.jpqlConteo(c)));
            assertDoesNotThrow(() -> compilar(MuestraCursorRepositoryImpl.jpqlHuerfanas(c)));
            assertDoesNotThrow(() -> compilar(MuestraCursorRepositoryImpl.jpqlAlicuotas(c)));
        }
    }
}
