package imss.gob.mx.cohorte.services.reportes;

import imss.gob.mx.cohorte.infrastructure.minio.MinioStorageService;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.reportes.ImagenReporte;
import imss.gob.mx.cohorte.modules.reportes.ImagenReporteRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ValidationException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * La galería de imágenes de una institución.
 *
 * <p>Las plantillas no guardan bytes ni URLs, solo {@code imagen:{id}}. Quien la
 * dibuja —el editor en pantalla y el maquetador en el PDF— resuelve esa referencia
 * cuando toca, y así los dos enseñan lo mismo aunque la imagen se sustituya después.</p>
 */
@Service
@AllArgsConstructor
public class ImagenReporteService {

    /**
     * Solo PNG y JPEG.
     *
     * <p>SVG queda fuera a propósito, y no por capricho del motor de PDF: un SVG es un
     * documento que puede traer scripts dentro, y servirlo desde nuestro dominio lo
     * ejecutaría en la sesión de quien abra la galería. Un logo no necesita eso.</p>
     */
    private static final Set<String> TIPOS = Set.of("image/png", "image/jpeg");

    /**
     * 2 MB. El límite global del multipart son 50 MB, pensado para los documentos
     * clínicos; un membrete de 50 MB solo engorda cada PDF que lo lleve.
     */
    private static final long MAX_BYTES = 2L * 1024 * 1024;

    /** Por encima de esto se avisa: en una hoja carta no se va a ver mejor. */
    public static final int ANCHO_RECOMENDADO_PX = 2000;

    private static final int MAX_NOMBRE = 120;

    private final ImagenReporteRepository repository;
    private final InstitucionContextService institucionContextService;
    private final MinioStorageService storage;

