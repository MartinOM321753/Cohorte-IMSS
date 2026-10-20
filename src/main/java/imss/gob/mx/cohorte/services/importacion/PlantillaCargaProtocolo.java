package imss.gob.mx.cohorte.services.importacion;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Arma la plantilla vacía de la carga masiva por protocolo.
 *
 * <p>Mismo criterio que {@link PlantillaCargaMuestras}: se genera en cada descarga
 * porque un .xlsx guardado como recurso se corrompe en cuanto pasa por Maven, el IDE
 * o Git con {@code core.autocrlf}. Los encabezados de la hoja de datos son los nombres
 * que acepta {@code CargaMasivaProtocoloService.colIndices}; si cambian allí, cambian
 * aquí. La prueba {@code PlantillaCargaProtocoloTest} vigila que coincidan.</p>
 *
 * <p>La hoja de datos va primero y LEEME después: el importador lee todas las hojas y
 * le pide a cada una el protocolo al que pertenece, así que una hoja de instrucciones
 * delante se leería como datos y se quejaría de que faltan columnas.</p>
 */
@Component
public class PlantillaCargaProtocolo {

    public static final String NOMBRE_ARCHIVO = "plantilla-carga-protocolo.xlsx";

    private static final String HOJA_INSTRUCCIONES = "LEEME";
    private static final String HOJA_DATOS = "ALICUOTAS";

    private static final short AZUL = IndexedColors.DARK_BLUE.getIndex();
    private static final short GRIS = IndexedColors.GREY_50_PERCENT.getIndex();

    /** Columna de la hoja de datos: el encabezado exacto que lee el importador. */
    record Columna(String encabezado, String descripcion, int ancho, boolean obligatoria) {}

    static final List<Columna> COLUMNAS = List.of(
            new Columna("folio",
                    "Folio del participante. Se guarda como texto para conservar los ceros a la izquierda.",
                    14, true),
            new Columna("numero_alicuota",
                    "Número de la alícuota dentro del tubo: 1, 2, 3… (también se acepta «alicuota»). "
                            + "Si la columna no está en el archivo, se numeran en el orden de las filas.",
                    18, false),
            new Columna("volumen",
                    "Volumen de la alícuota en la unidad del tubo. Use 0 si no se recolectó.",
                    12, true),
            new Columna("unidad",
                    "Unidad del volumen, p. ej. mL. Si va vacía, se usa la unidad configurada en el tubo.",
                    12, false),
            new Columna("fecha",
                    "Fecha de la toma: 2026-06-23, 23/06/2026 o 23-jun-2026. Basta con una fila del folio; "
                            + "si no hay ninguna, se usa la fecha por omisión de la pantalla.",
                    20, false),
            new Columna("codigo_caja",
                    "Código de la caja tal como está dada de alta (también se acepta «caja»).",
                    16, false),
            new Columna("posicion",
                    "Posición dentro de la caja: la letra es la fila y el número la columna, como B2.",
                    12, false),
            new Columna("observaciones",
                    "Texto libre. Las alícuotas que falten se crean con volumen 0 y la observación «No recolectada».",
                    40, false));

    public byte[] generar() {
        try (Workbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            escribirEncabezados(libro);
            escribirInstrucciones(libro);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo armar la plantilla de carga por protocolo", e);
        }
    }

    // ── Hoja de datos ────────────────────────────────────────────────────────

    private void escribirEncabezados(Workbook libro) {
        Sheet hoja = libro.createSheet(HOJA_DATOS);
        CellStyle obligatorio = estiloEncabezado(libro, AZUL);
        CellStyle opcional = estiloEncabezado(libro, GRIS);

        Row cabecera = hoja.createRow(0);
        for (int i = 0; i < COLUMNAS.size(); i++) {
            Columna c = COLUMNAS.get(i);
            Cell celda = cabecera.createCell(i);
            celda.setCellValue(c.encabezado());
            celda.setCellStyle(c.obligatoria() ? obligatorio : opcional);
            hoja.setColumnWidth(i, c.ancho() * 256);
            // El folio como texto: Excel se come los ceros a la izquierda en cuanto
            // alguien teclea 001103 y lo convierte en 1103.
            if (c.encabezado().equals("folio")) {
                hoja.setDefaultColumnStyle(i, estiloTextoForzado(libro));
            }
        }

        // Encabezado fijo y filtro: con cientos de alícuotas, saber qué columna se
        // está llenando es lo que permite revisar el archivo.
        hoja.createFreezePane(0, 1);
        hoja.setAutoFilter(new CellRangeAddress(0, 0, 0, COLUMNAS.size() - 1));
    }

