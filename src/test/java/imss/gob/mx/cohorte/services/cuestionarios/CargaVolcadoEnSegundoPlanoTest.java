package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.carga.EstadoTareaVolcado;
import imss.gob.mx.cohorte.modules.cuestionarios.carga.TareaCargaVolcado;
import imss.gob.mx.cohorte.modules.cuestionarios.carga.TareaCargaVolcadoRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.CuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.discrepancias.DiscrepanciaLecturaRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacionRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import org.springframework.data.jpa.repository.Query;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.importacion.ArchivoInvalidoException;
import imss.gob.mx.cohorte.services.importacion.LectorArchivoTabular;
import imss.gob.mx.cohorte.services.importacion.TablaLeida;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import org.springframework.security.access.AccessDeniedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * La carga del volcado corriendo en segundo plano.
 *
 * <p>Lo que se prueba aquí no es que guarde bien —eso es de {@code guardarFila}— sino
 * las tres promesas que la hacen usable y que no se ven leyendo el código: que una fila
 * mala no tira la carga entera, que lo que se corta queda dicho en lugar de quedar en
 * silencio, y que el avance se publica mientras avanza y no al final, que es lo único
 * que permite enseñárselo a quien subió el archivo.</p>
 */
class CargaVolcadoEnSegundoPlanoTest {

    private LectorArchivoTabular lector;
    private CargaVolcadoService volcado;
    private RegistroTareaVolcado registro;
    private CargaVolcadoWorker worker;

    private Institucion institucion;
    private BeanUser usuario;
    /** El instrumento llega resuelto: en el hilo de la carga no hay a quien preguntarle. */
    private TipoCuestionario instrumento;

    @BeforeEach
    void preparar() {
        lector = mock(LectorArchivoTabular.class);
        volcado = mock(CargaVolcadoService.class);
        registro = mock(RegistroTareaVolcado.class);
        worker = new CargaVolcadoWorker(lector, volcado, registro);

        institucion = new Institucion();
        institucion.setId(1L);
        usuario = new BeanUser();
        instrumento = new TipoCuestionario();
        instrumento.setId(3L);
        instrumento.setNombre("Cuestionario general");
    }

    /** Un análisis ya listo, con tantas filas como se pidan. */
    private CargaVolcadoService.Analisis analisisDe(int filas) {
        CargaVolcadoService.Analisis a = new CargaVolcadoService.Analisis();
        a.tipo = new TipoCuestionario();
        a.idInstitucion = 1L;
        a.columnaEtiqueta = 0;
        List<List<String>> celdas = new java.util.ArrayList<>();
        List<Integer> numeros = new java.util.ArrayList<>();
        for (int i = 0; i < filas; i++) {
            celdas.add(List.of("C1/00000" + (i + 1) + "/F4"));
            numeros.add(i + 2);   // +2: la 1 es el encabezado
        }
        a.tabla = new TablaLeida(List.of("FOLIO"), celdas, numeros);
        return a;
    }

    private CargaVolcadoService.ResultadoFila guardada(int numeroFila) {
        return new CargaVolcadoService.ResultadoFila(
                numeroFila, "C1/000001/F4", null, true, 2000, 0, 0);
    }

    @Test
    @DisplayName("Una fila que falla no se lleva por delante a las demás")
    void unaFilaMalaNoParaLaCarga() {
        CargaVolcadoService.Analisis a = analisisDe(3);
        when(lector.leerHojas(any(byte[].class), any())).thenReturn(List.of());
        when(volcado.preparar(any(), any(), anyLong())).thenReturn(a);
        when(volcado.guardarFila(same(a), eq(0), anyBoolean(), any(), any(), any()))
                .thenReturn(guardada(2));
        when(volcado.guardarFila(same(a), eq(1), anyBoolean(), any(), any(), any()))
                .thenThrow(new RuntimeException("se cayo la conexion"));
        when(volcado.guardarFila(same(a), eq(2), anyBoolean(), any(), any(), any()))
                .thenReturn(guardada(4));

        worker.procesar(7L, new byte[]{1}, "volcado.xlsx", instrumento, false,
                institucion, usuario);

        ArgumentCaptor<CargaVolcadoService.ResultadoFila> anotadas =
                ArgumentCaptor.forClass(CargaVolcadoService.ResultadoFila.class);
        verify(registro, times(3)).anotar(eq(7L), anotadas.capture());

        // Las tres filas se informan, y la que falló dice por qué y con qué número de
        // fila del archivo: sin eso no hay forma de repetir solo esa.
        List<CargaVolcadoService.ResultadoFila> r = anotadas.getAllValues();
        assertNull(r.get(0).motivoOmision());
        assertEquals(3, r.get(1).numeroFila());
        assertTrue(r.get(1).motivoOmision().contains("se cayo la conexion"),
                "el motivo tiene que decir qué pasó: " + r.get(1).motivoOmision());
        assertNull(r.get(2).motivoOmision());

        // Y la carga termina. Terminada no quiere decir que todo entrara.
        verify(registro).terminar(7L);
        verify(registro, never()).fallar(anyLong(), any());
    }

