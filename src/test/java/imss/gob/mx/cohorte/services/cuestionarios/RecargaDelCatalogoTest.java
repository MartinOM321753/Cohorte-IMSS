package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacionRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.OpcionVariableRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.services.importacion.LectorArchivoTabular;
import imss.gob.mx.cohorte.services.importacion.TablaLeida;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Que al recargar el catálogo se vea lo que de verdad cambió, y que solo eso se
 * escriba.
 *
 * <h3>Por qué necesita prueba</h3>
 *
 * <p>La recarga dejó de reescribir las dos mil preguntas en cada pasada —tardaba más
 * de lo que el navegador espera— y ahora escribe nada más las que cambiaron. Eso solo
 * es seguro si la comparación ve todo lo que se escribe, y no lo veía: la fila y la
 * columna de las celdas de una rejilla se deducen <b>después</b> de leer el archivo,
 * así que al comparar seguían valiendo {@code null} y un bloque que solo gana su tabla
 * se daba por «igual». Mientras se reescribía todo, eso no se notaba; en cuanto se
 * dejó de reescribir, ese reparto no se habría aplicado nunca.</p>
 *
 * <p>El bloque de abajo es la forma mínima de una rejilla —tres filas por dos
 * columnas— y plantea la misma situación que las tablas reales del instrumento.</p>
 */
class RecargaDelCatalogoTest {

    private VariableCuestionarioRepository variables;
    private RespuestaCuestionarioRepository respuestas;
    private CargaCatalogoService servicio;

    /** Tres frutas por dos formas de prepararlas: seis celdas. */
    private static final List<String> CELDAS = List.of(
            "Manzana-Cruda", "Manzana-Cocida",
            "Platano-Cruda", "Platano-Cocida",
            "Mango-Cruda", "Mango-Cocida");

    @BeforeEach
    void preparar() {
        LectorArchivoTabular lector = mock(LectorArchivoTabular.class);
        variables = mock(VariableCuestionarioRepository.class);
        respuestas = mock(RespuestaCuestionarioRepository.class);

        SeccionCuestionarioRepository secciones = mock(SeccionCuestionarioRepository.class);
        TipoCuestionarioService tipos = mock(TipoCuestionarioService.class);

        TipoCuestionario tipo = new TipoCuestionario();
        tipo.setId(1L);
        tipo.setNombre("Cuestionario General");
        when(tipos.uno(anyLong())).thenReturn(tipo);

        SeccionCuestionario bloque = new SeccionCuestionario();
        bloque.setNombre("B1");
        when(secciones.findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(anyLong()))
                .thenReturn(List.of(bloque));

        servicio = new CargaCatalogoService(lector,
                new CargaCatalogoService.TipoCuestionarioRepositorios(
                        variables, mock(OpcionVariableRepository.class), secciones,
                        respuestas, mock(ReglaValidacionRepository.class), tipos));

        when(lector.leerHojas(any(MultipartFile.class), any()))
                .thenReturn(List.of(
                        new LectorArchivoTabular.HojaLeida("Variables", hojaVariables()),
                        new LectorArchivoTabular.HojaLeida("Catalogo", hojaOpciones())));
    }

