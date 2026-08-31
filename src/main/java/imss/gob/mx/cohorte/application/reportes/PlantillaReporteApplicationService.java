package imss.gob.mx.cohorte.application.reportes;

import com.fasterxml.jackson.databind.ObjectMapper;
import imss.gob.mx.cohorte.controllers.reportes.dto.PlantillaReporteMapper;
import imss.gob.mx.cohorte.controllers.reportes.dto.PlantillaReporteRequestDTO;
import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.reportes.PlantillaReporte;
import imss.gob.mx.cohorte.modules.reportes.TipoReporte;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.reportes.PlantillaReporteService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Casos de uso del catálogo de plantillas.
 *
 * <p>El aislamiento por institución lo resuelve {@link PlantillaReporteService}, que
 * comprueba la pertenencia en toda consulta por id. Aquí se añade lo que corresponde a
 * la capa de entrada: que el diseño recibido sea JSON de verdad.</p>
 */
@Service
@AllArgsConstructor
@RequireModulo(ModuloSistema.REPORTES)
public class PlantillaReporteApplicationService {

    private final PlantillaReporteService service;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PlantillaReporte> listar() {
        return service.getAll();
    }

    @Transactional(readOnly = true)
    public List<PlantillaReporte> listarActivasPorTipo(TipoReporte tipo) {
        return service.getActivasPorTipo(tipo);
    }

    @Transactional(readOnly = true)
    public PlantillaReporte obtener(Long id) {
        return service.getById(id);
    }

    @Transactional(readOnly = true)
    public PlantillaReporte obtenerPredeterminada(TipoReporte tipo) {
        return service.getPredeterminada(tipo);
    }

    @Transactional
    public PlantillaReporte crear(PlantillaReporteRequestDTO dto) {
        verificarDisenoEsJson(dto.getDiseno());
        return service.create(PlantillaReporteMapper.toEntity(dto));
    }

    @Transactional
    public PlantillaReporte actualizar(Long id, PlantillaReporteRequestDTO dto) {
        verificarDisenoEsJson(dto.getDiseno());
        return service.update(id, PlantillaReporteMapper.toEntity(dto));
    }

    @Transactional
    public boolean toggleActivo(Long id) {
        return service.toggleActivo(id);
    }

    @Transactional
    public void establecerPredeterminada(Long id) {
        service.establecerPredeterminada(id);
    }

    @Transactional
    public void eliminar(Long id) {
        service.delete(id);
    }

    /**
     * El diseño se guarda como texto, así que nada impide meter cualquier cosa en esa
     * columna. Si no es JSON válido, el fallo aparecería mucho después —al emitir, y
     * como un error del motor de maquetado—, sin ninguna pista de que el problema venía
     * de cómo se guardó. Más vale rechazarlo aquí.
     */
    private void verificarDisenoEsJson(String diseno) {
        try {
            objectMapper.readTree(diseno);
        } catch (Exception e) {
            throw new ValidationException("El diseño de la plantilla no es un JSON válido.");
        }
    }
}
