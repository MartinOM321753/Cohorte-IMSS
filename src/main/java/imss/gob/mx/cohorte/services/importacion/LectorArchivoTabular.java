package imss.gob.mx.cohorte.services.importacion;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Abre un CSV o un XLSX y devuelve su contenido como texto.
 *
 * <p>No sabe nada de estudios, examenes, alias ni folios: recibe bytes y entrega
 * una tabla. Es a proposito, para que la misma pieza sirva a los dos importadores
 * y exista un solo sitio donde endurecer la lectura. Duplicarla significaria dos
 * sitios donde equivocarse en seguridad, y el que se quedara atras no lo notaria
 * nadie hasta que llegara el archivo equivocado.</p>
 *
 * <h3>De que protege</h3>
 * <ul>
 *   <li><b>Bomba ZIP.</b> Un .xlsx es un ZIP con XML dentro. Con la configuracion
 *       por omision de POI, un archivo de un megabyte puede expandirse hasta
 *       agotar la memoria del proceso.</li>
 *   <li><b>Archivo disfrazado.</b> La extension la pone quien sube el archivo; lo
 *       que manda es el contenido.</li>
 *   <li><b>Volumen.</b> Topes de bytes, filas, columnas y tamano de celda.</li>
 *   <li><b>Formulas.</b> Se rechazan: Excel guarda el ultimo resultado calculado,
 *       que puede no corresponder a los datos actuales si el archivo se edito sin
 *       recalcular, y no hay forma de saberlo desde fuera. Un resultado clinico
 *       que quiza pertenece a otros datos es peor que no tener resultado.</li>
 * </ul>
 */
@Service
public class LectorArchivoTabular {

    /** Firma de un ZIP; todo .xlsx empieza asi. */
    private static final byte[] FIRMA_ZIP = {0x50, 0x4B, 0x03, 0x04};

    private static final char BOM = '﻿';

