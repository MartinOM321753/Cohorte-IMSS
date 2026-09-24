package imss.gob.mx.cohorte.services.importacion;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La plantilla que se descarga desde la pantalla.
 *
 * <p>Se genera en vez de guardarse como archivo porque un .xlsx es un ZIP y no
 * sobrevive a que alguien lo trate como texto: el filtrado de recursos de Maven,
 * la copia de recursos del IDE, o Git con {@code core.autocrlf} y sin
 * {@code .gitattributes}. Cuando eso pasa, Excel ofrece «recuperar el máximo de
 * contenido posible» y no aparece nada.</p>
 *
 * <p>La prueba que de verdad importa es la de ida y vuelta: que el archivo que se
 * entrega lo pueda leer el mismo importador que va a recibirlo de vuelta lleno.
 * Sin ella, la plantilla y {@link ColumnaMuestra} pueden separarse en silencio —
 * una columna nueva en el importador que la plantilla no trae, o al revés— y eso
 * no se nota hasta que alguien sube un archivo y no entiende por qué falla.</p>
 */
class PlantillaCargaMuestrasTest {

    private PlantillaCargaMuestras plantilla;
    private byte[] libro;

    @BeforeEach
    void preparar() {
        plantilla = new PlantillaCargaMuestras();
        libro = plantilla.generar();
    }

    @Test
    @DisplayName("Lo que se entrega es un ZIP íntegro, que es lo que un .xlsx tiene que ser")
    void esUnZipIntegro() {
        assertNotNull(libro);
        assertTrue(libro.length > 0, "la plantilla no puede venir vacía");
        // Firma local de ZIP. Si aquí hay otra cosa, alguien lo convirtió a texto
        // por el camino y Excel ya no lo va a abrir.
        assertEquals(0x50, libro[0] & 0xFF, "falta la firma PK");
        assertEquals(0x4B, libro[1] & 0xFF, "falta la firma PK");

        List<String> entradas = new ArrayList<>();
        assertDoesNotThrow(() -> {
            try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(libro))) {
                for (var e = zip.getNextEntry(); e != null; e = zip.getNextEntry()) {
                    entradas.add(e.getName());
                    zip.readAllBytes();   // recorrerlo entero delata un ZIP truncado
                }
            }
        }, "el ZIP no se pudo recorrer completo");
        assertFalse(entradas.isEmpty());
    }

    @Test
    @DisplayName("Trae las dos hojas: la de llenar y las instrucciones")
    void traeLasDosHojas() throws Exception {
        try (Workbook w = WorkbookFactory.create(new ByteArrayInputStream(libro))) {
            assertNotNull(w.getSheet("LEEME"), "faltan las instrucciones");
            assertNotNull(w.getSheet("MUESTRAS"), "falta la hoja de datos");
            assertEquals(1, w.getSheet("MUESTRAS").getLastRowNum() + 1,
                    "la plantilla va vacía: solo el encabezado");
        }
    }

    /**
     * El lector toma la PRIMERA hoja del libro. Con las instrucciones delante,
     * subir la plantilla llena hacía que el importador leyera el instructivo y
     * se quejara de que faltaban todas las columnas.
     */
    @Test
    @DisplayName("La hoja de datos va primero, que es la que el lector abre")
    void laHojaDeDatosVaPrimero() throws Exception {
        try (Workbook w = WorkbookFactory.create(new ByteArrayInputStream(libro))) {
            assertEquals("MUESTRAS", w.getSheetAt(0).getSheetName());
        }
    }

    /**
     * La de ida y vuelta. Si alguien añade una columna a {@link ColumnaMuestra} y
     * olvida la plantilla, esto falla aquí y no en manos del usuario.
     */
    @Test
    @DisplayName("El importador reconoce todas las columnas de su propia plantilla")
    void idaYVuelta() {
        var archivo = new MockMultipartFile("archivo", PlantillaCargaMuestras.NOMBRE_ARCHIVO,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", libro);

        TablaLeida tabla = new LectorArchivoTabular().leer(archivo);
        var emparejado = ColumnaMuestra.emparejar(tabla.encabezados());

        assertTrue(emparejado.sinProblemas(), () -> "la plantilla no encaja: " + emparejado.problemas());
        for (ColumnaMuestra c : ColumnaMuestra.values()) {
            assertTrue(emparejado.trae(c), "la plantilla no trae la columna " + c);
        }
    }

    /**
     * La plantilla lleva el nombre y el consecutivo para que una persona pueda
     * leer la hoja. Si el importador los avisara como columnas desconocidas
     * estaría avisando de nuestras propias columnas, y el usuario aprendería a
     * ignorar los avisos.
     */
    @Test
    @DisplayName("Las columnas informativas de la plantilla no se avisan como desconocidas")
    void lasInformativasNoSeAvisan() {
        var archivo = new MockMultipartFile("archivo", PlantillaCargaMuestras.NOMBRE_ARCHIVO,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", libro);

        TablaLeida tabla = new LectorArchivoTabular().leer(archivo);
        var emparejado = ColumnaMuestra.emparejar(tabla.encabezados());

        assertTrue(emparejado.ignoradas().isEmpty(),
                () -> "no debería avisar de: " + emparejado.ignoradas());
        for (String informativa : ColumnaMuestra.encabezadosInformativos()) {
            assertTrue(tabla.encabezados().contains(informativa),
                    "la plantilla debería traer la columna informativa " + informativa);
        }
    }

    @Test
    @DisplayName("Las instrucciones describen todas las columnas, sin dejarse ninguna")
    void lasInstruccionesEstanCompletas() throws Exception {
        try (Workbook w = WorkbookFactory.create(new ByteArrayInputStream(libro))) {
            Sheet leeme = w.getSheet("LEEME");
            StringBuilder texto = new StringBuilder();
            for (var fila : leeme) {
                for (var celda : fila) {
                    if (celda.getCellType() == org.apache.poi.ss.usermodel.CellType.STRING) {
                        texto.append(celda.getStringCellValue()).append('\n');
                    }
                }
            }
            String todo = texto.toString();
            for (ColumnaMuestra c : ColumnaMuestra.values()) {
                assertTrue(todo.contains(c.tituloPreferido()),
                        "las instrucciones no mencionan " + c.tituloPreferido());
                assertTrue(todo.contains(c.descripcion()),
                        "las instrucciones no explican " + c.tituloPreferido());
            }
        }
    }

    @Test
    @DisplayName("Generarla dos veces da la misma plantilla")
    void esReproducible() throws Exception {
        // Se comparan los encabezados y no los bytes: un .xlsx lleva dentro la
        // fecha de creación, así que dos libros idénticos difieren en algún byte.
        // Lo que tiene que ser estable es el contrato, no el envoltorio.
        byte[] otra = plantilla.generar();
        assertEquals(encabezadosDe(libro), encabezadosDe(otra));
    }

    private static List<String> encabezadosDe(byte[] xlsx) throws Exception {
        try (Workbook w = WorkbookFactory.create(new ByteArrayInputStream(xlsx))) {
            List<String> nombres = new ArrayList<>();
            for (var celda : w.getSheetAt(0).getRow(0)) {
                nombres.add(celda.getStringCellValue());
            }
            return nombres;
        }
    }
}
