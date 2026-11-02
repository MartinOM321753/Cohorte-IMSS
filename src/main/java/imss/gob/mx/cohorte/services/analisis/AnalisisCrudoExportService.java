package imss.gob.mx.cohorte.services.analisis;

import imss.gob.mx.cohorte.services.analisis.AnalisisExportService.Descarga;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Exportación cruda (nivel participante) para el permiso
 * {@code ANALISIS_EXPORTA_CRUDO}. No aplica supresión k-anónima: baja una fila
 * por resultado. Formatos CSV (UTF-8 con BOM) y XLSX (Apache POI en modo
 * streaming, SXSSF, para no cargar todo en memoria con exportaciones grandes).
 */
@Service
public class AnalisisCrudoExportService {

    private static final String[] CABECERA = {
            "folio", "institucion", "sexo", "edad", "fecha", "metrica", "unidad", "valor", "valor_texto"
    };

    public Descarga aCsv(String metrica, String unidad, List<FilaCruda> filas, String separador) {
        String sep = ";".equals(separador) ? ";" : ",";
        StringBuilder sb = new StringBuilder("﻿");
        sb.append(String.join(sep, CABECERA)).append("\r\n");
        String u = unidad != null ? unidad : "";
        for (FilaCruda f : filas) {
            sb.append(esc(f.folio(), sep)).append(sep)
              .append(esc(f.institucion(), sep)).append(sep)
              .append(esc(f.sexo(), sep)).append(sep)
              .append(f.edad() != null ? f.edad().toString() : "").append(sep)
              .append(esc(f.fecha(), sep)).append(sep)
              .append(esc(metrica, sep)).append(sep)
              .append(esc(u, sep)).append(sep)
              .append(f.valorNumerico() != null ? f.valorNumerico().toString() : "").append(sep)
              .append(esc(f.valorTexto(), sep))
              .append("\r\n");
        }
        return new Descarga(nombre("csv"), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    public Descarga aXlsx(String metrica, String unidad, List<FilaCruda> filas) {
        String u = unidad != null ? unidad : "";
        try (SXSSFWorkbook wb = new SXSSFWorkbook(200); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            SXSSFSheet hoja = wb.createSheet("Análisis");
            Row cab = hoja.createRow(0);
            for (int i = 0; i < CABECERA.length; i++) cab.createCell(i).setCellValue(CABECERA[i]);

            int r = 1;
            for (FilaCruda f : filas) {
                Row row = hoja.createRow(r++);
                row.createCell(0).setCellValue(nvl(f.folio()));
                row.createCell(1).setCellValue(nvl(f.institucion()));
                row.createCell(2).setCellValue(nvl(f.sexo()));
                Cell edad = row.createCell(3);
                if (f.edad() != null) edad.setCellValue(f.edad());
                row.createCell(4).setCellValue(nvl(f.fecha()));
                row.createCell(5).setCellValue(nvl(metrica));
                row.createCell(6).setCellValue(u);
                Cell valor = row.createCell(7);
                if (f.valorNumerico() != null) valor.setCellValue(f.valorNumerico());
                row.createCell(8).setCellValue(nvl(f.valorTexto()));
            }
            wb.write(out);
            wb.dispose();
            return new Descarga(nombre("xlsx"), out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el archivo XLSX de análisis", e);
        }
    }

    private String nombre(String ext) {
        return "analisis_crudo_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;
    }

    private String nvl(String s) {
        return s != null ? s : "";
    }

    private String esc(String v, String sep) {
        if (v == null) return "";
        boolean necesita = v.contains(sep) || v.contains("\"") || v.contains("\n") || v.contains("\r");
        return necesita ? "\"" + v.replace("\"", "\"\"") + "\"" : v;
    }
}
