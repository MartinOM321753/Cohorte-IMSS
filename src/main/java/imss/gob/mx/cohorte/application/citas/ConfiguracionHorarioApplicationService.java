package imss.gob.mx.cohorte.application.citas;

import imss.gob.mx.cohorte.modules.cita.ConfiguracionHorario;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.citas.ConfiguracionHorarioService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfiguracionHorarioApplicationService {

    private final ConfiguracionHorarioService service;
    private final InstitucionContextService institucionContext;

    public List<ConfiguracionHorario> listar() {
        return service.listarPorInstitucion(institucionContext.getIdInstitucionActual());
    }

    public ConfiguracionHorario obtener(Long id) {
        return service.obtenerPorId(id, institucionContext.getIdInstitucionActual());
    }

    public ConfiguracionHorario obtenerActiva() {
        return service.obtenerActiva(institucionContext.getIdInstitucionActual());
    }

    public ConfiguracionHorario crear(ConfiguracionHorario config) {
        return service.crear(config, institucionContext.getIdInstitucionActual());
    }

    public ConfiguracionHorario actualizar(Long id, ConfiguracionHorario datos) {
        return service.actualizar(id, datos, institucionContext.getIdInstitucionActual());
    }

    public void activar(Long id) {
        service.activar(id, institucionContext.getIdInstitucionActual());
    }

    public void eliminar(Long id) {
        service.eliminar(id, institucionContext.getIdInstitucionActual());
    }
}
