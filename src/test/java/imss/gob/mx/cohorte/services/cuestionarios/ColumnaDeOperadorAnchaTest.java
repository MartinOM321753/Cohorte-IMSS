package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacionRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariableRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.services.importacion.ArchivoInvalidoException;
import imss.gob.mx.cohorte.services.importacion.LectorArchivoTabular;
import imss.gob.mx.cohorte.services.importacion.TablaLeida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Que el catálogo no se cargue contra una columna que no le cabe.
 *
 * <h3>Por qué necesita prueba</h3>
 *
 * <p>{@code operador_dependencia} nació {@code VARCHAR(10)} y {@code ddl-auto=update}
 * no ensancha columnas. {@code SIN_RESPUESTA} mide 13: cargado contra la columna
 * angosta se guarda como «SIN_RESPUE» y el cuestionario no abre. La guarda tiene que
 * saltar <b>antes</b> de escribir, y tiene que dejar pasar todo lo demás —los
 * catálogos viejos, las bases ya ensanchadas y las que no se pueden medir—, porque
 * una guarda que estorba donde no debe se acaba quitando.</p>
 */
class ColumnaDeOperadorAnchaTest {

    private EsquemaOperadorDependencia esquema;
    private CargaCatalogoService servicio;
    private LectorArchivoTabular lector;

    @BeforeEach
    void preparar() {
        lector = mock(LectorArchivoTabular.class);
        esquema = mock(EsquemaOperadorDependencia.class);

        TipoCuestionarioService tipos = mock(TipoCuestionarioService.class);
        TipoCuestionario tipo = new TipoCuestionario();
        tipo.setId(1L);
        tipo.setNombre("Cuestionario General");
        when(tipos.uno(anyLong())).thenReturn(tipo);

        servicio = new CargaCatalogoService(lector,
                new CargaCatalogoService.TipoCuestionarioRepositorios(
                        mock(VariableCuestionarioRepository.class),
                        mock(OpcionVariableRepository.class),
                        mock(SeccionCuestionarioRepository.class),
                        mock(RespuestaCuestionarioRepository.class),
                        mock(ReglaValidacionRepository.class), tipos),
                esquema);
    }

    /** Dos preguntas: la 2 depende de la 1 con el operador que se pida. */
    private void archivoConOperador(String operador) {
        TablaLeida variables = new TablaLeida(
                List.of("Campo", "Pregunta", "Tipo", "Bloque", "Depende de", "Operador"),
                List.of(List.of("P191", "A que se dedica", "TEXTO", "B1", "", ""),
                        List.of("P192", "Ocupacion", "TEXTO", "B1", "P191", operador)),
                List.of(2, 3));
        TablaLeida opciones = new TablaLeida(
                List.of("Campo", "Codigo", "Valor"), List.of(), List.of());
        when(lector.leerHojas(any(MultipartFile.class), any()))
                .thenReturn(List.of(
                        new LectorArchivoTabular.HojaLeida("Variables", variables),
                        new LectorArchivoTabular.HojaLeida("Catalogo", opciones)));
    }

    private void previsualizar() {
        servicio.previsualizar(
                new MockMultipartFile("f", "catalogo.xlsx", null, new byte[]{1}), 1L, null);
    }

    @Test
    @DisplayName("con la columna en 10, un catalogo con SIN_RESPUESTA no se carga")
    void columnaAngostaConOperadorNuevo() {
        archivoConOperador("SIN_RESPUESTA");
        when(esquema.anchoActual()).thenReturn(OptionalInt.of(10));

        ArchivoInvalidoException e = assertThrows(ArchivoInvalidoException.class, this::previsualizar);
        assertTrue(e.getMessage().contains("2026-11-06_operador_dependencia_ancho.sql"),
                "el mensaje tiene que decir cual es el script: " + e.getMessage());
    }

    @Test
    @DisplayName("lo mismo con CON_RESPUESTA")
    void tambienConRespuesta() {
        archivoConOperador("CON_RESPUESTA");
        when(esquema.anchoActual()).thenReturn(OptionalInt.of(10));
        assertThrows(ArchivoInvalidoException.class, this::previsualizar);
    }

    @Test
    @DisplayName("ensanchada a 20, carga")
    void columnaAncha() {
        archivoConOperador("SIN_RESPUESTA");
        when(esquema.anchoActual()).thenReturn(OptionalInt.of(20));
        assertDoesNotThrow(this::previsualizar);
    }

    @Test
    @DisplayName("un catalogo sin operadores nuevos carga aunque la columna siga en 10")
    void catalogoViejoNoSeEstorba() {
        archivoConOperador("DISTINTO");   // 8 caracteres: cabe en 10
        when(esquema.anchoActual()).thenReturn(OptionalInt.of(10));
        assertDoesNotThrow(this::previsualizar);
    }

    @Test
    @DisplayName("si no se puede medir la columna, no bloquea")
    void sinMedidaNoBloquea() {
        archivoConOperador("SIN_RESPUESTA");
        when(esquema.anchoActual()).thenReturn(OptionalInt.empty());
        assertDoesNotThrow(this::previsualizar);
    }
}
