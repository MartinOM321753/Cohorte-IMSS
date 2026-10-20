package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.importacion.LectorArchivoTabular;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Recorre el volcado en segundo plano, un participante por vez.
 *
 * <h3>Qué cambia respecto de hacerlo en la petición</h3>
 *
 * <p>Nada de lo que se interpreta ni de lo que se guarda: es el mismo análisis y la
 * misma escritura. Lo que cambia es quién espera. La petición que sube el archivo
 * contesta en cuanto la carga queda apuntada, y esto sigue trabajando después: con
 * cientos de participantes —dos mil respuestas cada uno— no hay navegador ni proxy
 * que aguante la espera, y una petición que muere por tiempo de espera mientras el
 * servidor sigue escribiendo es lo peor de los dos mundos: el usuario cree que falló
 * y los datos entran.</p>
 *
 * <h3>Lo que se pierde, dicho claro</h3>
 *
 * <p>Los errores del archivo entero —que no se pueda leer, que no traiga la columna
 * del folio— ya no se contestan en el mismo momento: quedan en la tarea como motivo
 * del fallo. En la práctica no se descubren aquí, porque la previsualización es el
 * paso anterior y es la que los enseña antes de que nadie confirme.</p>
 */
@Service
@AllArgsConstructor
@Slf4j
public class CargaVolcadoWorker {

    private final LectorArchivoTabular lector;
    private final CargaVolcadoService volcado;
    private final RegistroTareaVolcado registro;

    /**
     * @param contenido   el archivo ya leído a memoria. Viaja así porque el
     *                    {@code MultipartFile} no sobrevive a la petición: el
     *                    contenedor borra el temporal en cuanto contesta.
     * @param tipo        el instrumento ya resuelto, no su id. Aquí no hay usuario
     *                    autenticado, y buscarlo por id acabaría preguntándole al
     *                    contexto de seguridad si el instrumento es suyo: eso ya se
     *                    comprobó al encolar, con el usuario delante.
     * @param institucion y {@code usuario}, explícitos por lo mismo.
     */
    @Async("cargasExecutor")
    public void procesar(Long idTarea, byte[] contenido, String nombreArchivo,
                         TipoCuestionario tipo, boolean sobrescribir,
                         Institucion institucion, BeanUser usuario) {
        CargaVolcadoService.Analisis a;
        try {
            a = volcado.preparar(lector.leerHojas(contenido, CargaVolcadoService.topes()),
                    tipo, institucion.getId());
            // Los problemas de celda son para la previsualización. Juntarlos aquí serían
            // millones de objetos vivos que nadie va a leer.
            a.acumularProblemas = false;
        } catch (Exception e) {
            log.error("No se pudo leer el volcado \"{}\" de la tarea {}: {}",
                    nombreArchivo, idTarea, e.getMessage(), e);
            registro.fallar(idTarea, mensajeDe(e));
            return;
        }

        int total = a.tabla.totalFilas();
        registro.totalDeFilas(idTarea, total);

        for (int f = 0; f < total; f++) {
            try {
                registro.anotar(idTarea, volcado.guardarFila(
                        a, f, sobrescribir, nombreArchivo, institucion, usuario));
            } catch (Exception e) {
                // Una fila que falla no para la carga. Su transacción se deshizo sola,
                // así que lo que queda es un participante sin cargar —no uno a medias—
                // y eso se puede repetir; tumbar las demás filas no arreglaría nada.
                log.warn("La fila {} del volcado \"{}\" no se pudo guardar: {}",
                        f + 1, nombreArchivo, e.getMessage(), e);
                registro.anotar(idTarea, CargaVolcadoService.ResultadoFila.omitida(
                        numeroDeFila(a, f), etiquetaDe(a, f),
                        "No se pudo guardar: " + mensajeDe(e)));
            }
        }

        registro.terminar(idTarea);
        log.info("Volcado \"{}\" (tarea {}): {} fila(s) recorrida(s)",
                nombreArchivo, idTarea, total);
    }

    /** El número de fila tal como lo ve quien abre el archivo, si se puede saber. */
    private int numeroDeFila(CargaVolcadoService.Analisis a, int indice) {
        try {
            return a.tabla.numerosDeFila().get(indice);
        } catch (RuntimeException e) {
            return indice + 1;
        }
    }

    private String etiquetaDe(CargaVolcadoService.Analisis a, int indice) {
        try {
            return a.tabla.filas().get(indice).get(a.columnaEtiqueta);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * El mensaje que se le va a enseñar a una persona.
     *
     * <p>Un {@code NullPointerException} sin mensaje dejaría la tarea diciendo
     * «null», que no informa de nada; el nombre de la excepción al menos dice por
     * dónde buscar en la bitácora.</p>
     */
    private String mensajeDe(Exception e) {
        return e.getMessage() == null || e.getMessage().isBlank()
                ? e.getClass().getSimpleName() : e.getMessage();
    }
}
