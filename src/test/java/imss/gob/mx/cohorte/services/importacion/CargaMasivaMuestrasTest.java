package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenicaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCajaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucionRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.HistorialCambioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.TipoMuestraService;
import imss.gob.mx.cohorte.services.importacion.PrevisualizacionCargaMuestras.FilaPrevisualizada;
import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * La carga masiva de muestras, del archivo a los viales en sus cajas.
 *
 * <p>Lo que se prueba aquí no es que el archivo se lea —de eso ya se encargan
 * las pruebas del lector—, sino las decisiones que solo existen en esta carga:
 * de dónde sale el volumen cuando la celda viene vacía, qué cuenta como error y
 * qué como aviso, y sobre todo <b>en qué estado queda la muestra padre</b>, que
 * es lo único del modelo que aquí se construye al revés de como se construye
 * siempre: deduciéndola de sus hijas.</p>
 */
class CargaMasivaMuestrasTest {

    private static final long ID_INSTITUCION = 1L;

    private LectorArchivoTabular lector;
    private TipoMuestraService tipoMuestraService;
    private PacienteRepository pacienteRepository;
    private ParticipanteAccesoService accesoService;
    private MuestraRepository muestraRepository;
    private CajaCriogenicaRepository cajaRepository;
    private PosicionCajaRepository posicionCajaRepository;
    private MuestraTipoInstitucionRepository muestraTipoInstitucionRepository;
    private InstitucionContextService contexto;
    private HistorialCambioMuestraService historial;
    private CargaMasivaMuestrasService servicio;

    private TipoMuestra suero;
    private TuboMuestra tuboSuero;
    private CajaCriogenica caja;
    private final Map<String, PosicionCaja> huecos = new HashMap<>();
    private long siguienteId;

    @BeforeEach
    void preparar() {
        lector = mock(LectorArchivoTabular.class);
        tipoMuestraService = mock(TipoMuestraService.class);
        pacienteRepository = mock(PacienteRepository.class);
        accesoService = mock(ParticipanteAccesoService.class);
        muestraRepository = mock(MuestraRepository.class);
        cajaRepository = mock(CajaCriogenicaRepository.class);
        posicionCajaRepository = mock(PosicionCajaRepository.class);
        muestraTipoInstitucionRepository = mock(MuestraTipoInstitucionRepository.class);
        contexto = mock(InstitucionContextService.class);
        historial = mock(HistorialCambioMuestraService.class);

        servicio = new CargaMasivaMuestrasService(lector, tipoMuestraService, pacienteRepository,
                accesoService, muestraRepository, cajaRepository, posicionCajaRepository,
                muestraTipoInstitucionRepository, contexto, historial);

        siguienteId = 100L;
        huecos.clear();

        Institucion institucion = new Institucion();
        institucion.setId(ID_INSTITUCION);

        // Catálogo: un tipo «Suero» con un tubo de 12 × 500 µL.
        suero = new TipoMuestra();
        suero.setId(10L);
        suero.setNombre("Suero");
        suero.setActivo(true);
        tuboSuero = tubo(20L, "Vial de suero", "S", 12, 500.0, "µL");
        tuboSuero.setTipoMuestra(suero);
        suero.setTubos(new ArrayList<>(List.of(tuboSuero)));

        caja = new CajaCriogenica();
        caja.setId(30L);
        caja.setCodigoCaja("SUERO-1");
        caja.setFilas(10);
        caja.setColumnas(10);
        caja.setActivo(true);
        caja.setInstitucion(institucion);

        when(contexto.getIdInstitucionActual()).thenReturn(ID_INSTITUCION);
        when(contexto.getInstitucionActual()).thenReturn(institucion);
        when(contexto.getUsuarioActual()).thenReturn(new BeanUser());
        when(accesoService.institucionesAlcanzables()).thenReturn(List.of(ID_INSTITUCION));
        when(tipoMuestraService.getAllActivos()).thenReturn(List.of(suero));
        when(cajaRepository.findByCodigoCajaAndInstitucion_Id(anyString(), eq(ID_INSTITUCION)))
                .thenAnswer(inv -> "SUERO-1".equalsIgnoreCase(inv.getArgument(0))
                        ? Optional.of(caja) : Optional.empty());
        when(posicionCajaRepository.findByCaja_IdAndFilaAndColumna(anyLong(), any(), any()))
                .thenAnswer(inv -> Optional.of(hueco(inv.getArgument(1), inv.getArgument(2))));
        when(muestraRepository.findMaxLoteByFolioAndTuboPrefix(anyString(), anyString())).thenReturn(0);
        when(muestraRepository.findEtiquetasExistentes(any(), anyLong())).thenReturn(List.of());
        when(muestraRepository.save(any(Muestra.class))).thenAnswer(inv -> {
            Muestra m = inv.getArgument(0);
            if (m.getId() == null) m.setId(siguienteId++);
            return m;
        });
        when(posicionCajaRepository.save(any(PosicionCaja.class))).thenAnswer(inv -> inv.getArgument(0));

        participante("001103", true);
    }

