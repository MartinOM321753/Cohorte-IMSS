package imss.gob.mx.cohorte.services.almacenamiento.muestra;

import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ParametroEstudioMuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.estudios.ResultadoEstudioMuestraRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ParametroEstudioMuestraService {

    private final ParametroEstudioMuestraRepository repository;
    private final ResultadoEstudioMuestraRepository resultadoEstudioMuestraRepository;
    private final TipoEstudioMuestraService tipoEstudioMuestraService;
    private final InstitucionContextService institucionContextService;

    @Transactional(readOnly = true)
    public List<ParametroEstudioMuestra> getByTipo(Long idTipo) {
        tipoEstudioMuestraService.getById(idTipo);   // valida institución
        return repository.findAllByTipoEstudioMuestra_IdOrderByOrdenAscIdAsc(idTipo);
    }

    @Transactional(readOnly = true)
    public ParametroEstudioMuestra getById(Long id) {
        ParametroEstudioMuestra parametro = repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró el parámetro de estudio de muestra"));
        // Un parámetro no guarda institución: la hereda del tipo del que cuelga.
        institucionContextService.verificarPertenece(
                parametro.getTipoEstudioMuestra().getInstitucion());
        return parametro;
    }

    @Transactional(rollbackFor = Exception.class)
    public ParametroEstudioMuestra create(ParametroEstudioMuestra parametro) {
        repository.findByTipoEstudioMuestra_IdAndNombreIgnoreCase(
                parametro.getTipoEstudioMuestra().getId(), parametro.getNombre()
        ).ifPresent(p -> {
            throw new ObjConflictException("Ya existe un parámetro con ese nombre en este tipo de estudio");
        });
        // Al final, igual que en el catálogo de estudios: es donde el
        // administrador espera encontrar lo que acaba de dar de alta.
        parametro.setOrden(siguienteOrden(parametro.getTipoEstudioMuestra().getId()));
        return repository.save(parametro);
    }

    private int siguienteOrden(Long idTipo) {
        return repository.findAllByTipoEstudioMuestra_Id(idTipo).stream()
                .map(ParametroEstudioMuestra::getOrden)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .map(max -> max + 1)
                .orElse(0);
    }

    /**
     * Coloca los parámetros del tipo en el orden que describe {@code idsEnOrden}.
     * Mismo contrato que en el catálogo de estudios: la lista llega completa y se
     * escribe entera, o no se escribe nada.
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ParametroEstudioMuestra> reordenar(Long idTipo, List<Long> idsEnOrden) {
        tipoEstudioMuestraService.getById(idTipo);   // valida institución

        List<ParametroEstudioMuestra> parametros = repository.findAllByTipoEstudioMuestra_Id(idTipo);
        java.util.Map<Long, ParametroEstudioMuestra> porId = parametros.stream()
                .collect(java.util.stream.Collectors.toMap(ParametroEstudioMuestra::getId, p -> p));

        java.util.Set<Long> recibidos = new java.util.LinkedHashSet<>(idsEnOrden);
        if (recibidos.size() != idsEnOrden.size()) {
            throw new ObjConflictException("La lista de orden trae parámetros repetidos");
        }
        if (!recibidos.equals(porId.keySet())) {
            throw new ObjConflictException(
                    "La lista de orden no coincide con los parámetros del tipo de estudio. "
                            + "Vuelva a abrir el catálogo: es posible que alguien lo haya cambiado.");
        }

        int orden = 0;
        for (Long id : idsEnOrden) {
            porId.get(id).setOrden(orden++);
        }
        return repository.saveAll(parametros.stream()
                .sorted(java.util.Comparator.comparing(ParametroEstudioMuestra::getOrden))
                .toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public ParametroEstudioMuestra update(Long id, ParametroEstudioMuestra datos) {
        ParametroEstudioMuestra paramBD = getById(id);
        if (!datos.getNombre().equalsIgnoreCase(paramBD.getNombre())) {
            repository.findByTipoEstudioMuestra_IdAndNombreIgnoreCase(
                    paramBD.getTipoEstudioMuestra().getId(), datos.getNombre()
            ).ifPresent(p -> {
                throw new ObjConflictException("Ya existe un parámetro con ese nombre en este tipo de estudio");
            });
            paramBD.setNombre(datos.getNombre());
        }
        if (datos.getTipo() != paramBD.getTipo()
                && resultadoEstudioMuestraRepository.existsByParametro_Id(paramBD.getId())) {
            throw new ObjConflictException("No se puede cambiar el tipo del parámetro porque tiene resultados registrados");
        }

        paramBD.setUnidad(datos.getUnidad());
        paramBD.setTipo(datos.getTipo());
        paramBD.setValorMinimo(datos.getValorMinimo());
        paramBD.setValorMaximo(datos.getValorMaximo());
        return repository.save(paramBD);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        ParametroEstudioMuestra param = getById(id);
        repository.delete(param);
    }
}
