package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.Cuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.CuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.EstadoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.OrigenCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.discrepancias.DiscrepanciaLectura;
import imss.gob.mx.cohorte.modules.cuestionarios.discrepancias.DiscrepanciaLecturaRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.MarcaAusencia;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacion;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacionRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.TipoVariable;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.VariableCuestionarioRepository;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.importacion.ArchivoInvalidoException;
import imss.gob.mx.cohorte.services.importacion.LectorArchivoTabular;
import imss.gob.mx.cohorte.services.importacion.LimitesArchivo;
import imss.gob.mx.cohorte.services.importacion.TablaLeida;
import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

/**
 * Carga el volcado que produce Remark al escanear las hojas en papel.
 *
 * <h3>Dos pasos, no uno</h3>
 *
 * <p>{@link #previsualizar} hace todo el trabajo sin escribir: lee el archivo, empareja
 * las columnas con el catálogo, interpreta cada celda y cuenta. La guardada es el
 * segundo paso y ya no pasa por aquí de una vez: se encola y corre en segundo plano
 * —ver {@link CargaVolcadoWorker}— pidiendo a {@link #guardarFila} un participante
 * cada vez. El archivo se vuelve a subir en el segundo paso en lugar de quedarse en
 * memoria entre peticiones —un volcado son megabytes y habría que guardarlo por
 * usuario y por sesión, con todo lo que eso trae— y, de paso, la segunda lectura
 * comprueba contra el catálogo de ese momento, no el de hace diez minutos.</p>
 *
 * <h3>El instrumento se elige; de la etiqueta solo sale el folio</h3>
 *
 * <p>Remark exporta un volcado por plantilla, así que de qué cuestionario es el archivo
 * lo sabe quien lo sube y se indica al cargarlo. La columna de etiqueta sirve para una
 * sola cosa: saber de qué participante es cada fila. Y para eso da igual que traiga la
 * etiqueta completa —{@code C1/000001/F4}— o el folio pelado —{@code 000001}—, que es
 * como han llegado los dos volcados recibidos.</p>
 *
 * <p>Antes mandaba la etiqueta y el instrumento seleccionado se compraba contra ella.
 * La idea era impedir que las respuestas del general se archivaran como minimental,
 * donde los mismos códigos significan otra cosa; pero el precio era rechazar archivos
 * buenos por cómo estaban rotulados, y la protección de verdad está en otro sitio: las
 * columnas se emparejan con el catálogo del instrumento elegido, así que un archivo
 * subido en la plantilla equivocada aparece en la previsualización con casi ninguna
 * columna emparejada, antes de escribir nada. El rotulado se sigue mirando y, si no
 * coincide, se avisa.</p>
 */
@Service
@AllArgsConstructor
@Slf4j
public class CargaVolcadoService {

    /** Nombres con los que Remark titula la pestaña de respuestas. */
    private static final List<String> NOMBRES_RESPUESTAS = List.of("DATOS LIMPIOS", "DATOS", "LIMPIOS");

    /** Nombres con los que titula la pestaña de lecturas en desacuerdo. */
    private static final List<String> NOMBRES_DISCREPANCIAS = List.of("DUPLICADOS", "DISCREPANCIAS");

    /** Encabezados de la hoja de desacuerdos, tal como los escribe Remark. */
    private static final String COL_VARIABLE = "VARIABLE";
    private static final String COL_CELDA = "CELDA";
    private static final String COL_PREVIO = "VALOR PREVIO";
    private static final String COL_NUEVO = "VALOR NUEVO";
    private static final String COL_FILA = "FILA";

    /** Cuántos problemas se detallan en la previsualización. */
    private static final int MAX_PROBLEMAS_LISTADOS = 200;

    private final LectorArchivoTabular lector;
    private final CuestionarioRepository cuestionarioRepository;
    private final RespuestaCuestionarioRepository respuestaRepository;
    private final DiscrepanciaLecturaRepository discrepanciaRepository;
    private final VariableCuestionarioRepository variableRepository;
    private final PacienteRepository pacienteRepository;
    private final ReglaValidacionRepository reglaRepository;
    private final TipoCuestionarioService tipoService;
    private final InstitucionContextService contexto;

    static LectorArchivoTabular.Topes topes() {
        return new LectorArchivoTabular.Topes(
                LimitesArchivo.MAX_FILAS_CUESTIONARIO, LimitesArchivo.MAX_COLUMNAS_CUESTIONARIO);
    }

    // ── Paso 1: previsualizar ────────────────────────────────────────────────

