package imss.gob.mx.cohorte.services.estudios;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.OrdenDeResultados;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * En qué orden se lee un estudio que ya estaba guardado cuando el administrador
 * reacomoda el catálogo.
 *
 * <p>Es la mitad menos obvia de la funcionalidad. La fila del resultado guarda su
 * propio {@code ordenResultado}, escrito el día de la captura, y lo natural sería
 * pensar que ahí vive la posición del parámetro. No: ese número dice a qué grupo
 * pertenece la medición —en la captura normal vale cero para todas—, y dentro del
 * grupo el orden lo terminaba decidiendo el id. Por eso reacomodar el catálogo sí
 * alcanza a los estudios viejos: lo único que se pisa es un desempate por id que
 * nadie eligió.</p>
 */
class OrdenDeResultadosTest {

    private ResultadoEstudio resultado(long id, long idParametro, Integer ordenCatalogo,
                                       String grupo, int ordenResultado) {
        ParametroEstudio parametro = new ParametroEstudio();
        parametro.setId(idParametro);
        parametro.setOrden(ordenCatalogo);

        ResultadoEstudio r = new ResultadoEstudio();
        r.setId(id);
        r.setParametro(parametro);
        r.setGrupoCodigo(grupo);
        r.setOrdenResultado(ordenResultado);
        return r;
    }

    private List<Long> idsOrdenados(List<ResultadoEstudio> resultados) {
        return resultados.stream().sorted(OrdenDeResultados.POR_CATALOGO)
                .map(ResultadoEstudio::getId).toList();
    }

    @Test
    @DisplayName("Manda el orden del catálogo, no el de inserción")
    void mandaElCatalogo() {
        // Se capturaron en el orden 10, 11, 12; hoy el catálogo los quiere al revés.
        List<ResultadoEstudio> resultados = List.of(
                resultado(10L, 1L, 2, "ROOT", 0),
                resultado(11L, 2L, 1, "ROOT", 0),
                resultado(12L, 3L, 0, "ROOT", 0));

        assertEquals(List.of(12L, 11L, 10L), idsOrdenados(resultados));
    }

    @Test
    @DisplayName("Los grupos no se mezclan: primero el grupo, luego el orden dentro de él")
    void losGruposMandanPrimero() {
        List<ResultadoEstudio> resultados = List.of(
                resultado(20L, 1L, 1, "GRUPO_2", 2),
                resultado(21L, 2L, 0, "GRUPO_2", 2),
                resultado(22L, 1L, 1, "GRUPO_1", 1),
                resultado(23L, 2L, 0, "GRUPO_1", 1));

        // Grupo 1 completo y ordenado por catálogo, después el grupo 2 igual.
        assertEquals(List.of(23L, 22L, 21L, 20L), idsOrdenados(resultados));
    }

    @Test
    @DisplayName("Una fila sin parámetro se va al final, no se cuela en medio")
    void elHuerfanoAlFinal() {
        ResultadoEstudio huerfano = new ResultadoEstudio();
        huerfano.setId(30L);
        huerfano.setGrupoCodigo("ROOT");
        huerfano.setOrdenResultado(0);

        List<ResultadoEstudio> resultados = List.of(
                huerfano,
                resultado(31L, 1L, 1, "ROOT", 0),
                resultado(32L, 2L, 0, "ROOT", 0));

        assertEquals(List.of(32L, 31L, 30L), idsOrdenados(resultados));
    }

    @Test
    @DisplayName("Con el mismo orden en el catálogo, el id sigue desempatando")
    void elIdDesempata() {
        List<ResultadoEstudio> resultados = List.of(
                resultado(41L, 1L, 0, "ROOT", 0),
                resultado(40L, 2L, 0, "ROOT", 0));

        assertEquals(List.of(40L, 41L), idsOrdenados(resultados));
    }
}
