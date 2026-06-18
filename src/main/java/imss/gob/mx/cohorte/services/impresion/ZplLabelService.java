package imss.gob.mx.cohorte.services.impresion;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.DisposicionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.TipoCodigo;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;

@Service
public class ZplLabelService {

    public String generarZplMuestra(Muestra muestra, ConfiguracionEtiqueta config) {
        return generarZplLote(List.of(muestra), config);
    }

    public String generarZplLoteCompleto(Muestra padre, List<Muestra> alicuotas, ConfiguracionEtiqueta config) {
        List<Muestra> todas = new ArrayList<>();
        todas.add(padre);
        todas.addAll(alicuotas);
        return generarZplLote(todas, config);
    }

    public String generarZplLote(List<Muestra> muestras, ConfiguracionEtiqueta config) {
        int perRow = config.getEtiquetasPorFila();
        StringBuilder zpl = new StringBuilder();
        for (int i = 0; i < muestras.size(); i += perRow) {
            int end = Math.min(i + perRow, muestras.size());
            zpl.append(generarZplFila(muestras.subList(i, end), config));
        }
        return zpl.toString();
    }

    private String generarZplFila(List<Muestra> fila, ConfiguracionEtiqueta config) {
        int labelW = config.getAnchoDots();
        int labelH = config.getAltoDots();
        int topMargin = config.getMargenSuperiorDots();
        int perRow = config.getEtiquetasPorFila();
        int totalW = perRow * labelW;

        StringBuilder zpl = new StringBuilder();
        zpl.append("^XA\n");
        zpl.append("^CI28\n");
        zpl.append("^PW").append(totalW).append("^LL").append(labelH).append("\n");

        for (int i = 0; i < fila.size(); i++) {
            int xBase = i * labelW;
            appendLabelContent(zpl, fila.get(i), xBase, labelW, topMargin, config);
        }

        zpl.append("^XZ\n");
        return zpl.toString();
    }

    private void appendLabelContent(StringBuilder zpl, Muestra muestra,
                                     int xBase, int labelW, int topMargin,
                                     ConfiguracionEtiqueta config) {
        int mx = config.getMargenIzquierdoDots();
        int usableW = labelW - mx * 2;

        String etiqueta = sanitizar(muestra.getEtiqueta());
        String nombre = truncar(sanitizar(construirNombrePaciente(muestra.getPaciente())), 24);

        int fontNombre = config.getTamanoFuenteNombre();
        int fontEtiqueta = config.getTamanoFuenteEtiqueta();
        TipoCodigo tipoCodigo = config.getTipoCodigo();
        int moduloCodigo = config.getModuloCodigo();
        DisposicionEtiqueta disposicion = config.getDisposicion();
        boolean showNombre = Boolean.TRUE.equals(config.getMostrarNombre());
        boolean showCodigo = Boolean.TRUE.equals(config.getMostrarCodigo());
        boolean showEtiqueta = Boolean.TRUE.equals(config.getMostrarEtiqueta());
        int gapNombre = config.getEspaciadoNombre() != null ? config.getEspaciadoNombre() : 4;
        int gapCodigo = config.getEspaciadoCodigo() != null ? config.getEspaciadoCodigo() : 10;
        int gapEtiqueta = config.getEspaciadoEtiqueta() != null ? config.getEspaciadoEtiqueta() : 4;

        int dmDots = estimarTamanoCodigo(tipoCodigo, moduloCodigo, etiqueta.length());

        int y = topMargin;
        List<ElementoEtiqueta> elementos = obtenerOrdenElementos(disposicion);

        for (ElementoEtiqueta elem : elementos) {
            switch (elem) {
                case NOMBRE:
                    if (showNombre) {
                        appendTexto(zpl, xBase + mx, y, fontNombre, usableW, nombre);
                        y += fontNombre + gapNombre;
                    }
                    break;
                case CODIGO:
                    if (showCodigo) {
                        appendCodigo(zpl, xBase, y, tipoCodigo, moduloCodigo, etiqueta, labelW, mx, dmDots);
                        y += dmDots + gapCodigo;
                    }
                    break;
                case ETIQUETA:
                    if (showEtiqueta) {
                        appendTexto(zpl, xBase + mx, y, fontEtiqueta, usableW, etiqueta);
                        y += fontEtiqueta + gapEtiqueta;
                    }
                    break;
            }
        }
    }

    private enum ElementoEtiqueta { NOMBRE, CODIGO, ETIQUETA }

    private List<ElementoEtiqueta> obtenerOrdenElementos(DisposicionEtiqueta disposicion) {
        switch (disposicion) {
            case NOMBRE_CODIGO_ETIQUETA:
                return List.of(ElementoEtiqueta.NOMBRE, ElementoEtiqueta.CODIGO, ElementoEtiqueta.ETIQUETA);
            case CODIGO_NOMBRE_ETIQUETA:
                return List.of(ElementoEtiqueta.CODIGO, ElementoEtiqueta.NOMBRE, ElementoEtiqueta.ETIQUETA);
            case CODIGO_ETIQUETA:
                return List.of(ElementoEtiqueta.CODIGO, ElementoEtiqueta.ETIQUETA);
            case NOMBRE_ETIQUETA_CODIGO:
                return List.of(ElementoEtiqueta.NOMBRE, ElementoEtiqueta.ETIQUETA, ElementoEtiqueta.CODIGO);
            default:
                return List.of(ElementoEtiqueta.NOMBRE, ElementoEtiqueta.CODIGO, ElementoEtiqueta.ETIQUETA);
        }
    }

    private void appendTexto(StringBuilder zpl, int x, int y, int fontSize, int usableW, String texto) {
        zpl.append("^FO").append(x).append(",").append(y);
        zpl.append("^A0N,").append(fontSize).append(",").append(fontSize);
        zpl.append("^FB").append(usableW).append(",1,0,C");
        zpl.append("^FD").append(texto).append("^FS\n");
    }

    private void appendCodigo(StringBuilder zpl, int xBase, int y,
                               TipoCodigo tipo, int modulo, String data,
                               int labelW, int mx, int dmDots) {
        int usableW = labelW - mx * 2;
        int codeX = xBase + mx + Math.max(0, (usableW - dmDots) / 2);

        zpl.append("^FO").append(codeX).append(",").append(y);
        switch (tipo) {
            case DATAMATRIX:
                zpl.append("^BXN,").append(modulo).append(",200");
                break;
            case CODE_128:
                zpl.append("^BCN,").append(modulo * 10).append(",Y,N,N");
                break;
            case QR_CODE:
                zpl.append("^BQN,2,").append(modulo);
                break;
        }
        zpl.append("^FD").append(data).append("^FS\n");
    }

    private int estimarTamanoCodigo(TipoCodigo tipo, int modulo, int dataLen) {
        switch (tipo) {
            case DATAMATRIX:
                int cells = (int) Math.ceil(Math.sqrt(dataLen * 8.0));
                return Math.max(cells * modulo, modulo * 12);
            case CODE_128:
                return (11 * dataLen + 35) * modulo;
            case QR_CODE:
                int qrCells = (int) Math.ceil(Math.sqrt(dataLen * 10.0));
                return Math.max(qrCells * modulo, modulo * 15);
            default:
                return 100;
        }
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
}
