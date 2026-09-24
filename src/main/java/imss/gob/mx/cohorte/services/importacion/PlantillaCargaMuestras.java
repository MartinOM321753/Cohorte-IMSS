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
 * Arma la plantilla vacía de la carga de muestras.
 *
 * <h3>Por qué se genera y no se guarda como archivo</h3>
 * <p>Un .xlsx es un ZIP, y un ZIP no sobrevive a que alguien lo trate como
 * texto. Guardado en {@code src/main/resources} pasaba por demasiadas manos que
 * pueden hacer justamente eso: el filtrado de recursos de Maven, la copia de
 * recursos del IDE, y sobre todo Git —este repositorio tiene
 * {@code core.autocrlf=true} y no tiene {@code .gitattributes}, así que al
 * versionarlo le cambiaría los saltos de línea y lo dejaría irreparable para
 * todo el que clonara—. El síntoma es siempre el mismo y no dice nada útil:
 * Excel abre el archivo y ofrece «recuperar el máximo de contenido posible».</p>
 *
 * <p>Generarlo en cada descarga cuesta unos milisegundos y elimina la categoría
 * entera de problemas: no hay binario que corromper.</p>
 *
 * <h3>Por qué sale de {@link ColumnaMuestra}</h3>
 * <p>La plantilla es, literalmente, la documentación de ese enum. Si viviera
 * aparte podrían dejar de coincidir —una columna nueva en el importador que la
 * plantilla no trae, o al revés— y eso no se nota hasta que alguien sube un
 * archivo y no entiende por qué falla.</p>
 */
@Component
public class PlantillaCargaMuestras {

    public static final String NOMBRE_ARCHIVO = "plantilla-carga-muestras.xlsx";

    private static final String HOJA_INSTRUCCIONES = "LEEME";
    private static final String HOJA_DATOS = "MUESTRAS";

    /** Azul de los encabezados que el importador lee. */
    private static final short AZUL = IndexedColors.DARK_BLUE.getIndex();
    /** Gris de los que ignora. */
    private static final short GRIS = IndexedColors.GREY_50_PERCENT.getIndex();

    public byte[] generar() {
        try (Workbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            // La hoja de datos va PRIMERO y no es cosmético: el lector toma la
            // primera hoja del libro. Con las instrucciones delante, subir la
            // plantilla llena hacía que el importador leyera el instructivo y
            // se quejara de que faltan todas las columnas.
            escribirEncabezados(libro);
            escribirInstrucciones(libro);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            // Escribir en memoria no falla por causas que el usuario pueda
            // arreglar, así que no se le ofrece un mensaje que sugiera lo
            // contrario: esto es un fallo del servidor.
            throw new UncheckedIOException("No se pudo armar la plantilla de carga de muestras", e);
        }
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

        fila.titulo(titulo, "PLANTILLA", "Carga masiva de muestras y alícuotas — cohorteApp");
        fila.saltar();
        fila.par("Una fila, un vial.",
                "No se cargan los viales que no existen ni los huecos libres de la caja: "
                + "un hueco vacío ya nace libre al generar la caja.");
        fila.par("Se identifica por folio.",
                "El nombre y el consecutivo son informativos; el importador los ignora sin avisar.");
        fila.saltar();

        fila.titulo(titulo, "COLUMNAS", "");
        for (ColumnaMuestra c : ColumnaMuestra.values()) {
            fila.par(c.tituloPreferido(), c.descripcion());
        }
        for (String informativa : ColumnaMuestra.encabezadosInformativos()) {
            fila.par(informativa, "INFORMATIVA. El importador la ignora.");
        }
        fila.saltar();

        fila.titulo(titulo, "NORMALIZACIONES", "las aplica el importador sin preguntar");
        fila.par("folio", "Se rellena con ceros a 6 dígitos: 1103 se busca como 001103. Excel se come "
                + "los ceros a la izquierda al guardar un CSV, así que se intenta con y sin relleno.");
        fila.par("tipo y tubo", "Se comparan sin acentos, sin mayúsculas y con los espacios colapsados: "
                + "CRIOTUBO 300 MG y Criotubo 300 mg son el mismo tubo.");
        fila.par("fecha", "Se admite 2026-06-23, 2026-06-23 08:30, 23/06/2026 o 23-jun-2026. El orden "
                + "día/mes se decide mirando el archivo completo, nunca fila a fila.");
        fila.par("hora", "Si la fecha no trae hora, se completa con la hora válida más cercana según el "
                + "horario configurado, respetando siempre el día que traiga la fila.");
        fila.par("volumen", "Vacío hereda el nominal del tubo. Con dato manda el archivo. Un volumen "
                + "menor al nominal NO es un faltante: es lo que salió en ese vial.");
        fila.par("unidad", "No se convierte nunca. Si no coincide con la del tubo, la fila es un error.");
        fila.par("posición", "La letra es la fila y el número la columna, como en una placa de laboratorio.");
        fila.saltar();

        fila.titulo(aviso, "ANTES DE CARGAR", "esto no lo crea la carga; tiene que existir ya");
        fila.par("Participantes", "Dados de alta, activos y con su folio a 6 dígitos.");
        fila.par("Tipos y tubos", "Con el nombre exacto que use este archivo, y con su número de "
                + "alícuotas, volumen nominal y unidad ya configurados.");
        fila.par("Cajas", "Dadas de alta con el mismo código que ponga la columna codigoCaja, colgadas "
                + "de un piso de refrigerador, y con sus posiciones generadas.");
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

    // ── Hoja de datos ────────────────────────────────────────────────────────

    private void escribirEncabezados(Workbook libro) {
        Sheet hoja = libro.createSheet(HOJA_DATOS);
        CellStyle leido = estiloEncabezado(libro, AZUL);
        CellStyle ignorado = estiloEncabezado(libro, GRIS);

        Row cabecera = hoja.createRow(0);
        int col = 0;

        for (ColumnaMuestra c : ColumnaMuestra.values()) {
            Cell celda = cabecera.createCell(col);
            celda.setCellValue(c.tituloPreferido());
            celda.setCellStyle(leido);
            hoja.setColumnWidth(col, c.anchoPlantilla() * 256);
            // El folio va como texto o Excel se come sus ceros a la izquierda en
            // cuanto alguien teclee 001103 y lo convierta en 1103.
            if (c == ColumnaMuestra.FOLIO) {
                hoja.setDefaultColumnStyle(col, estiloTextoForzado(libro));
            }
            col++;
        }

        List<String> informativas = ColumnaMuestra.encabezadosInformativos();
        for (String nombre : informativas) {
            Cell celda = cabecera.createCell(col);
            celda.setCellValue(nombre);
            celda.setCellStyle(ignorado);
            hoja.setColumnWidth(col, 28 * 256);
            col++;
        }

        // El encabezado se queda a la vista al desplazarse: con seiscientas filas,
        // saber qué columna se está llenando es la diferencia entre poder revisar
        // el archivo y no poder.
        hoja.createFreezePane(0, 1);
        hoja.setAutoFilter(new CellRangeAddress(0, 0, 0, col - 1));
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