    public PrevisualizacionVolcado previsualizar(MultipartFile archivo, Long idTipo) {
        Analisis a = analizar(archivo, idTipo);

        List<PrevisualizacionVolcado.FilaPrevia> filas = new ArrayList<>();
        for (FilaAnalizada f : a.filas) {
            filas.add(new PrevisualizacionVolcado.FilaPrevia(
                    f.numeroFila, f.etiqueta, f.folio,
                    f.paciente == null ? null : nombreDe(f.paciente),
                    f.motivoRechazo, f.yaExiste,
                    f.conValor, f.sinRespuesta, f.noAutorizadas, f.conError, f.noPreguntadas,
                    f.avisoCriterio, f.edad, f.avisosDeRegla));
        }

        List<PrevisualizacionVolcado.ProblemaCelda> problemas = a.problemas.size() > MAX_PROBLEMAS_LISTADOS
                ? a.problemas.subList(0, MAX_PROBLEMAS_LISTADOS)
                : a.problemas;

        return new PrevisualizacionVolcado(
                a.claveDetectada, a.avisoEtiqueta, a.tipo.getNombre(), a.hojaRespuestas, a.hojaDiscrepancias,
                a.totalColumnas, a.columnasEmparejadas, a.columnasSinVariable, a.variablesSinColumna,
                filas, problemas,
                Math.max(0, a.problemas.size() - MAX_PROBLEMAS_LISTADOS),
                a.desacuerdos.size());
    }

    // ── Paso 2: confirmar ────────────────────────────────────────────────────

    /**
     * Guarda el volcado.
     *
     * <p>Todo en una transacción: si una fila falla, no queda media carga. Con un
     * archivo de diez participantes y dos mil respuestas cada uno eso son veinte mil
     * inserciones, que es mucho pero cabe; el tope de filas del formato está puesto
     * justo para que siga cabiendo.</p>
     */
    /**
     * Lo que pasó con un participante del archivo.
     *
     * @param motivoOmision null si se guardó; si no, por qué se dejó fuera
     */
    public record ResultadoFila(int numeroFila, String etiqueta, String motivoOmision,
                                boolean creado, int respuestas, int pendientes,
                                int discrepancias) {

        static ResultadoFila omitida(int numeroFila, String etiqueta, String motivo) {
            return new ResultadoFila(numeroFila, etiqueta, motivo, false, 0, 0, 0);
        }
    }

    /**
     * Guarda un solo participante del volcado.
     *
     * <h3>Una transacción por participante, no una por archivo</h3>
     *
     * <p>Antes el archivo entero se guardaba en una sola transacción, con el argumento
     * de que así no queda media carga. Con un volcado de diez participantes era cierto
     * y salía gratis; con cientos deja de serlo: la transacción tendría que sostener
     * cientos de miles de inserciones y, si la última fila falla, se tira el trabajo de
     * todas las anteriores sin que eso arregle nada —el archivo se vuelve a cargar
     * igual—.</p>
     *
     * <p>Lo que de verdad no puede quedar a medias es un cuestionario: un participante
     * con la mitad de sus respuestas sería un dato falso y silencioso. Y eso es lo que
     * esta transacción garantiza. Si una fila falla, las demás siguen y la que falló se
     * informa como omitida, que es exactamente lo que alguien necesita para repetirla.</p>
     *
     * @param institucion la institución responsable, explícita: esto corre en un hilo
     *                    donde no hay usuario autenticado del que deducirla
     * @param usuario     quien subió el archivo, para que el cuestionario diga de quién
     *                    vino aunque se haya guardado media hora después
     */
    @Transactional
    public ResultadoFila guardarFila(Analisis a, int indiceFila, boolean sobrescribir,
                                     String nombreArchivo, Institucion institucion,
                                     BeanUser usuario) {
        FilaAnalizada f = analizarFila(a, indiceFila, true);

        if (f.motivoRechazo != null && !(f.yaExiste && sobrescribir)) {
            return ResultadoFila.omitida(f.numeroFila, f.etiqueta, f.motivoRechazo);
        }

        Cuestionario cuestionario = cuestionarioRepository
                .findByPaciente_IdAndTipoCuestionario_Id(f.paciente.getId(), a.tipo.getId())
                .orElse(null);
        boolean creado = false;

        if (cuestionario != null) {
            if (!sobrescribir) {
                return ResultadoFila.omitida(f.numeroFila, f.etiqueta,
                        "Ya existe un cuestionario para este participante.");
            }
            if (cuestionario.getEstado() == EstadoCuestionario.VALIDADO) {
                // Un cuestionario validado lleva la firma de quien lo reviso contra
                // la hoja. Sobrescribirlo borraria ese trabajo sin dejar rastro, asi
                // que ni «sobrescribir» alcanza: hay que reabrirlo a proposito.
                return ResultadoFila.omitida(f.numeroFila, f.etiqueta,
                        "El cuestionario ya fue validado. Reabrelo si de verdad quieres reemplazarlo.");
            }
            respuestaRepository.deleteAllByCuestionario_Id(cuestionario.getId());
            discrepanciaRepository.deleteAllByCuestionario_Id(cuestionario.getId());
        } else {
            cuestionario = new Cuestionario();
            cuestionario.setPaciente(f.paciente);
            cuestionario.setTipoCuestionario(a.tipo);
            cuestionario.setInstitucion(institucion);
            cuestionario.setOrigen(OrigenCuestionario.PAPEL_REMARK);
            cuestionario.setUsuarioCaptura(usuario);
            creado = true;
        }

        cuestionario.setEtiqueta(f.etiqueta);
        cuestionario.setArchivoOrigen(nombreArchivo);
        cuestionario.setEstado(EstadoCuestionario.BORRADOR);
        cuestionario.setRespuestasPendientes(f.noAutorizadas + f.conError);
        cuestionario.setFueraDeCriterio(f.avisoCriterio != null);
        cuestionario.setMotivoFueraDeCriterio(f.avisoCriterio);
        cuestionario = cuestionarioRepository.save(cuestionario);

        for (RespuestaCuestionario r : f.respuestas) r.setCuestionario(cuestionario);
        respuestaRepository.saveAll(f.respuestas);

        int discrepancias = 0;
        List<DiscrepanciaLectura> suyas = a.desacuerdos.get(f.numeroFila);
        if (suyas != null) {
            for (DiscrepanciaLectura d : suyas) d.setCuestionario(cuestionario);
            discrepanciaRepository.saveAll(suyas);
            discrepancias = suyas.size();
        }

        return new ResultadoFila(f.numeroFila, f.etiqueta, null, creado,
                f.respuestas.size(), f.noAutorizadas + f.conError, discrepancias);
    }

