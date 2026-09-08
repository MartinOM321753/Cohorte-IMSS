package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Recargar un estudio por archivo no debe borrar lo que el archivo no menciona.
 *
 * <p>Al recargar con la política de reemplazar, el servicio vaciaba la lista de
 * resultados y metía los del archivo. Mientras el archivo tuviera columna para
 * todos los parámetros daba igual, porque los reponía todos. Deja de dar igual en
 * cuanto un parámetro sale del catálogo: su columna ya no viaja en el archivo, pero
 * su valor sigue siendo parte de esa captura, y el reemplazo lo borraba sin que
 * nadie lo hubiera pedido.</p>
 *
 * <p>Se prueba la regla de decisión —qué se conserva y qué se sustituye— sin
 * levantar el contexto: es lógica sobre dos listas.</p>
 */
class ReemplazoConservaLoNoMencionadoTest {

    private ResultadoEstudio resultado(long idParametro, String valor) {
        ParametroEstudio p = new ParametroEstudio();
        p.setId(idParametro);
        ResultadoEstudio r = new ResultadoEstudio();
        r.setParametro(p);
        r.setValorTexto(valor);
        return r;
    }

    /**
     * La misma decisión que toma reemplazarResultados: se queda lo previo cuyo
     * parámetro no viene en la carga, y encima se añade todo lo que sí viene.
     */
    private List<ResultadoEstudio> reemplazar(List<ResultadoEstudio> previos,
                                              List<ResultadoEstudio> nuevos) {
        Set<Long> vienenEnLaCarga = nuevos.stream()
                .map(r -> r.getParametro().getId())
                .collect(Collectors.toSet());

        List<ResultadoEstudio> finales = new ArrayList<>(previos.stream()
                .filter(r -> r.getParametro() != null
                        && !vienenEnLaCarga.contains(r.getParametro().getId()))
                .toList());
        finales.addAll(nuevos);
        return finales;
    }

    private List<Long> idsDe(List<ResultadoEstudio> rs) {
        return rs.stream().map(r -> r.getParametro().getId()).sorted().toList();
    }

    @Test
    @DisplayName("Un parámetro que el archivo no menciona conserva su valor")
    void conservaLoQueElArchivoNoMenciona() {
        List<ResultadoEstudio> previos = List.of(
                resultado(121L, "sinusal"),
                resultado(229L, "valor del parámetro retirado"));

        // El archivo solo trae la columna del 121: el 229 ya no está en el catálogo.
        List<ResultadoEstudio> resultado = reemplazar(previos, List.of(resultado(121L, "no sinusal")));

        assertEquals(List.of(121L, 229L), idsDe(resultado),
                "El parámetro retirado tiene que seguir ahí");
        assertEquals("valor del parámetro retirado",
                resultado.stream().filter(r -> r.getParametro().getId() == 229L)
                        .findFirst().orElseThrow().getValorTexto(),
                "Y con su valor intacto");
    }

    @Test
    @DisplayName("Lo que el archivo sí trae se sustituye por el valor nuevo")
    void sustituyeLoQueElArchivoTrae() {
        List<ResultadoEstudio> resultado = reemplazar(
                List.of(resultado(121L, "sinusal")),
                List.of(resultado(121L, "no sinusal")));

        assertEquals(1, resultado.size(), "No puede quedar duplicado el mismo parámetro");
        assertEquals("no sinusal", resultado.get(0).getValorTexto());
    }

    @Test
    @DisplayName("Una carga que trae todo deja el estudio exactamente como el archivo")
    void cargaCompletaReemplazaTodo() {
        List<ResultadoEstudio> resultado = reemplazar(
                List.of(resultado(121L, "a"), resultado(122L, "b")),
                List.of(resultado(121L, "x"), resultado(122L, "y")));

        assertEquals(List.of(121L, 122L), idsDe(resultado));
        assertTrue(resultado.stream().allMatch(r -> List.of("x", "y").contains(r.getValorTexto())),
                "Ningún valor viejo debe sobrevivir cuando el archivo lo menciona");
    }

    @Test
    @DisplayName("Un estudio sin resultados previos queda solo con los del archivo")
    void estudioVacio() {
        List<ResultadoEstudio> resultado = reemplazar(List.of(), List.of(resultado(121L, "a")));
        assertEquals(List.of(121L), idsDe(resultado));
    }
}
