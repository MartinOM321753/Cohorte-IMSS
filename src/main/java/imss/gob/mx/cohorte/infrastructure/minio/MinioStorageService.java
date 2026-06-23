package imss.gob.mx.cohorte.infrastructure.minio;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import imss.gob.mx.cohorte.utils.Exceptions.exceptions.MinioUnavailableException;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;


/**
 * Servicio de almacenamiento de objetos usando MinIO (S3-compatible).
 * Gestiona la subida, descarga (URL firmada) y eliminación de archivos.
 * <p>
 * Si MinIO no está disponible al arrancar, el servicio queda en modo degradado:
 * el backend inicia normalmente y sólo lanza excepción cuando se intenta
 * usar alguna operación de archivos.
 */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final MinioProperties properties;

    /** true si MinIO respondió correctamente en el @PostConstruct */
    private volatile boolean minioAvailable = false;

    public MinioStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    /**
     * Intenta crear el bucket al arrancar.
     * Si MinIO no está disponible, sólo registra una advertencia y continúa
     * (el backend no muere; las operaciones de archivo fallarán con mensaje claro).
     */
    @PostConstruct
    public void init() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(properties.getBucket()).build()
                );
                log.info("Bucket MinIO '{}' creado.", properties.getBucket());
            } else {
                log.info("Bucket MinIO '{}' ya existe.", properties.getBucket());
            }
            minioAvailable = true;
        } catch (Exception e) {
            log.warn("MinIO no disponible al iniciar — las operaciones de archivos estarán deshabilitadas: {}", e.getMessage());
        }
    }

    /** Devuelve {@code true} si MinIO está disponible (intenta reconectar si estaba caído). */
    public boolean isAvailable() {
        if (!minioAvailable) {
            tryReconnect();
        }
        return minioAvailable;
    }

    /**
     * Intenta reconectar con MinIO si el flag está en {@code false}.
     * Se invoca lazily en cada consulta de disponibilidad para auto-recuperarse
     * cuando MinIO se levanta después del backend.
     */
    private void tryReconnect() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(properties.getBucket()).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(properties.getBucket()).build()
                );
                log.info("Bucket MinIO '{}' creado en reconexión.", properties.getBucket());
            }
            minioAvailable = true;
            log.info("Reconexión con MinIO exitosa — servicio de archivos habilitado.");
        } catch (Exception e) {
            log.debug("MinIO sigue sin disponible: {}", e.getMessage());
        }
    }

    /** Verifica disponibilidad y lanza {@link MinioUnavailableException} si MinIO está caído. */
    private void requireMinio() {
        if (!isAvailable()) {
            throw new MinioUnavailableException();
        }
    }

    /**
     * Sube un archivo a MinIO.
     *
     * @param inputStream  contenido del archivo
     * @param objectKey    ruta/nombre del objeto dentro del bucket (ej. "estudios/12/uuid-archivo.pdf")
     * @param contentType  MIME type del archivo
     * @param size         tamaño en bytes (-1 si desconocido)
     */
    public void upload(InputStream inputStream, String objectKey, String contentType, long size) {
        requireMinio();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .stream(inputStream, size, -1)
                            .contentType(contentType != null ? contentType : "application/octet-stream")
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al subir archivo a MinIO [" + objectKey + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Genera una URL firmada (presigned) de descarga temporal.
     * La URL expira según {@code minio.presigned-expiry-minutes} (default 60 min).
     */
    public String getPresignedUrl(String objectKey) {
        requireMinio();
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(properties.getPresignedExpiryMinutes(), TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al generar URL firmada para [" + objectKey + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Devuelve un InputStream del objeto para streaming seguro a través del backend.
     * El caller es responsable de cerrar el stream.
     */
    public InputStream getObjectStream(String objectKey) {
        requireMinio();
        try {
            return minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al obtener archivo de MinIO [" + objectKey + "]: " + e.getMessage(), e);
        }
    }

    /**
     * Comprueba si un objeto existe en el bucket sin descargar su contenido.
     * Usa {@code statObject} (HEAD request) — muy ligero.
     * Devuelve {@code false} si el objeto no existe o si MinIO no está disponible.
     */
    public boolean objectExists(String objectKey) {
        requireMinio();
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
            return true;
        } catch (io.minio.errors.ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                return false;
            }
            throw new RuntimeException("Error al verificar existencia en MinIO [" + objectKey + "]: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error al verificar existencia en MinIO [" + objectKey + "]: " + e.getMessage(), e);
        }
    }

    /** Elimina un objeto del bucket. */
    public void delete(String objectKey) {
        requireMinio();
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(properties.getBucket())
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Error al eliminar archivo de MinIO [" + objectKey + "]: " + e.getMessage(), e);
        }
    }
}
