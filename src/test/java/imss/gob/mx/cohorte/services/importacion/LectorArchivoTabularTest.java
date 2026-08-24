package imss.gob.mx.cohorte.services.importacion;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas del lector compartido.
 *
 * <p>Cubren sobre todo lo que llega mal desde el mundo real —BOM, separador
 * regional, comas dentro de comillas, columnas fantasma de Excel— y lo que llega
 * con mala intencion —extension mentirosa, formulas—. Que un archivo correcto se
 * lea bien es la parte facil.</p>
 */
class LectorArchivoTabularTest {

    private final LectorArchivoTabular lector = new LectorArchivoTabular();

    private MockMultipartFile csv(String nombre, String contenido) {
        return new MockMultipartFile("archivo", nombre, "text/csv",
                contenido.getBytes(StandardCharsets.UTF_8));
    }

    // ── CSV ──────────────────────────────────────────────────────────────────

    @Test
    void leeUnCsvNormal() {
        TablaLeida t = lector.leer(csv("datos.csv",
                "fecha,folio,peso\n2026-08-01,000502,72.5\n2026-08-02,000503,80"));

        assertEquals(java.util.List.of("fecha", "folio", "peso"), t.encabezados());
        assertEquals(2, t.totalFilas());
        assertEquals(java.util.List.of("2026-08-01", "000502", "72.5"), t.filas().get(0));
    }

    @Test
    void conservaLosCerosALaIzquierdaDelFolio() {
        // En CSV el folio es texto y debe llegar intacto; el estrago de Excel
        // ocurre antes, al guardar, y lo detecta el importador.
        TablaLeida t = lector.leer(csv("d.csv", "folio\n000502"));
        assertEquals("000502", t.filas().get(0).get(0));
    }

    @Test
    void quitaElBomQueAnadeExcel() {
        TablaLeida t = lector.leer(csv("d.csv", "﻿fecha,folio\n2026-08-01,000502"));
        // Sin quitarlo, el primer encabezado seria "﻿fecha" y no emparejaria
        // con ningun alias.
        assertEquals("fecha", t.encabezados().get(0));
    }

    @Test
    void reconoceElPuntoYComaDeExcelEnEspanol() {
        TablaLeida t = lector.leer(csv("d.csv", "fecha;folio;peso\n2026-08-01;000502;72,5"));
        assertEquals(3, t.encabezados().size());
        assertEquals("72,5", t.filas().get(0).get(2));  // la coma decimal la resuelve el importador
    }

    @Test
    void unaComaDentroDeComillasNoCorreLasColumnas() {
        TablaLeida t = lector.leer(csv("d.csv",
                "nombre,folio,sexo\n\"Gomez, Juan\",000502,M"));
        assertEquals(java.util.List.of("Gomez, Juan", "000502", "M"), t.filas().get(0));
    }

    @Test
    void unaFilaConDemasiadosValoresSeRechaza() {
        // Sin comillas, la coma del nombre produce un valor de mas: a partir de ahi
        // las columnas ya no significan lo que dice el encabezado.
        ArchivoInvalidoException e = assertThrows(ArchivoInvalidoException.class,
                () -> lector.leer(csv("d.csv", "nombre,folio,sexo\nGomez, Juan,000502,M")));
        assertTrue(e.getMessage().contains("fila 2"), e.getMessage());
    }

    @Test
    void lasFilasCortasSeRellenan() {
        TablaLeida t = lector.leer(csv("d.csv", "a,b,c\n1,2"));
        assertEquals(java.util.List.of("1", "2", ""), t.filas().get(0));
    }

    @Test
    void lasLineasEnBlancoNoCuentanComoFila() {
        TablaLeida t = lector.leer(csv("d.csv", "a\n1\n\n\n2"));
        assertEquals(2, t.totalFilas());
    }

    @Test
    void elNumeroDeFilaApuntaALaDelArchivo() {
        // El usuario busca el problema en su hoja de calculo, no en un indice base 0.
        TablaLeida t = lector.leer(csv("d.csv", "a\n1\n\n2"));
        assertEquals(java.util.List.of(2, 4), t.numerosDeFila());
    }

    @Test
    void unArchivoSinEncabezadosSeRechaza() {
        assertThrows(ArchivoInvalidoException.class, () -> lector.leer(csv("d.csv", "")));
    }

    // ── Tipo real del archivo ────────────────────────────────────────────────

    @Test
    void unCsvDisfrazadoDeXlsxSeRechaza() {
        // La extension la pone quien sube el archivo; manda el contenido.
        ArchivoInvalidoException e = assertThrows(ArchivoInvalidoException.class,
                () -> lector.leer(csv("datos.xlsx", "a,b\n1,2")));
        assertTrue(e.getMessage().contains("no lo es"), e.getMessage());
    }

