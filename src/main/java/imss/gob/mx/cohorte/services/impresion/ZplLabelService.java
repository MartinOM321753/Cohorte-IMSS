package imss.gob.mx.cohorte.services.impresion;

import imss.gob.mx.cohorte.controllers.impresion.dto.LabelDataDTO;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.documentos.Documento;
import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.DisposicionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.TipoCodigo;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Service
public class ZplLabelService {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    public String generarZplMuestra(Muestra muestra, ConfiguracionEtiqueta config) {
        return generarZplLote(List.of(muestra), config, false);
    }

    public String generarZplLoteCompleto(Muestra padre, List<Muestra> alicuotas, ConfiguracionEtiqueta config) {
        List<Muestra> todas = new ArrayList<>();
        todas.add(padre);
        todas.addAll(alicuotas);
        return generarZplLote(todas, config, false);
    }

    public String generarZplLote(List<Muestra> muestras, ConfiguracionEtiqueta config) {
        return generarZplLote(muestras, config, false);
    }

    /**
     * @param marcoDepuracion dibuja el contorno de cada etiqueta. Sirve para
     *                        calibrar contra el troquel del rollo; no debe quedar
     *                        encendido para impresión normal.
     */
    public String generarZplLote(List<Muestra> muestras, ConfiguracionEtiqueta config,
                                  boolean marcoDepuracion) {
        int carriles = config.getCarrilesRolloEfectivo();
        StringBuilder zpl = new StringBuilder();
        for (int i = 0; i < muestras.size(); i += carriles) {
            int end = Math.min(i + carriles, muestras.size());
            zpl.append(generarZplFila(muestras.subList(i, end), config, marcoDepuracion));
        }
        return zpl.toString();
    }

    /**
     * Imprime las muestras en los carriles que el operador eligió.
     *
     * {@code acomodo} describe la superficie del rollo tal como se ve en pantalla:
     * una posición por carril, en orden de avance, con {@code null} donde el
     * carril queda en blanco. La lista se corta en filas del tamaño del rollo.
     *
     * Existe porque la Zebra consume el papel por filas completas: si se manda
     * una sola etiqueta en un rollo de tres carriles, los otros dos troqueles
     * salen vacíos y no se recuperan. No se puede evitar el gasto, pero sí se
     * puede decidir en qué carril cae cada etiqueta, que es lo que el operador
     * necesita para aprovechar una tira ya empezada.
     */
    public String generarZplAcomodado(List<Muestra> acomodo, ConfiguracionEtiqueta config,
                                       boolean marcoDepuracion) {
        int carriles = config.getCarrilesRolloEfectivo();
        int labelW = config.getAnchoDots();
        StringBuilder zpl = new StringBuilder();

        for (int inicio = 0; inicio < acomodo.size(); inicio += carriles) {
            int fin = Math.min(inicio + carriles, acomodo.size());
            List<Muestra> fila = acomodo.subList(inicio, fin);

            // Una fila entera vacía no se imprime: mandarla gastaría una vuelta
            // de rollo para no dibujar nada.
            if (fila.stream().allMatch(java.util.Objects::isNull)) continue;

            abrirFormato(zpl, config, carriles);
            for (int carril = 0; carril < fila.size(); carril++) {
                Muestra m = fila.get(carril);
                if (m == null) continue;
                int xBase = carril * labelW;
                if (marcoDepuracion) appendMarco(zpl, xBase, config);
                String etiqueta = sanitizar(m.getEtiqueta());
                String nombre = truncar(sanitizar(construirNombrePaciente(m.getPaciente())), 24);
                appendEtiqueta(zpl, xBase, nombre, etiqueta, etiqueta, config);
            }
            zpl.append("^XZ\n");
        }
        return zpl.toString();
    }

    // ── Emisión del formato ─────────────────────────────────────────────────

