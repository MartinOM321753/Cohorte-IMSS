package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.cita.ConfiguracionHorario;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.citas.ConfiguracionHorarioService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Arma la plantilla vacía de una carga masiva de resultados —estudios o
 * exámenes—, con sus dos columnas de control (folio y fecha) y una columna por
 * cada destino del catálogo.
 *
 * <h3>Por qué se genera y no se guarda como archivo</h3>
 * <p>Igual que {@link PlantillaCargaMuestras}: un .xlsx es un ZIP y no sobrevive
 * a que alguien lo trate como texto —el filtrado de recursos de Maven, la copia
 * del IDE, o Git con {@code core.autocrlf}—. El síntoma es siempre el mismo y no
 * dice nada útil: Excel ofrece «recuperar el máximo de contenido posible».
 * Generarlo en cada descarga cuesta milisegundos y quita el binario de en
 * medio.</p>
 *
 * <h3>El encabezado que se escribe en cada columna</h3>
 * <p>Es la cara «de salida» de la misma regla que aplica
 * {@link EmparejadorColumnas} al leer: un destino sin alias se titula con su
 * <b>nombre</b> —que es como el emparejador lo reconoce cuando no hay alias—; un
 * destino con alias se titula con uno de ellos, porque en cuanto hay alias el
 * nombre deja de emparejar y una columna titulada con el nombre no volvería a
 * importarse.</p>
 *
 * <h3>Qué es una «versión»</h3>
 * <p>Un mismo catálogo puede recibir archivos de instrumentos distintos, y cada
 * instrumento titula sus columnas a su manera: esos títulos son los alias, en el
 * orden en que se dieron de alta. La <b>versión k</b> de la plantilla toma, en
 * cada columna, su alias en la posición k. El número de versiones es cuántos
 * alias tiene el destino que más tiene; los que tienen menos se quedan en su
 * último alias —nunca caen al nombre, porque titulado con el nombre esa columna
 * ya no emparejaría—. Un catálogo sin alias en ningún destino tiene una sola
 * versión: la de los nombres.</p>
 *
 * <h3>El membrete y el color</h3>
 * <p>La hoja de instrucciones lleva el logo y el nombre del sistema, y todos los
 * encabezados van con el verde de la interfaz —el {@code --primary} del tema por
 * defecto—, para que la plantilla se reconozca como parte de la aplicación y no
 * como un archivo suelto de dudosa procedencia.</p>
 */
@Component
@RequiredArgsConstructor
public class PlantillaCargaResultados {

    private final ConfiguracionHorarioService horarioService;
    private final InstitucionContextService institucionContext;

    private static final String HOJA_INSTRUCCIONES = "LEEME";
    private static final String HOJA_DATOS = "RESULTADOS";

    /** Título de la columna del participante. La escribe el emparejador como FOLIO. */
    private static final String COL_FOLIO = "folio";
    /** Título de la columna de la fecha. */
    private static final String COL_FECHA = "fecha";

    private static final String NOMBRE_SISTEMA = "CohorteApp";

    /**
     * El verde del sistema: el {@code --primary} del tema por defecto (imss-verde),
     * {@code oklch(0.36 0.05 156)}, convertido a sRGB. Está fijado aquí a propósito:
     * POI no puede leer el CSS, así que el color se duplica y se deja anclado a su
     * origen con este comentario para que se actualicen juntos.
     */
    private static final byte[] VERDE_SISTEMA = {(byte) 0x26, (byte) 0x45, (byte) 0x32};

    /** El logo, ya reescalado, cacheado tras la primera descarga. */
    private static volatile byte[] logoCacheado;
    private static volatile boolean logoIntentado;

    /**
     * Un destino del catálogo visto por la plantilla: su nombre y sus alias en el
     * orden configurado.
     *
     * @param nombre         el nombre del parámetro o del examen
     * @param aliasOrdenados los alias tal como los escribió el usuario, en su orden
     */
    public record Columna(String nombre, List<String> aliasOrdenados) {}

    /**
     * Cuántas versiones de plantilla ofrecer: tantas como alias tenga el destino
     * que más tiene, y nunca menos de una.
     */
    public int numeroDeVersiones(List<Columna> columnas) {
        int max = columnas.stream()
                .mapToInt(c -> c.aliasOrdenados() == null ? 0 : c.aliasOrdenados().size())
                .max().orElse(0);
        return Math.max(1, max);
    }