    // ── Andamiaje ────────────────────────────────────────────────────────────

    private TuboMuestra tubo(long id, String nombre, String prefijo, int alicuotas,
                             Double volumen, String unidad) {
        TuboMuestra t = new TuboMuestra();
        t.setId(id);
        t.setNombre(nombre);
        t.setPrefijoCodigo(prefijo);
        t.setNumeroAlicuotas(alicuotas);
        t.setVolumenAlicuota(volumen);
        t.setUnidadVolumen(unidad);
        t.setActivo(true);
        return t;
    }

    private PosicionCaja hueco(Integer fila, Integer columna) {
        return huecos.computeIfAbsent(fila + ":" + columna, k -> {
            PosicionCaja p = new PosicionCaja();
            p.setId(1000L + fila * 100 + columna);
            p.setCaja(caja);
            p.setFila(fila);
            p.setColumna(columna);
            p.setOcupada(false);
            return p;
        });
    }

    private Paciente participante(String folio, boolean activo) {
        Paciente p = new Paciente();
        p.setId(500L + folio.hashCode() % 100);
        p.setFolio(folio);
        p.setUuid("uuid-" + folio);
        p.setActivo(activo);
        when(pacienteRepository.findByFolioAndInstitucion_IdIn(eq(folio), any()))
                .thenReturn(Optional.of(p));
        return p;
    }

    private static final List<String> ENCABEZADOS = List.of(
            "folio", "tipoMuestra", "tubo", "fecha", "numeroAlicuota",
            "volumen", "unidad", "codigoCaja", "posicion");

    /** Una fila con los valores en el orden de {@link #ENCABEZADOS}. */
    private static List<String> fila(String... valores) {
        List<String> f = new ArrayList<>(Arrays.asList(valores));
        while (f.size() < ENCABEZADOS.size()) f.add("");
        return f;
    }

    private static TablaLeida tabla(List<String>... filas) {
        List<Integer> numeros = new ArrayList<>();
        for (int i = 0; i < filas.length; i++) numeros.add(i + 2);
        return new TablaLeida(ENCABEZADOS, Arrays.asList(filas), numeros);
    }

    private PrevisualizacionCargaMuestras previa(TablaLeida t) {
        return servicio.revalidar(t, null);
    }

    private static List<String> mensajes(FilaPrevisualizada f) {
        return f.errores().stream().map(PrevisualizacionCargaMuestras.Problema::mensaje).toList();
    }

    // ── Lo que sale bien ─────────────────────────────────────────────────────

