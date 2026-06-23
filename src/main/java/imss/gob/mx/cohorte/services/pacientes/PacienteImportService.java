package imss.gob.mx.cohorte.services.pacientes;

import imss.gob.mx.cohorte.controllers.pacientes.dto.ImportResultDTO;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.persona.Persona;
import imss.gob.mx.cohorte.modules.persona.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PacienteImportService {

    private final PacienteRepository pacienteRepository;
    private final PersonaRepository personaRepository;
    private final FolioGeneratorService folioGeneratorService;

    private static final List<String> COLUMNAS_ESPERADAS = List.of(
            "folio", "nombre", "apellidoPaterno", "apellidoMaterno",
            "curp", "fechaNacimiento", "sexo", "telefono", "email"
    );

    @Transactional
    public ImportResultDTO importar(MultipartFile archivo, Institucion institucion) {
        String nombre = archivo.getOriginalFilename();
        if (nombre == null) nombre = "";

        List<Map<String, String>> filas;
        if (nombre.endsWith(".xlsx") || nombre.endsWith(".xls")) {
            filas = parsearExcel(archivo);
        } else {
            filas = parsearCsv(archivo);
        }

        List<ImportResultDTO.FilaError> errores = new ArrayList<>();
        int exitosos = 0;
        int duplicados = 0;

        for (int i = 0; i < filas.size(); i++) {
            Map<String, String> fila = filas.get(i);
            int numFila = i + 2; // +2 porque fila 1 es header
            String folioRaw = fila.getOrDefault("folio", "").trim();

            try {
                String nombreVal = fila.getOrDefault("nombre", "").trim();
                String apPaterno = fila.getOrDefault("apellidoPaterno", "").trim();

                if (nombreVal.isEmpty() || apPaterno.isEmpty()) {
                    errores.add(ImportResultDTO.FilaError.builder()
                            .fila(numFila).folio(folioRaw)
                            .motivo("Nombre y apellido paterno son obligatorios")
                            .build());
                    continue;
                }

                String folio;
                if (folioRaw.isEmpty()) {
                    folio = folioGeneratorService.generarFolio();
                } else {
                    folio = folioGeneratorService.normalizar(folioRaw);
                    if (pacienteRepository.existsByFolio(folio)) {
                        duplicados++;
                        errores.add(ImportResultDTO.FilaError.builder()
                                .fila(numFila).folio(folio)
                                .motivo("El folio ya existe")
                                .build());
                        continue;
                    }
                }

                String curp = fila.getOrDefault("curp", "").trim().toUpperCase();
                if (!curp.isEmpty() && personaRepository.existsByCurp(curp)) {
                    duplicados++;
                    errores.add(ImportResultDTO.FilaError.builder()
                            .fila(numFila).folio(folio)
                            .motivo("El CURP '" + curp + "' ya existe")
                            .build());
                    continue;
                }

                String emailVal = fila.getOrDefault("email", "").trim();
                if (!emailVal.isEmpty() && personaRepository.findByEmail(emailVal).isPresent()) {
                    duplicados++;
                    errores.add(ImportResultDTO.FilaError.builder()
                            .fila(numFila).folio(folio)
                            .motivo("El email '" + emailVal + "' ya existe")
                            .build());
                    continue;
                }

                String telefonoVal = fila.getOrDefault("telefono", "").trim();
                if (!telefonoVal.isEmpty() && personaRepository.findByTelefono(telefonoVal).isPresent()) {
                    duplicados++;
                    errores.add(ImportResultDTO.FilaError.builder()
                            .fila(numFila).folio(folio)
                            .motivo("El teléfono '" + telefonoVal + "' ya existe")
                            .build());
                    continue;
                }

                Persona persona = new Persona();
                persona.setNombre(nombreVal);
                persona.setApellidoPaterno(apPaterno);
                persona.setApellidoMaterno(emptyToNull(fila.getOrDefault("apellidoMaterno", "")));
                persona.setCurp(curp.isEmpty() ? null : curp);
                persona.setTelefono(telefonoVal.isEmpty() ? null : telefonoVal);
                persona.setEmail(emailVal.isEmpty() ? null : emailVal);

                String fechaNacStr = fila.getOrDefault("fechaNacimiento", "").trim();
                if (!fechaNacStr.isEmpty()) {
                    persona.setFechaNacimiento(parsearFecha(fechaNacStr));
                }

                String sexoStr = fila.getOrDefault("sexo", "").trim().toUpperCase();
                if ("M".equals(sexoStr) || "F".equals(sexoStr)) {
                    persona.setSexo(Persona.Sexo.valueOf(sexoStr));
                }

                persona.setFechaRegistro(LocalDateTime.now());
                persona.setFechaActualizacion(LocalDateTime.now());
                persona = personaRepository.save(persona);

                Paciente paciente = new Paciente();
                paciente.setFolio(folio);
                paciente.setPersona(persona);
                paciente.setInstitucion(institucion);
                paciente.setActivo(true);
                paciente.setFechaRegistro(LocalDateTime.now());
                paciente.setFechaActualizacion(LocalDateTime.now());
                paciente.setUuid(UUID.randomUUID().toString());
                pacienteRepository.save(paciente);

                exitosos++;

            } catch (Exception e) {
                errores.add(ImportResultDTO.FilaError.builder()
                        .fila(numFila).folio(folioRaw)
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

    private List<Map<String, String>> parsearCsv(MultipartFile archivo) {
        List<Map<String, String>> filas = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(archivo.getInputStream(), StandardCharsets.UTF_8))) {

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

    private List<Map<String, String>> parsearExcel(MultipartFile archivo) {
        List<Map<String, String>> filas = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(archivo.getInputStream())) {
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

    private LocalDate parsearFecha(String valor) {
        List<DateTimeFormatter> formatos = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,           // yyyy-MM-dd
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("d/M/yyyy"),
                DateTimeFormatter.ofPattern("MM/dd/yyyy")
        );
        for (DateTimeFormatter fmt : formatos) {
            try {
                return LocalDate.parse(valor, fmt);
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("Formato de fecha no reconocido: " + valor);
    }

    private String emptyToNull(String valor) {
        if (valor == null) return null;
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public List<String> getColumnasEsperadas() {
        return COLUMNAS_ESPERADAS;
    }
}