    /**
     * El encabezado de una columna para la versión pedida.
     *
     * <p>Sin alias, el nombre. Con alias, el de la posición {@code version}
     * (base 1); si el destino no llega a esa posición se queda en su último alias
     * —jamás en el nombre, que con alias configurados no emparejaría—.</p>
     */
    public String encabezado(Columna c, int version) {
        List<String> alias = c.aliasOrdenados();
        if (alias == null || alias.isEmpty()) {
            return c.nombre();
        }
        int indice = Math.min(Math.max(version, 1), alias.size()) - 1;
        return alias.get(indice);
    }

    /**
     * @param titulo     el nombre que va en la cabecera de la hoja de instrucciones
     *                   (p. ej. el tipo de estudio, o «Exámenes de laboratorio»)
     * @param columnas   los destinos del catálogo, en el orden en que van las columnas
     * @param version    qué juego de alias usar (base 1); se acota a [1, numeroDeVersiones]
     */
    public byte[] generar(String titulo, List<Columna> columnas, int version) {
        int versiones = numeroDeVersiones(columnas);
        int v = Math.min(Math.max(version, 1), versiones);
        String notaFecha = notaFecha();

        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {

            // La hoja de datos va PRIMERO: el lector toma la primera hoja del
            // libro. Con las instrucciones delante, subir la plantilla llena hacía
            // que el importador leyera el instructivo y se quejara de que faltan
            // todas las columnas.
            escribirEncabezados(libro, columnas, v, notaFecha);
            escribirInstrucciones(libro, titulo, columnas, v, versiones, notaFecha);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            // Escribir en memoria no falla por causas que el usuario pueda
            // arreglar: esto es un fallo del servidor, no un mensaje que sugiera
            // lo contrario.
            throw new UncheckedIOException("No se pudo armar la plantilla de carga de resultados", e);
        }
    }

    // ── Hoja de datos ──────────────────────────────────────────────────────────

    private void escribirEncabezados(XSSFWorkbook libro, List<Columna> columnas,
                                     int version, String notaFecha) {
        Sheet hoja = libro.createSheet(HOJA_DATOS);
        CellStyle encabezado = estiloEncabezado(libro);
        CellStyle textoForzado = estiloTextoForzado(libro);

        Row cabecera = hoja.createRow(0);
        int col = 0;

        // El folio va como texto o Excel se come sus ceros a la izquierda en cuanto
        // alguien teclee 001103 y lo convierta en 1103.
        Cell folio = cabecera.createCell(col);
        folio.setCellValue(COL_FOLIO);
        folio.setCellStyle(encabezado);
        hoja.setColumnWidth(col, 14 * 256);
        hoja.setDefaultColumnStyle(col, textoForzado);
        col++;

        Cell fecha = cabecera.createCell(col);
        fecha.setCellValue(COL_FECHA);
        fecha.setCellStyle(encabezado);
        hoja.setColumnWidth(col, 20 * 256);
        // La nota va pegada al encabezado de la fecha: es donde el usuario mira
        // cuando duda de cómo escribirla, mejor que enterrada en el instructivo.
        anotarFecha(libro, hoja, fecha, col, notaFecha);
        col++;

        for (Columna c : columnas) {
            String enc = encabezado(c, version);
            Cell celda = cabecera.createCell(col);
            celda.setCellValue(enc);
            celda.setCellStyle(encabezado);
            hoja.setColumnWidth(col, anchoPara(enc) * 256);
            col++;
        }

        // El encabezado se queda a la vista al desplazarse: con muchas filas, saber
        // qué columna se está llenando es la diferencia entre poder revisar el
        // archivo y no poder.
        hoja.createFreezePane(0, 1);
        if (col > 0) {
            hoja.setAutoFilter(new CellRangeAddress(0, 0, 0, col - 1));
        }
    }