    /**
     * Preámbulo de cada formato.
     *
     * Antes solo se emitía {@code ^XA ^CI28 ^PW ^LL}, y eso dejaba dos cabos
     * sueltos que explican por qué la Zebra no respetaba el margen izquierdo:
     *
     * <ul>
     *   <li>Sin {@code ^LH} el formato hereda el origen que la impresora tenga
     *       guardado de un trabajo anterior o de su configuración de fábrica, así
     *       que las mismas coordenadas caen en sitios distintos según la máquina.
     *   <li>{@code ^PW} se fijaba en carriles por ancho de etiqueta, sin comparar
     *       contra el cabezal. Pedir más ancho del que la impresora tiene hace que
     *       ella lo recorte por su cuenta y corra el origen.
     * </ul>
     *
     * {@code ^LT0} devuelve el desplazamiento vertical a cero por el mismo motivo
     * que {@code ^LH}: es estado que sobrevive entre trabajos.
     */
    private void abrirFormato(StringBuilder zpl, ConfiguracionEtiqueta config, int carriles) {
        int anchoCabezalDots = (int) (config.getAnchoCabezalMm() / 25.4 * config.getDpi());
        int anchoPedido = carriles * config.getAnchoDots();
        int anchoImpresion = Math.min(anchoPedido, anchoCabezalDots);

        zpl.append("^XA\n^CI28\n");
        zpl.append("^LH").append(config.getOffsetLhXDots()).append(",")
           .append(config.getOffsetLhYDots()).append("\n");
        zpl.append("^LT0\n");
        zpl.append("^PW").append(anchoImpresion)
           .append("^LL").append(config.getAltoDots()).append("\n");
    }

    /**
     * Contorno de la etiqueta, para depurar. Es el equivalente del marco de
     * calibración de la hoja Avery: permite ver en el papel dónde cree la
     * impresora que está el recuadro, en vez de deducirlo del contenido.
     */
    private void appendMarco(StringBuilder zpl, int xBase, ConfiguracionEtiqueta config) {
        zpl.append("^FO").append(xBase).append(",0")
           .append("^GB").append(config.getAnchoDots()).append(",")
           .append(config.getAltoDots()).append(",2^FS\n");
    }

    /**
     * Contenido que se codifica dentro del simbolo de una etiqueta de documento.
     *
     * Code 128 lleva siempre el codigo de la etiqueta: es un codigo lineal, y una
     * URL completa da un simbolo de mas de 100 mm de ancho que no cabe en ninguna
     * etiqueta. En DataMatrix y QR si se puede elegir, porque llevar el enlace
     * permite abrir el documento con solo escanearlo.
     */
    public String contenidoCodigoDocumento(Documento documento, ConfiguracionEtiqueta config,
                                            boolean incluirEnlace) {
        if (!incluirEnlace || config.getTipoCodigo() == TipoCodigo.CODE_128) {
            return documento.getEtiqueta();
        }
        return frontendUrl + "/documento/" + documento.getEtiqueta();
    }

    public String generarZplDocumento(Documento documento, ConfiguracionEtiqueta config,
                                       boolean incluirEnlace) {
        String etiqueta = sanitizar(documento.getEtiqueta());
        String nombre = truncar(sanitizar(documento.getNombreOriginal()), 24);
        String codigoData = contenidoCodigoDocumento(documento, config, incluirEnlace);
        return generarZplGenericoConUrl(etiqueta, nombre, codigoData, config);
    }

    public String generarZplDocumentos(List<Documento> documentos, ConfiguracionEtiqueta config,
                                        boolean incluirEnlace) {
        return generarZplDocumentos(documentos, config, incluirEnlace, false);
    }