    // ── Analisis comun a los dos pasos ───────────────────────────────────────

    /**
     * Todo lo que hace falta para leer el archivo, menos lo de cada fila.
     *
     * <p>No es {@code private} porque la carga en segundo plano la recorre fila por
     * fila desde fuera: {@link CargaVolcadoWorker} prepara esto una vez y pide cada
     * participante por separado, para que cada uno se guarde en su propia transacción
     * y no haya que tener dos millones de respuestas en memoria a la vez.</p>
     */
    static final class Analisis {
        TipoCuestionario tipo;
        /** La hoja de respuestas, ya leída. Se recorre fila por fila. */
        TablaLeida tabla;
        int columnaEtiqueta;
        /** Qué pregunta del catálogo hay en cada columna, o null si ninguna. */
        VariableCuestionario[] deCadaColumna;
        /**
         * La institución con la que se resuelven los folios.
         *
         * <p>Va aquí y no se consulta al contexto de seguridad porque la carga corre
         * en un hilo aparte, donde ese contexto no existe: quien la pidió ya no está
         * esperando. Pasarla explícitamente es lo que impide que una carga en segundo
         * plano termine buscando participantes en la institución equivocada —o en
         * ninguna.</p>
         */
        Long idInstitucion;
        /**
         * Si se van juntando los problemas de celda.
         *
         * <p>Solo la previsualización los quiere: son para enseñárselos a quien decide
         * si confirma. Al guardar hay que no juntarlos, porque con cinco mil
         * participantes serían millones de objetos vivos para nada.</p>
         */
        boolean acumularProblemas = true;
        String claveDetectada;
        String avisoEtiqueta;
        String hojaRespuestas;
        String hojaDiscrepancias;
        int totalColumnas;
        int columnasEmparejadas;
        List<String> columnasSinVariable = new ArrayList<>();
        List<String> variablesSinColumna = new ArrayList<>();
        List<FilaAnalizada> filas = new ArrayList<>();
        List<PrevisualizacionVolcado.ProblemaCelda> problemas = new ArrayList<>();
        Map<Integer, List<DiscrepanciaLectura>> desacuerdos = new HashMap<>();
        /** El catalogo por codigo, para seguir las cadenas de saltos al interpretar. */
        Map<String, VariableCuestionario> llaves = new HashMap<>();
        Map<String, VariableCuestionario> porCodigo = new HashMap<>();
        List<ReglaValidacion> reglas = List.of();
    }

    static final class FilaAnalizada {
        int numeroFila;
        String etiqueta;
        String folio;
        Paciente paciente;
        String motivoRechazo;
        boolean yaExiste;
        int conValor, sinRespuesta, noAutorizadas, conError, noPreguntadas;
        String avisoCriterio;
        Integer edad;
        /** Lo que incumple alguna regla de la plantilla. No impide cargar la fila. */
        List<String> avisosDeRegla = new ArrayList<>();
        List<RespuestaCuestionario> respuestas = new ArrayList<>();
    }