    // ── Hoja de instrucciones ────────────────────────────────────────────────

    private void escribirInstrucciones(Workbook libro) {
        Sheet hoja = libro.createSheet(HOJA_INSTRUCCIONES);
        hoja.setColumnWidth(0, 22 * 256);
        hoja.setColumnWidth(1, 95 * 256);

        CellStyle titulo = estiloTitulo(libro, AZUL);
        CellStyle aviso = estiloTitulo(libro, IndexedColors.DARK_YELLOW.getIndex());
        CellStyle clave = estiloClave(libro);
        CellStyle texto = estiloTexto(libro);

        Fila fila = new Fila(hoja, clave, texto);

        fila.titulo(titulo, "PLANTILLA", "Carga masiva por protocolo — cohorteApp");
        fila.saltar();
        fila.par("Una fila, una alícuota.",
                "Todas las filas de un mismo folio describen la misma toma del participante.");
        fila.par("Varias hojas.",
                "Cada hoja se asigna a un protocolo en la pantalla. Un folio no puede aparecer en dos "
                        + "hojas para el mismo protocolo.");
        fila.par("Se identifica por folio.",
                "El importador busca al participante por su folio; el resto de los datos del participante "
                        + "no se toca.");
        fila.saltar();

        fila.titulo(titulo, "COLUMNAS", "");
        for (Columna c : COLUMNAS) {
            fila.par(c.encabezado() + (c.obligatoria() ? "  (obligatoria)" : "  (opcional)"), c.descripcion());
        }
        fila.saltar();

        fila.titulo(aviso, "ANTES DE CARGAR", "esto no lo crea la carga; tiene que existir ya");
        fila.par("Participantes", "Dados de alta, activos y con su folio.");
        fila.par("Protocolo", "Con sus tubos primarios, su número de alícuotas, volumen nominal y unidad ya configurados.");
        fila.par("Cajas", "Si usa codigo_caja o posicion, las cajas deben estar dadas de alta con sus posiciones generadas.");
    }

    /** Lleva la cuenta del renglón para que las instrucciones se lean como un texto. */
    private static final class Fila {
        private final Sheet hoja;
        private final CellStyle clave;
        private final CellStyle texto;
        private int siguiente = 0;

        Fila(Sheet hoja, CellStyle clave, CellStyle texto) {
            this.hoja = hoja;
            this.clave = clave;
            this.texto = texto;
        }

        void par(String izquierda, String derecha) {
            Row r = hoja.createRow(siguiente++);
            Cell a = r.createCell(0);
            a.setCellValue(izquierda);
            a.setCellStyle(clave);
            Cell b = r.createCell(1);
            b.setCellValue(derecha);
            b.setCellStyle(texto);
            // Sin alto explícito, una celda con ajuste de texto se dibuja de una
            // línea y esconde el resto.
            r.setHeightInPoints(Math.max(1, (derecha.length() / 95) + 1) * hoja.getDefaultRowHeightInPoints());
        }

        void titulo(CellStyle estilo, String izquierda, String derecha) {
            Row r = hoja.createRow(siguiente++);
            Cell a = r.createCell(0);
            a.setCellValue(izquierda);
            a.setCellStyle(estilo);
            Cell b = r.createCell(1);
            b.setCellValue(derecha);
            b.setCellStyle(estilo);
        }

        void saltar() {
            siguiente++;
        }
    }

    // ── Estilos ──────────────────────────────────────────────────────────────

    private CellStyle estiloEncabezado(Workbook libro, short fondo) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(fondo);
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        return estilo;
    }

    private CellStyle estiloTitulo(Workbook libro, short fondo) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(fondo);
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle estiloClave(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        estilo.setFont(fuente);
        estilo.setVerticalAlignment(VerticalAlignment.TOP);
        return estilo;
    }

    private CellStyle estiloTexto(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setWrapText(true);
        estilo.setVerticalAlignment(VerticalAlignment.TOP);
        return estilo;
    }

    private CellStyle estiloTextoForzado(Workbook libro) {
        CellStyle estilo = libro.createCellStyle();
        estilo.setDataFormat(libro.createDataFormat().getFormat("@"));
        return estilo;
    }
}
