package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.parametros.AliasParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.AliasParametroEstudioRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
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
 * Administra los alias con los que los instrumentos titulan las columnas de un
 * parametro.
 *
 * <p>Sigue la misma semantica que las opciones de un parametro: la lista que
 * llega reemplaza a la que habia. Es lo que espera el formulario del catalogo,
 * donde el usuario edita el conjunto completo y guarda.</p>
 */
@Service
@AllArgsConstructor
public class AliasParametroEstudioService {

    private final AliasParametroEstudioRepository aliasRepository;

    @Transactional(readOnly = true)
    public List<AliasParametroEstudio> getByParametro(Long idParametro) {
        return aliasRepository.findAllByParametro_IdOrderByOrdenAsc(idParametro);
    }

    /**
     * Deja el parametro exactamente con los alias indicados.
     *
     * <p>Antes de tocar la base se comprueban dos cosas que el indice unico
     * tambien impide, pero que conviene atajar aqui para poder explicar el
     * motivo en lugar de devolver un error de restriccion:</p>
     *
     * <ol>
     *   <li>que la propia lista no traiga dos alias que normalizan igual
     *       —"Sistolica" y "SISTOLICA" son el mismo—;</li>
     *   <li>que ninguno este ya tomado por OTRO parametro del mismo tipo de
     *       estudio, porque entonces una columna del archivo no sabria a cual
     *       de los dos resolverse.</li>
     * </ol>
     */
    @Transactional
    public void reemplazarAlias(ParametroEstudio parametro, List<String> alias) {
        // Se trabaja sobre la coleccion del padre y no con deleteAllBy..., para que
        // Hibernate gestione el ciclo de vida (cascade + orphanRemoval) sin que la
        // coleccion en memoria quede desincronizada de la base. Es el mismo patron
        // que OpcionParametroService.
        parametro.getAlias().clear();
        // El flush no es cosmetico: al vaciar la sesion, Hibernate ejecuta los
        // INSERT antes que los DELETE. Sin forzarlo aqui, volver a guardar un
        // alias que ya existia chocaria contra el indice unico —el borrado aun
        // no habria llegado a la base— y la edicion fallaria sin motivo visible.
        aliasRepository.flush();

        if (alias == null || alias.isEmpty()) return;

        Long idTipo = parametro.getTipoEstudio().getId();

        // LinkedHashMap: conserva el orden en que el usuario los escribio y a la
        // vez descarta los repetidos, quedandose con la primera grafia.
        Map<String, String> porNormalizado = new LinkedHashMap<>();
        for (String bruto : alias) {
            String normalizado = NormalizadorAlias.normalizar(bruto);
            if (normalizado == null) continue;   // vacios: el formulario deja filas en blanco
            String previo = porNormalizado.putIfAbsent(normalizado, bruto.trim());
            if (previo != null) {
                throw new ObjConflictException(
                        "El alias \"" + bruto.trim() + "\" esta repetido: ya lo cubre \"" + previo
                                + "\". Se comparan sin acentos, mayusculas ni espacios de sobra.");
            }
        }

        int orden = 0;
        for (Map.Entry<String, String> e : porNormalizado.entrySet()) {
            Optional<AliasParametroEstudio> ajeno =
                    aliasRepository.findByIdTipoEstudioAndAliasNormalizado(idTipo, e.getKey());
            if (ajeno.isPresent() && !ajeno.get().getParametro().getId().equals(parametro.getId())) {
                throw new ObjConflictException(
                        "El alias \"" + e.getValue() + "\" ya esta asignado al parametro \""
                                + ajeno.get().getParametro().getNombre()
                                + "\" dentro de este tipo de estudio. Una columna del archivo solo "
                                + "puede corresponder a un parametro.");
            }

            AliasParametroEstudio a = new AliasParametroEstudio();
            a.setParametro(parametro);
            a.setAlias(e.getValue());
            a.setOrden(orden++);
            parametro.getAlias().add(a);
        }
    }
}
