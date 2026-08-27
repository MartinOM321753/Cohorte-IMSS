package imss.gob.mx.cohorte.controllers.estudios.dto;

import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Un resultado guardado tiene que decir a qué parámetro pertenece por su id.
 *
 * <p>La respuesta solo llevaba el nombre del parámetro, así que la pantalla emparejaba
 * comparando cadenas. Eso ata los resultados históricos a la ortografía exacta que
 * tenía el parámetro el día de la captura: renombrarlo —o corregirle una tilde—
 * desconecta de golpe todos sus resultados anteriores, que al quedarse sin pareja
 * desaparecen de la vista sin avisar.</p>
 *
 * <p>El nombre se sigue enviando, pero solo para mostrarse.</p>
 */
class ResultadoIdentificaParametroTest {

    private ResultadoEstudio resultadoDe(Long idParametro, String nombreParametro) {
        ParametroEstudio parametro = new ParametroEstudio();
        parametro.setId(idParametro);
        parametro.setNombre(nombreParametro);

        ResultadoEstudio resultado = new ResultadoEstudio();
        resultado.setParametro(parametro);
        resultado.setValorNumerico(72.0);
        resultado.setGrupoCodigo("ROOT");
        resultado.setOrdenResultado(0);
        return resultado;
    }

    private EstudioMedico estudioCon(ResultadoEstudio... resultados) {
        EstudioMedico estudio = new EstudioMedico();
        estudio.setId(1L);
        estudio.setResultadoEstudio(List.of(resultados));
        return estudio;
    }

    @Test
    @DisplayName("La respuesta lleva el id del parámetro, no solo su nombre")
    void llevaElIdDelParametro() {
        EstudioMedicoResponseDTO dto = EstudioMapper.toResponseDTO(
                estudioCon(resultadoDe(229L, "P")));

        ResultadoEstudioResponseDTO resultado = dto.getResultados().get(0);
        assertEquals(229L, resultado.getIdParametro(),
                "Sin el id no hay forma estable de saber a qué parámetro pertenece");
        assertEquals("P", resultado.getParametro(),
                "El nombre se conserva para mostrarlo");
    }

    /**
     * Dos parámetros del mismo estudio con nombres parecidos: emparejar por texto
     * es justo lo que falla aquí.
     */
    @Test
    @DisplayName("Cada resultado conserva su propio id aunque los nombres se parezcan")
    void cadaResultadoConservaSuId() {
        EstudioMedicoResponseDTO dto = EstudioMapper.toResponseDTO(estudioCon(
                resultadoDe(121L, "Eje P"),
                resultadoDe(229L, "P")));

        assertEquals(121L, dto.getResultados().get(0).getIdParametro());
        assertEquals(229L, dto.getResultados().get(1).getIdParametro());
    }

    @Test
    @DisplayName("Un resultado sin parámetro no revienta el mapeo")
    void resultadoSinParametroNoRevienta() {
        ResultadoEstudio huerfano = new ResultadoEstudio();
        huerfano.setGrupoCodigo("ROOT");
        huerfano.setOrdenResultado(0);

        EstudioMedicoResponseDTO dto = EstudioMapper.toResponseDTO(estudioCon(huerfano));

        assertNull(dto.getResultados().get(0).getIdParametro());
        assertNull(dto.getResultados().get(0).getParametro());
    }
}