    /** Cuelga la nota de la celda de la fecha como comentario emergente. */
    private void anotarFecha(XSSFWorkbook libro, Sheet hoja, Cell celda, int col, String nota) {
        Drawing<?> dibujo = hoja.createDrawingPatriarch();
        CreationHelper helper = libro.getCreationHelper();
        ClientAnchor ancla = helper.createClientAnchor();
        // La caja del comentario abarca varias celdas para que el texto quepa sin
        // recortarse al abrirlo.
        ancla.setCol1(col);
        ancla.setCol2(col + 4);
        ancla.setRow1(1);
        ancla.setRow2(9);
        Comment comentario = dibujo.createCellComment(ancla);
        comentario.setString(helper.createRichTextString(nota));
        comentario.setAuthor(NOMBRE_SISTEMA);
        celda.setCellComment(comentario);
    }

    /** Ancho holgado sin pasarse: cabe el título sin dejar una columna enorme. */
    private int anchoPara(String titulo) {
        return Math.min(40, Math.max(14, titulo.length() + 4));
    }

    // ── Hoja de instrucciones ────────────────────────────────────────────────

    private void escribirInstrucciones(XSSFWorkbook libro, String titulo, List<Columna> columnas,
                                       int version, int versiones, String notaFecha) {
        Sheet hoja = libro.createSheet(HOJA_INSTRUCCIONES);
        hoja.setColumnWidth(0, 30 * 256);
        hoja.setColumnWidth(1, 90 * 256);

        CellStyle estiloTitulo = estiloTitulo(libro, VERDE_SISTEMA);
        CellStyle aviso = estiloTituloIndexado(libro, IndexedColors.DARK_YELLOW.getIndex());
        CellStyle clave = estiloClave(libro);
        CellStyle texto = estiloTexto(libro);

        int inicio = escribirMembrete(libro, hoja, estiloTitulo);
        Fila fila = new Fila(hoja, clave, texto, inicio);

        fila.titulo(estiloTitulo, "PLANTILLA", titulo);
        if (versiones > 1) {
            fila.par("Versión", "Versión " + version + " de " + versiones
                    + ". Cada versión titula las columnas con el juego de alias de esa posición; "
                    + "elija la que corresponde al instrumento con el que va a llenar el archivo.");
        }
        fila.saltar();

        fila.par("Una fila, un participante.",
                "En cada fila van todos los resultados de ese participante en esa fecha.");
        fila.par("Se identifica por folio.",
                "El folio se rellena con ceros a la izquierda: 1103 se registra como 001103.");
        fila.par("Columnas de sobra.",
                "Las columnas que el catálogo no reconoce se avisan y se ignoran; no hace falta borrarlas.");
        fila.saltar();

        fila.titulo(estiloTitulo, "COLUMNAS DE CONTROL", "obligatorias, no las quite");
        fila.par(COL_FOLIO, "El folio del participante. Déjelo como texto para no perder los ceros de la izquierda.");
        fila.par(COL_FECHA, notaFecha);
        fila.saltar();

        fila.titulo(estiloTitulo, "COLUMNAS DE RESULTADO", "una por cada dato que se registra");
        for (Columna c : columnas) {
            String enc = encabezado(c, version);
            String nota = enc.equalsIgnoreCase(c.nombre())
                    ? "" // el título ya es el nombre; no hay nada que aclarar
                    : "Corresponde a: " + c.nombre();
            fila.par(enc, nota);
        }
        fila.saltar();

        fila.titulo(aviso, "ANTES DE CARGAR", "esto no lo crea la carga; tiene que existir ya");
        fila.par("Participantes", "Dados de alta, activos y con su folio.");
        fila.par("No cambie los encabezados",
                "El importador reconoce las columnas por su título. Si lo cambia, esa columna se ignora.");
    }

