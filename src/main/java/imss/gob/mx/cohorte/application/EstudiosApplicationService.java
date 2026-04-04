package imss.gob.mx.cohorte.application;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.services.estudios.EstudioService;
import imss.gob.mx.cohorte.services.estudios.ParametroEstudioService;
import imss.gob.mx.cohorte.services.estudios.ResultadoService;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.services.pacientes.PacienteService;
import imss.gob.mx.cohorte.services.usuarios.UserService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class EstudiosApplicationService {

    private final EstudioService estudioService;
    private final TipoService tipoEstudioService;
    private final ResultadoService resultadoService;
    private final PacienteService pacienteService;
    private final UserService userService;
    private final ParametroEstudioService parametroService;

    // ─────────────────────────────────────────────
    // CRUD
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<EstudioMedico> getAllEstudios() {
        return estudioService.getAll();
    }

    @Transactional(readOnly = true)
    public EstudioMedico getEstudio(Long id) {
        return findEstudioOrThrow(id);
    }

    @Transactional
    public EstudioMedico createEstudio(EstudioMedico estudioMedico) {
        resolveRelaciones(estudioMedico);
        guardarResultados(estudioMedico); // se guardan explícitamente los resultados, si existen
        return estudioService.create(estudioMedico);
    }

    @Transactional
    public EstudioMedico updateEstudio(Long id, EstudioMedico estudioMedico) {
        EstudioMedico existing = findEstudioOrThrow(id);

        resolveRelaciones(estudioMedico);

        // Actualiza los campos principales
        existing.setPaciente(estudioMedico.getPaciente());
        existing.setUsuarioRealiza(estudioMedico.getUsuarioRealiza());
        existing.setTipoEstudio(estudioMedico.getTipoEstudio());
        existing.setObservaciones(estudioMedico.getObservaciones());
        existing.setFechaEstudio(estudioMedico.getFechaEstudio());

        // sincronización explícita de resultados (update seguro, elimina los results antiguos no presentes):
        sincronizarResultados(existing, estudioMedico.getResultadoEstudio());

        return estudioService.update(existing);
    }

//    @Transactional
//    public void deleteEstudio(Long id) {
//        EstudioMedico estudio = findEstudioOrThrow(id);
//        // Primero elimina todos los resultados asociados para evitar huérfanos
//        if (estudio.getResultadoEstudio() != null && !estudio.getResultadoEstudio().isEmpty()) {
//            for (ResultadoEstudio r : estudio.getResultadoEstudio()) {
//                resultadoService.delete(r.getId());
//            }
//        }
//        estudioService.delete(id);
//    }

    // ─────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────


    private void resolveRelaciones(EstudioMedico estudioMedico) {
        if (estudioMedico.getPaciente() == null || estudioMedico.getPaciente().getId() == null) {
            throw new ObjNotFoundException("Falta información de paciente");
        }
        if (estudioMedico.getUsuarioRealiza() == null || estudioMedico.getUsuarioRealiza().getId() == null) {
            throw new ObjNotFoundException("Falta información de usuario");
        }
        if (estudioMedico.getTipoEstudio() == null || estudioMedico.getTipoEstudio().getId() == null) {
            throw new ObjNotFoundException("Falta información de tipo de estudio");
        }

        Paciente paciente = findPacienteOrThrow(estudioMedico.getPaciente().getUuid());
        BeanUser usuario = findUsuarioOrThrow(estudioMedico.getUsuarioRealiza().getUUID());
        TipoEstudio tipoEstudio = findTipoEstudioOrThrow(estudioMedico.getTipoEstudio().getId());

        resolveResultados(estudioMedico, tipoEstudio);

        estudioMedico.setPaciente(paciente);
        estudioMedico.setUsuarioRealiza(usuario);
        estudioMedico.setTipoEstudio(tipoEstudio);
    }

    /**
     * Valida y enlaza cada resultado con su parámetro y el estudio padre. 
     * Verifica que el tipo de estudio del parámetro coincida con el del estudio.
     */
    private void resolveResultados(EstudioMedico estudioMedico, TipoEstudio tipoEstudio) {
        if (estudioMedico.getResultadoEstudio() == null || estudioMedico.getResultadoEstudio().isEmpty()) {
            estudioMedico.setResultadoEstudio(new ArrayList<>()); // Mejor deja una lista vacía en vez de null para evitar problemas con JPA
            return;
        }

        for (ResultadoEstudio resultado : estudioMedico.getResultadoEstudio()) {
            if (resultado.getParametro() == null || resultado.getParametro().getId() == null) {
                throw new ObjNotFoundException("Falta información del parámetro en un resultado");
            }
            ParametroEstudio parametro = findParametroOrThrow(resultado.getParametro().getId());

            TipoEstudio tipoDelParametro = parametro.getTipoEstudio();
            if (tipoDelParametro == null)
                throw new ObjNotFoundException("El parámetro no tiene tipo de estudio asociado");

            if (!Objects.equals(tipoDelParametro.getId(), tipoEstudio.getId()))
                throw new ObjConflictException("El tipo de estudio del parámetro no coincide con el del estudio");

            resultado.setParametro(parametro);
            resultado.setEstudio(estudioMedico);
        }
    }

    /**
     * Guarda explícitamente los resultados si existen.
     */
    private void guardarResultados(EstudioMedico estudioMedico) {
        if (estudioMedico.getResultadoEstudio() != null) {
            for (ResultadoEstudio resultado : estudioMedico.getResultadoEstudio()) {
                resultadoService.create(resultado);
            }
        }
    }

    /**
     * Sincroniza los resultados de un estudio: agrega nuevos, actualiza existentes y elimina los eliminados.
     */
    private void sincronizarResultados(EstudioMedico estudio, List<ResultadoEstudio> nuevosResultados) {
        // Si no hay resultados, los elimina todos
        if (nuevosResultados == null || nuevosResultados.isEmpty()) {
            if (estudio.getResultadoEstudio() != null) {
                for (ResultadoEstudio r : estudio.getResultadoEstudio()) {
                    resultadoService.delete(r.getId());
                }
                estudio.setResultadoEstudio(new ArrayList<>());
            }
            return;
        }
        // Mapear los resultados existentes y los nuevos por id parámetro para sincronizar
        Map<Long, ResultadoEstudio> actuales = estudio.getResultadoEstudio() == null ? new HashMap<>() :
            estudio.getResultadoEstudio().stream()
                    .filter(r -> r.getParametro() != null && r.getParametro().getId() != null)
                    .collect(Collectors.toMap(r -> r.getParametro().getId(), r -> r));

        List<ResultadoEstudio> resultadoFinal = new ArrayList<>();
        for (ResultadoEstudio nuevo : nuevosResultados) {
            if (nuevo.getParametro() == null || nuevo.getParametro().getId() == null)
                throw new ObjNotFoundException("Falta info. de parámetro en resultado");

            ResultadoEstudio existente = actuales.get(nuevo.getParametro().getId());
            if (existente != null) {
                // Actualiza el valor
                existente.setValorNumerico(nuevo.getValorNumerico());
                existente.setValorTexto(nuevo.getValorTexto());
                resultadoService.update(existente);
                resultadoFinal.add(existente);
                actuales.remove(nuevo.getParametro().getId());
            } else {
                // Nuevo resultado
                nuevo.setEstudio(estudio);
                ResultadoEstudio creado = resultadoService.create(nuevo);
                resultadoFinal.add(creado);
            }
        }
        // Elimina los que ya no están
        for (ResultadoEstudio r : actuales.values()) {
            resultadoService.delete(r.getId());
        }
        estudio.setResultadoEstudio(resultadoFinal);
    }


    private EstudioMedico findEstudioOrThrow(Long id) {
        EstudioMedico estudio = estudioService.getOne(id);
        if (estudio == null) throw new ObjNotFoundException("No se encontró el estudio");
        return estudio;
    }

    private Paciente findPacienteOrThrow(String uuid) {
        Paciente paciente = pacienteService.getByUUID(uuid);
        if (paciente == null) throw new ObjNotFoundException("No se encontró el paciente");
        return paciente;
    }

    private BeanUser findUsuarioOrThrow(String uuid) {
        BeanUser user = userService.getByUUID(uuid);
        if (user == null) throw new ObjNotFoundException("No se encontró el usuario");
        return user;
    }

    private TipoEstudio findTipoEstudioOrThrow(Long id) {
        TipoEstudio tipo = tipoEstudioService.getOne(id);
        if (tipo == null) throw new ObjNotFoundException("No se encontró el tipo de estudio");
        return tipo;
    }

    private ParametroEstudio findParametroOrThrow(Long id) {
        ParametroEstudio parametro = parametroService.getOne(id);
        if (parametro == null) throw new ObjNotFoundException("No se encontró el parámetro");
        return parametro;
    }
}