    @Test
    void unXlsxDisfrazadoDeCsvSeRechaza() throws Exception {
        byte[] libro = libroSimple();
        MockMultipartFile f = new MockMultipartFile("archivo", "datos.csv", "text/csv", libro);
        ArchivoInvalidoException e = assertThrows(ArchivoInvalidoException.class, () -> lector.leer(f));
        assertTrue(e.getMessage().contains(".xlsx"), e.getMessage());
    }

    @Test
    void elFormatoXlsAntiguoSeRechazaConIndicacion() {
        ArchivoInvalidoException e = assertThrows(ArchivoInvalidoException.class,
                () -> lector.leer(csv("viejo.xls", "a,b\n1,2")));
        assertTrue(e.getMessage().contains(".xlsx"), e.getMessage());
    }

    @Test
    void unArchivoDemasiadoGrandeSeRechazaSinLeerlo() {
        byte[] enorme = new byte[(int) LimitesArchivo.MAX_BYTES + 1];
        MockMultipartFile f = new MockMultipartFile("archivo", "d.csv", "text/csv", enorme);
        assertThrows(ArchivoInvalidoException.class, () -> lector.leer(f));
    }

    @Test
    void demasiadasFilasSeRechazan() {
        StringBuilder sb = new StringBuilder("a\n");
        for (int i = 0; i < LimitesArchivo.MAX_FILAS + 5; i++) sb.append(i).append('\n');
        ArchivoInvalidoException e = assertThrows(ArchivoInvalidoException.class,
                () -> lector.leer(csv("d.csv", sb.toString())));
        assertTrue(e.getMessage().contains("filas"), e.getMessage());
    }

    @Test
    void unaCeldaDesmesuradaSeRechaza() {
        String larga = "x".repeat(LimitesArchivo.MAX_CARACTERES_CELDA + 1);
        assertThrows(ArchivoInvalidoException.class, () -> lector.leer(csv("d.csv", "a\n" + larga)));
    }

    // ── XLSX ─────────────────────────────────────────────────────────────────

    private byte[] libroSimple() throws Exception {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Datos");
            Row enc = hoja.createRow(0);
            enc.createCell(0).setCellValue("fecha");
            enc.createCell(1).setCellValue("folio");
            enc.createCell(2).setCellValue("peso");
            Row f1 = hoja.createRow(1);
            f1.createCell(0).setCellValue("2026-08-01");
            f1.createCell(1).setCellValue(502);        // numerico, como lo deja Excel
            f1.createCell(2).setCellValue(72.5);
            libro.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void leeUnXlsx() throws Exception {
        MockMultipartFile f = new MockMultipartFile("archivo", "d.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", libroSimple());
        TablaLeida t = lector.leer(f);

        assertEquals(java.util.List.of("fecha", "folio", "peso"), t.encabezados());
        assertEquals(1, t.totalFilas());
        // Un entero no debe salir como "502.0" o dejaria de emparejar con el folio.
        assertEquals("502", t.filas().get(0).get(1));
        assertEquals("72.5", t.filas().get(0).get(2));
    }

    @Test
    void unaCeldaConFormulaSeRechazaSenalandoDonde() throws Exception {
        byte[] libro;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Datos");
            Row enc = hoja.createRow(0);
            enc.createCell(0).setCellValue("a");
            enc.createCell(1).setCellValue("b");
            Row f1 = hoja.createRow(1);
            f1.createCell(0).setCellValue(2);
            f1.createCell(1).setCellFormula("A2*2");
            wb.write(out);
            libro = out.toByteArray();
        }
        MockMultipartFile f = new MockMultipartFile("archivo", "d.xlsx", "application/vnd.ms-excel", libro);

        ArchivoInvalidoException e = assertThrows(ArchivoInvalidoException.class, () -> lector.leer(f));
        assertTrue(e.getMessage().contains("B2"), "debe indicar la celda: " + e.getMessage());
        assertTrue(e.getMessage().contains("formula"), e.getMessage());
    }

    @Test
    void lasFilasVaciasIntercaladasSeSaltan() throws Exception {
        byte[] libro;
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Datos");
            hoja.createRow(0).createCell(0).setCellValue("a");
            hoja.createRow(1).createCell(0).setCellValue("1");
            hoja.createRow(2);                                     // fila vacia
            hoja.createRow(3).createCell(0).setCellValue("2");
            wb.write(out);
            libro = out.toByteArray();
        }
        MockMultipartFile f = new MockMultipartFile("archivo", "d.xlsx", "application/vnd.ms-excel", libro);
        TablaLeida t = lector.leer(f);

        assertEquals(2, t.totalFilas());
        assertEquals(java.util.List.of(2, 4), t.numerosDeFila());
    }
}