    @Test
    @DisplayName("Un archivo correcto arma un lote con su etiqueta y la de cada vial")
    void armaElLote() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1"),
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "2", "", "", "SUERO-1", "A2")));

        assertTrue(p.puedeConfirmarse(), () -> "errores: " + mensajes(p.filas().get(0)));
        assertEquals(1, p.lotes().size());
        assertEquals(2, p.resumen().totalFilas());
        assertEquals(2, p.resumen().vialesConPosicion());

        var lote = p.lotes().get(0);
        assertEquals("S/001103/I1F4-L1", lote.etiquetaPadre());
        assertEquals("S/001103/I1F4-L1/1-12", p.filas().get(0).etiquetaPrevista());
        assertEquals("S/001103/I1F4-L1/2-12", p.filas().get(1).etiquetaPrevista());
        // El denominador es el cupo del tubo, no el tamaño de esta tanda: el lote
        // no se cierra al crearse y un «1-2» impreso impediría completarlo.
        assertEquals(12, lote.configuradas());
        assertEquals(2, lote.viales());
    }

    @Test
    @DisplayName("El folio se reintenta con ceros a la izquierda, como lo guarda el sistema")
    void rellenaElFolio() {
        // Excel se come los ceros al pasar por un CSV: "1103" es el folio "001103".
        when(pacienteRepository.findByFolioAndInstitucion_IdIn(eq("1103"), any()))
                .thenReturn(Optional.empty());

        var p = previa(tabla(
                fila("1103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1")));

        assertTrue(p.puedeConfirmarse(), () -> "errores: " + mensajes(p.filas().get(0)));
        assertEquals("001103", p.filas().get(0).folio());
    }

    // ── Volumen ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("La celda de volumen vacía hereda el nominal del tubo, y se cuenta")
    void heredaElVolumenDelTubo() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1")));

        var f = p.filas().get(0);
        assertEquals(500.0, f.volumen());
        assertEquals("µL", f.unidad());
        assertTrue(f.volumenHeredado());
        assertEquals(1, p.resumen().volumenesHeredados());
    }

    /**
     * La decisión del cliente, y la que más cambia el comportamiento: un vial con
     * menos de lo nominal no es un faltante que haya que justificar, es lo que
     * salió. Así que no genera aviso ni deja «pendiente» nada en la padre.
     */
    @Test
    @DisplayName("Un volumen MENOR al nominal se guarda tal cual y sin decir nada")
    void elVolumenMenorNoEsUnFaltante() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "300", "", "SUERO-1", "A1")));

        var f = p.filas().get(0);
        assertEquals(300.0, f.volumen());
        assertFalse(f.volumenHeredado());
        assertTrue(f.avisos().isEmpty(), () -> "no debería avisar: " + f.avisos());
        assertTrue(p.puedeConfirmarse());
    }

    @Test
    @DisplayName("Un volumen MAYOR al nominal se acepta, pero se avisa por si es un cero de más")
    void elVolumenMayorSeAvisa() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "5000", "", "SUERO-1", "A1")));

        var f = p.filas().get(0);
        assertEquals(5000.0, f.volumen());
        assertFalse(f.avisos().isEmpty());
        assertTrue(p.puedeConfirmarse(), "avisar no puede impedir guardar");
        assertEquals(1, p.resumen().filasConAvisos());
    }

    /**
     * El sistema no tiene factores de conversión. Aceptar otra unidad significaría
     * guardar un número que dice una cosa distinta de la que parece.
     */
    @Test
    @DisplayName("Una unidad distinta a la del tubo es error, no una conversión")
    void noConvierteUnidades() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "0.5", "mL", "SUERO-1", "A1")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(mensajes(p.filas().get(0)).stream().anyMatch(m -> m.contains("No se convierten")),
                () -> "" + mensajes(p.filas().get(0)));
    }

    @Test
    @DisplayName("La misma unidad escrita de otra forma no es un error")
    void laUnidadSeComparaNormalizada() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "500", " µL ", "SUERO-1", "A1")));

        assertTrue(p.puedeConfirmarse(), () -> "errores: " + mensajes(p.filas().get(0)));
    }

    // ── Huecos ───────────────────────────────────────────────────────────────

    /**
     * El caso del archivo real: dos viales reclamando la misma celda por un error
     * de tecleo. El índice único no salta hasta el commit, así que sin esta
     * comprobación el problema aparecería a mitad de la escritura y sin decir
     * cuál de las dos filas era la culpable.
     */
    @Test
    @DisplayName("Dos filas del mismo archivo al mismo hueco: la segunda es error")
    void detectaElHuecoRepetidoEnElArchivo() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "J10"),
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "2", "", "", "SUERO-1", "J10")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(p.filas().get(0).errores().isEmpty(), "la primera se queda el hueco");
        assertTrue(mensajes(p.filas().get(1)).stream().anyMatch(m -> m.contains("Otra fila")),
                () -> "" + mensajes(p.filas().get(1)));
    }

    @Test
    @DisplayName("Un hueco ya ocupado en la base detiene la fila")
    void detectaElHuecoYaOcupado() {
        hueco(1, 1).setOcupada(true);

        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(mensajes(p.filas().get(0)).stream().anyMatch(m -> m.contains("ya está ocupado")),
                () -> "" + mensajes(p.filas().get(0)));
    }

    @Test
    @DisplayName("Un hueco fuera de la rejilla de la caja detiene la fila")
    void detectaElHuecoFueraDeLaCaja() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "K11")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(mensajes(p.filas().get(0)).stream().anyMatch(m -> m.contains("10x10")),
                () -> "" + mensajes(p.filas().get(0)));
    }

    @Test
    @DisplayName("Caja sin hueco, o hueco sin caja, es error: media ubicación no ubica nada")
    void exigeLasDosMitadesDeLaUbicacion() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", ""),
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "2", "", "", "", "A2")));

        assertFalse(p.puedeConfirmarse());
        assertEquals(2, p.resumen().filasConProblemas());
    }

    @Test
    @DisplayName("Sin caja ni hueco el vial entra sin posición, que es un estado válido")
    void admiteVialesSinUbicar() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "", "")));

        assertTrue(p.puedeConfirmarse(), () -> "errores: " + mensajes(p.filas().get(0)));
        assertEquals(1, p.resumen().vialesSinPosicion());
    }

    // ── Catálogo y cupo ──────────────────────────────────────────────────────

    @Test
    @DisplayName("Pedir una alícuota por encima del cupo del tubo detiene la fila")
    void respetaElCupoDelTubo() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "13", "", "", "SUERO-1", "A1")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(mensajes(p.filas().get(0)).stream().anyMatch(m -> m.contains("define 12")),
                () -> "" + mensajes(p.filas().get(0)));
    }

    @Test
    @DisplayName("Un tubo que no es del tipo indicado no se resuelve")
    void elTuboTieneQueSerDelTipo() {
        var p = previa(tabla(
                fila("001103", "Suero", "Criotubo 300 mg", "2026-06-23", "1", "", "", "SUERO-1", "A1")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(mensajes(p.filas().get(0)).stream().anyMatch(m -> m.contains("no tiene ningún tubo")),
                () -> "" + mensajes(p.filas().get(0)));
    }

    /**
     * Misma regla que PacienteEstadoValidator: con el participante inactivo sus
     * muestras están en cuarentena. Detectarlo en la revisión evita que la
     * escritura reviente a mitad de la transacción.
     */
    @Test
    @DisplayName("Un participante inactivo detiene la fila antes de escribir nada")
    void rechazaParticipanteInactivo() {
        participante("009999", false);

        var p = previa(tabla(
                fila("009999", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(mensajes(p.filas().get(0)).stream().anyMatch(m -> m.contains("inactivo")),
                () -> "" + mensajes(p.filas().get(0)));
    }

    @Test
    @DisplayName("Dos filas con la misma alícuota del mismo lote: la segunda es error")
    void detectaElVialRepetido() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1"),
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A2")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(mensajes(p.filas().get(1)).stream().anyMatch(m -> m.contains("ya trae otra fila")),
                () -> "" + mensajes(p.filas().get(1)));
    }

    // ── Fecha ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Sin fecha en el archivo y sin fecha elegida, la fila no se puede cargar")
    void exigeAlgunaFecha() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "", "1", "", "", "SUERO-1", "A1")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(mensajes(p.filas().get(0)).stream().anyMatch(m -> m.contains("no trae fecha")),
                () -> "" + mensajes(p.filas().get(0)));
    }

    @Test
    @DisplayName("Sin fecha en el archivo se usa la elegida en la pantalla")
    void usaLaFechaElegida() {
        var p = servicio.revalidar(tabla(
                fila("001103", "Suero", "Vial de suero", "", "1", "", "", "SUERO-1", "A1")),
                "2026-06-23T09:30");

        assertTrue(p.puedeConfirmarse(), () -> "errores: " + mensajes(p.filas().get(0)));
        assertEquals("2026-06-23T09:30", p.filas().get(0).fecha());
    }

    /**
     * Una hoja de cálculo entrega las fechas sin hora a medianoche. Lo que NO se
     * hace es mover el día para que caiga en horario: el día es un dato que el
     * archivo sí trae, y cambiarlo silenciosamente movería la muestra a otra
     * jornada.
     */
    @Test
    @DisplayName("Una fecha sin hora toma la hora elegida, pero conserva su propio día")
    void completaLaHoraSinMoverElDia() {
        var p = servicio.revalidar(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1")),
                "2026-09-21T16:00");

        assertEquals("2026-06-23T16:00", p.filas().get(0).fecha());
        assertEquals(1, p.resumen().filasSinHora());
    }

    /**
     * Ninguna hoja de cálculo trae hora, así que marcarlo fila a fila pintaría de
     * ámbar el archivo entero y enterraría los avisos que sí señalan algo raro.
     * Se dice una vez, en el resumen.
     */
    @Test
    @DisplayName("Que falte la hora se cuenta en el resumen, no se avisa en cada fila")
    void laHoraNoEnsuciaLasFilas() {
        var p = servicio.revalidar(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1"),
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "2", "", "", "SUERO-1", "A2")),
                "2026-09-21T16:00");

        assertEquals(2, p.resumen().filasSinHora());
        assertEquals(0, p.resumen().filasConAvisos(), "el ámbar se reserva para lo excepcional");
    }

    /**
     * Sin fecha SÍ va fila a fila: el día forma parte de lo que define una
     * muestra padre, así que una fila sin fecha acaba en un lote distinto del de
     * sus hermanas, y eso hay que verlo donde pasa.
     */
    @Test
    @DisplayName("Una fila sin fecha sí se avisa: cae en otro lote")
    void laFilaSinFechaSiSeAvisa() {
        var p = servicio.revalidar(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1"),
                fila("001103", "Suero", "Vial de suero", "", "2", "", "", "SUERO-1", "A2")),
                "2026-09-21T16:00");

        assertTrue(p.filas().get(0).avisos().isEmpty());
        assertFalse(p.filas().get(1).avisos().isEmpty());
        assertEquals(2, p.lotes().size(), "días distintos son tubos distintos");
    }

    /**
     * «No encontré desempate» no es «hay algo que elegir». Con fechas ISO el orden
     * día/mes no se aplica a nada, y decir que admiten dos lecturas es una falsa
     * alarma — de las que enseñan a ignorar los avisos.
     */
    @Test
    @DisplayName("Con fechas ISO no se avisa de ambigüedad: no hay nada que elegir")
    void noInventaAmbiguedadConFechasIso() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1")));

        assertFalse(p.fechaAmbigua());
    }

    @Test
    @DisplayName("Con fechas numéricas que no desempatan sí se avisa")
    void avisaLaAmbiguedadDeVerdad() {
        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "03/04/2026", "1", "", "", "SUERO-1", "A1"),
                fila("001103", "Suero", "Vial de suero", "05/06/2026", "2", "", "", "SUERO-1", "A2")));

        assertTrue(p.fechaAmbigua(), "3/4 y 5/6 se leen de las dos formas");
    }

    // ── Numeración de lotes ──────────────────────────────────────────────────

    /**
     * Dos tubos pueden compartir prefijo —o no definir ninguno y caer los dos en
     * el de omisión—. Si sus lotes se numeraran igual, sus etiquetas chocarían
     * contra uk_muestra_etiqueta_institucion en mitad de la escritura.
     */
    @Test
    @DisplayName("Dos lotes del mismo folio y prefijo se numeran distinto")
    void numeraLosLotesDelMismoPrefijo() {
        TuboMuestra otro = tubo(21L, "Vial pequeño", "S", 4, 100.0, "µL");
        otro.setTipoMuestra(suero);
        suero.getTubos().add(otro);

        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1"),
                fila("001103", "Suero", "Vial pequeño", "2026-06-23", "1", "", "", "SUERO-1", "A2")));

        assertTrue(p.puedeConfirmarse(), () -> "errores: " + mensajes(p.filas().get(1)));
        assertEquals(2, p.lotes().size());
        assertEquals("S/001103/I1F4-L1", p.lotes().get(0).etiquetaPadre());
        assertEquals("S/001103/I1F4-L2", p.lotes().get(1).etiquetaPadre());
        assertNotEquals(p.filas().get(0).etiquetaPrevista(), p.filas().get(1).etiquetaPrevista());
    }

    @Test
    @DisplayName("El lote arranca después del último que ya existe para ese folio")
    void continuaLaNumeracionExistente() {
        when(muestraRepository.findMaxLoteByFolioAndTuboPrefix("001103", "S")).thenReturn(3);

        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1")));

        assertEquals("S/001103/I1F4-L4", p.lotes().get(0).etiquetaPadre());
    }

    @Test
    @DisplayName("Una etiqueta que ya existe detiene la carga: puede ser un lote cargado dos veces")
    void detectaLaEtiquetaRepetida() {
        when(muestraRepository.findEtiquetasExistentes(any(), anyLong()))
                .thenReturn(List.of("S/001103/I1F4-L1/1-12"));

        var p = previa(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1")));

        assertFalse(p.puedeConfirmarse());
        assertTrue(mensajes(p.filas().get(0)).stream().anyMatch(m -> m.contains("Ya existe")),
                () -> "" + mensajes(p.filas().get(0)));
    }

    // ── Escritura ────────────────────────────────────────────────────────────

    /**
     * El corazón de esta carga. La padre se deduce de sus hijas, así que su
     * volumen es lo que QUEDA por repartir, no la suma de todo. Escribir la suma
     * y además marcar las hijas materializadas contaría cada mililitro dos veces.
     */
    @Test
    @DisplayName("Con todos los viales ubicados, la padre nace en cero y ya agotada")
    void laPadreNaceAgotada() {
        var resultado = servicio.confirmar(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1"),
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "2", "", "", "SUERO-1", "A2")),
                null);

        assertEquals(1, resultado.padresCreadas());
        assertEquals(2, resultado.alicuotasCreadas());
        assertEquals(2, resultado.alicuotasUbicadas());
        assertEquals(1, resultado.padresAgotadas());

        Muestra padre = guardadas().stream().filter(m -> m.getMuestraPadre() == null).findFirst().orElseThrow();
        assertEquals(0.0, padre.getValor());
        assertEquals(0.0, padre.getValorComprometido());
        assertNotNull(padre.getFechaAgotamiento());
        assertEquals(EstadoMuestra.SIN_POSICION, padre.getEstadoMuestra());
        assertNull(padre.getPosicionCaja(), "la padre es un registro, no ocupa hueco");

        List<Muestra> alicuotas = guardadas().stream().filter(m -> m.getMuestraPadre() != null).toList();
        assertEquals(2, alicuotas.size());
        for (Muestra a : alicuotas) {
            assertEquals(EstadoMuestra.EN_BIOBANCO, a.getEstadoMuestra());
            assertNotNull(a.getFechaMaterializacion(), "ocupar un hueco es materializarse");
            assertEquals(500.0, a.getCantidadDescontadaPadre());
            assertEquals(12, a.getTotalAlicuotas());
        }
    }

    /**
     * El otro lado del invariante: lo que todavía no tiene hueco sigue siendo una
     * promesa. Su volumen queda reservado en la padre —valor y comprometido a la
     * vez, para que el disponible dé cero— y la padre no se agota.
     */
    @Test
    @DisplayName("Un vial sin ubicar deja su volumen reservado y la padre sin agotar")
    void elVialSinUbicarQuedaComprometido() {
        var resultado = servicio.confirmar(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1"),
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "2", "", "", "", "")),
                null);

        assertEquals(1, resultado.alicuotasUbicadas());
        assertEquals(0, resultado.padresAgotadas());

        Muestra padre = guardadas().stream().filter(m -> m.getMuestraPadre() == null).findFirst().orElseThrow();
        assertEquals(500.0, padre.getValor());
        assertEquals(500.0, padre.getValorComprometido());
        assertEquals(0.0, padre.getValorDisponible(), "lo reservado no se puede prometer dos veces");
        assertNull(padre.getFechaAgotamiento());

        Muestra sinUbicar = guardadas().stream()
                .filter(m -> m.getMuestraPadre() != null && m.getPosicionCaja() == null)
                .findFirst().orElseThrow();
        assertNull(sinUbicar.getFechaMaterializacion());
        assertNull(sinUbicar.getCantidadDescontadaPadre());
        assertEquals(EstadoMuestra.SIN_POSICION, sinUbicar.getEstadoMuestra());
    }

    @Test
    @DisplayName("Ubicar un vial marca su hueco como ocupado")
    void ocupaElHueco() {
        servicio.confirmar(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1")), null);

        assertTrue(hueco(1, 1).getOcupada());
        verify(posicionCajaRepository).save(hueco(1, 1));
    }

    @Test
    @DisplayName("Confirmar con filas rotas no escribe nada")
    void noEscribeSiHayErrores() {
        assertThrows(ArchivoInvalidoException.class, () -> servicio.confirmar(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "99", "", "", "SUERO-1", "A1")),
                null));

        verify(muestraRepository, never()).save(any(Muestra.class));
        verify(posicionCajaRepository, never()).save(any(PosicionCaja.class));
    }

    /**
     * Un renglón por vial y uno por lote, no uno por cada paso de la contabilidad:
     * el historial de la padre tiene que poder leerse, y seiscientos renglones de
     * descuento lo enterrarían.
     */
    @Test
    @DisplayName("Cada vial y cada lote dejan su renglón de historial, y ninguno más")
    void dejaHistorialLegible() {
        servicio.confirmar(tabla(
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "1", "", "", "SUERO-1", "A1"),
                fila("001103", "Suero", "Vial de suero", "2026-06-23", "2", "", "", "SUERO-1", "A2")), null);

        // 1 REGISTRO de la padre + 1 MUESTRA_AGOTADA + 1 REGISTRO por vial.
        verify(historial, times(4)).registrarEvento(any(), any(), any(), any(), any(), anyString(), any());
    }

    private List<Muestra> guardadas() {
        ArgumentCaptor<Muestra> captor = ArgumentCaptor.forClass(Muestra.class);
        verify(muestraRepository, atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }
}
