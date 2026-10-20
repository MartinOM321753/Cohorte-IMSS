package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariableRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Crear una pregunta desde la pantalla, con lo que la pantalla no manda.
 *
 * <h3>Por qué esto necesita prueba</h3>
 *
 * <p>Los campos que no se mandan llegan en {@code null}, y eso es deliberado: {@code
 * null} significa «no lo dije», que es lo que permite que la edición deje en paz lo que
 * nadie tocó. Pero cuatro de esos campos son columnas obligatorias —orden, obligatoria,
 * documentada, activa—, así que en el alta hay que rellenarlos. Si no, crear una
 * pregunta termina en un error de la base con un mensaje que no dice nada, y eso no se
 * ve leyendo el código: se ve la primera vez que alguien pulsa «Crear».</p>
 */
class AltaDePreguntaTest {

    private VariableCuestionarioRepository variables;
    private VariableCuestionarioService servicio;
    private TipoCuestionarioService tipoService;

    @BeforeEach
    void preparar() {
        variables = mock(VariableCuestionarioRepository.class);
        var opciones = mock(OpcionVariableRepository.class);
        var respuestas = mock(RespuestaCuestionarioRepository.class);
        tipoService = mock(TipoCuestionarioService.class);
        var contexto = mock(InstitucionContextService.class);

        servicio = new VariableCuestionarioService(variables, opciones, respuestas,
                tipoService, contexto);

        Institucion institucion = new Institucion();
        institucion.setId(1L);
        TipoCuestionario tipo = new TipoCuestionario();
        tipo.setId(3L);
        tipo.setInstitucion(institucion);

        SeccionCuestionario bloque = new SeccionCuestionario();
        bloque.setId(7L);
        bloque.setNombre("Tabaco");

        when(tipoService.uno(3L)).thenReturn(tipo);
        when(tipoService.seccion(7L)).thenReturn(bloque);
        when(variables.findByTipoCuestionario_IdAndCodigoIgnoreCase(anyLong(), any()))
                .thenReturn(Optional.empty());
        when(variables.save(any())).thenAnswer(i -> i.getArgument(0));
        when(variables.ultimoOrdenDe(7L)).thenReturn(12);
        when(respuestas.existsByVariable_Id(anyLong())).thenReturn(false);
    }

    private VariableCuestionario soloLoIndispensable() {
        VariableCuestionario datos = new VariableCuestionario();
        datos.setCodigo("P12_B");
        datos.setTipo(TipoVariable.NUMERO);
        // Todo lo demas se queda en null: es lo que manda la pantalla cuando el
        // investigador solo escribe el codigo y el tipo.
        datos.setOrden(null);
        datos.setObligatoria(null);
        datos.setDocumentada(null);
        datos.setActiva(null);
        return datos;
    }

    @Test
    @DisplayName("lo que no se manda se rellena, en lugar de reventar contra la base")
    void seRellenaLoOmitido() {
        VariableCuestionario creada = servicio.crear(3L, soloLoIndispensable(), 7L, List.of());

        assertNotNull(creada.getOrden(), "las cuatro columnas son obligatorias");
        assertNotNull(creada.getObligatoria());
        assertNotNull(creada.getDocumentada());
        assertNotNull(creada.getActiva());

        assertFalse(creada.getObligatoria(), "una pregunta nueva no obliga a nadie");
        assertTrue(creada.getActiva(), "se crea para usarse");
    }

    @Test
    @DisplayName("la pregunta nueva va al final de su bloque, no al principio")
    void vaAlFinal() {
        VariableCuestionario creada = servicio.crear(3L, soloLoIndispensable(), 7L, List.of());

        // Con orden 0 aparecería ANTES de las que ya estaban, moviendo de sitio un
        // bloque que alguien ya ordenó como el papel.
        assertEquals(13, creada.getOrden());
    }

    @Test
    @DisplayName("sin bloque tambien se puede crear")
    void sinBloque() {
        VariableCuestionario creada = servicio.crear(3L, soloLoIndispensable(), null, List.of());
        assertEquals(0, creada.getOrden());
        assertNull(creada.getSeccion());
    }

    @Test
    @DisplayName("«documentada» se deduce del enunciado, no se supone")
    void documentadaSaleDelEnunciado() {
        VariableCuestionario sinTexto = servicio.crear(3L, soloLoIndispensable(), 7L, List.of());
        assertFalse(sinTexto.getDocumentada(),
                "sin enunciado es justo una de las que faltan por redactar: marcarla como "
                        + "documentada la esconde de esa lista");

        VariableCuestionario datos = soloLoIndispensable();
        datos.setPregunta("¿Cuántos cigarros al día?");
        assertTrue(servicio.crear(3L, datos, 7L, List.of()).getDocumentada());
    }

    @Test
    @DisplayName("lo que si se manda se respeta")
    void loMandadoManda() {
        VariableCuestionario datos = soloLoIndispensable();
        datos.setOrden(2);
        datos.setObligatoria(true);
        datos.setActiva(false);
        datos.setDocumentada(true);

        VariableCuestionario creada = servicio.crear(3L, datos, 7L, List.of());

        assertEquals(2, creada.getOrden());
        assertTrue(creada.getObligatoria());
        assertFalse(creada.getActiva());
        assertTrue(creada.getDocumentada());
        verify(variables, never()).ultimoOrdenDe(anyLong());
    }
}