    @Test
    @DisplayName("Si el archivo no se puede leer, la tarea queda fallida y lo dice")
    void elArchivoIlegibleDejaDichoElMotivo() {
        when(lector.leerHojas(any(byte[].class), any()))
                .thenThrow(new ArchivoInvalidoException("El archivo no trae la columna FOLIO."));

        worker.procesar(9L, new byte[]{1}, "otro.xlsx", instrumento, false,
                institucion, usuario);

        verify(registro).fallar(9L, "El archivo no trae la columna FOLIO.");
        verify(registro, never()).terminar(anyLong());
        verify(volcado, never()).guardarFila(any(), anyInt(), anyBoolean(), any(), any(), any());
    }

    @Test
    @DisplayName("El total se publica al empezar: es el denominador del avance")
    void elTotalSePublicaAntesDeGuardar() {
        CargaVolcadoService.Analisis a = analisisDe(2);
        when(lector.leerHojas(any(byte[].class), any())).thenReturn(List.of());
        when(volcado.preparar(any(), any(), anyLong())).thenReturn(a);
        when(volcado.guardarFila(any(), anyInt(), anyBoolean(), any(), any(), any()))
                .thenReturn(guardada(2));

        worker.procesar(11L, new byte[]{1}, "volcado.xlsx", instrumento, false,
                institucion, usuario);

        InOrder orden = inOrder(registro);
        orden.verify(registro).totalDeFilas(11L, 2);
        orden.verify(registro, times(2)).anotar(eq(11L), any());
        orden.verify(registro).terminar(11L);
    }

    @Test
    @DisplayName("Al cargar no se juntan los problemas de celda")
    void alCargarNoSeAcumulanProblemas() {
        CargaVolcadoService.Analisis a = analisisDe(1);
        assertTrue(a.acumularProblemas, "por omisión sí se juntan: es lo que quiere la previa");
        when(lector.leerHojas(any(byte[].class), any())).thenReturn(List.of());
        when(volcado.preparar(any(), any(), anyLong())).thenReturn(a);
        when(volcado.guardarFila(any(), anyInt(), anyBoolean(), any(), any(), any()))
                .thenReturn(guardada(2));

        worker.procesar(13L, new byte[]{1}, "volcado.xlsx", instrumento, false,
                institucion, usuario);

        // Con cinco mil participantes serían millones de objetos vivos que nadie mira.
        assertFalse(a.acumularProblemas);
    }

