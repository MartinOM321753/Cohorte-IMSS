package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.services.importacion.EmparejadorColumnas.Destino;
import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Comment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La plantilla de resultados que se descarga desde la carga masiva de estudios y
 * de exámenes.
 *
 * <p>El generador recibe la configuración de horario y el contexto de institución;
 * aquí van en null a propósito, para probar el formato del archivo sin levantar el
 * contexto de Spring. La nota de la fecha cae entonces a su texto sin horario, que
 * es justo el camino que hay que verificar que no rompe nada.</p>
 */
class PlantillaCargaResultadosTest {

    /** Un destino sin alias (se titula por su nombre) y otro con dos (dos versiones). */
    private static final List<PlantillaCargaResultados.Columna> COLUMNAS = List.of(
            new PlantillaCargaResultados.Columna("Glucosa", List.of()),
            new PlantillaCargaResultados.Columna("Colesterol", List.of("COL", "Chol")));

    private final PlantillaCargaResultados plantilla = new PlantillaCargaResultados(null, null);

    @Test
    @DisplayName("Cuenta una versión por cada alias del destino que más tiene")
    void cuentaVersiones() {
        assertEquals(2, plantilla.numeroDeVersiones(COLUMNAS));
        assertEquals(1, plantilla.numeroDeVersiones(List.of(
                new PlantillaCargaResultados.Columna("Glucosa", List.of()))),
                "sin alias en ningún destino, una sola versión: la de los nombres");
    }

    @Test
    @DisplayName("Lo que se entrega es un ZIP íntegro con la hoja de datos primero")
    void esUnZipConDatosPrimero() throws Exception {
        byte[] libro = plantilla.generar("Química sanguínea", COLUMNAS, 1);
        assertEquals(0x50, libro[0] & 0xFF, "falta la firma PK");
        assertEquals(0x4B, libro[1] & 0xFF, "falta la firma PK");
        try (Workbook w = WorkbookFactory.create(new ByteArrayInputStream(libro))) {
            assertEquals("RESULTADOS", w.getSheetAt(0).getSheetName(),
                    "el lector abre la primera hoja: tiene que ser la de datos");
            assertNotNull(w.getSheet("LEEME"), "faltan las instrucciones");
        }
    }

    @Test
    @DisplayName("Los encabezados cambian con la versión, y el destino sin alias se queda en su nombre")
    void encabezadosPorVersion() throws Exception {
        assertEquals(List.of("folio", "fecha", "Glucosa", "COL"), encabezados(1));
        // Versión 2: Colesterol pasa a su segundo alias; Glucosa, sin alias, sigue
        // siendo su nombre (no cae a nada raro).
        assertEquals(List.of("folio", "fecha", "Glucosa", "Chol"), encabezados(2));
    }

    @Test
    @DisplayName("El importador reconoce los encabezados de su propia plantilla (ida y vuelta)")
    void idaYVuelta() throws Exception {
        List<String> enc = encabezados(1);

        // Los mismos destinos que el catálogo, como los ve el emparejador.
        List<Destino> destinos = new ArrayList<>();
        long id = 1;
        for (PlantillaCargaResultados.Columna c : COLUMNAS) {
            List<String> norm = c.aliasOrdenados().stream().map(NormalizadorAlias::normalizar).toList();
            destinos.add(new Destino(id++, c.nombre(), norm, c.aliasOrdenados()));
        }

        var emparejado = EmparejadorColumnas.emparejar(enc, destinos);
        assertTrue(emparejado.sinConflictos(),
                () -> "la plantilla no encaja: " + emparejado.problemas());
        assertTrue(emparejado.destinosSinColumna().isEmpty(),
                () -> "quedaron destinos sin columna: " + emparejado.destinosSinColumna());
    }

    @Test
    @DisplayName("Los encabezados llevan el verde del sistema")
    void encabezadosConColorDelSistema() throws Exception {
        byte[] libro = plantilla.generar("Química sanguínea", COLUMNAS, 1);
        try (Workbook w = WorkbookFactory.create(new ByteArrayInputStream(libro))) {
            Cell folio = w.getSheet("RESULTADOS").getRow(0).getCell(0);
            XSSFColor color = ((XSSFCellStyle) folio.getCellStyle()).getFillForegroundColorColor();
            assertNotNull(color, "el encabezado tiene que ir con relleno de color");
            byte[] rgb = color.getRGB();
            assertArrayEquals(new byte[]{(byte) 0x26, (byte) 0x45, (byte) 0x32}, rgb,
                    "el color no es el --primary del tema por defecto");
        }
    }

    @Test
    @DisplayName("La hoja de instrucciones lleva el logo del sistema")
    void llevaLogo() throws Exception {
        byte[] libro = plantilla.generar("Química sanguínea", COLUMNAS, 1);
        try (Workbook w = WorkbookFactory.create(new ByteArrayInputStream(libro))) {
            assertFalse(((XSSFWorkbook) w).getAllPictures().isEmpty(),
                    "no se incrustó el logo en la plantilla");
        }
    }

    @Test
    @DisplayName("La columna de la fecha lleva una nota con el formato hora:minutos")
    void notaEnLaFecha() throws Exception {
        byte[] libro = plantilla.generar("Química sanguínea", COLUMNAS, 1);
        try (Workbook w = WorkbookFactory.create(new ByteArrayInputStream(libro))) {
            Row cabecera = w.getSheet("RESULTADOS").getRow(0);
            Cell fecha = cabecera.getCell(1);
            assertEquals("fecha", fecha.getStringCellValue());
            Comment nota = fecha.getCellComment();
            assertNotNull(nota, "la columna de la fecha tiene que llevar una nota");
            String texto = nota.getString().getString().toLowerCase();
            assertTrue(texto.contains("hora") && texto.contains("minutos"),
                    "la nota debe explicar cómo poner hora y minutos");
        }
    }

    @Test
    @DisplayName("Deja un ejemplo en target/ para inspección manual")
    void dejaEjemplo() throws Exception {
        Path salida = Path.of("target", "plantilla-resultados-ejemplo.xlsx");
        Files.write(salida, plantilla.generar("Química sanguínea", COLUMNAS, 1));
        assertTrue(Files.size(salida) > 0);
    }

    // ── Utilidades ─────────────────────────────────────────────────────────────

    private List<String> encabezados(int version) throws Exception {
        byte[] libro = plantilla.generar("Química sanguínea", COLUMNAS, version);
        try (Workbook w = WorkbookFactory.create(new ByteArrayInputStream(libro))) {
            List<String> nombres = new ArrayList<>();
            for (Cell celda : w.getSheet("RESULTADOS").getRow(0)) {
                nombres.add(celda.getStringCellValue());
            }
            return nombres;
        }
    }
}
