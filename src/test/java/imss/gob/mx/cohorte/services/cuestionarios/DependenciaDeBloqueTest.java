package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariableRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OperadorDependencia;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Poner de una vez la pregunta llave de todo un bloque.
 *
 * <p>Existe por una razón de cantidad: el «Ninguna» que encabeza la 112 apaga las
 * ciento treinta y cinco celdas de la rejilla. Capturarlo pregunta por pregunta no es
 * trabajoso, es que nadie lo haría — y quien lo intentara se saltaría alguna sin que
 * nada lo delatara.</p>
 */
class DependenciaDeBloqueTest {

    private VariableCuestionarioRepository variables;
    private VariableCuestionarioService servicio;
    private List<VariableCuestionario> bloque;

    @BeforeEach
    void preparar() {
        variables = mock(VariableCuestionarioRepository.class);
        var opciones = mock(OpcionVariableRepository.class);
        var respuestas = mock(RespuestaCuestionarioRepository.class);
        var tipoService = mock(TipoCuestionarioService.class);
        var contexto = mock(InstitucionContextService.class);

        servicio = new VariableCuestionarioService(variables, opciones, respuestas,
                tipoService, contexto);

        SeccionCuestionario seccion = new SeccionCuestionario();
        seccion.setId(7L);
        seccion.setNombre("P112");
        when(tipoService.seccion(7L)).thenReturn(seccion);

        bloque = new ArrayList<>();
        bloque.add(pregunta("P112_1"));                       // el «Ninguna», la llave
        for (int i = 2; i <= 10; i++) bloque.add(pregunta("P112_" + i));
        when(variables.findDeSeccionConOpciones(7L)).thenReturn(bloque);
    }

    private VariableCuestionario pregunta(String codigo) {
        VariableCuestionario v = new VariableCuestionario();
        v.setCodigo(codigo);
        return v;
    }

    private Map<String, VariableCuestionario> porCodigo() {
        return bloque.stream().collect(Collectors.toMap(VariableCuestionario::getCodigo, v -> v));
    }

    @Test
    @DisplayName("se la pone a todo el bloque menos a la propia llave")
    void todasMenosLaLlave() {
        int tocadas = servicio.dependenciaDeBloque(7L, "P112_1", "1", OperadorDependencia.DISTINTO);

        assertEquals(9, tocadas);
        var mapa = porCodigo();

        // La llave no puede depender de si misma: segun el operador no se mostraria
        // nunca o se mostraria siempre, y en los dos casos por un motivo invisible.
        assertNull(mapa.get("P112_1").getDependeDe());

        for (int i = 2; i <= 10; i++) {
            VariableCuestionario v = mapa.get("P112_" + i);
            assertEquals("P112_1", v.getDependeDe());
            assertEquals("1", v.getValorHabilitante());
            assertEquals(OperadorDependencia.DISTINTO, v.getOperadorDependencia());
        }
        verify(variables).saveAll(bloque);
    }

    @Test
    @DisplayName("la llave se reconoce aunque se escriba de otra forma")
    void llaveConOtrasMayusculas() {
        servicio.dependenciaDeBloque(7L, "p112_1", "1", OperadorDependencia.DISTINTO);

        assertNull(porCodigo().get("P112_1").getDependeDe(),
                "una diferencia de mayusculas no deberia hacer que la llave dependa de si misma");
    }

    @Test
    @DisplayName("sin llave se quita la dependencia de todo el bloque")
    void quitarla() {
        servicio.dependenciaDeBloque(7L, "P112_1", "1", OperadorDependencia.DISTINTO);
        int tocadas = servicio.dependenciaDeBloque(7L, null, null, null);

        assertEquals(10, tocadas, "sin llave que excluir, se tocan todas");
        for (VariableCuestionario v : bloque) {
            assertNull(v.getDependeDe());
            assertNull(v.getValorHabilitante());
            assertEquals(OperadorDependencia.IGUAL, v.getOperadorDependencia());
        }
    }

    @Test
    @DisplayName("sin operador se asume igualdad, que es lo de siempre")
    void operadorPorOmision() {
        servicio.dependenciaDeBloque(7L, "P112_1", "1", null);

        assertEquals(OperadorDependencia.IGUAL, porCodigo().get("P112_2").getOperadorDependencia());
    }
}
