package imss.gob.mx.cohorte.application.impresion;

import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.impresion.ConfiguracionEtiquetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfiguracionEtiquetaApplicationService {

    private final ConfiguracionEtiquetaService service;
    private final InstitucionContextService institucionContext;

    public List<ConfiguracionEtiqueta> listar() {
        return service.listarPorInstitucion(institucionContext.getIdInstitucionActual());
    }

    public List<ConfiguracionEtiqueta> listarActivas() {
        return service.listarActivasPorInstitucion(institucionContext.getIdInstitucionActual());
    }

    public ConfiguracionEtiqueta obtener(Long id) {
        return service.obtenerPorId(id, institucionContext.getIdInstitucionActual());
    }

    public ConfiguracionEtiqueta obtenerPredeterminada() {
        return service.obtenerPredeterminada(institucionContext.getIdInstitucionActual());
    }

    public ConfiguracionEtiqueta crear(ConfiguracionEtiqueta config) {
        return service.crear(config, institucionContext.getIdInstitucionActual());
    }

    public ConfiguracionEtiqueta actualizar(Long id, ConfiguracionEtiqueta datos) {
        return service.actualizar(id, datos, institucionContext.getIdInstitucionActual());
    }

    public void toggleActivo(Long id) {
        service.toggleActivo(id, institucionContext.getIdInstitucionActual());
    }

    public void establecerPredeterminada(Long id) {
        service.establecerPredeterminada(id, institucionContext.getIdInstitucionActual());
    }
}
