package imss.gob.mx.cohorte.application.reportes;

import imss.gob.mx.cohorte.modules.institucion.ModuloSistema;
import imss.gob.mx.cohorte.modules.reportes.ImagenReporte;
import imss.gob.mx.cohorte.security.institucion.RequireModulo;
import imss.gob.mx.cohorte.services.reportes.ImagenReporteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

/**
 * La galería de imágenes, orquestada para el controlador.
 *
 * <p>Lleva el mismo guardián de módulo que las plantillas: una institución que no
 * tiene contratado Reportes tampoco debe poder llenar el bucket de logos.</p>
 */
@Service
@RequiredArgsConstructor
@RequireModulo(ModuloSistema.REPORTES)
public class ImagenReporteApplicationService {

    private final ImagenReporteService service;

    @Transactional(readOnly = true)
    public List<ImagenReporte> listar() {
        return service.getAll();
    }

    @Transactional(readOnly = true)
    public ImagenReporte obtener(Long id) {
        return service.getById(id);
    }

    @Transactional(readOnly = true)
    public InputStream contenido(Long id) {
        return service.contenidoDe(service.getById(id));
    }

    @Transactional
    public ImagenReporte subir(MultipartFile archivo, String nombre) {
        return service.subir(archivo, nombre);
    }

    @Transactional
    public ImagenReporte renombrar(Long id, String nombre) {
        return service.renombrar(id, nombre);
    }

    @Transactional
    public void eliminar(Long id) {
        service.eliminar(id);
    }

    @Transactional(readOnly = true)
    public List<String> plantillasQueLaUsan(Long id) {
        return service.plantillasQueLaUsan(id);
    }
}
