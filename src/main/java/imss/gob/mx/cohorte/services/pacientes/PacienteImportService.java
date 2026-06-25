package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.controllers.pacientes.dto.ImportResultDTO;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PacienteImportService {

    private final PacienteImportRowService pacienteImportRowService;

    private static final List<String> COLUMNAS_ESPERADAS = List.of(
            "folio", "nombre", "segundoNombre", "tercerNombre", "apellidoPaterno", "apellidoMaterno",
            "curp", "fechaNacimiento", "sexo", "telefono", "email"
    );

    /**
     * Recibe el contenido del archivo como bytes (no MultipartFile) porque esto
     * se invoca desde un metodo @Async, despues de que la peticion HTTP original
     * ya terminó — el MultipartFile y su stream/archivo temporal ya no son
     * validos para entonces.
     */
    public ImportResultDTO importar(byte[] contenido, String nombreArchivo, Institucion institucion) {
        String nombre = nombreArchivo != null ? nombreArchivo : "";

        List<Map<String, String>> filas;
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            filas = parsearExcel(contenido);
        } else {
            filas = parsearCsv(contenido);
        }

        List<ImportResultDTO.FilaError> errores = new ArrayList<>();
        int exitosos = 0;
        int duplicados = 0;

        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numFila = i + 2; // +2 porque fila 1 es header

            try {
                // Cada fila se procesa en su propia transaccion (ver
                // PacienteImportRowService) para que generarFolio() vea los
                // folios ya confirmados de filas previas del mismo lote, y
                // para que un error en una fila no corrompa la sesion de
                // Hibernate de las demas.
                PacienteImportRowService.Resultado resultado = pacienteImportRowService.guardarFila(fila, institucion);
                switch (resultado.estado()) {
                    case EXITOSO -> exitosos++;
                    case DUPLICADO -> {
                        duplicados++;
                        errores.add(ImportResultDTO.FilaError.builder()
                                .fila(numFila).folio(resultado.folio())
                                .motivo(resultado.motivo())
                                .build());
                    }
                    case ERROR -> errores.add(ImportResultDTO.FilaError.builder()
                            .fila(numFila).folio(resultado.folio())
                            .motivo(resultado.motivo())
                            .build());
                }
            } catch (Exception e) {
                errores.add(ImportResultDTO.FilaError.builder()
                        .fila(numFila).folio(fila.getOrDefault("folio", "").trim())
                        .motivo(e.getMessage())
                        .build());
            }
        }

        return ImportResultDTO.builder()
                .totalFilas(filas.size())
                .exitosos(exitosos)
                .errores(errores.size())
                .duplicados(duplicados)
                .detalleErrores(errores)
                .build();
    }

    private List<Map<String, String>> parsearCsv(byte[] contenido) {
        List<Map<String, String>> filas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(contenido), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null) return filas;

            // BOM UTF-8
            if (headerLine.startsWith("﻿")) {
                headerLine = headerLine.substring(1);
            }

            String[] headers = headerLine.split(",");
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim().replace("\"", "");
            }

            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] valores = line.split(",", -1);
                Map<String, String> fila = new LinkedHashMap<>();
                for (int i = 0; i < headers.length && i < valores.length; i++) {
                    fila.put(headers[i], valores[i].trim().replace("\"", ""));
                }
                filas.add(fila);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el archivo CSV: " + e.getMessage(), e);
        }
        return filas;
    }

    private List<Map<String, String>> parsearExcel(byte[] contenido) {
        List<Map<String, String>> filas = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(contenido))) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) return filas;

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) return filas;

            List<String> headers = new ArrayList<>();
            for (Cell cell : headerRow) {
                headers.add(getCellValueAsString(cell).trim());
            }

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                Map<String, String> fila = new LinkedHashMap<>();
                boolean vacia = true;
                for (int j = 0; j < headers.size(); j++) {
                    Cell cell = row.getCell(j);
                    String valor = cell != null ? getCellValueAsString(cell).trim() : "";
                    if (!valor.isEmpty()) vacia = false;
                    fila.put(headers.get(j), valor);
                }
                if (!vacia) filas.add(fila);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el archivo Excel: " + e.getMessage(), e);
        }
        return filas;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDateTime dateTime = cell.getLocalDateTimeCellValue();
                    if (dateTime == null) return "";
                    return dateTime.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE);
                }
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val) && !Double.isInfinite(val)) {
                    return String.valueOf((long) val);
                }
                return String.valueOf(val);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                try {
                    return cell.getStringCellValue();
                } catch (Exception e) {
                    return String.valueOf(cell.getNumericCellValue());
                }
            default:
                return "";
        }
    }

    public List<String> getColumnasEsperadas() {
        return COLUMNAS_ESPERADAS;
    }
}
