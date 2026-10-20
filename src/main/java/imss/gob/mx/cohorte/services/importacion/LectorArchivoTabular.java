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
import java.util.Locale;

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
 *   <li><b>Formulas.</b> Se rechazan en modo {@link ModoLectura#CANONICO}: Excel
 *       guarda el ultimo resultado calculado, que puede no corresponder a los
 *       datos actuales si el archivo se edito sin recalcular, y no hay forma de
 *       saberlo desde fuera. Un resultado clinico que quiza pertenece a otros
 *       datos es peor que no tener resultado.</li>
 * </ul>
 */
@Service
public class LectorArchivoTabular {

    /** Firma de un ZIP; todo .xlsx empieza asi. */
    private static final byte[] FIRMA_ZIP = {0x50, 0x4B, 0x03, 0x04};

    private static final char BOM = '﻿';

    /**
     * Como se convierte cada celda a texto. Son dos politicas distintas y las dos
     * son correctas; lo que cambia es para que se va a usar el valor.
     */
    public enum ModoLectura {
        /**
         * Valor canonico, pensado para <b>interpretar</b>: fechas en ISO-8601 y
         * numeros sin el formato que Excel les aplica encima, para poder parsearlos
         * sin depender de la configuracion regional de quien exporto. Las formulas
         * se rechazan.
         */
        CANONICO,

        /**
         * Texto tal como se ve en la hoja, pensado para <b>reproducir</b>.
         *
         * <p>Existe porque hay usos en los que el requisito es literalmente que lo
         * que esta en la celda sea lo que sale: imprimir una etiqueta, por ejemplo.
         * Ahi el valor canonico es el equivocado — una fecha saldria como
         * {@code 2026-03-12T00:00} y un porcentaje que en pantalla dice 15.5% se
         * imprimiria como 0.155.</p>
         *
         * <p>Las formulas no rechazan el archivo: se lee su ultimo resultado
         * calculado y la celda se reporta como derivada, para poder avisarlo. La
         * cautela clinica que justifica el rechazo no aplica a un texto que el
         * usuario va a revisar en pantalla antes de imprimirlo.</p>
         */
        COMO_SE_VE
    }

    /**
     * Cuanto ancho y cuanto largo admite esta lectura.
     *
     * <p>Los topes son del formato que se espera, no del lector: doscientas columnas
     * son de sobra para el export de un instrumento medico y rechazan un archivo
     * equivocado antes de que llegue a interpretarse, mientras que el volcado de un
     * cuestionario escaneado trae legitimamente una columna por pregunta. Un solo
     * tope para los dos casos tendria que ser el alto, y entonces el caso comun
     * perderia la comprobacion que mas le sirve.</p>
     */
    public record Topes(int maxFilas, int maxColumnas) {
        public static Topes porOmision() {
            return new Topes(LimitesArchivo.MAX_FILAS, LimitesArchivo.MAX_COLUMNAS);
        }
    }

    /** Una hoja del libro, con su nombre tal como esta en la pestana. */
    public record HojaLeida(String nombre, TablaLeida tabla) {}

    /** Una celda cuyo valor venia de una formula, en modo {@link ModoLectura#COMO_SE_VE}. */
    public record CeldaDerivada(int fila, int columna, String referencia) {}

    /**
     * Lo que devuelve la lectura reproducible: la tabla y las celdas sobre las que
     * conviene avisar. Se mantiene aparte de {@link TablaLeida} para no cambiarle
     * la forma a lo que ya consumen los importadores.
     */
    public record TablaConAvisos(TablaLeida tabla, List<CeldaDerivada> celdasDerivadas) {}

    /** Lectura canonica: la que usan los importadores. Rechaza formulas. */
    public TablaLeida leer(MultipartFile archivo) {
        return leer(archivo, Topes.porOmision());
    }

    /** Lectura canonica con topes propios del formato que se espera. */
    public TablaLeida leer(MultipartFile archivo, Topes topes) {
        return leerInterno(archivo, ModoLectura.CANONICO, topes).tabla();
    }

    /** Lectura reproducible: el texto tal como se ve en Excel, con avisos. */
    public TablaConAvisos leerComoSeVe(MultipartFile archivo) {
        return leerInterno(archivo, ModoLectura.COMO_SE_VE, Topes.porOmision());
    }

    /**
     * Todas las hojas del libro, cada una con su nombre.
     *
     * <p>Hace falta porque hay formatos en los que el dato esta repartido en varias
     * pestanas y cual es cual lo dice el nombre, no la posicion: el volcado de un
     * cuestionario trae las respuestas en una hoja y las lecturas en desacuerdo en
     * otra. Leer solo la primera perderia la mitad del archivo, y leerla por indice
     * daria por buena una suposicion sobre el orden de las pestanas que nadie
     * garantiza.</p>
     *
     * <p>Una hoja que no se puede interpretar como tabla —sin encabezados, o vacia—
     * se omite en lugar de tumbar la lectura: un libro de Excel suele arrastrar
     * pestanas de trabajo que no son datos.</p>
     */
    public List<HojaLeida> leerHojas(MultipartFile archivo, Topes topes) {
        return leerHojas(contenidoDe(archivo), topes);
    }

    /**
     * Lo mismo, pero sobre bytes que ya se tienen.
     *
     * <p>Hace falta para lo que se lee fuera de la peticion que subio el archivo: una
     * carga que corre en segundo plano ya no tiene el {@link MultipartFile} —el
     * contenedor lo libera al contestar— asi que recibe el contenido y nada mas. El
     * tope de tamano se comprueba igual: aqui los bytes ya estan en memoria, pero lo
     * que cuesta de verdad es lo que se expande al interpretarlos.</p>
     */
    public List<HojaLeida> leerHojas(byte[] contenido, Topes topes) {
        if (contenido == null || contenido.length == 0) {
            throw new ArchivoInvalidoException("No se recibio ningun archivo.");
        }
        if (contenido.length > LimitesArchivo.MAX_BYTES) {
            throw new ArchivoInvalidoException(
                    "El archivo pesa mas de " + (LimitesArchivo.MAX_BYTES / 1024 / 1024)
                            + " MB. Exporta solo las filas que necesitas cargar.");
        }
        if (!tieneFirmaZip(contenido)) {
            // Un CSV es una sola tabla sin nombre; se entrega como hoja unica para
            // que quien consuma esto no tenga que distinguir el caso.
            return List.of(new HojaLeida("", leerCsv(contenido, topes)));
        }
        return leerTodasLasHojas(contenido, topes);
    }

    private TablaConAvisos leerInterno(MultipartFile archivo, ModoLectura modo, Topes topes) {
        byte[] contenido = contenidoDe(archivo);

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
            return leerXlsx(contenido, modo, topes);
        }

        if (nombre.endsWith(".xlsx")) {
            throw new ArchivoInvalidoException("El archivo dice ser .xlsx pero su contenido no lo es.");
        }
        if (nombre.endsWith(".xls")) {
            throw new ArchivoInvalidoException(
                    "El formato .xls antiguo no esta soportado. Guarda el archivo como .xlsx o .csv.");
        }

        // Un CSV ya es texto literal: no hay formato de celda que reproducir ni
        // formulas que resolver, asi que los dos modos leen exactamente igual.
        return new TablaConAvisos(leerCsv(contenido, topes), List.of());
    }

    /** Comprueba el tamano y entrega los bytes. Comun a todas las formas de leer. */
    private byte[] contenidoDe(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ArchivoInvalidoException("No se recibio ningun archivo.");
        }
        if (archivo.getSize() > LimitesArchivo.MAX_BYTES) {
            throw new ArchivoInvalidoException(
                    "El archivo pesa mas de " + (LimitesArchivo.MAX_BYTES / 1024 / 1024)
                            + " MB. Exporta solo las filas que necesitas cargar.");
        }
        try {
            return archivo.getBytes();
        } catch (IOException e) {
            throw new ArchivoInvalidoException("No se pudo leer el archivo enviado.");
        }
    }

    private boolean tieneFirmaZip(byte[] contenido) {
        if (contenido.length < FIRMA_ZIP.length) return false;
        for (int i = 0; i < FIRMA_ZIP.length; i++) {
            if (contenido[i] != FIRMA_ZIP[i]) return false;
        }
        return true;
    }

    // ── CSV ──────────────────────────────────────────────────────────────────

    private TablaLeida leerCsv(byte[] contenido, Topes topes) {
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
            validarEncabezados(encabezados, topes);

            String linea;
            int numeroLinea = 1;
            while ((linea = lector.readLine()) != null) {
                numeroLinea++;
                if (linea.isBlank()) continue;

                if (filas.size() >= topes.maxFilas()) {
                    throw new ArchivoInvalidoException(
                            "El archivo tiene mas de " + topes.maxFilas()
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

    private TablaConAvisos leerXlsx(byte[] contenido, ModoLectura modo, Topes topes) {
        List<CeldaDerivada> derivadas = new ArrayList<>();
        DataFormatter formateador = formateador();

        try (Workbook libro = abrir(contenido)) {
            Sheet hoja = libro.getSheetAt(0);
            TablaLeida tabla = tablaDeHoja(hoja, modo, topes, formateador, derivadas);
            if (tabla == null) {
                throw new ArchivoInvalidoException("La primera hoja no tiene fila de encabezados.");
            }
            return new TablaConAvisos(tabla, derivadas);
        } catch (ArchivoInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw new ArchivoInvalidoException(
                    "No se pudo abrir el libro de Excel. Comprueba que no este protegido con contrasena "
                            + "y que sea un .xlsx valido.");
        }
    }

    /** Recorre el libro entero. Ver {@link #leerHojas}. */
    private List<HojaLeida> leerTodasLasHojas(byte[] contenido, Topes topes) {
        List<HojaLeida> hojas = new ArrayList<>();
        DataFormatter formateador = formateador();

        try (Workbook libro = abrir(contenido)) {
            for (int i = 0; i < libro.getNumberOfSheets(); i++) {
                Sheet hoja = libro.getSheetAt(i);
                TablaLeida tabla = tablaDeHoja(
                        hoja, ModoLectura.CANONICO, topes, formateador, new ArrayList<>());
                if (tabla != null) hojas.add(new HojaLeida(hoja.getSheetName(), tabla));
            }
        } catch (ArchivoInvalidoException e) {
            throw e;
        } catch (Exception e) {
            throw new ArchivoInvalidoException(
                    "No se pudo abrir el libro de Excel. Comprueba que no este protegido con contrasena "
                            + "y que sea un .xlsx valido.");
        }

        if (hojas.isEmpty()) {
            throw new ArchivoInvalidoException(
                    "Ninguna hoja del libro tiene una tabla con encabezados.");
        }
        return hojas;
    }

    /**
     * XSSFWorkbook directamente y no WorkbookFactory: asi solo se acepta xlsx y no
     * se abre la puerta a otros formatos que POI sepa interpretar.
     */
    private Workbook abrir(byte[] contenido) throws IOException {
        // Los topes de POI son estaticos y globales; se fijan en cada lectura para
        // no depender de que otro punto del sistema los haya dejado como estaban.
        ZipSecureFile.setMinInflateRatio(LimitesArchivo.RATIO_MINIMO_INFLADO);
        ZipSecureFile.setMaxEntrySize(LimitesArchivo.MAX_BYTES_ENTRADA_ZIP);

        Workbook libro = new XSSFWorkbook(new ByteArrayInputStream(contenido));
        if (libro.getNumberOfSheets() == 0) {
            throw new ArchivoInvalidoException("El libro no tiene ninguna hoja.");
        }
        return libro;
    }

    /**
     * El formateador aplica el formato que la celda tiene puesto en la hoja. Se le
     * da la configuracion regional del cliente porque de ella dependen los formatos
     * de fecha y numero heredados de Excel.
     */
    private DataFormatter formateador() {
        return new DataFormatter(Locale.of("es", "MX"));
    }

    /**
     * Una hoja convertida en tabla, o null si no tiene encabezados utilizables.
     *
     * <p>Devolver null en lugar de fallar es lo que permite recorrer un libro
     * entero: un archivo de Excel real arrastra pestanas de notas y de trabajo, y
     * que una de ellas no sea una tabla no invalida las que si lo son. Quien pide
     * una sola hoja —y por tanto sabe cual quiere— convierte ese null en error.</p>
     */
    private TablaLeida tablaDeHoja(Sheet hoja, ModoLectura modo, Topes topes,
                                   DataFormatter formateador, List<CeldaDerivada> derivadas) {
        Row filaEncabezados = hoja.getRow(hoja.getFirstRowNum());
        if (filaEncabezados == null) return null;

        List<String> encabezados = new ArrayList<>();
        for (int c = 0; c < filaEncabezados.getLastCellNum(); c++) {
            encabezados.add(valorDeCelda(filaEncabezados.getCell(c),
                    filaEncabezados.getRowNum() + 1, c, modo, formateador, derivadas));
        }
        encabezados = limpiar(encabezados);
        quitarColumnasVaciasFinales(encabezados);
        if (encabezados.isEmpty()) return null;
        validarEncabezados(encabezados, topes);

        List<List<String>> filas = new ArrayList<>();
        List<Integer> numeros = new ArrayList<>();

        for (int f = filaEncabezados.getRowNum() + 1; f <= hoja.getLastRowNum(); f++) {
            Row fila = hoja.getRow(f);
            if (fila == null) continue;

            List<String> celdas = new ArrayList<>();
            boolean vacia = true;
            for (int c = 0; c < encabezados.size(); c++) {
                String valor = valorDeCelda(fila.getCell(c), f + 1, c, modo, formateador, derivadas);
                if (!valor.isEmpty()) vacia = false;
                celdas.add(valor);
            }
            if (vacia) continue;   // filas en blanco intercaladas

            if (filas.size() >= topes.maxFilas()) {
                // Se nombra la hoja: un libro con varias pestanas se lee entero, y una
                // sola pestana larga tumba la lectura de todas. Sin el nombre, el
                // mensaje acusa a «el archivo» de algo que pasa en una hoja que a lo
                // mejor no es la que se queria cargar —el caso real es subir en esta
                // pantalla un archivo que era de otra—.
                String nombre = hoja.getSheetName();
                throw new ArchivoInvalidoException(
                        "La hoja \"" + nombre + "\" tiene mas de " + topes.maxFilas()
                                + " filas, que es el maximo de esta carga. Comprueba que sea el "
                                + "archivo correcto para esta pantalla; si lo es, dividelo.");
            }

            filas.add(celdas);
            numeros.add(f + 1);
        }

        return new TablaLeida(encabezados, filas, numeros);
    }

    /**
     * Texto de una celda, segun el modo de lectura.
     *
     * <p>En modo canonico las fechas salen en ISO para que el importador las
     * reconozca sin depender de la configuracion regional de quien exporto. En
     * modo reproducible sale lo que la hoja muestra, que es lo contrario: el
     * formato de la celda es justamente lo que hay que conservar.</p>
     */
    private String valorDeCelda(Cell celda, int numeroFila, int numeroColumna,
                                ModoLectura modo, DataFormatter formateador,
                                List<CeldaDerivada> derivadas) {
        if (celda == null) return "";

        if (modo == ModoLectura.COMO_SE_VE) {
            String valor = valorComoSeVe(celda, numeroFila, numeroColumna, formateador, derivadas);
            verificarLargo(valor, numeroFila, numeroColumna);
            return valor == null ? "" : valor.trim();
        }

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

    /**
     * Texto de la celda tal como lo muestra Excel.
     *
     * <p>Las formulas se resuelven leyendo el resultado que el propio archivo trae
     * guardado, sin recalcular nada: evaluar las formulas aqui significaria
     * ejecutar el libro ajeno en el servidor, con las funciones que traiga y los
     * enlaces externos que le hayan puesto. Ese resultado guardado es el mismo que
     * la persona ve en su pantalla, que es exactamente lo que se pide reproducir;
     * aun asi la celda se reporta como derivada para poder avisarlo.</p>
     */
    private String valorComoSeVe(Cell celda, int numeroFila, int numeroColumna,
                                 DataFormatter formateador, List<CeldaDerivada> derivadas) {
        if (celda.getCellType() != CellType.FORMULA) {
            // Cubre texto, numero, fecha, moneda, porcentaje y booleano: en todos
            // aplica el formato que la celda tiene puesto en la hoja.
            return formateador.formatCellValue(celda);
        }

        derivadas.add(new CeldaDerivada(numeroFila, numeroColumna,
                referencia(numeroFila, numeroColumna)));

        // Sin evaluador, formatCellValue devolveria el texto de la formula en vez
        // de su resultado, asi que hay que ir al valor cacheado a mano.
        return switch (celda.getCachedFormulaResultType()) {
            case STRING -> celda.getRichStringCellValue().getString();
            case BOOLEAN -> celda.getBooleanCellValue() ? "VERDADERO" : "FALSO";
            case NUMERIC -> formateador.formatRawCellContents(
                    celda.getNumericCellValue(),
                    celda.getCellStyle().getDataFormat(),
                    celda.getCellStyle().getDataFormatString());
            // Una formula en error (#N/A, #REF!) no tiene valor que imprimir. Se
            // deja vacia y la fila sale marcada por celda faltante.
            default -> "";
        };
    }

    // ── Comunes ──────────────────────────────────────────────────────────────

    private void validarEncabezados(List<String> encabezados, Topes topes) {
        if (encabezados.isEmpty()) {
            throw new ArchivoInvalidoException("El archivo no tiene encabezados.");
        }
        if (encabezados.size() > topes.maxColumnas()) {
            throw new ArchivoInvalidoException(
                    "El archivo tiene mas de " + topes.maxColumnas() + " columnas.");
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
