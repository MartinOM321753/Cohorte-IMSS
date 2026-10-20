package imss.gob.mx.cohorte.services.importacion;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * La plantilla de carga por protocolo.
 *
 * <p>Se vigila lo que el importador necesita para leerla: que la hoja de datos vaya
 * primero (el importador lee todas las hojas y le pide a cada una su protocolo), que los
 * encabezados sean los que acepta {@code CargaMasivaProtocoloService.colIndices}, y que
 * el folio quede como texto para no perder los ceros a la izquierda.</p>
 */
class PlantillaCargaProtocoloTest {

    private byte[] libro;

    @BeforeEach
    void preparar() {
        libro = new PlantillaCargaProtocolo().generar();
    }

    @Test
    @DisplayName("La hoja de datos va primero y la de instrucciones después")
    void hojaDeDatosPrimero() throws IOException {
        try (Workbook wb = abrir()) {
            assertEquals("ALICUOTAS", wb.getSheetName(0));
            assertEquals("LEEME", wb.getSheetName(1));
        }
    }

    @Test
    @DisplayName("Los encabezados son los que lee el importador, en el orden de la plantilla")
    void encabezadosAceptados() throws IOException {
        try (Workbook wb = abrir()) {
            Row cabecera = wb.getSheet("ALICUOTAS").getRow(0);
            List<String> encabezados = new ArrayList<>();
            for (int i = 0; i < PlantillaCargaProtocolo.COLUMNAS.size(); i++) {
                encabezados.add(cabecera.getCell(i).getStringCellValue());
            }
            assertEquals(List.of("folio", "numero_alicuota", "volumen", "unidad",
                    "fecha", "codigo_caja", "posicion", "observaciones"), encabezados);
        }
    }

    @Test
    @DisplayName("El folio va como texto para que Excel no le quite los ceros")
    void folioComoTexto() throws IOException {
        try (Workbook wb = abrir()) {
            Sheet datos = wb.getSheet("ALICUOTAS");
            short formatoTexto = wb.createDataFormat().getFormat("@");
            assertEquals(formatoTexto, datos.getColumnStyle(0).getDataFormat());
        }
    }

    @Test
    @DisplayName("Las instrucciones nombran las dos columnas obligatorias")
    void instruccionesNombranObligatorias() throws IOException {
        try (Workbook wb = abrir()) {
            Sheet leeme = wb.getSheet("LEEME");
            String todo = "";
            for (Row r : leeme) {
                if (r.getCell(0) != null) todo += r.getCell(0).getStringCellValue() + "\n";
            }
            assertTrue(todo.contains("folio  (obligatoria)"));
            assertTrue(todo.contains("volumen  (obligatoria)"));
        }
    }

    private Workbook abrir() throws IOException {
        return WorkbookFactory.create(new ByteArrayInputStream(libro));
    }
}
