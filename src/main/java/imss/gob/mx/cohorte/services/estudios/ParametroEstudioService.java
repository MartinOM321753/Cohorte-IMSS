package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudioRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudioRepository;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ParametroEstudioService {

    private final ParametroEstudioRepository parametroRepository;
    private final ResultadoEstudioRepository resultadoEstudioRepository;

    @Transactional(readOnly = true)
    public List<ParametroEstudio> getAll() {
        return parametroRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ParametroEstudio> getByTipoEstudio(Long tipoEstudioId) {
        return parametroRepository.findAllByTipoEstudio_Id(tipoEstudioId);
    }

    @Transactional(readOnly = true)
    public ParametroEstudio getOne(Long id) {
        return parametroRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el parametro de estudio"));
    }

    /**
     * Pone o quita de uso un parámetro, igual que el interruptor que ya existe para
     * la plantilla completa. Devuelve el estado en que quedó.
     *
     * <p>No toca los resultados ya capturados: un parámetro fuera de uso deja de
     * ofrecerse y de exigirse al capturar, pero lo que se registró con él sigue
     * siendo parte de esas capturas.</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean toggleActivo(Long id) {
        ParametroEstudio parametro = getOne(id);
        parametro.setActivo(!Boolean.TRUE.equals(parametro.getActivo()));
        parametroRepository.save(parametro);
        return parametro.getActivo();
    }

    @Transactional(rollbackFor = Exception.class)
    public ParametroEstudio create(ParametroEstudio parametroEstudio) {
        Optional<ParametroEstudio> parametro = parametroRepository.findByTipoEstudio_IdAndNombreIgnoreCase(
                parametroEstudio.getTipoEstudio().getId(),
                parametroEstudio.getNombre()
        );
        if (parametro.isPresent()) {
            throw new ObjConflictException("Ya existe un parametro con ese nombre para el tipo de estudio");
        }
        return parametroRepository.save(parametroEstudio);
    }

    @Transactional(rollbackFor = Exception.class)
    public ParametroEstudio update(ParametroEstudio parametroEstudio) {
        ParametroEstudio parametroDB = parametroRepository.findById(parametroEstudio.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el parametro de estudio"));

        Optional<ParametroEstudio> parametroDuplicado = parametroRepository.findByTipoEstudio_IdAndNombreIgnoreCase(
                parametroEstudio.getTipoEstudio().getId(),
                parametroEstudio.getNombre()
        );
        if (parametroDuplicado.isPresent() && !parametroDuplicado.get().getId().equals(parametroDB.getId())) {
            throw new ObjConflictException("Ya existe un parametro con ese nombre para el tipo de estudio");
        }

        if (parametroEstudio.getTipo() != parametroDB.getTipo()
                && resultadoEstudioRepository.existsByParametro_Id(parametroDB.getId())) {
            throw new ObjConflictException("No se puede cambiar el tipo del parámetro porque tiene resultados registrados");
        }

        if (parametroEstudio.getTipo() != TipoParametro.TEXTO_OPCIONES
                && parametroDB.getTipo() == TipoParametro.TEXTO_OPCIONES) {
            parametroDB.getOpciones().clear();
        }

        parametroDB.setTipoEstudio(parametroEstudio.getTipoEstudio());
        parametroDB.setNombre(parametroEstudio.getNombre());
        parametroDB.setUnidad(parametroEstudio.getUnidad());
        parametroDB.setTipo(parametroEstudio.getTipo());
        parametroDB.setValorMinMujeres(parametroEstudio.getValorMinMujeres());
        parametroDB.setValorMaxMujeres(parametroEstudio.getValorMaxMujeres());
        parametroDB.setValorMinHombres(parametroEstudio.getValorMinHombres());
        parametroDB.setValorMaxHombres(parametroEstudio.getValorMaxHombres());
        parametroDB.setMargenRevision(parametroEstudio.getMargenRevision());

        return parametroRepository.save(parametroDB);
    }

    @Transactional
    public ParametroEstudio delete(Long id) {
        ParametroEstudio parametro = parametroRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el parametro de estudio"));
        parametroRepository.delete(parametro);
        return parametro;
    }
}