    private TablaLeida hojaVariables() {
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < CELDAS.size(); i++) {
            filas.add(List.of("Q" + (i + 1), CELDAS.get(i), "CASILLA", "B1"));
        }
        return new TablaLeida(List.of("Campo", "Pregunta", "Tipo", "Bloque"), filas,
                numeros(filas.size()));
    }

    private TablaLeida hojaOpciones() {
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < CELDAS.size(); i++) {
            filas.add(List.of("Q" + (i + 1), "1", "Si"));
        }
        return new TablaLeida(List.of("Campo", "Codigo", "Valor"), filas, numeros(filas.size()));
    }

    private static List<Integer> numeros(int cuantas) {
        List<Integer> n = new ArrayList<>();
        for (int i = 0; i < cuantas; i++) n.add(i + 2);
        return n;
    }

    /**
     * Lo que ya está guardado: las mismas seis preguntas, en el mismo bloque y con la
     * misma opción. {@code conRejilla} decide si además tienen su fila y su columna.
     */
    private void yaGuardadas(boolean conRejilla) {
        yaGuardadas(conRejilla, 0);
    }

    /**
     * @param desplazamiento cuanto corrido esta el orden guardado respecto al del
     *                       archivo, como queda cuando alguien inserta preguntas mas
     *                       arriba en el libro
     */
    private void yaGuardadas(boolean conRejilla, int desplazamiento) {
        SeccionCuestionario bloque = new SeccionCuestionario();
        bloque.setNombre("B1");

        List<VariableCuestionario> existentes = new ArrayList<>();
        for (int i = 0; i < CELDAS.size(); i++) {
            VariableCuestionario v = new VariableCuestionario();
            v.setId((long) (i + 1));
            v.setCodigo("Q" + (i + 1));
            v.setColumnaVolcado("Q" + (i + 1));
            v.setPregunta(CELDAS.get(i));
            v.setTipo(TipoVariable.CASILLA);
            v.setSeccion(bloque);
            v.setObligatoria(false);
            v.setOrden(i + desplazamiento);
            if (conRejilla) {
                String[] partes = CELDAS.get(i).split("-");
                v.setFilaMatriz(partes[0]);
                v.setColumnaMatriz(partes[1]);
            }
            OpcionVariable o = new OpcionVariable();
            o.setCodigo("1");
            o.setEtiqueta("Si");
            o.setVariable(v);
            v.setOpciones(new ArrayList<>(List.of(o)));
            existentes.add(v);
        }
        when(variables.findTodasConOpciones(anyLong())).thenReturn(existentes);
    }

    private PrevisualizacionCatalogo previsualizar() {
        return servicio.previsualizar(
                new MockMultipartFile("f", "catalogo.xlsx", null, new byte[] {1}), 1L, null);
    }

    @Test
    @DisplayName("un bloque que gana su rejilla cuenta como cambio")
    void laRejillaCuentaComoCambio() {
        yaGuardadas(false);
        PrevisualizacionCatalogo p = previsualizar();
        assertEquals(6, p.variablesActualizadas(),
                "si esto da cero, el reparto en filas y columnas no se aplica al recargar");
        assertEquals(0, p.variablesSinCambio());
    }

    @Test
    @DisplayName("recargar el mismo archivo no cambia nada")
    void recargarLoMismoNoCambiaNada() {
        yaGuardadas(true);
        PrevisualizacionCatalogo p = previsualizar();
        assertEquals(6, p.variablesSinCambio(),
                "reescribir lo que no cambio es lo que sacaba la carga del tiempo de "
                        + "espera del navegador");
        assertEquals(0, p.variablesActualizadas());
        assertEquals(0, p.variablesNuevas());
    }

    @Test
    @DisplayName("a una pregunta que no cambio no se le pregunta por sus respuestas")
    void noSePreguntaPorLoQueNoCambio() {
        yaGuardadas(true);
        previsualizar();
        verify(respuestas, never()).existsByVariable_Id(anyLong());
    }

    /**
     * El caso que volvió a tumbar la carga después de darla por arreglada.
     *
     * <p>Insertar seis preguntas en medio del libro corre el orden de las mil
     * setecientas cuarenta y seis que vienen detrás. Ninguna cambia nada que pueda
     * tocar una respuesta —ni el tipo ni las opciones—, pero todas cuentan como
     * «actualiza», y preguntarle a la base por cada una son dos consultas: tres mil
     * quinientas para nada, que es lo que volvió a pasarse del tiempo de espera del
     * navegador.</p>
     */
    @Test
    @DisplayName("a una pregunta que solo se movio de sitio tampoco")
    void noSePreguntaPorLoQueSoloSeMovio() {
        yaGuardadas(true, 6);

        PrevisualizacionCatalogo p = previsualizar();
        assertEquals(6, p.variablesActualizadas(), "moverse si es un cambio que hay que guardar");

        verify(respuestas, never()).existsByVariable_Id(anyLong());
        verify(respuestas, never()).frecuenciaPorCodigo(anyLong());
    }

    @Test
    @DisplayName("a una que cambia de tipo si se le pregunta")
    void siSePreguntaPorElTipo() {
        // El archivo la trae como CASILLA. Guardada como OPCION_UNICA, el codigo que ya
        // esta en las respuestas puede dejar de interpretarse igual, y eso si hay que
        // mirarlo antes de cargar.
        yaGuardadas(true);
        List<VariableCuestionario> guardadas = variables.findTodasConOpciones(1L);
        guardadas.get(0).setTipo(TipoVariable.OPCION_UNICA);

        previsualizar();
        verify(respuestas).existsByVariable_Id(guardadas.get(0).getId());
    }
}