    @Test
    @DisplayName("No se admiten dos cargas del mismo instrumento a la vez")
    void unaCargaALaVezPorInstrumento() {
        TareaCargaVolcadoRepository tareas = mock(TareaCargaVolcadoRepository.class);
        TipoCuestionarioService tipos = mock(TipoCuestionarioService.class);
        InstitucionContextService contexto = mock(InstitucionContextService.class);
        CargaVolcadoWorker otroWorker = mock(CargaVolcadoWorker.class);
        TareaVolcadoService servicio = new TareaVolcadoService(
                tareas, registro, otroWorker, tipos, contexto);

        TipoCuestionario tipo = new TipoCuestionario();
        tipo.setNombre("Cuestionario general");
        when(tipos.uno(3L)).thenReturn(tipo);
        when(contexto.getInstitucionActual()).thenReturn(institucion);
        when(contexto.getUsuarioActual()).thenReturn(usuario);
        when(tareas.existsByInstitucion_IdAndTipoCuestionario_IdAndEstado(
                1L, 3L, EstadoTareaVolcado.EN_PROCESO)).thenReturn(true);

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "volcado.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});

        ObjConflictException e = assertThrows(ObjConflictException.class,
                () -> servicio.encolar(archivo, 3L, false));
        assertTrue(e.getMessage().contains("Cuestionario general"));

        // Lo importante no es el mensaje: es que no se lanzó una segunda carga. Dos
        // cargas del mismo archivo a la vez se pisan y dejan un informe ilegible.
        verify(otroWorker, never()).procesar(anyLong(), any(), any(), any(),
                anyBoolean(), any(), any());
        verify(registro, never()).crear(any(), any(), any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("La tarea queda confirmada antes de lanzar el trabajo")
    void primeroSeApuntaYDespuesSeTrabaja() {
        TareaCargaVolcadoRepository tareas = mock(TareaCargaVolcadoRepository.class);
        TipoCuestionarioService tipos = mock(TipoCuestionarioService.class);
        InstitucionContextService contexto = mock(InstitucionContextService.class);
        CargaVolcadoWorker otroWorker = mock(CargaVolcadoWorker.class);
        TareaVolcadoService servicio = new TareaVolcadoService(
                tareas, registro, otroWorker, tipos, contexto);

        TipoCuestionario tipo = new TipoCuestionario();
        tipo.setNombre("Cuestionario general");
        TareaCargaVolcado apuntada = new TareaCargaVolcado();
        apuntada.setId(42L);
        when(tipos.uno(3L)).thenReturn(tipo);
        when(contexto.getInstitucionActual()).thenReturn(institucion);
        when(contexto.getUsuarioActual()).thenReturn(usuario);
        when(registro.crear(any(), any(), any(), any(), anyBoolean())).thenReturn(apuntada);

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "volcado.xlsx", "application/vnd.ms-excel", new byte[]{1, 2, 3});

        assertEquals(42L, servicio.encolar(archivo, 3L, true).getId());

        // El orden importa: el hilo que trabaja busca la tarea por su número, así que
        // la creación tiene que estar confirmada antes de lanzarlo.
        InOrder orden = inOrder(registro, otroWorker);
        orden.verify(registro).crear(same(tipo), same(institucion), same(usuario),
                eq("volcado.xlsx"), eq(true));
        orden.verify(otroWorker).procesar(eq(42L), any(byte[].class), eq("volcado.xlsx"),
                same(tipo), eq(true), same(institucion), same(usuario));
    }

    @Test
    @DisplayName("preparar el archivo no necesita usuario autenticado")
    void prepararNoPreguntaQuienEs() {
        // Esto es la condicion exacta del hilo de la carga: no hay contexto de
        // seguridad. Los dos colaboradores que podrian consultarlo revientan si alguien
        // los toca, asi que la prueba falla si el analisis vuelve a depender de ellos.
        // Y volvio a depender: la primera carga en segundo plano murio con «no hay un
        // usuario autenticado en el contexto de seguridad» porque el instrumento se
        // resolvia aqui por su id.
        InstitucionContextService sinUsuario = mock(InstitucionContextService.class,
                invocacion -> { throw new AccessDeniedException("no hay usuario"); });
        TipoCuestionarioService sinPermisos = mock(TipoCuestionarioService.class,
                invocacion -> { throw new AccessDeniedException("no hay usuario"); });

        VariableCuestionarioRepository variables = mock(VariableCuestionarioRepository.class);
        ReglaValidacionRepository reglas = mock(ReglaValidacionRepository.class);
        when(variables.findTodasConOpciones(anyLong())).thenReturn(List.of());
        when(reglas.findAllByTipoCuestionario_IdAndActivaTrue(anyLong())).thenReturn(List.of());

        CargaVolcadoService servicio = new CargaVolcadoService(
                mock(LectorArchivoTabular.class),
                mock(CuestionarioRepository.class),
                mock(RespuestaCuestionarioRepository.class),
                mock(DiscrepanciaLecturaRepository.class),
                variables,
                mock(PacienteRepository.class),
                reglas,
                sinPermisos,
                sinUsuario);

        instrumento.setColumnaEtiqueta("FOLIO");
        TablaLeida tabla = new TablaLeida(
                List.of("FOLIO", "P1"),
                List.of(List.of("C1/000001/F4", "13-06-1968")),
                List.of(2));
        List<LectorArchivoTabular.HojaLeida> hojas =
                List.of(new LectorArchivoTabular.HojaLeida("Datos Limpios", tabla));

        CargaVolcadoService.Analisis a = servicio.preparar(hojas, instrumento, 1L);

        assertSame(instrumento, a.tipo, "el instrumento llega resuelto, no se busca");
        assertEquals(1L, a.idInstitucion, "la institucion tambien llega dada");
        assertEquals(0, a.columnaEtiqueta);
    }

    @Test
    @DisplayName("el catalogo se trae con su bloque: el motor lo lee donde ya no hay sesion")
    void elCatalogoViajaConSuBloque() throws Exception {
        // El motor de validaciones identifica un bloque por su nombre, leyendolo de la
        // pregunta: v.getSeccion().getNombre(). Dentro de una peticion da igual si la
        // seccion viene cargada o es un proxy, porque la sesion sigue abierta hasta que
        // se contesta; en el hilo de la carga no hay sesion y el proxy ya no se puede
        // inicializar. Costo siete de ocho participantes de la primera carga buena:
        // cada uno se caia con «Could not initialize proxy ... no session» en cuanto
        // una regla de descarte le hacia recorrer su bloque.
        //
        // Se comprueba la consulta y no el comportamiento porque el defecto no esta en
        // lo que el motor hace —eso ya se prueba en MotorValidacionesTest— sino en como
        // llegan las preguntas: sin el fetch, lo que falla es a kilometros de aqui y
        // solo con ciertas respuestas.
        Query consulta = VariableCuestionarioRepository.class
                .getMethod("findTodasConOpciones", Long.class)
                .getAnnotation(Query.class);

        assertNotNull(consulta, "la carga usa esta consulta para traer el catalogo");
        assertTrue(consulta.value().contains("JOIN FETCH v.seccion"),
                "sin traer el bloque, la carga en segundo plano se cae pregunta por "
                        + "pregunta: " + consulta.value());
        assertTrue(consulta.value().contains("JOIN FETCH v.opciones"),
                "y las opciones por lo mismo: se leen al interpretar cada celda");
    }
}