    public String generarZplDocumentos(List<Documento> documentos, ConfiguracionEtiqueta config,
                                        boolean incluirEnlace, boolean marcoDepuracion) {
        int carriles = config.getCarrilesRolloEfectivo();
        int labelW = config.getAnchoDots();

        StringBuilder zpl = new StringBuilder();
        for (int i = 0; i < documentos.size(); i += carriles) {
            int end = Math.min(i + carriles, documentos.size());
            List<Documento> fila = documentos.subList(i, end);

            abrirFormato(zpl, config, carriles);
            for (int j = 0; j < fila.size(); j++) {
                int xBase = j * labelW;
                if (marcoDepuracion) appendMarco(zpl, xBase, config);
                appendDocumento(zpl, xBase, fila.get(j), config, incluirEnlace);
            }
            zpl.append("^XZ\n");
        }
        return zpl.toString();
    }

    private void appendDocumento(StringBuilder zpl, int xBase, Documento documento,
                                  ConfiguracionEtiqueta config, boolean incluirEnlace) {
        String etiqueta = sanitizar(documento.getEtiqueta());
        String nombre = truncar(sanitizar(documento.getNombreOriginal()), 24);
        String codigoData = contenidoCodigoDocumento(documento, config, incluirEnlace);
        appendEtiqueta(zpl, xBase, nombre, etiqueta, codigoData, config);
    }

    private String generarZplGenericoConUrl(String etiqueta, String nombre, String codigoData,
                                             ConfiguracionEtiqueta config) {
        StringBuilder zpl = new StringBuilder();
        abrirFormato(zpl, config, config.getCarrilesRolloEfectivo());
        appendEtiqueta(zpl, 0, nombre, etiqueta, codigoData, config);
        zpl.append("^XZ\n");
        return zpl.toString();
    }

    /**
     * Emite el contenido de una etiqueta en el carril que empieza en {@code xBase}.
     *
     * Las posiciones las decide {@link MaquetadoEtiquetaZpl}, que ya garantizó que
     * todo cabe en el recuadro. Aquí solo se traducen a comandos.
     */
    private void appendEtiqueta(StringBuilder zpl, int xBase, String nombre, String etiqueta,
                                 String codigoData, ConfiguracionEtiqueta config) {
        var maquetado = MaquetadoEtiquetaZpl.calcular(config, nombre, etiqueta, codigoData);
        String datos = (codigoData != null && !codigoData.isEmpty()) ? codigoData : etiqueta;

        for (var elem : maquetado.elementos()) {
            switch (elem.tipo()) {
                case NOMBRE -> appendTexto(zpl, xBase + elem.x(), elem.y(),
                        elem.fuente(), elem.ancho(), nombre);
                case ETIQUETA -> appendTexto(zpl, xBase + elem.x(), elem.y(),
                        elem.fuente(), elem.ancho(), etiqueta);
                case CODIGO -> appendCodigo(zpl, xBase + elem.x(), elem.y(),
                        config.getTipoCodigo(), config.getModuloCodigo(), elem.escala(), datos);
            }
        }
    }

    /**
     * Una fila del rollo: tantas etiquetas como carriles tenga el medio.
     *
     * La Zebra avanza el papel por filas completas, así que el número de carriles
     * decide cuántas etiquetas se consumen en cada avance. Antes se usaba
     * {@code etiquetasPorFila}, que describe las columnas de la hoja Avery: una
     * configuración de hoja con 3 columnas obligaba al rollo a gastar 3 etiquetas
     * por avance aunque solo se hubiera mandado una.
     */
    private String generarZplFila(List<Muestra> fila, ConfiguracionEtiqueta config,
                                   boolean marcoDepuracion) {
        int carriles = config.getCarrilesRolloEfectivo();
        int labelW = config.getAnchoDots();

        StringBuilder zpl = new StringBuilder();
        abrirFormato(zpl, config, carriles);

        for (int i = 0; i < fila.size(); i++) {
            int xBase = i * labelW;
            if (marcoDepuracion) appendMarco(zpl, xBase, config);
            String etiqueta = sanitizar(fila.get(i).getEtiqueta());
            String nombre = truncar(sanitizar(construirNombrePaciente(fila.get(i).getPaciente())), 24);
            appendEtiqueta(zpl, xBase, nombre, etiqueta, etiqueta, config);
        }

        zpl.append("^XZ\n");
        return zpl.toString();
    }

