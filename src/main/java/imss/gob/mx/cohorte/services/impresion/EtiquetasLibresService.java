package imss.gob.mx.cohorte.services.impresion;

import imss.gob.mx.cohorte.controllers.impresion.dto.TablaEtiquetasDTO;
import imss.gob.mx.cohorte.services.importacion.ArchivoInvalidoException;
import imss.gob.mx.cohorte.services.importacion.LectorArchivoTabular;
import imss.gob.mx.cohorte.services.importacion.TablaLeida;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Prepara un archivo externo para imprimirse como etiquetas.
 *
 * <p>Es deliberadamente delgado. Todo el trabajo duro —los topes, la bomba ZIP,
 * el archivo disfrazado, las filas desalineadas— ya lo hace
 * {@link LectorArchivoTabular}, y el maquetado lo hace la pantalla. Aqui solo se
 * elige el modo de lectura correcto y se traducen a avisos las cosas que el
 * usuario deberia mirar antes de gastar hojas.</p>
 *
 * <p>No escribe nada ni consulta nada: el lote no se guarda. Si hay que
 * reimprimir, se vuelve a subir el archivo.</p>
 */
@Service
@RequiredArgsConstructor
public class EtiquetasLibresService {

    private final LectorArchivoTabular lector;

    /**
     * A partir de cuantas filas conviene advertir.
     *
     * <p>El tope del lector son 5 000 filas, pensado para exportes de instrumento.
     * En etiquetas ese numero significa algo muy distinto: sobre una hoja de 80
     * casillas son 62 paginas. Este umbral no impide nada, solo evita que alguien
     * mande a imprimir un archivo entero creyendo que eran unas cuantas.</p>
     */
    private static final int FILAS_PARA_ADVERTIR = 200;

    public TablaEtiquetasDTO leer(MultipartFile archivo) {
        LectorArchivoTabular.TablaConAvisos leido = lector.leerComoSeVe(archivo);
        TablaLeida tabla = leido.tabla();

        if (tabla.vacia()) {
            throw new ArchivoInvalidoException(
                    "El archivo tiene encabezados pero ninguna fila con datos. "
                            + "Cada fila del archivo es una etiqueta.");
        }

        List<String> avisos = new ArrayList<>();

        if (tabla.totalFilas() >= FILAS_PARA_ADVERTIR) {
            avisos.add("El archivo trae " + tabla.totalFilas()
                    + " filas, y cada una es una etiqueta. Revisa cuantas hojas son antes de imprimir.");
        }

        if (!leido.celdasDerivadas().isEmpty()) {
            avisos.add(descripcionDerivadas(leido.celdasDerivadas()));
        }

        return new TablaEtiquetasDTO(
                tabla.encabezados(),
                tabla.filas(),
                tabla.numerosDeFila(),
                avisos);
    }

    /**
     * Aviso de formulas, nombrando celdas concretas.
     *
     * <p>Se listan las primeras y se dice cuantas quedaron fuera: "hay formulas"
     * a secas obliga a buscarlas a mano por todo el archivo, y una lista de
     * doscientas referencias no se lee.</p>
     */
    private String descripcionDerivadas(List<LectorArchivoTabular.CeldaDerivada> derivadas) {
        int aMostrar = Math.min(5, derivadas.size());
        StringBuilder sb = new StringBuilder("Se imprimira el ultimo resultado guardado de ");
        sb.append(derivadas.size() == 1 ? "una celda con formula (" : derivadas.size() + " celdas con formula (");
        for (int i = 0; i < aMostrar; i++) {
            if (i > 0) sb.append(", ");
            sb.append(derivadas.get(i).referencia());
        }
        if (derivadas.size() > aMostrar) {
            sb.append(" y ").append(derivadas.size() - aMostrar).append(" mas");
        }
        sb.append("). Si el archivo se edito sin recalcular, ese valor puede no corresponder a los datos actuales.");
        return sb.toString();
    }
}
