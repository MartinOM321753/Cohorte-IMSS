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

    // ── Lectura reproducible (COMO_SE_VE) ────────────────────────────────────
    //
    // El modo canonico convierte cada celda a una forma inequivoca para poder
    // parsearla; este modo hace lo contrario y conserva el formato, porque quien
    // lo usa va a imprimir la celda, no a interpretarla. Las pruebas de abajo
    // fijan esa diferencia caso por caso: si algun dia los dos modos devuelven lo
    // mismo, es que uno de los dos dejo de servir para lo suyo.

    /** Libro con una celda de cada tipo que Excel formatea distinto de su valor crudo. */
    private byte[] libroConFormatos() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Datos");
            Row enc = hoja.createRow(0);
            enc.createCell(0).setCellValue("fecha");
            enc.createCell(1).setCellValue("avance");
            enc.createCell(2).setCellValue("folio");

            var formatos = wb.createDataFormat();

            var estiloFecha = wb.createCellStyle();
            estiloFecha.setDataFormat(formatos.getFormat("dd/mm/yyyy"));
            var estiloPorcentaje = wb.createCellStyle();
            estiloPorcentaje.setDataFormat(formatos.getFormat("0.0%"));

            Row f1 = hoja.createRow(1);
            var fecha = f1.createCell(0);
            fecha.setCellValue(java.time.LocalDate.of(2026, 3, 12));
            fecha.setCellStyle(estiloFecha);

            var avance = f1.createCell(1);
            avance.setCellValue(0.155);
            avance.setCellStyle(estiloPorcentaje);

            f1.createCell(2).setCellValue(502);

            wb.write(out);
            return out.toByteArray();
        }
    }

    private MockMultipartFile xlsx(byte[] libro) {
        return new MockMultipartFile("archivo", "d.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", libro);
    }

    @Test
    void laFechaSaleComoSeVeEnLaHojaYNoEnIso() throws Exception {
        byte[] libro = libroConFormatos();

        assertEquals("2026-03-12T00:00", lector.leer(xlsx(libro)).filas().get(0).get(0),
                "el modo canonico debe seguir dando ISO para poder parsearlo");
        assertEquals("12/03/2026", lector.leerComoSeVe(xlsx(libro)).tabla().filas().get(0).get(0),
                "en una etiqueta hay que imprimir lo que la persona ve en su hoja");
    }

    @Test
    void elPorcentajeConservaSuFormato() throws Exception {
        byte[] libro = libroConFormatos();

        // Es la diferencia mas traicionera de las dos lecturas: 0.155 y 15.5% son
        // el mismo dato, pero solo uno de los dos es el que hay que imprimir.
        assertEquals("0.155", lector.leer(xlsx(libro)).filas().get(0).get(1));
        assertEquals("15.5%", lector.leerComoSeVe(xlsx(libro)).tabla().filas().get(0).get(1));
    }

    @Test
    void unEnteroNoGanaDecimalesEnNingunModo() throws Exception {
        byte[] libro = libroConFormatos();

        assertEquals("502", lector.leer(xlsx(libro)).filas().get(0).get(2));
        assertEquals("502", lector.leerComoSeVe(xlsx(libro)).tabla().filas().get(0).get(2));
    }

    /** Libro con una formula ya calculada, como el que sale de guardar en Excel. */
    private byte[] libroConFormula() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet hoja = wb.createSheet("Datos");
            Row enc = hoja.createRow(0);
            enc.createCell(0).setCellValue("a");
            enc.createCell(1).setCellValue("b");
            Row f1 = hoja.createRow(1);
            f1.createCell(0).setCellValue(2);
            f1.createCell(1).setCellFormula("A2*2");
            // Sin esto la formula viaja sin resultado guardado, que no es como
            // llega un archivo que alguien guardo desde Excel.
            org.apache.poi.xssf.usermodel.XSSFFormulaEvaluator.evaluateAllFormulaCells(wb);
            wb.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void laFormulaNoRechazaElArchivoPeroSeReportaLaCelda() throws Exception {
        byte[] libro = libroConFormula();

        // Canonico: se rechaza, porque un resultado clinico que quiza pertenece a
        // otros datos es peor que no tener resultado.
        assertThrows(ArchivoInvalidoException.class, () -> lector.leer(xlsx(libro)));

        // Reproducible: se imprime lo que la hoja muestra, y se dice cual celda es.
        LectorArchivoTabular.TablaConAvisos leido = lector.leerComoSeVe(xlsx(libro));
        assertEquals("4", leido.tabla().filas().get(0).get(1));
        assertEquals(1, leido.celdasDerivadas().size());
        assertEquals("B2", leido.celdasDerivadas().get(0).referencia());
    }

    @Test
    void unCsvSeLeeIgualEnLosDosModos() {
        // Un CSV ya es texto literal: no hay formato que reproducir. Que los dos
        // modos coincidan aqui es lo correcto, y conviene fijarlo para que nadie
        // le invente un tratamiento distinto mas adelante.
        String contenido = "fecha,folio\n12/03/2026,000502";

        TablaLeida canonico = lector.leer(csv("d.csv", contenido));
        LectorArchivoTabular.TablaConAvisos comoSeVe = lector.leerComoSeVe(csv("d.csv", contenido));

        assertEquals(canonico.filas(), comoSeVe.tabla().filas());
        assertTrue(comoSeVe.celdasDerivadas().isEmpty());
    }
}