    /**
     * Escribe el membrete —logo y nombre del sistema— en las primeras filas de la
     * hoja y devuelve la fila en la que puede empezar el texto.
     *
     * <p>Si el logo no se puede leer, el membrete se queda solo con el nombre: un
     * logo ausente no es motivo para que la descarga falle.</p>
     */
    private int escribirMembrete(XSSFWorkbook libro, Sheet hoja, CellStyle estiloTitulo) {
        XSSFFont fuenteNombre = libro.createFont();
        fuenteNombre.setBold(true);
        fuenteNombre.setFontHeightInPoints((short) 18);
        fuenteNombre.setColor(colorSistema());
        XSSFCellStyle estiloNombre = libro.createCellStyle();
        estiloNombre.setFont(fuenteNombre);
        estiloNombre.setVerticalAlignment(VerticalAlignment.CENTER);

        Row rNombre = hoja.createRow(0);
        rNombre.setHeightInPoints(30);
        Cell cNombre = rNombre.createCell(1);
        cNombre.setCellValue(NOMBRE_SISTEMA);
        cNombre.setCellStyle(estiloNombre);

        Row rSub = hoja.createRow(1);
        Cell cSub = rSub.createCell(1);
        cSub.setCellValue("Cohorte de Trabajadores de la Salud");
        CellStyle estiloSub = estiloTexto(libro);
        cSub.setCellStyle(estiloSub);

        byte[] logo = logo();
        if (logo != null) {
            try {
                int idx = libro.addPicture(logo, Workbook.PICTURE_TYPE_PNG);
                Drawing<?> dibujo = hoja.createDrawingPatriarch();
                ClientAnchor ancla = libro.getCreationHelper().createClientAnchor();
                ancla.setCol1(0);
                ancla.setRow1(0);
                Picture pic = dibujo.createPicture(ancla, idx);
                // Sin resize la imagen se dibuja a su tamaño nativo; con el logo ya
                // reescalado a ~110 px cabe en la primera columna sin invadir el texto.
                pic.resize();
            } catch (RuntimeException e) {
                // Un fallo dibujando el logo no puede tumbar la descarga entera.
                logoCacheado = null;
            }
        }

        // Deja un renglón de aire entre el membrete y el primer bloque.
        return 3;
    }

    /** Lleva la cuenta del renglón para que las instrucciones se lean como un texto. */
    private static final class Fila {
        private final Sheet hoja;
        private final CellStyle clave;
        private final CellStyle texto;
        private int siguiente;

        Fila(Sheet hoja, CellStyle clave, CellStyle texto, int inicio) {
            this.hoja = hoja;
            this.clave = clave;
            this.texto = texto;
            this.siguiente = inicio;
        }

        void par(String izquierda, String derecha) {
            Row r = filaEn(siguiente++);
            Cell a = celdaEn(r, 0);
            a.setCellValue(izquierda);
            a.setCellStyle(clave);
            Cell b = celdaEn(r, 1);
            b.setCellValue(derecha);
            b.setCellStyle(texto);
            // Sin alto explícito, una celda con ajuste de texto se dibuja de una
            // línea y esconde el resto. Se cuenta también por los saltos de línea
            // que ya trae el texto (la nota de la fecha los usa).
            int porAncho = derecha.length() / 90;
            int porSaltos = (int) derecha.chars().filter(ch -> ch == '\n').count();
            int lineas = Math.max(1, porAncho + porSaltos + 1);
            r.setHeightInPoints(lineas * hoja.getDefaultRowHeightInPoints());
        }

        void titulo(CellStyle estilo, String izquierda, String derecha) {
            Row r = filaEn(siguiente++);
            Cell a = celdaEn(r, 0);
            a.setCellValue(izquierda);
            a.setCellStyle(estilo);
            Cell b = celdaEn(r, 1);
            b.setCellValue(derecha);
            b.setCellStyle(estilo);
        }

        void saltar() {
            siguiente++;
        }

        /** El membrete pudo crear ya algunas filas; reutilizarlas evita perderlas. */
        private Row filaEn(int i) {
            Row r = hoja.getRow(i);
            return r != null ? r : hoja.createRow(i);
        }

        private Cell celdaEn(Row r, int i) {
            Cell c = r.getCell(i);
            return c != null ? c : r.createCell(i);
        }
    }

    // ── Nota de la fecha ─────────────────────────────────────────────────────

    /**
     * La nota que explica cómo escribir la fecha y en qué horario se aceptan las
     * fechas, tomado de la configuración de la institución.
     */
    private String notaFecha() {
        StringBuilder sb = new StringBuilder();
        sb.append("Escriba la fecha CON hora y minutos.\n")
          .append("Ejemplos: 23/06/2026 08:30  (día/mes/año hora:minutos),\n")
          .append("2026-06-23 08:30  o  23-jun-2026 08:30.\n")
          .append("El orden día/mes se decide mirando el archivo completo, nunca fila a fila.\n\n");

        ConfiguracionHorario horario = horarioActiva();
        if (horario == null) {
            sb.append("No hay un horario de atención configurado: use la fecha y la hora "
                    + "reales en que se hizo el estudio.");
        } else {
            sb.append("Horario de atención configurado: de las ")
              .append(dosDigitos(horario.getHoraInicio())).append(":00 a las ")
              .append(dosDigitos(horario.getHoraFin())).append(":00 h, ")
              .append(diasHabiles(horario)).append(".\n")
              .append("Registre las fechas dentro de ese horario; una hora fuera de él se "
                      + "marca como dato por corregir.");
        }
        return sb.toString();
    }

