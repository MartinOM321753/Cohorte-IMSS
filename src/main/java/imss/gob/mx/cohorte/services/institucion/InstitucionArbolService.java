package imss.gob.mx.cohorte.services.institucion;

import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Operaciones puras sobre el árbol de instituciones: quién desciende de quién.
 *
 * <p>Existe porque la misma pregunta se estaba respondiendo en tres sitios a la
 * vez —{@code InstitucionJerarquiaService}, {@code InstitucionRegistroService} y
 * {@code InstitucionContextService} tenían cada uno su propia copia de
 * {@code esAncestra}— y el recorrido de descendientes vivía en un cuarto. Tres
 * copias de una regla son tres sitios donde cambiarla y uno que se olvida.</p>
 *
 * <p>No depende de nada salvo del repositorio, a propósito: así pueden colgar de
 * él tanto los servicios de negocio como los de seguridad sin ciclos.</p>
 *
 * <p>Ojo con lo que este servicio NO sabe: responde por la forma del árbol, no
 * por lo que cada institución haya decidido ver. La visibilidad configurable de
 * las hijas vive en {@link InstitucionVisibilidadService}, encima de esto. Aquí
 * el parentesco es un hecho, allí es una decisión.</p>
 */
@Service
@RequiredArgsConstructor
public class InstitucionArbolService {

    private final InstitucionRepository institucionRepository;

    /** Hijas directas. */
    @Transactional(readOnly = true)
    public List<Institucion> hijasDirectas(Long idPadre) {
        return institucionRepository.findAllByInstitucionPadre_Id(idPadre);
    }

    /**
     * Todas las descendientes, a cualquier profundidad. Sin la institución de
     * partida.
     */
    @Transactional(readOnly = true)
    public Set<Long> descendientes(Long idPadre) {
        Set<Long> acumulador = new LinkedHashSet<>();
        Set<Long> visitados = new HashSet<>();
        visitados.add(idPadre);
        recorrerDescendientes(idPadre, acumulador, visitados);
        return acumulador;
    }

    /**
     * La institución y todas sus descendientes. Es el alcance <em>administrativo</em>:
     * lo que una sede puede gestionar por el hecho de estar por encima en el árbol,
     * con independencia de qué participantes decida ver.
     */
    @Transactional(readOnly = true)
    public Set<Long> subarbol(Long id) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(id);
        ids.addAll(descendientes(id));
        return ids;
    }

    /** ¿{@code idPosibleAncestra} está en la cadena de padres de {@code idHija}? */
    @Transactional(readOnly = true)
    public boolean esAncestra(Long idPosibleAncestra, Long idHija) {
        if (idPosibleAncestra == null || idHija == null) return false;
        Institucion cursor = institucionRepository.findById(idHija).orElse(null);
        if (cursor == null) return false;
        cursor = cursor.getInstitucionPadre();
        while (cursor != null) {
            if (cursor.getId().equals(idPosibleAncestra)) return true;
            cursor = cursor.getInstitucionPadre();
        }
        return false;
    }

    /**
     * El conjunto de visitadas va aparte del resultado y arranca con la institución
     * de partida. Dos motivos, y el segundo no es evidente: un ciclo en la self-FK
     * —que la base de datos no impide— colgaría el servidor con un bucle infinito,
     * y sin sembrar la raíz ese mismo ciclo la devolvería como descendiente de sí
     * misma.
     */
    private void recorrerDescendientes(Long idPadre, Set<Long> acumulador, Set<Long> visitados) {
        for (Institucion hija : institucionRepository.findAllByInstitucionPadre_Id(idPadre)) {
            if (!visitados.add(hija.getId())) continue;
            acumulador.add(hija.getId());
            recorrerDescendientes(hija.getId(), acumulador, visitados);
        }
    }
}