    // ── Lectura ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ImagenReporte> getAll() {
        return repository.findAllByInstitucion_IdOrderByNombreAsc(
                institucionContextService.getIdInstitucionActual());
    }

    @Transactional(readOnly = true)
    public ImagenReporte getById(Long id) {
        ImagenReporte imagen = repository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontró la imagen con id: " + id));
        institucionContextService.verificarPertenece(imagen.getInstitucion());
        return imagen;
    }

    /** Los bytes, para servirlos por el backend. MinIO nunca se expone al cliente. */
    @Transactional(readOnly = true)
    public InputStream contenidoDe(ImagenReporte imagen) {
        return storage.getObjectStream(imagen.getObjectKey());
    }

    // ── Escritura ────────────────────────────────────────────────────────────

    @Transactional
    public ImagenReporte subir(MultipartFile archivo, String nombreDeseado) {
        byte[] contenido = leer(archivo);
        BufferedImage decodificada = decodificar(contenido);

        Institucion institucion = institucionContextService.getInstitucionActual();
        String nombre = nombreLibre(nombreDeseado, archivo.getOriginalFilename(), institucion.getId());

        String extension = "image/png".equals(archivo.getContentType()) ? ".png" : ".jpg";
        String objectKey = "reportes/imagenes/" + institucion.getId() + "/" + UUID.randomUUID() + extension;

        storage.upload(new ByteArrayInputStream(contenido), objectKey,
                archivo.getContentType(), contenido.length);

        ImagenReporte imagen = new ImagenReporte();
        imagen.setNombre(nombre);
        imagen.setObjectKey(objectKey);
        imagen.setContentType(archivo.getContentType());
        imagen.setBytes((long) contenido.length);
        imagen.setAnchoPx(decodificada.getWidth());
        imagen.setAltoPx(decodificada.getHeight());
        imagen.setInstitucion(institucion);

        return repository.save(imagen);
    }

    @Transactional
    public ImagenReporte renombrar(Long id, String nombre) {
        ImagenReporte imagen = getById(id);
        String limpio = nombre == null ? "" : nombre.trim();
        if (limpio.isEmpty()) throw new ValidationException("La imagen necesita un nombre.");
        if (limpio.length() > MAX_NOMBRE) {
            throw new ValidationException("El nombre no puede pasar de " + MAX_NOMBRE + " caracteres.");
        }

        repository.findByNombreIgnoreCaseAndInstitucion_Id(limpio, imagen.getInstitucion().getId())
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new ObjConflictException("Ya hay una imagen que se llama \"" + limpio + "\".");
                });

        imagen.setNombre(limpio);
        return repository.save(imagen);
    }

    /**
     * Borra la imagen, salvo que alguna plantilla la esté usando.
     *
     * <p>Se bloquea en vez de dejar el hueco porque el fallo sería silencioso y
     * tardío: la plantilla seguiría abriendo bien y el membrete desaparecería el día
     * que alguien emitiera un documento, sin nada que relacionara una cosa con la
     * otra. Decirlo aquí, con los nombres de las plantillas, es mucho más barato.</p>
     */
    @Transactional
    public void eliminar(Long id) {
        ImagenReporte imagen = getById(id);

        List<String> enUso = repository.nombresDePlantillasQueLaUsan(
                imagen.getInstitucion().getId(), id);
        if (!enUso.isEmpty()) {
            throw new ObjConflictException(
                    "No se puede eliminar \"" + imagen.getNombre() + "\" porque "
                    + (enUso.size() == 1 ? "la usa la plantilla " : "la usan las plantillas ")
                    + String.join(", ", enUso)
                    + ". Quítala de ahí antes de borrarla.");
        }

        // Primero la fila y después el objeto: si el borrado en MinIO falla, la
        // transacción revierte y no queda una referencia apuntando a nada. Al revés
        // quedaría una imagen en la galería que ya no se puede mostrar.
        repository.delete(imagen);
        storage.delete(imagen.getObjectKey());
    }

    /** Qué plantillas usan la imagen. Para avisar antes de intentar borrarla. */
    @Transactional(readOnly = true)
    public List<String> plantillasQueLaUsan(Long id) {
        ImagenReporte imagen = getById(id);
        return repository.nombresDePlantillasQueLaUsan(imagen.getInstitucion().getId(), id);
    }

    // ── Validación ───────────────────────────────────────────────────────────

    private byte[] leer(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new ValidationException("No llegó ningún archivo.");
        }
        if (!TIPOS.contains(archivo.getContentType())) {
            throw new ValidationException("Solo se admiten imágenes PNG o JPEG.");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new ValidationException("La imagen no puede pasar de 2 MB.");
        }
        try {
            return archivo.getBytes();
        } catch (Exception e) {
            throw new ValidationException("No se pudo leer el archivo: " + e.getMessage());
        }
    }

    /**
     * Comprueba que el archivo sea de verdad la imagen que dice ser.
     *
     * <p>El tipo lo declara quien sube, así que por sí solo no prueba nada: cualquier
     * cosa puede llegar rotulada como {@code image/png}. Descodificarla es lo único que
     * confirma que hay una imagen ahí dentro — y de paso da sus medidas.</p>
     */
    private BufferedImage decodificar(byte[] contenido) {
        try {
            BufferedImage imagen = ImageIO.read(new ByteArrayInputStream(contenido));
            if (imagen == null) {
                throw new ValidationException("El archivo no es una imagen PNG o JPEG válida.");
            }
            return imagen;
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("El archivo no es una imagen PNG o JPEG válida.");
        }
    }

    /**
     * El nombre con el que sale en la galería.
     *
     * <p>Si el que se pide ya está tomado se numera en vez de rechazar: quien sube dos
     * versiones de un logo no está cometiendo un error, y obligarle a inventar un
     * nombre distinto antes de ver la imagen sobra.</p>
     */
    private String nombreLibre(String deseado, String nombreArchivo, Long idInstitucion) {
        String base = deseado != null && !deseado.isBlank()
                ? deseado.trim()
                : limpiarNombreDeArchivo(nombreArchivo);
        if (base.length() > MAX_NOMBRE) base = base.substring(0, MAX_NOMBRE);

        for (int i = 1; i <= 100; i++) {
            String sufijo = i == 1 ? "" : " (" + i + ")";
            String recortada = base.length() + sufijo.length() > MAX_NOMBRE
                    ? base.substring(0, MAX_NOMBRE - sufijo.length())
                    : base;
            String candidato = recortada + sufijo;
            if (repository.findByNombreIgnoreCaseAndInstitucion_Id(candidato, idInstitucion).isEmpty()) {
                return candidato;
            }
        }
        throw new ObjConflictException("Ya hay demasiadas imágenes con ese nombre. Ponle otro.");
    }

    /** El nombre del archivo sin la extensión, o algo genérico si no trae nada usable. */
    static String limpiarNombreDeArchivo(String nombreArchivo) {
        if (nombreArchivo == null || nombreArchivo.isBlank()) return "Imagen";
        // Solo el último tramo: algunos navegadores mandan la ruta entera.
        String soloNombre = nombreArchivo.replace('\\', '/');
        soloNombre = soloNombre.substring(soloNombre.lastIndexOf('/') + 1);

        // >= 0 y no > 0: un archivo llamado solo «.png» se queda sin nada, y ahí entra
        // el nombre genérico. Dejar una tarjeta de la galería rotulada «.png» no
        // ayudaría a nadie a distinguirla de las demás.
        int punto = soloNombre.lastIndexOf('.');
        if (punto >= 0) soloNombre = soloNombre.substring(0, punto);

        soloNombre = soloNombre.trim();
        return soloNombre.isEmpty() ? "Imagen" : soloNombre;
    }
}
