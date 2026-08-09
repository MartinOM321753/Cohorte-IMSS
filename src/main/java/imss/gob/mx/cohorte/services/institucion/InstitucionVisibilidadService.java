package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import imss.gob.mx.cohorte.modules.institucion.VisibilidadInstitucionHija;
import imss.gob.mx.cohorte.modules.institucion.VisibilidadInstitucionHijaRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
// La del proyecto, no la de jakarta: el GlobalExceptionHandler solo mapea esta.
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Decide qué descendientes cuentan para el alcance de participantes.
 *
 * <p>Antes «hijas = siempre visibles» era una constante escrita dentro del
 * recorrido del árbol. Aquí se vuelve una decisión de cada institución, con dos
 * niveles: un valor por defecto en la propia institución y excepciones por hija.
 * La fila manda sobre el defecto; si no hay fila, manda el defecto.</p>
 *
 * <p>Tres propiedades que hay que sostener y que no son obvias:</p>
 * <ol>
 *   <li><b>Ocultar una hija oculta su rama entera.</b> Si se siguiera bajando por
 *       las nietas se vería a la nieta sin ver a su madre, y el árbol de Cobertura
 *       quedaría con agujeros en mitad de la jerarquía.</li>
 *   <li><b>Corta ver y corta registrar</b>, porque dar de alta un participante en
 *       una sede que no ves lo hace invisible desde el primer segundo. De eso se
 *       encarga {@code InstitucionRegistroService}, que parte de este recorrido.</li>
 *   <li><b>No corta administrar.</b> Usuarios, catálogos, módulos e instituciones
 *       siguen yendo por parentesco puro ({@code InstitucionArbolService}). Si
 *       ocultar una hija te la quitara también de la pantalla de instituciones, no
 *       habría forma de volver a mostrarla.</li>
 * </ol>
 *
 * <p>Lo que la institución ya le registró a esos participantes no se pierde: cae
 * en el modo de solo consulta, igual que cuando se revoca un permiso — ver
 * {@code ParticipanteAccesoService}.</p>
 */
@Service
@RequiredArgsConstructor
public class InstitucionVisibilidadService {

    private final InstitucionRepository institucionRepository;
    private final VisibilidadInstitucionHijaRepository visibilidadRepository;
    private final InstitucionArbolService arbol;
    private final InstitucionContextService institucionContextService;

    /**
     * Descendientes que {@code idPadre} ha decidido seguir viendo, a cualquier
     * profundidad. No incluye a la propia institución.
     */
    @Transactional(readOnly = true)
    public Set<Long> descendientesVisibles(Long idPadre) {
        Institucion padre = institucionRepository.findById(idPadre).orElse(null);
        if (padre == null) return Set.of();

        Set<Long> acumulador = new LinkedHashSet<>();
        Set<Long> visitados = new HashSet<>();
        visitados.add(idPadre);
        recorrer(padre, acumulador, visitados);
        return acumulador;
    }

    /** ¿Este padre ve hoy los participantes de esta hija directa? */
    @Transactional(readOnly = true)
    public boolean veAHija(Long idPadre, Long idHija) {
        return visibilidadRepository
                .findByInstitucionPadre_IdAndInstitucionHija_Id(idPadre, idHija)
                .map(VisibilidadInstitucionHija::getVerParticipantes)
                .orElseGet(() -> defectoDe(idPadre));
    }

    /**
     * Estado de todas las hijas directas, resolviendo ya el defecto. Alimenta la
     * pantalla, que necesita mostrar el interruptor de cada una sin repetir la
     * regla de precedencia en el cliente.
     */
    @Transactional(readOnly = true)
    public List<EstadoHija> estadoDeHijas(Long idPadre) {
        boolean defecto = defectoDe(idPadre);
        Map<Long, VisibilidadInstitucionHija> excepciones = new HashMap<>();
        for (VisibilidadInstitucionHija v : visibilidadRepository.findAllByInstitucionPadre_Id(idPadre)) {
            excepciones.put(v.getInstitucionHija().getId(), v);
        }

        List<EstadoHija> resultado = new ArrayList<>();
        for (Institucion hija : arbol.hijasDirectas(idPadre)) {
            VisibilidadInstitucionHija v = excepciones.get(hija.getId());
            resultado.add(new EstadoHija(
                    hija.getId(),
                    hija.getNombre(),
                    v != null ? v.getVerParticipantes() : defecto,
                    v != null,
                    v != null ? v.getUsuarioUuid() : null,
                    v != null ? v.getFechaActualizacion() : null));
        }
        return resultado;
    }

    /** Una hija directa con su visibilidad ya resuelta. */
    public record EstadoHija(Long idHija, String nombre, boolean verParticipantes,
                             boolean decisionExplicita, String usuarioUuid,
                             java.sql.Timestamp fechaActualizacion) {}

    // ── Administración ───────────────────────────────────────────────────────