    /**
     * Lee el archivo entero y analiza todas sus filas. Es lo que usa la
     * previsualización, que necesita poder enseñar el archivo completo.
     */
    private Analisis analizar(MultipartFile archivo, Long idTipo) {
        if (idTipo == null) {
            throw new ArchivoInvalidoException(
                    "Indica a que cuestionario pertenece el volcado antes de cargarlo.");
        }
        // Aqui si se resuelve con el servicio: esto corre en la peticion, hay usuario
        // autenticado y es justo donde toca comprobar que el instrumento sea suyo.
        Analisis a = preparar(lector.leerHojas(archivo, topes()), tipoService.uno(idTipo),
                contexto.getIdInstitucionActual());
        for (int f = 0; f < a.tabla.totalFilas(); f++) {
            // Sin conservar las respuestas: la previsualización solo enseña los
            // recuentos, y guardar los objetos de cada fila para después tirarlos
            // multiplicaría la memoria por dos mil sin que nadie los mire.
            a.filas.add(analizarFila(a, f, false));
        }
        return a;
    }

    /**
     * Todo lo que no depende de una fila concreta: qué instrumento es, qué pregunta
     * hay en cada columna, qué reglas rigen y qué lecturas vinieron en desacuerdo.
     *
     * <p>Está separado del recorrido de las filas porque la carga en segundo plano
     * prepara esto una vez y después pide los participantes de uno en uno.</p>
     *
     * <h3>El instrumento y la institución llegan resueltos</h3>
     *
     * <p>Y no se buscan aquí, aunque fuera más corto recibir los ids. Esto corre en el
     * hilo de la carga, donde no hay usuario autenticado: cualquier cosa que acabe
     * preguntándole al contexto de seguridad quién es —{@code TipoCuestionarioService
     * .uno} lo hace, para comprobar que el instrumento sea de su institución— falla ahí
     * con un «no hay un usuario autenticado» que no tiene nada que ver con el archivo.
     * Pasó: la primera carga en segundo plano murió justo así.</p>
     *
     * <p>La comprobación de permisos no se pierde, se mueve a donde tiene sentido: la
     * hace quien encola, en la petición, con el usuario delante. Recibir la entidad ya
     * resuelta es lo que hace imposible repetir el error, porque desde aquí ya no hay a
     * quién preguntarle.</p>
     */
    Analisis preparar(List<LectorArchivoTabular.HojaLeida> hojas, TipoCuestionario tipo,
                      Long idInstitucion) {
        LectorArchivoTabular.HojaLeida hojaRespuestas = elegirHoja(hojas, NOMBRES_RESPUESTAS);
        if (hojaRespuestas == null) {
            // Sin pestana reconocible se toma la mas ancha: es la de respuestas, porque
            // tiene una columna por pregunta. Elegir por posicion seria peor —el orden
            // de las pestanas no lo garantiza nadie— y rechazar el archivo tampoco,
            // porque quien exporta puede renombrar la hoja sin querer decir nada.
            hojaRespuestas = hojas.stream()
                    .max(Comparator.comparingInt(h -> h.tabla().encabezados().size()))
                    .orElseThrow(() -> new ArchivoInvalidoException("El archivo no trae ninguna tabla."));
        }
        LectorArchivoTabular.HojaLeida hojaDesacuerdos = elegirHoja(hojas, NOMBRES_DISCREPANCIAS);

        TablaLeida tabla = hojaRespuestas.tabla();
        if (tabla.vacia()) {
            throw new ArchivoInvalidoException("La hoja \"" + hojaRespuestas.nombre()
                    + "\" no trae ninguna fila de datos.");
        }

        Analisis a = new Analisis();
        a.tabla = tabla;
        a.idInstitucion = idInstitucion;
        a.hojaRespuestas = hojaRespuestas.nombre();
        a.hojaDiscrepancias = hojaDesacuerdos == null ? null : hojaDesacuerdos.nombre();
        a.totalColumnas = tabla.encabezados().size();

        // ── Instrumento: lo decide la plantilla seleccionada ──────────────────
        //
        // Antes lo decidia la etiqueta del archivo. Era un estorbo sin ganancia:
        // Remark exporta un volcado por plantilla, asi que quien lo sube ya sabe de
        // que cuestionario es, y la columna de etiqueta llega unas veces con la
        // etiqueta completa (C1/000001/F4) y otras con el folio pelado. Exigir la
        // clave ahi rechazaba archivos buenos. De la etiqueta ahora solo se saca el
        // folio, que es lo unico que identifica al participante.
        if (tipo == null) {
            throw new ArchivoInvalidoException(
                    "Indica a que cuestionario pertenece el volcado antes de cargarlo.");
        }
        a.tipo = tipo;

        int columnaEtiqueta = buscarColumnaEtiqueta(tabla, a.tipo.getColumnaEtiqueta());
        a.columnaEtiqueta = columnaEtiqueta;
        if (columnaEtiqueta < 0) {
            throw new ArchivoInvalidoException("El archivo no trae la columna \""
                    + a.tipo.getColumnaEtiqueta() + "\" con el folio del participante.");
        }

        // La clave de las etiquetas ya no manda, pero se sigue leyendo: si dice otro
        // instrumento —o varios— es casi seguro que el archivo no es el que se cree.
        // Avisa y deja decidir, que es distinto de rechazar.
        Set<String> clavesEnEtiquetas = clavesDe(tabla, columnaEtiqueta);
        a.claveDetectada = clavesEnEtiquetas.isEmpty() ? null : String.join(", ", clavesEnEtiquetas);
        a.avisoEtiqueta = avisoDeEtiquetas(clavesEnEtiquetas, a.tipo);

        // ── Emparejar columnas con el catalogo ──────────────────────────────────
        List<VariableCuestionario> variables = variableRepository.findTodasConOpciones(a.tipo.getId());
        Map<String, VariableCuestionario> porColumna = new HashMap<>();
        for (VariableCuestionario v : variables) {
            if (v.getColumnaNormalizada() != null) porColumna.put(v.getColumnaNormalizada(), v);
        }

        VariableCuestionario[] deCadaColumna = new VariableCuestionario[a.totalColumnas];
        a.deCadaColumna = deCadaColumna;
        Set<Long> emparejadas = new HashSet<>();
        for (int c = 0; c < a.totalColumnas; c++) {
            if (c == columnaEtiqueta) continue;
            String encabezado = tabla.encabezados().get(c);
            if (encabezado.isEmpty()) continue;
            VariableCuestionario v = porColumna.get(NormalizadorAlias.normalizar(encabezado));
            if (v == null) {
                a.columnasSinVariable.add(encabezado);
            } else {
                deCadaColumna[c] = v;
                emparejadas.add(v.getId());
            }
        }
        a.columnasEmparejadas = emparejadas.size();
        for (VariableCuestionario v : variables) {
            if (v.getColumnaNormalizada() != null && !emparejadas.contains(v.getId())) {
                a.variablesSinColumna.add(v.getCodigo());
            }
        }

        // ── Fila por fila ──────────────────────────────────────────────────────
        Map<String, VariableCuestionario> porCodigo = new HashMap<>();
        for (VariableCuestionario v : variables) porCodigo.put(v.getCodigo(), v);
        a.llaves = porCodigo;
        a.porCodigo = porCodigo;
        a.reglas = reglaRepository.findAllByTipoCuestionario_IdAndActivaTrue(a.tipo.getId());

        if (hojaDesacuerdos != null) {
            a.desacuerdos = leerDesacuerdos(hojaDesacuerdos.tabla(), porCodigo, tabla);
        }

        return a;
    }

