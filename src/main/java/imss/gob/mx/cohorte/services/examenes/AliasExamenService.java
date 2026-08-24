package imss.gob.mx.cohorte.services.examenes;

import imss.gob.mx.cohorte.modules.examenes.AliasExamen;
import imss.gob.mx.cohorte.modules.examenes.AliasExamenRepository;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Alias con los que los instrumentos titulan la columna de un examen.
 *
 * <p>Gemelo de {@code AliasParametroEstudioService}, con un ambito distinto: en
 * examenes no hay plantilla que agrupe, cada examen es un analito suelto de una
 * institucion. Asi que la colision a evitar es entre examenes de la MISMA
 * institucion, no dentro de un tipo.</p>
 */
@Service
@AllArgsConstructor
public class AliasExamenService {

    private final AliasExamenRepository aliasRepository;

    @Transactional(readOnly = true)
    public List<AliasExamen> getByExamen(Long idExamen) {
        return aliasRepository.findAllByExamen_IdOrderByOrdenAsc(idExamen);
    }

    /**
     * Deja el examen exactamente con los alias indicados.
     *
     * <p>Mismas dos comprobaciones que en estudios: que la lista no traiga dos
     * alias que normalizan igual, y que ninguno este tomado por otro examen de la
     * institucion. La segunda es la que importa: dos examenes compartiendo alias
     * significa que una columna del archivo no sabria a cual resolverse, y el
     * resultado acabaria guardado en el analito equivocado.</p>
     */
    @Transactional
    public void reemplazarAlias(Examen examen, List<String> alias) {
        // Sobre la coleccion del padre, no por repositorio: con orphanRemoval, borrar
        // por detras deja la coleccion en memoria desincronizada de la base.
        examen.getAlias().clear();
        // Hibernate ejecuta los INSERT antes que los DELETE al vaciar la sesion.
        // Sin este flush, reeditar un alias existente chocaria contra el indice unico.
        aliasRepository.flush();

        if (alias == null || alias.isEmpty()) return;

        Long idInstitucion = examen.getInstitucion().getId();

        Map<String, String> porNormalizado = new LinkedHashMap<>();
        for (String bruto : alias) {
            String normalizado = NormalizadorAlias.normalizar(bruto);
            if (normalizado == null) continue;
            String previo = porNormalizado.putIfAbsent(normalizado, bruto.trim());
            if (previo != null) {
                throw new ObjConflictException(
                        "El alias \"" + bruto.trim() + "\" esta repetido: ya lo cubre \"" + previo
                                + "\". Se comparan sin acentos, mayusculas ni espacios de sobra.");
            }
        }

        int orden = 0;
        for (Map.Entry<String, String> e : porNormalizado.entrySet()) {
            Optional<AliasExamen> ajeno =
                    aliasRepository.findByIdInstitucionAndAliasNormalizado(idInstitucion, e.getKey());
            if (ajeno.isPresent() && !ajeno.get().getExamen().getId().equals(examen.getId())) {
                throw new ObjConflictException(
                        "El alias \"" + e.getValue() + "\" ya esta asignado al examen \""
                                + ajeno.get().getExamen().getParametro()
                                + "\" en esta institucion. Una columna del archivo solo puede "
                                + "corresponder a un examen.");
            }

            AliasExamen a = new AliasExamen();
            a.setExamen(examen);
            a.setAlias(e.getValue());
            a.setOrden(orden++);
            examen.getAlias().add(a);
        }
    }
}