    public TablaLeida leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("No se recibio ningun archivo.");
        }
        if (archivo.getSize() > LimitesArchivo.MAX_BYTES) {
            throw new ArchivoInvalidoException(
                    "El archivo pesa mas de " + (LimitesArchivo.MAX_BYTES / 1024 / 1024)
                            + " MB. Exporta solo las filas que necesitas cargar.");
        }

        byte[] contenido;
        try {
            contenido = archivo.getBytes();
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo enviado.");
        }

        String nombre = archivo.getOriginalFilename() == null
                ? "" : archivo.getOriginalFilename().toLowerCase();

        // El contenido manda sobre la extension: quien sube el archivo controla el
        // nombre, no lo que hay dentro.
        if (tieneFirmaZip(contenido)) {
            if (!nombre.endsWith(".xlsx")) {
                throw new ArchivoInvalidoException(
                        "El archivo parece un libro de Excel pero no tiene extension .xlsx. "
                                + "Vuelve a exportarlo o renombralo correctamente.");
            }
            return leerXlsx(contenido);
        }

        if (nombre.endsWith(".xlsx")) {
            throw new ArchivoInvalidoException("El archivo dice ser .xlsx pero su contenido no lo es.");
        }
        if (nombre.endsWith(".xls")) {
            throw new ArchivoInvalidoException(
                    "El formato .xls antiguo no esta soportado. Guarda el archivo como .xlsx o .csv.");
        }

        return leerCsv(contenido);
    }

    private boolean tieneFirmaZip(byte[] contenido) {
        if (contenido.length < FIRMA_ZIP.length) return false;
        for (int i = 0; i < FIRMA_ZIP.length; i++) {
            if (contenido[i] != FIRMA_ZIP[i]) return false;
        }
        return true;
    }

    // ── CSV ──────────────────────────────────────────────────────────────────

    private TablaLeida leerCsv(byte[] contenido) {
        List<String> encabezados;
        List<List<String>> filas = new ArrayList<>();
        List<Integer> numeros = new ArrayList<>();

        try (BufferedReader lector = new BufferedReader(
                new InputStreamReader(new ByteArrayInputStream(contenido), StandardCharsets.UTF_8))) {

            String cabecera = lector.readLine();
            if (cabecera == null || cabecera.isBlank()) {
                throw new ArchivoInvalidoException("El archivo esta vacio o no tiene fila de encabezados.");
            }
            // El BOM que anade Excel al guardar como CSV UTF-8 se pegaria al primer
            // encabezado y lo dejaria sin emparejar con ningun alias.
            if (cabecera.charAt(0) == BOM) cabecera = cabecera.substring(1);

            char separador = ParserCsv.detectarSeparador(cabecera);
            encabezados = limpiar(ParserCsv.partir(cabecera, separador));
            quitarColumnasVaciasFinales(encabezados);
            validarEncabezados(encabezados);

            String linea;
            int numeroLinea = 1;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.isBlank()) continue;

                if (filas.size() >= LimitesArchivo.MAX_FILAS) {
                    throw new ArchivoInvalidoException(
                            "El archivo tiene mas de " + LimitesArchivo.MAX_FILAS
                                    + " filas. Divide la carga en varios archivos.");
                }

                List<String> celdas = limpiar(ParserCsv.partir(linea, separador));
                for (int c = 0; c < celdas.size(); c++) {
                    verificarLargo(celdas.get(c), numeroLinea, c);
                }
                filas.add(ajustar(celdas, encabezados.size(), numeroLinea));
                numeros.add(numeroLinea);
            }
        } catch (ArchivoInvalidoException e) {
            throw e;
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo CSV.");
        }

        return new TablaLeida(encabezados, filas, numeros);
    }

    // ── XLSX ─────────────────────────────────────────────────────────────────

    private TablaLeida leerXlsx(byte[] contenido) {
        // Los topes de POI son estaticos y globales; se fijan en cada lectura para
        // no depender de que otro punto del sistema los haya dejado como estaban.
        ZipSecureFile.setMinInflateRatio(LimitesArchivo.RATIO_MINIMO_INFLADO);
        ZipSecureFile.setMaxEntrySize(LimitesArchivo.MAX_BYTES_ENTRADA_ZIP);

        List<String> encabezados;
        List<List<String>> filas = new ArrayList<>();
        List<Integer> numeros = new ArrayList<>();

        // XSSFWorkbook directamente y no WorkbookFactory: asi solo se acepta xlsx y
        // no se abre la puerta a otros formatos que POI sepa interpretar.
        try (Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            if (libro.getNumberOfSheets() == 0) {
                throw new ArchivoInvalidoException("El libro no tiene ninguna hoja.");
            }
            Sheet hoja = libro.getSheetAt(0);

            Row filaEncabezados = hoja.getRow(hoja.getFirstRowNum());
            if (filaEncabezados == null) {
                throw new ArchivoInvalidoException("La primera hoja no tiene fila de encabezados.");
            }

            encabezados = new ArrayList<>();
            for (int c = 0; c < filaEncabezados.getLastCellNum(); c++) {
                encabezados.add(valorDeCelda(filaEncabezados.getCell(c),
                        filaEncabezados.getRowNum() + 1, c));
            }
            encabezados = limpiar(encabezados);
            quitarColumnasVaciasFinales(encabezados);
            validarEncabezados(encabezados);

            for (int f = filaEncabezados.getRowNum() + 1; f <= hoja.getLastRowNum(); f++) {
                Row fila = hoja.getRow(f);
                if (fila == null) continue;

                List<String> celdas = new ArrayList<>();
                boolean vacia = true;
                for (int c = 0; c < encabezados.size(); c++) {
                    String valor = valorDeCelda(fila.getCell(c), f + 1, c);
                    if (!valor.isEmpty()) vacia = false;
                    celdas.add(valor);
                }
                if (vacia) continue;   // filas en blanco intercaladas

                if (filas.size() >= LimitesArchivo.MAX_FILAS) {
                    throw new ArchivoInvalidoException(
                            "El archivo tiene mas de " + LimitesArchivo.MAX_FILAS
                                    + " filas. Divide la carga en varios archivos.");
                }

                filas.add(celdas);
                numeros.add(f + 1);
            }
        } catch (ArchivoInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw new ArchivoInvalidoException(
                    "No se pudo abrir el libro de Excel. Comprueba que no este protegido con contrasena "
                            + "y que sea un .xlsx valido.");
        }

        return new TablaLeida(encabezados, filas, numeros);
    }

    /**
     * Texto de una celda. Las fechas salen en ISO para que el importador las
     * reconozca sin depender de la configuracion regional de quien exporto.
     */
    private String valorDeCelda(Cell celda, int numeroFila, int numeroColumna) {
        if (celda == null) return "";

        CellType tipo = celda.getCellType();
        if (tipo == CellType.FORMULA) {
            throw new ArchivoInvalidoException(
                    "La celda " + referencia(numeroFila, numeroColumna) + " contiene una formula. "
                            + "Excel guarda su ultimo resultado calculado, que puede no corresponder a los "
                            + "datos actuales. Copia esa columna y pegala como valor antes de subir el archivo.");
        }

        String valor = switch (tipo) {
            case STRING -> celda.getStringCellValue();
            case BOOLEAN -> String.valueOf(celda.getBooleanCellValue());
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(celda)) {
                    var f = celda.getLocalDateTimeCellValue();
                    yield f == null ? "" : f.toString();   // ISO-8601
                }
                double d = celda.getNumericCellValue();
                // Sin esto un folio 502 sale "502.0" y deja de emparejar.
                yield (d == Math.floor(d) && !Double.isInfinite(d))
                        ? String.valueOf((long) d)
                        : String.valueOf(d);
            }
            default -> "";
        };

        verificarLargo(valor, numeroFila, numeroColumna);
        return valor == null ? "" : valor.trim();
    }

    // ── Comunes ──────────────────────────────────────────────────────────────

    private void validarEncabezados(List<String> encabezados) {
        if (encabezados.isEmpty()) {
            throw new ArchivoInvalidoException("El archivo no tiene encabezados.");
        }
        if (encabezados.size() > LimitesArchivo.MAX_COLUMNAS) {
            throw new ArchivoInvalidoException(
                    "El archivo tiene mas de " + LimitesArchivo.MAX_COLUMNAS + " columnas.");
        }
    }

    /** Excel arrastra columnas vacias a la derecha de lo que se escribio. */
    private void quitarColumnasVaciasFinales(List<String> encabezados) {
        while (!encabezados.isEmpty() && encabezados.get(encabezados.size() - 1).isEmpty()) {
            encabezados.remove(encabezados.size() - 1);
        }
    }

    private List<String> limpiar(List<String> celdas) {
        List<String> salida = new ArrayList<>(celdas.size());
        for (String c : celdas) salida.add(c == null ? "" : c.trim());
        return salida;
    }

    /**
     * Iguala el numero de celdas al de encabezados.
     *
     * <p>Las filas cortas se rellenan y las largas se rechazan. Rellenar una corta
     * es seguro —faltan datos al final—, pero una fila con celdas de mas significa
     * que el separador se colo dentro de un valor sin entrecomillar, y ahi las
     * columnas ya no corresponden a lo que dice el encabezado.</p>
     */
    private List<String> ajustar(List<String> celdas, int esperadas, int numeroFila) {
        if (celdas.size() > esperadas) {
            throw new ArchivoInvalidoException(
                    "La fila " + numeroFila + " tiene " + celdas.size() + " valores y el encabezado declara "
                            + esperadas + " columnas. Suele ocurrir cuando un valor contiene el separador "
                            + "y no esta entre comillas.");
        }
        List<String> salida = new ArrayList<>(celdas);
        while (salida.size() < esperadas) salida.add("");
        return salida;
    }

    private void verificarLargo(String valor, int fila, int columna) {
        if (valor != null && valor.length() > LimitesArchivo.MAX_CARACTERES_CELDA) {
            throw new ArchivoInvalidoException(
                    "La celda " + referencia(fila, columna) + " tiene mas de "
                            + LimitesArchivo.MAX_CARACTERES_CELDA + " caracteres.");
        }
    }

    /** Referencia estilo hoja de calculo (B7), que es como el usuario la va a buscar. */
    private String referencia(int fila, int columna) {
        StringBuilder letra = new StringBuilder();
        int c = columna;
        do {
            letra.insert(0, (char) ('A' + (c % 26)));
            c = c / 26 - 1;
        } while (c >= 0);
        return letra + String.valueOf(fila);
    }
}