    /**
     * Fija si un padre ve los participantes de una hija concreta.
     *
     * <p>Exige que sea hija <em>directa</em>: la visibilidad de una nieta la decide
     * su propia madre. Permitir saltos convertiría el árbol en un grafo de
     * excepciones cruzadas donde nadie sabría qué regla ganó.</p>
     */
    @Transactional
    public VisibilidadInstitucionHija fijarVisibilidad(Long idPadre, Long idHija,
                                                      boolean verParticipantes, String usuarioUuid) {
        verificarPuedeDecidirPor(idPadre);
        Institucion padre = institucionRepository.findById(idPadre)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución padre"));
        Institucion hija = institucionRepository.findById(idHija)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución hija"));

        if (hija.getInstitucionPadre() == null
                || !hija.getInstitucionPadre().getId().equals(idPadre)) {
            throw new ValidationException(
                    "Solo se puede decidir la visibilidad de una institución hija directa");
        }

        VisibilidadInstitucionHija registro = visibilidadRepository
                .findByInstitucionPadre_IdAndInstitucionHija_Id(idPadre, idHija)
                .orElseGet(() -> {
                    VisibilidadInstitucionHija nuevo = new VisibilidadInstitucionHija();
                    nuevo.setInstitucionPadre(padre);
                    nuevo.setInstitucionHija(hija);
                    return nuevo;
                });

        registro.setVerParticipantes(verParticipantes);
        registro.setUsuarioUuid(usuarioUuid);
        return visibilidadRepository.save(registro);
    }

    /**
     * Cambia el valor por defecto de una institución y arrastra a sus hijas
     * directas.
     *
     * <p>Se borran las excepciones en lugar de reescribirlas: quien apaga el
     * interruptor general está diciendo «ninguna», y dejar excepciones vivas haría
     * que una hija reapareciera sin motivo aparente. Además, así las sedes que se
     * creen después heredan la decisión.</p>
     */
    @Transactional
    public void fijarDefecto(Long idPadre, boolean verParticipantes) {
        verificarPuedeDecidirPor(idPadre);
        Institucion padre = institucionRepository.findById(idPadre)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la institución"));
        padre.setVerParticipantesHijas(verParticipantes);
        institucionRepository.save(padre);
        visibilidadRepository.deleteAll(visibilidadRepository.findAllByInstitucionPadre_Id(idPadre));
    }

    // ── Interno ──────────────────────────────────────────────────────────────

    /**
     * Quién decide qué ve una institución: ella misma o una ancestra. El endpoint
     * recibe el id del padre por la URL, así que sin esta comprobación bastaría
     * cambiar un número para dejar ciega a una sede ajena.
     */
    private void verificarPuedeDecidirPor(Long idPadre) {
        Long idActual = institucionContextService.getIdInstitucionActual();
        if (idActual.equals(idPadre)) return;
        if (arbol.esAncestra(idActual, idPadre)) return;
        throw new AccessDeniedException(
                "Solo la propia institución o una superior puede decidir qué ve");
    }

    private boolean defectoDe(Long idInstitucion) {
        return institucionRepository.findById(idInstitucion)
                .map(i -> !Boolean.FALSE.equals(i.getVerParticipantesHijas()))
                .orElse(true);
    }

    /**
     * Baja por el árbol saltándose las ramas ocultas.
     *
     * <p>Las visitadas van aparte del resultado y arrancan con la institución de
     * partida: un ciclo en la self-FK —que la base de datos no impide— colgaría el
     * servidor, y sin sembrar la raíz la devolvería como descendiente de sí misma.</p>
     */
    private void recorrer(Institucion padre, Set<Long> acumulador, Set<Long> visitados) {
        // Se recibe la entidad, no el id: el valor por defecto está en la propia
        // institución y bajar por ids obligaba a releerla en cada nivel. Este
        // recorrido corre en casi todas las peticiones.
        boolean defecto = !Boolean.FALSE.equals(padre.getVerParticipantesHijas());

        Set<Long> ocultas = new HashSet<>();
        Set<Long> visiblesExplicitas = new HashSet<>();
        for (VisibilidadInstitucionHija v : visibilidadRepository.findAllByInstitucionPadre_Id(padre.getId())) {
            if (Boolean.FALSE.equals(v.getVerParticipantes())) ocultas.add(v.getInstitucionHija().getId());
            else visiblesExplicitas.add(v.getInstitucionHija().getId());
        }

        for (Institucion hija : arbol.hijasDirectas(padre.getId())) {
            Long idHija = hija.getId();
            boolean visible = visiblesExplicitas.contains(idHija)
                    || (!ocultas.contains(idHija) && defecto);
            if (!visible) continue;          // cortar la hija corta su rama entera
            if (!visitados.add(idHija)) continue;
            acumulador.add(idHija);
            recorrer(hija, acumulador, visitados);
        }
    }
}