    /** El horario activo de la institución, o null si no falla ni hay ninguno. */
    private ConfiguracionHorario horarioActiva() {
        try {
            return horarioService.obtenerActiva(institucionContext.getIdInstitucionActual());
        } catch (RuntimeException e) {
            // La plantilla se puede armar sin el horario; su ausencia no debe
            // impedir descargarla.
            return null;
        }
    }

    private static String dosDigitos(Integer hora) {
        int h = hora == null ? 0 : hora;
        return (h < 10 ? "0" : "") + h;
    }

    /** Los días marcados como hábiles, en orden y en lenguaje natural. */
    private static String diasHabiles(ConfiguracionHorario h) {
        List<String> dias = new ArrayList<>();
        if (Boolean.TRUE.equals(h.getLunes()))     dias.add("lunes");
        if (Boolean.TRUE.equals(h.getMartes()))    dias.add("martes");
        if (Boolean.TRUE.equals(h.getMiercoles())) dias.add("miércoles");
        if (Boolean.TRUE.equals(h.getJueves()))    dias.add("jueves");
        if (Boolean.TRUE.equals(h.getViernes()))   dias.add("viernes");
        if (Boolean.TRUE.equals(h.getSabado()))    dias.add("sábado");
        if (Boolean.TRUE.equals(h.getDomingo()))   dias.add("domingo");

        if (dias.isEmpty()) return "sin días configurados";
        if (dias.size() == 1) return dias.get(0);
        String ultimo = dias.remove(dias.size() - 1);
        return String.join(", ", dias) + " y " + ultimo;
    }

    // ── Logo ─────────────────────────────────────────────────────────────────

    /**
     * El logo del sistema, reescalado a un tamaño de membrete y cacheado.
     *
     * <p>Se lee del classpath y se reduce con AWT en vez de guardar ya pequeño el
     * binario: el original es la única copia y reescalarlo cuesta una vez por
     * arranque. Si algo falla —recurso ausente, imagen ilegible— devuelve null y
     * la plantilla sale sin logo, nunca con un error.</p>
     */
    private static byte[] logo() {
        if (logoIntentado) return logoCacheado;
        synchronized (PlantillaCargaResultados.class) {
            if (logoIntentado) return logoCacheado;
            logoCacheado = cargarLogo();
            logoIntentado = true;
            return logoCacheado;
        }
    }

    private static byte[] cargarLogo() {
        try (InputStream in = PlantillaCargaResultados.class
                .getResourceAsStream("/plantillas/cohorte-logo.png")) {
            if (in == null) return null;
            BufferedImage original = ImageIO.read(in);
            if (original == null) return null;

            int ancho = 110;
            int alto = Math.max(1, Math.round(
                    original.getHeight() * (ancho / (float) original.getWidth())));
            BufferedImage escalada = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = escalada.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(original, 0, 0, ancho, alto, null);
            g.dispose();

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(escalada, "png", out);
            return out.toByteArray();
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    // ── Estilos ────────────────────────────────────────────────────────────────

    private static XSSFColor colorSistema() {
        return new XSSFColor(VERDE_SISTEMA, null);
    }

    private XSSFCellStyle estiloEncabezado(XSSFWorkbook libro) {
        XSSFCellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(colorSistema());
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estilo.setAlignment(HorizontalAlignment.CENTER);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);
        return estilo;
    }

    /** Título con fondo del color del sistema (verde de la interfaz). */
    private XSSFCellStyle estiloTitulo(XSSFWorkbook libro, byte[] rgb) {
        XSSFCellStyle estilo = libro.createCellStyle();
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(new XSSFColor(rgb, null));
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    /** Título con un color indexado de POI (para el aviso ámbar). */
    private CellStyle estiloTituloIndexado(Workbook libro, short fondo) {
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