    private void appendTexto(StringBuilder zpl, int x, int y, int fontSize, int usableW, String texto) {
        zpl.append("^FO").append(x).append(",").append(y);
        zpl.append("^A0N,").append(fontSize).append(",").append(fontSize);
        zpl.append("^FB").append(usableW).append(",1,0,C");
        zpl.append("^FD").append(texto).append("^FS\n");
    }

    /**
     * Emite el símbolo en la posición que le asignó el maquetado.
     *
     * La {@code escala} ya viene reducida si el símbolo no cabía: dibujarlo con la
     * configurada lo sacaría del recuadro. Antes se centraba usando un ancho que
     * podía exceder el de la etiqueta y solo se acotaba por la izquierda, así que
     * por la derecha se salía sin que nada lo advirtiera.
     */
    private void appendCodigo(StringBuilder zpl, int x, int y,
                               TipoCodigo tipo, int modulo, int escala, String data) {
        // ^BY fija el ancho de la barra angosta. Solo tiene efecto en los codigos
        // lineales; en DataMatrix y QR el tamano va en el propio comando.
        if (tipo == TipoCodigo.CODE_128) {
            zpl.append("^BY").append(Math.max(1, escala)).append("\n");
        }

        zpl.append("^FO").append(x).append(",").append(y);
        switch (tipo) {
            case DATAMATRIX -> zpl.append("^BXN,").append(escala).append(",200");
            case CODE_128 -> zpl.append("^BCN,").append(modulo * 10).append(",Y,N,N");
            case QR_CODE -> zpl.append("^BQN,2,").append(escala);
        }
        zpl.append("^FD").append(data).append("^FS\n");
    }

    private String sanitizar(String texto) {
        if (texto == null) return "";
        String normalized = Normalizer.normalize(texto, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                         .replaceAll("[^\\x20-\\x7E]", "");
    }

    private String truncar(String texto, int maxLen) {
        if (texto == null) return "";
        return texto.length() <= maxLen ? texto : texto.substring(0, maxLen - 1) + ".";
    }

    private String construirNombrePaciente(Paciente p) {
        if (p == null || p.getPersona() == null) return "";
        var per = p.getPersona();
        String nombre = per.getNombre() != null ? per.getNombre() : "";
        String ap = per.getApellidoPaterno() != null ? per.getApellidoPaterno() : "";
        String am = per.getApellidoMaterno() != null ? per.getApellidoMaterno() : "";
        return (nombre + " " + ap + " " + am).trim();
    }

    // ── Datos estructurados para impresión por navegador ────────────────────

    public LabelDataDTO extraerDatosMuestra(Muestra muestra) {
        String etiqueta = muestra.getEtiqueta();
        String nombre = truncar(construirNombrePaciente(muestra.getPaciente()), 24);
        return LabelDataDTO.builder()
                .id(muestra.getId())
                .etiqueta(etiqueta)
                .nombre(nombre)
                .codigoDatos(etiqueta)
                .build();
    }

    public List<LabelDataDTO> extraerDatosMuestras(List<Muestra> muestras) {
        return muestras.stream().map(this::extraerDatosMuestra).toList();
    }

    public LabelDataDTO extraerDatosDocumento(Documento documento, ConfiguracionEtiqueta config,
                                               boolean incluirEnlace) {
        String etiqueta = documento.getEtiqueta();
        String nombre = truncar(documento.getNombreOriginal(), 24);
        String codigoData = contenidoCodigoDocumento(documento, config, incluirEnlace);
        return LabelDataDTO.builder()
                .id(documento.getId())
                .etiqueta(etiqueta)
                .nombre(nombre)
                .codigoDatos(codigoData)
                .build();
    }
}
