package imss.gob.mx.cohorte.services.reportes;

/**
 * Dónde y de qué tamaño se dibuja una imagen dentro de su caja, en milímetros.
 *
 * <p>Existe porque el motor de PDF <b>no conoce {@code object-fit}</b> —no aparece
 * en ninguna parte de sus fuentes—. Con él, «entera» y «recortada» se veían bien en
 * el editor y en el papel salían estiradas, sin que nada avisara: la vista previa
 * mentía justo en lo que se había pedido comprobar.</p>
 *
 * <p>Calculando aquí el tamaño y la posición exactos, editor y documento dibujan lo
 * mismo con propiedades que ambos entienden: una caja con recorte y la imagen
 * colocada dentro. El gemelo en TypeScript vive en {@code types.ts}, y las dos
 * implementaciones tienen que decidir igual.</p>
 *
 * @param anchoMm     ancho con el que se pinta la imagen
 * @param altoMm      alto con el que se pinta
 * @param izquierdaMm desplazamiento desde el borde izquierdo de la caja
 * @param arribaMm    desplazamiento desde el borde superior de la caja
 */
public record EncuadreImagen(double anchoMm, double altoMm,
                             double izquierdaMm, double arribaMm) {

    /** Sin medidas de la imagen no hay proporción que respetar: se estira. */
    public static EncuadreImagen estirado(double cajaAnchoMm, double cajaAltoMm) {
        return new EncuadreImagen(cajaAnchoMm, cajaAltoMm, 0, 0);
    }

    /**
     * Resuelve el encuadre.
     *
     * @param ajuste  contener, cubrir, estirar o libre
     * @param zoom    solo en libre; 1 es el tamaño que tendría en contener
     * @param dxMm    solo en libre; cuánto se corre respecto al centro
     */
    public static EncuadreImagen de(double cajaAnchoMm, double cajaAltoMm,
                                    Integer anchoPx, Integer altoPx,
                                    String ajuste, double zoom, double dxMm, double dyMm) {
        String modo = ajuste == null || ajuste.isBlank() ? "contener" : ajuste;
        if ("estirar".equals(modo)) return estirado(cajaAnchoMm, cajaAltoMm);

        if (anchoPx == null || altoPx == null || anchoPx <= 0 || altoPx <= 0) {
            return estirado(cajaAnchoMm, cajaAltoMm);
        }

        double proporcion = (double) anchoPx / altoPx;

        // «Contener» toma el lado que se queda corto; «cubrir», el que se pasa.
        double cabeAncho = cajaAnchoMm;
        double cabeAlto = cajaAltoMm * proporcion;
        double base = "cubrir".equals(modo)
                ? Math.max(cabeAncho, cabeAlto)
                : Math.min(cabeAncho, cabeAlto);

        boolean libre = "libre".equals(modo);
        double factor = libre ? Math.max(0.05, zoom) : 1;
        double ancho = base * factor;
        double alto = ancho / proporcion;

        double dx = libre ? dxMm : 0;
        double dy = libre ? dyMm : 0;

        return new EncuadreImagen(ancho, alto,
                (cajaAnchoMm - ancho) / 2 + dx,
                (cajaAltoMm - alto) / 2 + dy);
    }
}
