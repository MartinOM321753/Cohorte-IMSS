package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudioRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class TipoService {

    private final TipoEstudioRepository tipoEstudioRepository;
    private final InstitucionContextService institucionContextService;
    private final imss.gob.mx.cohorte.services.institucion.InstitucionJerarquiaService institucionJerarquiaService;
    private final imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository estudioMedicoRepository;

    /** Todos los TipoEstudio de la institución actual, filtrados por estado. */
    public List<TipoEstudio> getAllByStatus(Boolean status) {
        return tipoEstudioRepository.findAllByActivoAndInstitucion_Id(
                status, institucionContextService.getIdInstitucionActual());
    }

    /** Todos los TipoEstudio de la institución actual (activos e inactivos). */
    public List<TipoEstudio> getAllByInstitucion() {
        return tipoEstudioRepository.findAllByInstitucion_Id(
                institucionContextService.getIdInstitucionActual());
    }

    public TipoEstudio getByName(String nombre) {
        return tipoEstudioRepository.findByNombreIgnoreCaseAndInstitucion_Id(
                nombre, institucionContextService.getIdInstitucionActual())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio solicitado"));
    }

    /**
     * Lectura de la definicion de un tipo de estudio para CONSULTAR un estudio que
     * lo usa. El catalogo es por institucion, asi que ver un estudio de otra sede
     * obliga a leer su tipo: sin esto la consulta rebota al pedir los parametros.
     *
     * <p>Solo lectura. {@link #getOne(Long)} sigue exigiendo que el tipo sea de la
     * institucion propia, de modo que no se puede crear ni editar un estudio a
     * partir del catalogo de otra sede.</p>
     */
    public TipoEstudio getOneParaLectura(Long id) {
        TipoEstudio tipo = tipoEstudioRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio solicitado"));

        java.util.List<Long> alcanzables = institucionJerarquiaService.getInstitucionesVisibles(
                institucionContextService.getIdInstitucionActual());

        if (alcanzables.contains(tipo.getInstitucion().getId())) return tipo;

        // Alcanzar la sede del catalogo no es la unica via: con la colaboracion entre
        // sedes se puede leer un estudio de una institucion que no se alcanza, porque
        // el permiso lo da el participante. En ese caso la definicion se abre solo si
        // hay algun estudio de este tipo que ya se podia consultar.
        if (estudioMedicoRepository.existeEstudioLegibleDeTipo(id, alcanzables)) return tipo;

        throw new org.springframework.security.access.AccessDeniedException(
                "El tipo de estudio pertenece a otra institución");
    }

    public TipoEstudio getOne(Long id) {
        TipoEstudio tipo = tipoEstudioRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio solicitado"));
        if (!tipo.getActivo()) {
            throw new ObjNotFoundException("El tipo de estudio no se encuentra activo");
        }
        institucionContextService.verificarPertenece(tipo.getInstitucion());
        return tipo;
    }

    public TipoEstudio create(TipoEstudio tipoEstudio) {
        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        Optional<TipoEstudio> existente = tipoEstudioRepository.findByNombreIgnoreCaseAndInstitucion_Id(
                tipoEstudio.getNombre(), idInstitucion);
        if (existente.isPresent()) {
            throw new ObjConflictException("Ya existe un tipo de estudio con ese nombre");
        }
        tipoEstudio.setInstitucion(institucionContextService.getInstitucionActual());
        tipoEstudio.setFechaCreacion(LocalDateTime.now());
        return tipoEstudioRepository.save(tipoEstudio);
    }

    public TipoEstudio update(TipoEstudio tipoEstudio) {
        TipoEstudio tipoBD = tipoEstudioRepository.findById(tipoEstudio.getId())
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio"));
        institucionContextService.verificarPertenece(tipoBD.getInstitucion());

        if (!tipoEstudio.getNombre().equalsIgnoreCase(tipoBD.getNombre())) {
            Long idInstitucion = institucionContextService.getIdInstitucionActual();
            Optional<TipoEstudio> duplicado = tipoEstudioRepository.findByNombreIgnoreCaseAndInstitucion_Id(
                    tipoEstudio.getNombre(), idInstitucion);
            if (duplicado.isPresent()) {
                throw new ObjConflictException("Ya existe un tipo de estudio con ese nombre");
            }
            tipoBD.setNombre(tipoEstudio.getNombre());
        }

        tipoBD.setParametros(tipoEstudio.getParametros());
        tipoBD.setDescripcion(tipoEstudio.getDescripcion());
        tipoBD.setActivo(tipoEstudio.getActivo());

        return tipoEstudioRepository.save(tipoBD);
    }

    public void delete(TipoEstudio tipo) {
        institucionContextService.verificarPertenece(tipo.getInstitucion());
        tipoEstudioRepository.delete(tipo);
    }

    public Boolean Active(Long id) {
        TipoEstudio tipoEstudio = tipoEstudioRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro el tipo de estudio"));
        institucionContextService.verificarPertenece(tipoEstudio.getInstitucion());
        tipoEstudio.setActivo(!tipoEstudio.getActivo());
        tipoEstudioRepository.save(tipoEstudio);
        return true;
    }
}