    /**
     * Analiza una fila: a quién pertenece, qué dice cada celda y qué reglas incumple.
     *
     * @param conservarRespuestas si se guardan los objetos de respuesta de la fila.
     *                            Al cargar hacen falta; al previsualizar no, y con
     *                            miles de participantes son la diferencia entre
     *                            caber en memoria y no caber.
     */
    FilaAnalizada analizarFila(Analisis a, int indiceFila, boolean conservarRespuestas) {
        TablaLeida tabla = a.tabla;
        int columnaEtiqueta = a.columnaEtiqueta;
        VariableCuestionario[] deCadaColumna = a.deCadaColumna;
        List<String> celdas = tabla.filas().get(indiceFila);
        FilaAnalizada fila = new FilaAnalizada();
        fila.numeroFila = tabla.numerosDeFila().get(indiceFila);
        fila.etiqueta = celdas.get(columnaEtiqueta);

        // ── Identificar al participante ────────────────────────────────────────
        if (fila.etiqueta == null || fila.etiqueta.isBlank()) {
            fila.motivoRechazo = "La fila no trae etiqueta de participante.";
            return fila;
        }

        // De la etiqueta solo importa el folio. La clave, si viene, no decide nada:
        // el instrumento es el que se selecciono al subir el archivo. Una fila
        // etiquetada de otro instrumento no se rechaza aqui —se avisa una sola vez
        // arriba, para todo el archivo— porque rechazarla fila por fila dejaria la
        // carga a medias por una diferencia de rotulado.
        EtiquetaCuestionario.Folio leido = EtiquetaCuestionario.folioDe(fila.etiqueta);
        if (!leido.valido()) {
            fila.motivoRechazo = leido.motivo();
            return fila;
        }
        fila.folio = leido.folio();

        fila.paciente = pacienteRepository
                .findByFolioAndInstitucion_Id(fila.folio, a.idInstitucion)
                .orElse(null);
        if (fila.paciente == null) {
            fila.motivoRechazo = "No hay ningun participante con el folio " + fila.folio
                    + " en esta institucion.";
            return fila;
        }

        // El criterio se evalua pero NO rechaza la fila. Ver Cuestionario#fueraDeCriterio:
        // descartar una hoja ya contestada por no encajar con el publico del instrumento
        // perderia el dato y, con el, la senal de que hubo un error de aplicacion.
        var veredicto = CriterioAplicabilidad.evaluar(a.tipo, fila.paciente, null);
        fila.edad = veredicto.edad();
        if (!veredicto.aplica() || veredicto.indeterminado()) {
            fila.avisoCriterio = veredicto.motivo();
        }

        fila.yaExiste = cuestionarioRepository.existsByPaciente_IdAndTipoCuestionario_Id(
                fila.paciente.getId(), a.tipo.getId());
        if (fila.yaExiste) {
            fila.motivoRechazo = "El participante " + fila.folio + " ya tiene un "
                    + a.tipo.getNombre() + " registrado.";
        }

        // ── Interpretar las celdas ─────────────────────────────────────────────
        //
        // Se interpretan incluso en las filas rechazadas: el recuento es lo que le dice
        // al usuario si vale la pena reintentar con «sobrescribir», y sin el la pantalla
        // solo diria «ya existe» sin decir que trae el archivo.
        Map<String, String> contestados = new HashMap<>();
        for (int c = 0; c < deCadaColumna.length && c < celdas.size(); c++) {
            VariableCuestionario v = deCadaColumna[c];
            if (v == null) continue;
            InterpreteCelda.Interpretacion i = InterpreteCelda.interpretar(v, celdas.get(c));
            if (i.marca() == MarcaAusencia.NINGUNA && i.codigo() != null) {
                contestados.putIfAbsent(v.getCodigo(), i.codigo());
            }
        }

        for (int c = 0; c < deCadaColumna.length && c < celdas.size(); c++) {
            VariableCuestionario v = deCadaColumna[c];
            if (v == null) continue;
            if (!Boolean.TRUE.equals(v.getActiva())) continue;
            if (v.getTipo() == TipoVariable.GRUPO) continue;

            if (!EvaluadorDependencias.habilitada(v, contestados, a.llaves)) {
                // No se preguntó: no se guarda fila. Ver RespuestaCuestionario.
                fila.noPreguntadas++;
                continue;
            }

            InterpreteCelda.Interpretacion i = InterpreteCelda.interpretar(v, celdas.get(c));

            RespuestaCuestionario r = new RespuestaCuestionario();
            r.setVariable(v);
            r.setOrdenRespuesta(0);
            r.setMarcaAusencia(i.marca());
            r.setValorCodigo(i.codigo());
            r.setValorNumerico(i.numero());
            r.setValorTexto(i.texto());
            r.setValorFecha(i.fecha());
            r.setCeldaOrigen(referencia(fila.numeroFila, c));
            // El valor literal solo se conserva cuando no se pudo interpretar: ahi es la
            // unica forma de recuperar el dato si manana cambia la interpretacion.
            r.setValorCrudo(i.tieneProblema() ? i.crudo() : null);
            if (conservarRespuestas) fila.respuestas.add(r);

            switch (i.marca()) {
                case NINGUNA -> fila.conValor++;
                case NO_AUTORIZADO -> fila.noAutorizadas++;
                case ERROR_LECTURA -> fila.conError++;
                default -> fila.sinRespuesta++;
            }

            if (i.tieneProblema() && a.acumularProblemas) {
                // Un problema no tumba la carga: el dato llega de un escaner y perderlo
                // por no encajar con el catalogo seria quedarse sin la informacion y sin
                // la senal de que el catalogo hay que corregirlo.
                a.problemas.add(new PrevisualizacionVolcado.ProblemaCelda(
                        fila.numeroFila, referencia(fila.numeroFila, c), v.getCodigo(), i.problema()));
            }
        }

        // ── Las reglas de la plantilla ───────────────────────────────────
        //
        // Se revisan despues de interpretar, con todo lo de la fila ya leido, y
        // NINGUNA rechaza nada. La hoja ya se contesto en papel: el dato existe
        // aunque incumpla, y tirarlo seria perder la respuesta y, con ella, la
        // senal de que la aplicacion del cuestionario salio mal.
        if (!a.reglas.isEmpty()) {
            for (MotorValidaciones.Aviso aviso : MotorValidaciones.revisar(
                    a.reglas, contestados, a.porCodigo, fila.edad)) {
                fila.avisosDeRegla.add(aviso.codigo() + ": " + aviso.mensaje());
            }
        }

        return fila;
    }

