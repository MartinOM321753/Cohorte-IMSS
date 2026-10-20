package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.carga.EstadoTareaVolcado;
import imss.gob.mx.cohorte.modules.cuestionarios.carga.TareaCargaVolcado;
import imss.gob.mx.cohorte.modules.cuestionarios.carga.TareaCargaVolcadoRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.importacion.ArchivoInvalidoException;
import imss.gob.mx.cohorte.services.importacion.LimitesArchivo;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Encola la carga de un volcado y contesta en qué va.
 *
 * <h3>Por qué la carga se encola en lugar de hacerse</h3>
 *
 * <p>Cada fila de un volcado es un cuestionario completo: dos mil respuestas. Cien
 * participantes son doscientas mil inserciones, y eso tarda más de lo que una
 * petición HTTP puede estar abierta sin que el navegador o el proxy de enmedio la
 * corten. Cortada la petición, el servidor sigue escribiendo y quien subió el archivo
 * no sabe si se guardó: acaba subiéndolo otra vez por si acaso. Encolarla convierte
 * eso en una pregunta con respuesta —«va en la fila 340 de 800»— y de paso permite
 * subir archivos mucho más grandes de los que antes cabían.</p>
 *
 * <p>Esta clase no lleva {@code @Transactional} en {@link #encolar} y es
 * deliberado: la tarea tiene que quedar <em>confirmada</em> antes de lanzar el hilo
 * que la va a trabajar, porque ese hilo la busca por su número. Dentro de una
 * transacción abierta, el trabajo arrancaría buscando algo que todavía no existe para
 * nadie más.</p>
 */
@Service
@AllArgsConstructor
public class TareaVolcadoService {

    private final TareaCargaVolcadoRepository repositorio;
    private final RegistroTareaVolcado registro;
    private final CargaVolcadoWorker worker;
    private final TipoCuestionarioService tipos;
    private final InstitucionContextService contexto;

    /**
     * Apunta la carga y la lanza.
     *
     * @return la tarea recién creada, para que el cliente pueda preguntar por ella
     */
    public TareaCargaVolcado encolar(MultipartFile archivo, Long idTipo, boolean sobrescribir) {
        if (idTipo == null) {
            throw new ArchivoInvalidoException(
                    "Indica a que cuestionario pertenece el volcado antes de cargarlo.");
        }
        TipoCuestionario tipo = tipos.uno(idTipo);
        Institucion institucion = contexto.getInstitucionActual();
        BeanUser usuario = contexto.getUsuarioActual();

        // Una sola carga a la vez por instrumento. No es por la base —la llave única de
        // cuestionario ya impide duplicar a un participante— sino porque dos cargas del
        // mismo archivo a la vez se pisan: la segunda va informando «ya existe» de lo
        // que la primera acaba de escribir, y el informe queda ilegible. El caso real es
        // el doble clic, o el usuario que reintenta porque no ve avance.
        if (repositorio.existsByInstitucion_IdAndTipoCuestionario_IdAndEstado(
                institucion.getId(), idTipo, EstadoTareaVolcado.EN_PROCESO)) {
            throw new ObjConflictException("Ya hay una carga de " + tipo.getNombre()
                    + " en proceso. Espera a que termine para subir otro archivo.");
        }

        byte[] contenido = bytesDe(archivo);
        String nombre = archivo.getOriginalFilename();

        TareaCargaVolcado tarea = registro.crear(tipo, institucion, usuario, nombre, sobrescribir);
        // Al trabajo se le pasa el instrumento ya resuelto, no su id: en su hilo no hay
        // usuario autenticado, y buscarlo alli acabaria preguntando al contexto de
        // seguridad si es suyo. Eso es lo que se acaba de comprobar aqui.
        worker.procesar(tarea.getId(), contenido, nombre, tipo, sobrescribir,
                institucion, usuario);
        return tarea;
    }

    /** En qué va la carga. Es lo que la pantalla pregunta cada pocos segundos. */
    @Transactional(readOnly = true)
    public InformeCargaVolcado consultar(Long idTarea) {
        TareaCargaVolcado t = repositorio.conFilasOmitidas(idTarea)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la carga solicitada"));
        contexto.verificarPertenece(t.getInstitucion().getId());
        return InformeCargaVolcado.de(t);
    }

    /**
     * Las últimas cargas de la institución.
     *
     * <p>Hace falta porque la carga ya no vive en la pantalla que la lanzó: quien
     * cierra la pestaña —o se va a comer— tiene que poder volver y ver cómo acabó.</p>
     */
    @Transactional(readOnly = true)
    public List<InformeCargaVolcado> recientes() {
        return repositorio.findTop10ByInstitucion_IdOrderByFechaInicioDesc(
                        contexto.getIdInstitucionActual())
                .stream().map(InformeCargaVolcado::de).toList();
    }

    /**
     * El archivo a memoria.
     *
     * <p>Tiene que leerse aquí, en el hilo de la petición: el temporal que el
     * contenedor crea para el {@code MultipartFile} se borra en cuanto se contesta,
     * así que un hilo que lo abriera después no encontraría nada.</p>
     */
    private byte[] bytesDe(MultipartFile archivo) {
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
}
