package imss.gob.mx.cohorte.infrastructure.minio;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;


/**
 * Servicio de almacenamiento de objetos usando MinIO (S3-compatible).
 * Gestiona la subida, descarga (URL firmada) y eliminación de archivos.
 */
@Service
public class MinioStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private final MinioClient minioClient;
    private final MinioProperties properties;

    public MinioStorageService(MinioClient minioClient, MinioProperties properties) {
        this.minioClient = minioClient;
        this.properties = properties;
    }

    /** Crea el bucket si no existe al arrancar la aplicación. */
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
        } catch (Exception e) {
            log.error("Error al inicializar bucket MinIO: {}", e.getMessage());
            throw new RuntimeException("No se pudo conectar con MinIO al iniciar: " + e.getMessage(), e);
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

    /** Elimina un objeto del bucket. */
    public void delete(String objectKey) {
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
