package imss.gob.mx.cohorte.modules.reportes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Igual que con las plantillas: toda consulta lleva la institución. Una imagen es del
 * catálogo de quien la subió, y buscar por id a secas es lo que deja leer el membrete
 * de otra institución pasando un número.
 */
@Repository
public interface ImagenReporteRepository extends JpaRepository<ImagenReporte, Long> {

    List<ImagenReporte> findAllByInstitucion_IdOrderByNombreAsc(Long idInstitucion);

    Optional<ImagenReporte> findByNombreIgnoreCaseAndInstitucion_Id(String nombre, Long idInstitucion);

    /**
     * Las plantillas que usan esta imagen.
     *
     * <p>Busca el texto de la referencia dentro del diseño. Es una búsqueda por
     * contenido y no un join porque el diseño es un árbol JSON en una columna: la
     * alternativa sería normalizar los elementos en tablas solo para poder responder
     * esta pregunta, que se hace una vez al borrar.</p>
     *
     * <p>Se compara contra {@code "imagen:{id}"} <b>con las comillas</b> del JSON, o
     * borrar la imagen 1 se creería usada por toda plantilla que llevara la 10.</p>
     */
    @Query("""
            SELECT p.nombre FROM PlantillaReporte p
            WHERE p.institucion.id = :idInstitucion
              AND p.diseno LIKE CONCAT('%"imagen:', :idImagen, '"%')
            ORDER BY p.nombre
            """)
    List<String> nombresDePlantillasQueLaUsan(@Param("idInstitucion") Long idInstitucion,
                                              @Param("idImagen") Long idImagen);
}
