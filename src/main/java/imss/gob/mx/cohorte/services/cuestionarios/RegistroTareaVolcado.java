package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.carga.EstadoTareaVolcado;
import imss.gob.mx.cohorte.modules.cuestionarios.carga.FilaOmitidaVolcado;
import imss.gob.mx.cohorte.modules.cuestionarios.carga.TareaCargaVolcado;
import imss.gob.mx.cohorte.modules.cuestionarios.carga.TareaCargaVolcadoRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Lo que se escribe de una carga mientras corre.
 *
 * <h3>Por qué está aparte del trabajo</h3>
 *
 * <p>Cada método de aquí es una transacción propia y corta, y eso es el punto: el
 * avance tiene que quedar confirmado en la base en el momento, porque lo que lo
 * consulta es otra petición —la pantalla preguntando «¿ya acabó?»— y lo que no está
 * confirmado no se ve desde fuera. Si esto viviera dentro del método que recorre el
 * archivo, todo el avance se escribiría en una sola transacción que no se confirma
 * hasta el final: el progreso sería invisible justo mientras hace falta.</p>
 *
 * <p>Y por eso también está en su propia clase. Llamar a un método transactional de
 * la misma clase no abre transacción —la llamada no pasa por el proxy de Spring—, así
 * que juntarlo con el recorrido daría un código que parece correcto y no lo es.</p>
 */
@Service
@AllArgsConstructor
public class RegistroTareaVolcado {

    /** Hasta cuántas filas omitidas se guardan. Ver {@link #anotar}. */
    private static final int MAX_OMITIDAS_GUARDADAS = 500;

    private final TareaCargaVolcadoRepository repositorio;

    /**
     * Apunta la carga antes de que empiece.
     *
     * <p>Se confirma aquí, antes de lanzar el trabajo, porque el hilo que va a
     * trabajar busca la tarea por su número: si la creación aún no estuviera
     * confirmada, no la encontraría.</p>
     */
    @Transactional
    public TareaCargaVolcado crear(TipoCuestionario tipo, Institucion institucion,
                                   BeanUser usuario, String nombreArchivo,
                                   boolean sobrescribir) {
        TareaCargaVolcado t = new TareaCargaVolcado();
        t.setTipoCuestionario(tipo);
        t.setInstitucion(institucion);
        t.setUsuario(usuario);
        t.setNombreArchivo(nombreArchivo);
        t.setSobrescribir(sobrescribir);
        t.setEstado(EstadoTareaVolcado.EN_PROCESO);
        return repositorio.save(t);
    }

    /** Cuántas filas trae el archivo, en cuanto se sabe. Es el denominador del avance. */
    @Transactional
    public void totalDeFilas(Long idTarea, int total) {
        TareaCargaVolcado t = repositorio.findById(idTarea).orElse(null);
        if (t == null) return;
        t.setTotalFilas(total);
        repositorio.save(t);
    }

    /**
     * Suma lo que pasó con un participante.
     *
     * <p>Las filas omitidas se guardan hasta un tope. No es por espacio: pasado cierto
     * punto, una lista de miles de motivos iguales no se lee, y que una carga de un
     * archivo equivocado —donde ninguna fila cuadra— pueda escribir cinco mil renglones
     * de bitácora no le sirve a nadie. El recuento de procesadas sigue siendo exacto.</p>
     */
    @Transactional
    public void anotar(Long idTarea, CargaVolcadoService.ResultadoFila fila) {
        TareaCargaVolcado t = repositorio.findById(idTarea).orElse(null);
        if (t == null) return;

        t.setFilasProcesadas(t.getFilasProcesadas() + 1);
        if (fila.motivoOmision() != null) {
            if (t.getFilasOmitidas().size() < MAX_OMITIDAS_GUARDADAS) {
                t.getFilasOmitidas().add(new FilaOmitidaVolcado(t, fila.numeroFila(),
                        recortar(fila.etiqueta(), 60), recortar(fila.motivoOmision(), 500)));
            }
        } else {
            if (fila.creado()) t.setCuestionariosCreados(t.getCuestionariosCreados() + 1);
            t.setRespuestasGuardadas(t.getRespuestasGuardadas() + fila.respuestas());
            t.setPendientesDeLectura(t.getPendientesDeLectura() + fila.pendientes());
            t.setDiscrepanciasGuardadas(t.getDiscrepanciasGuardadas() + fila.discrepancias());
        }
        repositorio.save(t);
    }

    @Transactional
    public void terminar(Long idTarea) {
        TareaCargaVolcado t = repositorio.findById(idTarea).orElse(null);
        if (t == null) return;
        t.setEstado(EstadoTareaVolcado.TERMINADA);
        t.setFechaFin(LocalDateTime.now());
        repositorio.save(t);
    }

    /**
     * Da la carga por fallida.
     *
     * <p>Lo que se guardó antes del corte se queda: son cuestionarios completos de
     * participantes reales, y borrarlos para «dejarlo limpio» perdería dato bueno.
     * Las cifras dicen hasta dónde llegó, que es lo que hay que saber para repetirla.</p>
     */
    @Transactional
    public void fallar(Long idTarea, String motivo) {
        TareaCargaVolcado t = repositorio.findById(idTarea).orElse(null);
        if (t == null) return;
        t.setEstado(EstadoTareaVolcado.FALLIDA);
        t.setMensajeError(recortar(motivo, 1000));
        t.setFechaFin(LocalDateTime.now());
        repositorio.save(t);
    }

    private static String recortar(String texto, int largo) {
        if (texto == null) return null;
        return texto.length() <= largo ? texto : texto.substring(0, largo);
    }
}
