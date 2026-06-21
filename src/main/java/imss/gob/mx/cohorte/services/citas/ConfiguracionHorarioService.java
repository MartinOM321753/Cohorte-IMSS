package imss.gob.mx.cohorte.services.citas;

import imss.gob.mx.cohorte.modules.cita.ConfiguracionHorario;
import imss.gob.mx.cohorte.modules.cita.ConfiguracionHorarioRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfiguracionHorarioService {

    private final ConfiguracionHorarioRepository repository;
    private final InstitucionRepository institucionRepository;

    public List<ConfiguracionHorario> listarPorInstitucion(Long institucionId) {
        return repository.findByInstitucionIdOrderByNombre(institucionId);
    }

    public ConfiguracionHorario obtenerPorId(Long id, Long institucionId) {
        return repository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new RuntimeException("Configuración de horario no encontrada"));
    }

    public ConfiguracionHorario obtenerActiva(Long institucionId) {
        return repository.findByInstitucionIdAndActivaTrue(institucionId)
                .orElse(null);
    }

    @Transactional
    public ConfiguracionHorario crear(ConfiguracionHorario config, Long institucionId) {
        validarNombreUnico(institucionId, config.getNombre(), null);
        validarHorario(config);

        Institucion inst = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));
        config.setInstitucion(inst);

        if (Boolean.TRUE.equals(config.getActiva())) {
            desactivarActual(institucionId);
        }

        return repository.save(config);
    }

    @Transactional
    public ConfiguracionHorario actualizar(Long id, ConfiguracionHorario datos, Long institucionId) {
        ConfiguracionHorario config = obtenerPorId(id, institucionId);
        validarNombreUnico(institucionId, datos.getNombre(), id);
        validarHorario(datos);

        config.setNombre(datos.getNombre());
        config.setHoraInicio(datos.getHoraInicio());
        config.setHoraFin(datos.getHoraFin());
        config.setLunes(datos.getLunes());
        config.setMartes(datos.getMartes());
        config.setMiercoles(datos.getMiercoles());
        config.setJueves(datos.getJueves());
        config.setViernes(datos.getViernes());
        config.setSabado(datos.getSabado());
        config.setDomingo(datos.getDomingo());

        if (Boolean.TRUE.equals(datos.getActiva()) && !Boolean.TRUE.equals(config.getActiva())) {
            desactivarActual(institucionId);
        }
        config.setActiva(datos.getActiva());

        return repository.save(config);
    }

    @Transactional
    public void activar(Long id, Long institucionId) {
        ConfiguracionHorario config = obtenerPorId(id, institucionId);
        desactivarActual(institucionId);
        config.setActiva(true);
        repository.save(config);
    }

    @Transactional
    public void eliminar(Long id, Long institucionId) {
        ConfiguracionHorario config = obtenerPorId(id, institucionId);
        if (Boolean.TRUE.equals(config.getActiva())) {
            throw new RuntimeException("No se puede eliminar la configuración activa. Activa otra configuración primero.");
        }
        repository.delete(config);
    }

    private void desactivarActual(Long institucionId) {
        repository.findByInstitucionIdAndActivaTrue(institucionId)
                .ifPresent(actual -> {
                    actual.setActiva(false);
                    repository.save(actual);
                });
    }

    private void validarNombreUnico(Long institucionId, String nombre, Long excludeId) {
        boolean duplicado = excludeId != null
                ? repository.existsByInstitucionIdAndNombreAndIdNot(institucionId, nombre, excludeId)
                : repository.existsByInstitucionIdAndNombre(institucionId, nombre);
        if (duplicado) {
            throw new RuntimeException("Ya existe una configuración de horario con ese nombre en esta institución");
        }
    }

    private void validarHorario(ConfiguracionHorario config) {
        if (config.getHoraInicio() >= config.getHoraFin()) {
            throw new RuntimeException("La hora de inicio debe ser menor a la hora de fin");
        }
        boolean alMenosUnDia = Boolean.TRUE.equals(config.getLunes())
                || Boolean.TRUE.equals(config.getMartes())
                || Boolean.TRUE.equals(config.getMiercoles())
                || Boolean.TRUE.equals(config.getJueves())
                || Boolean.TRUE.equals(config.getViernes())
                || Boolean.TRUE.equals(config.getSabado())
                || Boolean.TRUE.equals(config.getDomingo());
        if (!alMenosUnDia) {
            throw new RuntimeException("Debe seleccionar al menos un día de la semana");
        }
    }
}