    // ── Hoja de lecturas en desacuerdo ───────────────────────────────────────

    /**
     * Lee la hoja «Duplicados» y la agrupa por celda.
     *
     * <p>Agrupar es lo que hace que esto sea correcto. La hoja trae una fila por
     * <em>par</em> de lecturas, así que una celda leída de tres formas aparece en tres
     * filas con el mismo «valor previo»: tomadas de una en una parecerían tres
     * correcciones distintas de la misma celda. Reunidas por celda son lo que son, una
     * celda con tres lecturas y ninguna decisión.</p>
     *
     * <p>El «valor previo» es, en los dos volcados recibidos, el que quedó en la hoja de
     * respuestas —cincuenta y dos de cincuenta y dos—. Por eso se registra como el valor
     * en firme y los demás como alternas, en lugar de aplicar el «valor nuevo» por
     * encima: aplicarlo sobrescribiría el dato con una lectura que nadie adjudicó.</p>
     */
    private Map<Integer, List<DiscrepanciaLectura>> leerDesacuerdos(
            TablaLeida hoja, Map<String, VariableCuestionario> porCodigo, TablaLeida respuestas) {

        int cVariable = indiceDe(hoja, COL_VARIABLE);
        int cCelda = indiceDe(hoja, COL_CELDA);
        int cPrevio = indiceDe(hoja, COL_PREVIO);
        int cNuevo = indiceDe(hoja, COL_NUEVO);
        int cFila = indiceDe(hoja, COL_FILA);

        if (cVariable < 0 || cCelda < 0 || cFila < 0) {
            // La hoja existe pero no tiene la forma esperada. Se omite en lugar de
            // tumbar la carga: las respuestas son el dato principal y perderlas por una
            // pestana auxiliar seria desproporcionado. La previsualizacion informa que
            // vinieron cero discrepancias, que es lo que el usuario necesita ver.
            log.warn("La hoja de lecturas en desacuerdo no trae las columnas esperadas; se omite.");
            return new HashMap<>();
        }

        // celda -> acumulado, para poder reunir las varias lecturas de una misma celda
        Map<String, DiscrepanciaLectura> porCelda = new LinkedHashMap<>();
        Map<String, Integer> filaDeCelda = new HashMap<>();
        Map<String, List<String>> alternasDeCelda = new HashMap<>();

        for (List<String> f : hoja.filas()) {
            String codigo = valor(f, cVariable);
            String celda = valor(f, cCelda).replace("$", "");
            String previo = valor(f, cPrevio);
            String nuevo = valor(f, cNuevo);
            Integer numeroFila = aEntero(valor(f, cFila));
            if (celda.isEmpty() || numeroFila == null) continue;

            DiscrepanciaLectura d = porCelda.get(celda);
            if (d == null) {
                d = new DiscrepanciaLectura();
                d.setColumnaVolcado(codigo);
                d.setVariable(porCodigo.get(codigo));
                d.setCeldaOrigen(celda);
                d.setValorEnFirme(previo);
                porCelda.put(celda, d);
                filaDeCelda.put(celda, numeroFila);
                alternasDeCelda.put(celda, new ArrayList<>());
            }
            List<String> alternas = alternasDeCelda.get(celda);
            if (!nuevo.isEmpty() && !nuevo.equals(d.getValorEnFirme()) && !alternas.contains(nuevo)) {
                alternas.add(nuevo);
            }
        }

        Map<Integer, List<DiscrepanciaLectura>> porFila = new HashMap<>();
        for (Map.Entry<String, DiscrepanciaLectura> e : porCelda.entrySet()) {
            DiscrepanciaLectura d = e.getValue();
            d.setLecturasAlternas(String.join(" | ", alternasDeCelda.get(e.getKey())));
            porFila.computeIfAbsent(filaDeCelda.get(e.getKey()), k -> new ArrayList<>()).add(d);
        }
        return porFila;
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private LectorArchivoTabular.HojaLeida elegirHoja(
            List<LectorArchivoTabular.HojaLeida> hojas, List<String> nombres) {
        for (LectorArchivoTabular.HojaLeida h : hojas) {
            String n = NormalizadorAlias.normalizar(h.nombre());
            if (n == null) continue;
            for (String candidato : nombres) {
                if (n.equals(candidato) || n.contains(candidato)) return h;
            }
        }
        return null;
    }

    /**
     * Localiza la columna de la etiqueta.
     *
     * <p>Busca primero el nombre configurado en el instrumento y, si no está, cualquiera
     * de los que han llegado hasta ahora. La lista de alternativas existe porque el
     * nombre ya cambió una vez entre dos archivos del mismo origen: el primero la
     * llamaba {@code Id-Participante} y el siguiente {@code Folio}.</p>
     */
    private int buscarColumnaEtiqueta(TablaLeida tabla, String configurada) {
        List<String> candidatos = new ArrayList<>();
        if (configurada != null && !configurada.isBlank()) {
            candidatos.add(NormalizadorAlias.normalizar(configurada));
        }
        candidatos.addAll(List.of("FOLIO", "ID-PARTICIPANTE", "ID PARTICIPANTE", "ETIQUETA", "NIP"));

        for (String candidato : candidatos) {
            for (int c = 0; c < tabla.encabezados().size(); c++) {
                if (candidato.equals(NormalizadorAlias.normalizar(tabla.encabezados().get(c)))) return c;
            }
        }
        return -1;
    }

    /**
     * Las claves de instrumento que aparecen en las etiquetas del archivo.
     *
     * <p>Mira todas las filas, no la primera: la primera puede venir vacía —en el
     * volcado recibido, dos de diez lo están—. Puede devolver el conjunto vacío, y
     * eso es normal: cuando la columna trae el folio pelado no hay ninguna clave que
     * leer.</p>
     */
    private Set<String> clavesDe(TablaLeida tabla, int columnaEtiqueta) {
        Set<String> claves = new LinkedHashSet<>();
        if (columnaEtiqueta < 0) return claves;
        for (List<String> f : tabla.filas()) {
            String etiqueta = valor(f, columnaEtiqueta);
            if (etiqueta.isEmpty() || EtiquetaCuestionario.esFolioPelado(etiqueta)) continue;
            EtiquetaCuestionario.Lectura l = EtiquetaCuestionario.leer(etiqueta);
            if (l.valida()) claves.add(l.clave());
        }
        return claves;
    }

    /**
     * Lo que hay que decirle al usuario sobre el rotulado, sin impedirle cargar.
     *
     * <p>Son dos señales distintas y conviene no confundirlas. Varias claves en un
     * archivo quieren decir que el volcado junta hojas de instrumentos distintos, y
     * entonces casi nada de lo que viene encaja con el catálogo elegido. Una sola
     * clave que no es la del instrumento seleccionado suele ser lo contrario: el
     * archivo correcto subido en la plantilla equivocada, y ahi el recuento de
     * columnas emparejadas de la previsualización lo deja claro de inmediato.</p>
     *
     * <p>Ninguna de las dos rechaza el archivo. Lo que identifica al participante es
     * el folio, y el instrumento lo eligió una persona; corregirle el rotulado a
     * costa de no cargar nada seria cambiar un problema de datos por uno peor.</p>
     */
    private String avisoDeEtiquetas(Set<String> claves, TipoCuestionario tipo) {
        if (claves.isEmpty()) return null;
        if (claves.size() > 1) {
            return "Las etiquetas del archivo traen claves de varios instrumentos ("
                    + String.join(", ", claves) + "). Comprueba que el volcado sea de uno solo.";
        }
        String unica = claves.iterator().next();
        if (unica.equalsIgnoreCase(tipo.getClave())) return null;
        return "Las etiquetas del archivo dicen " + unica + " y se va a cargar como "
                + tipo.getClave() + " (" + tipo.getNombre() + "). Revisa que sea la plantilla "
                + "correcta: las columnas emparejadas de abajo lo confirman.";
    }

    private int indiceDe(TablaLeida tabla, String encabezado) {
        for (int c = 0; c < tabla.encabezados().size(); c++) {
            if (encabezado.equals(NormalizadorAlias.normalizar(tabla.encabezados().get(c)))) return c;
        }
        return -1;
    }

    private String valor(List<String> fila, int indice) {
        if (indice < 0 || indice >= fila.size()) return "";
        String v = fila.get(indice);
        return v == null ? "" : v.trim();
    }

    private Integer aEntero(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Referencia estilo hoja de cálculo ({@code G6}), que es como el usuario la va a buscar. */
    private String referencia(int fila, int columna) {
        StringBuilder letra = new StringBuilder();
        int c = columna;
        do {
            letra.insert(0, (char) ('A' + (c % 26)));
            c = c / 26 - 1;
        } while (c >= 0);
        return letra + String.valueOf(fila);
    }

    private String nombreDe(Paciente paciente) {
        var persona = paciente.getPersona();
        if (persona == null) return paciente.getFolio();
        return String.join(" ",
                        Objects.toString(persona.getNombre(), ""),
                        Objects.toString(persona.getApellidoPaterno(), ""),
                        Objects.toString(persona.getApellidoMaterno(), ""))
                .replaceAll("\\s+", " ").trim();
    }
}
