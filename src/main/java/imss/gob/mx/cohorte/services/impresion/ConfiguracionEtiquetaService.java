package imss.gob.mx.cohorte.services.impresion;

import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiqueta;
import imss.gob.mx.cohorte.modules.impresion.ConfiguracionEtiquetaRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.institucion.InstitucionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConfiguracionEtiquetaService {

    private final ConfiguracionEtiquetaRepository repository;
    private final InstitucionRepository institucionRepository;

    public List<ConfiguracionEtiqueta> listarPorInstitucion(Long institucionId) {
        return repository.findByInstitucionIdOrderByNombre(institucionId);
    }

    public List<ConfiguracionEtiqueta> listarActivasPorInstitucion(Long institucionId) {
        return repository.findByInstitucionIdAndActivoTrueOrderByNombre(institucionId);
    }

    public ConfiguracionEtiqueta obtenerPorId(Long id, Long institucionId) {
        return repository.findByIdAndInstitucionId(id, institucionId)
                .orElseThrow(() -> new RuntimeException("Configuración de etiqueta no encontrada"));
    }

    public ConfiguracionEtiqueta obtenerPredeterminada(Long institucionId) {
        return repository.findByInstitucionIdAndPredeterminadaTrue(institucionId)
                .orElse(null);
    }

    @Transactional
    public ConfiguracionEtiqueta crear(ConfiguracionEtiqueta config, Long institucionId) {
        validarNombreUnico(institucionId, config.getNombre(), null);

        Institucion inst = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new RuntimeException("Institución no encontrada"));
        config.setInstitucion(inst);

        if (Boolean.TRUE.equals(config.getPredeterminada())) {
            quitarPredeterminadaActual(institucionId);
        }

        return repository.save(config);
    }

    @Transactional
    public ConfiguracionEtiqueta actualizar(Long id, ConfiguracionEtiqueta datos, Long institucionId) {
        ConfiguracionEtiqueta config = obtenerPorId(id, institucionId);
        validarNombreUnico(institucionId, datos.getNombre(), id);

        config.setNombre(datos.getNombre());
        config.setAnchoMm(datos.getAnchoMm());
        config.setAltoMm(datos.getAltoMm());
        config.setDpi(datos.getDpi());
        config.setEtiquetasPorFila(datos.getEtiquetasPorFila());
        config.setMargenIzquierdoMm(datos.getMargenIzquierdoMm());
        config.setMargenSuperiorMm(datos.getMargenSuperiorMm());
        config.setTipoCodigo(datos.getTipoCodigo());
        config.setModuloCodigo(datos.getModuloCodigo());
        config.setTamanoFuenteNombre(datos.getTamanoFuenteNombre());
        config.setTamanoFuenteEtiqueta(datos.getTamanoFuenteEtiqueta());
        config.setEspaciadoNombre(datos.getEspaciadoNombre());
        config.setEspaciadoCodigo(datos.getEspaciadoCodigo());
        config.setEspaciadoEtiqueta(datos.getEspaciadoEtiqueta());
        config.setMostrarNombre(datos.getMostrarNombre());
        config.setMostrarCodigo(datos.getMostrarCodigo());
        config.setMostrarEtiqueta(datos.getMostrarEtiqueta());
        config.setDisposicion(datos.getDisposicion());

        if (Boolean.TRUE.equals(datos.getPredeterminada()) && !Boolean.TRUE.equals(config.getPredeterminada())) {
            quitarPredeterminadaActual(institucionId);
        }
        config.setPredeterminada(datos.getPredeterminada());

        return repository.save(config);
    }

    @Transactional
    public void toggleActivo(Long id, Long institucionId) {
        ConfiguracionEtiqueta config = obtenerPorId(id, institucionId);
        config.setActivo(!config.getActivo());
        repository.save(config);
    }

    @Transactional
    public void establecerPredeterminada(Long id, Long institucionId) {
        ConfiguracionEtiqueta config = obtenerPorId(id, institucionId);
        quitarPredeterminadaActual(institucionId);
        config.setPredeterminada(true);
        repository.save(config);
    }

    private void quitarPredeterminadaActual(Long institucionId) {
        repository.findByInstitucionIdAndPredeterminadaTrue(institucionId)
                .ifPresent(actual -> {
                    actual.setPredeterminada(false);
                    repository.save(actual);
                });
    }

    private void validarNombreUnico(Long institucionId, String nombre, Long excludeId) {
        boolean duplicado = excludeId != null
                ? repository.existsByInstitucionIdAndNombreAndIdNot(institucionId, nombre, excludeId)
                : repository.existsByInstitucionIdAndNombre(institucionId, nombre);
        if (duplicado) {
            throw new RuntimeException("Ya existe una configuración con ese nombre en esta institución");
        }
    }
}
