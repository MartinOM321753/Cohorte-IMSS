package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.examenes.AliasExamen;
import imss.gob.mx.cohorte.modules.examenes.AliasExamenRepository;
import imss.gob.mx.cohorte.modules.examenes.Examen;
import imss.gob.mx.cohorte.modules.examenes.ExamenRepository;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamen;
import imss.gob.mx.cohorte.modules.examenes.resultados.ResultadoExamenRepository;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.importacion.ConversorValor.ValorNoValidoException;
import imss.gob.mx.cohorte.services.importacion.EmparejadorColumnas.Columna;
import imss.gob.mx.cohorte.services.importacion.EmparejadorColumnas.Rol;
import imss.gob.mx.cohorte.services.importacion.NormalizadorFecha.FechaNoReconocidaException;
import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Carga masiva de resultados de laboratorio.
 *
 * <h3>En que se parece y en que no a la de estudios</h3>
 *
 * <p>Comparte todo lo dificil: el lector de archivos, la conversion de valores, la
 * inferencia del orden de la fecha y el emparejado de columnas por alias. Lo que
 * cambia es la forma del dato.</p>
 *
 * <p>Un estudio es una unidad: una fila del archivo produce un estudio con todos
 * sus parametros dentro, y si falta uno el estudio queda incompleto. Un archivo
 * de laboratorio no funciona asi: cada columna es un examen distinto, cada celda
 * es un resultado independiente, y que un perfil traiga glucosa pero no
 * colesterol es lo normal, no un error.</p>
 *
 * <p>De ahi las dos diferencias de fondo: no hay que elegir un tipo antes de
 * subir el archivo —los examenes se buscan todos—, y una celda vacia se omite en
 * silencio en vez de detener nada.</p>
 */
@Service
@RequiredArgsConstructor
public class CargaMasivaExamenesService {

    private final LectorArchivoTabular lector;
    private final ExamenRepository examenRepository;
    private final AliasExamenRepository aliasExamenRepository;
    private final ResultadoExamenRepository resultadoExamenRepository;
    private final PacienteRepository pacienteRepository;
    private final ParticipanteAccesoService accesoService;
    private final InstitucionContextService institucionContextService;

    private static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Transactional(readOnly = true)
    public PrevisualizacionCargaExamenes previsualizar(MultipartFile archivo) {
        return analizar(lector.leer(archivo));
    }

    /** Vuelve a analizar la tabla ya corregida, sin volver a subir el archivo. */
    @Transactional(readOnly = true)
    public PrevisualizacionCargaExamenes revalidar(TablaLeida tabla) {
        verificarLimites(tabla);
        return analizar(tabla);
    }

    // ── Analisis ─────────────────────────────────────────────────────────────

    private PrevisualizacionCargaExamenes analizar(TablaLeida tabla) {
        if (tabla.vacia()) {
            throw new ArchivoInvalidoException("El archivo no tiene ninguna fila de datos.");
        }

        Long idInstitucion = institucionContextService.getIdInstitucionActual();
        List<Examen> examenes = examenRepository.findAllByActivoAndInstitucion_Id(true, idInstitucion);
        if (examenes.isEmpty()) {
            throw new ArchivoInvalidoException(
                    "No hay examenes configurados en el catalogo, asi que no hay donde poner los resultados.");
        }

        // Los alias de todos los examenes de golpe: uno por examen serian tantas
        // consultas como examenes tenga el catalogo.
        Map<Long, List<AliasExamen>> aliasPorExamen = new HashMap<>();
        for (AliasExamen a : aliasExamenRepository.findAllByIdInstitucion(idInstitucion)) {
            aliasPorExamen.computeIfAbsent(a.getExamen().getId(), k -> new ArrayList<>()).add(a);
        }

        Map<Long, Examen> examenPorId = new HashMap<>();
        List<EmparejadorColumnas.Destino> destinos = new ArrayList<>();
        for (Examen e : examenes) {
            examenPorId.put(e.getId(), e);
            destinos.add(EmparejadorColumnas.desdeExamen(e, aliasPorExamen.get(e.getId())));
        }

        var emparejado = EmparejadorColumnas.emparejar(tabla.encabezados(), destinos);

        List<String> problemas = new ArrayList<>(emparejado.problemas());
        List<Columna> deExamen = emparejado.conRol(Rol.PARAMETRO);

        // A diferencia de los estudios, aqui faltar examenes es normal. Lo que no
        // puede faltar es que haya al menos uno: si ninguna columna se reconoce,
        // el archivo no tiene nada que guardar y casi siempre significa que los
        // alias no estan configurados.
        if (deExamen.isEmpty() && problemas.isEmpty()) {
            problemas.add("Ninguna columna del archivo corresponde a un examen del catalogo. "
                    + "Configura los alias de cada examen con el nombre exacto que usa el laboratorio.");
        }

        if (!problemas.isEmpty()) {
            return soloEstructura(tabla, emparejado, problemas);
        }

        int colFolio = emparejado.indiceDe(Rol.FOLIO);
        int colFecha = emparejado.indiceDe(Rol.FECHA);

        var interpretacion = NormalizadorFecha.inferirOrden(
                tabla.filas().stream().map(f -> f.get(colFecha)).toList());

        Map<String, Paciente> porFolio = resolverParticipantes(tabla, colFolio);
        Map<String, Long> yaRegistrados = buscarYaRegistrados(porFolio.values(), deExamen);

        List<PrevisualizacionCargaExamenes.FilaExamenes> filas = new ArrayList<>();
        int inservibles = 0, listos = 0, conError = 0, vacias = 0, duplicados = 0;

        for (int i = 0; i < tabla.filas().size(); i++) {
            var fila = interpretarFila(tabla.filas().get(i), tabla.numerosDeFila().get(i),
                    colFolio, colFecha, deExamen, porFolio, interpretacion.orden(), yaRegistrados);
            filas.add(fila);

            if (fila.inservible()) {
                inservibles++;
                continue;   // sus celdas no cuentan: no se va a guardar nada de esta fila
            }
            for (var v : fila.valores()) {
                if (v.vacio()) vacias++;
                else if (v.error() != null) conError++;
                else if (v.idResultadoExistente() != null) duplicados++;
                else listos++;
            }
        }

        return new PrevisualizacionCargaExamenes(
                List.of(),
                emparejado.conRol(Rol.IGNORADA).stream().map(Columna::encabezado).toList(),
                emparejado.destinosSinColumna().stream()
                        .map(EmparejadorColumnas.Destino::nombre).toList(),
                interpretacion.orden().name(),
                interpretacion.ambiguo(),
                columnasReconocidas(deExamen, examenPorId),
                tabla, colFolio, colFecha,
                filas,
                new PrevisualizacionCargaExamenes.Resumen(
                        filas.size(), inservibles, listos, conError, vacias, duplicados,
                        deExamen.size(), emparejado.conRol(Rol.IGNORADA).size()));
    }

    private PrevisualizacionCargaExamenes.FilaExamenes interpretarFila(
            List<String> celdas, int numeroDeFila, int colFolio, int colFecha,
            List<Columna> deExamen, Map<String, Paciente> porFolio,
            NormalizadorFecha.Orden orden, Map<String, Long> yaRegistrados) {

        String folio = celdas.get(colFolio).trim();
        Paciente paciente = porFolio.get(claveFolio(folio));

        String errorParticipante = null;
        if (folio.isEmpty()) {
            errorParticipante = "La fila no trae folio.";
        } else if (paciente == null) {
            errorParticipante = "No hay ningun participante con folio \"" + folio
                    + "\" al que esta institucion tenga acceso.";
        }

        String fechaTexto = null;
        String errorFecha = null;
        try {
            fechaTexto = NormalizadorFecha.parsear(celdas.get(colFecha), orden).format(SALIDA);
        } catch (FechaNoReconocidaException e) {
            errorFecha = e.getMessage();
        }

        List<PrevisualizacionCargaExamenes.ValorExamen> valores = new ArrayList<>();
        for (Columna c : deExamen) {
            String crudo = celdas.get(c.indice());
            boolean vacio = crudo == null || crudo.isBlank();

            String error = null;
            if (!vacio) {
                try {
                    // El valor de un examen siempre es numerico: valor_obtenido es un
                    // double no nulo en la base, asi que no hay otra forma posible.
                    ConversorValor.convertir(crudo, TipoParametro.NUMERICO, null);
                } catch (ValorNoValidoException e) {
                    error = e.getMessage();
                }
            }

            Long existente = (!vacio && error == null && paciente != null && fechaTexto != null)
                    ? yaRegistrados.get(claveResultado(paciente.getId(), c.destino().id(), fechaTexto))
                    : null;

            valores.add(new PrevisualizacionCargaExamenes.ValorExamen(
                    c.destino().id(), crudo == null ? "" : crudo, vacio, error, existente));
        }

        return new PrevisualizacionCargaExamenes.FilaExamenes(
                numeroDeFila, folio,
                paciente != null ? paciente.getUuid() : null,
                paciente != null ? nombreCompleto(paciente) : null,
                errorParticipante, fechaTexto, errorFecha, valores);
    }

    // ── Escritura ────────────────────────────────────────────────────────────

    /** Que hacer con los resultados que ya estan registrados. */
    public enum PoliticaDuplicados { OMITIR, REEMPLAZAR }

    /**
     * Escribe los resultados. Unico metodo que modifica datos.
     *
     * <p>Vuelve a analizar la tabla entera antes de escribir, por lo mismo que en
     * estudios: la previsualizacion paso por el cliente y volvio.</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultadoCarga confirmar(TablaLeida tabla, PoliticaDuplicados politica) {
        verificarLimites(tabla);
        PrevisualizacionCargaExamenes previa = analizar(tabla);

        if (!previa.problemasDeEstructura().isEmpty()) {
            throw new ArchivoInvalidoException(
                    "El archivo no encaja con el catalogo de examenes. Revisalo antes de guardar.");
        }
        if (previa.resumen().filasInservibles() > 0 || previa.resumen().resultadosConError() > 0) {
            throw new ArchivoInvalidoException(
                    "Todavia quedan datos por corregir. Corrigelos antes de guardar.");
        }

        var usuario = institucionContextService.getUsuarioActual();
        Map<Long, Examen> examenPorId = new HashMap<>();
        for (var c : previa.columnas()) {
            examenPorId.put(c.idExamen(), examenRepository.findById(c.idExamen())
                    .orElseThrow(() -> new ArchivoInvalidoException(
                            "Un examen del archivo ya no existe en el catalogo. Vuelve a revisar el archivo.")));
        }

        List<ResultadoCarga.Detalle> detalle = new ArrayList<>();
        int registrados = 0, reemplazados = 0, omitidos = 0;

        for (var fila : previa.filas()) {
            Paciente paciente = accesoService.resolver(fila.uuidParticipante());
            LocalDateTime fecha = LocalDateTime.parse(fila.fecha());

            for (var v : fila.valores()) {
                // Un hueco no es un dato que falte: es un examen que no se hizo.
                if (v.vacio()) continue;

                if (v.idResultadoExistente() != null && politica == PoliticaDuplicados.OMITIR) {
                    omitidos++;
                    detalle.add(new ResultadoCarga.Detalle(fila.numeroDeFila(), fila.folio(),
                            fila.nombreParticipante(), fila.fecha(), "OMITIDO", v.idResultadoExistente()));
                    continue;
                }

                ResultadoExamen r = v.idResultadoExistente() != null
                        ? resultadoExamenRepository.findById(v.idResultadoExistente()).orElseThrow()
                        : new ResultadoExamen();

                r.setPaciente(paciente);
                r.setExamen(examenPorId.get(v.idExamen()));
                r.setFechaResultado(fecha);
                r.setValorObtenido(ConversorValor.convertir(
                        v.crudo(), TipoParametro.NUMERICO, null).numerico());
                r.setUsuarioRegistro(usuario);
                if (r.getFechaRegistro() == null) {
                    r.setFechaRegistro(new Timestamp(System.currentTimeMillis()));
                }
                // La sede que captura, no la del participante: al atender entre
                // sedes pueden ser distintas y el registro guarda quien lo hizo.
                r.setInstitucion(institucionContextService.getInstitucionActual());

                boolean eraNuevo = v.idResultadoExistente() == null;
                ResultadoExamen guardado = resultadoExamenRepository.save(r);

                if (eraNuevo) registrados++; else reemplazados++;
                detalle.add(new ResultadoCarga.Detalle(fila.numeroDeFila(), fila.folio(),
                        fila.nombreParticipante(), fila.fecha(),
                        eraNuevo ? "REGISTRADO" : "REEMPLAZADO", guardado.getId()));
            }
        }

        return new ResultadoCarga(registrados, reemplazados, omitidos, detalle);
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    /**
     * Lo que ya esta registrado, por participante, examen y dia.
     *
     * <p>Mas fino que en estudios: alli el choque es por estudio completo, aqui
     * cada examen va por su cuenta, asi que repetir la glucosa no debe arrastrar
     * al colesterol de la misma fila.</p>
     */
    private Map<String, Long> buscarYaRegistrados(java.util.Collection<Paciente> pacientes,
                                                  List<Columna> deExamen) {
        if (pacientes.isEmpty() || deExamen.isEmpty()) return Map.of();

        List<Long> idsPacientes = pacientes.stream().map(Paciente::getId).distinct().toList();
        List<Long> idsExamenes = deExamen.stream().map(c -> c.destino().id()).toList();

        Map<String, Long> mapa = new HashMap<>();
        for (Object[] f : resultadoExamenRepository.buscarPorPacientesYExamenes(idsPacientes, idsExamenes)) {
            Long idPaciente = (Long) f[0];
            Long idExamen = (Long) f[1];
            LocalDateTime fecha = (LocalDateTime) f[2];
            Long idResultado = (Long) f[3];
            if (fecha == null) continue;
            mapa.putIfAbsent(claveResultado(idPaciente, idExamen, fecha.format(SALIDA)), idResultado);
        }
        return mapa;
    }

    private static String claveResultado(Long idPaciente, Long idExamen, String fechaIso) {
        return idPaciente + "|" + idExamen + "|" + fechaIso.substring(0, 10);
    }

    private Map<String, Paciente> resolverParticipantes(TablaLeida tabla, int colFolio) {
        List<Long> alcanzables = accesoService.institucionesAlcanzables();
        Map<String, Paciente> encontrados = new HashMap<>();

        for (List<String> fila : tabla.filas()) {
            String folio = fila.get(colFolio).trim();
            if (folio.isEmpty()) continue;
            String clave = claveFolio(folio);
            if (encontrados.containsKey(clave)) continue;

            Optional<Paciente> p = pacienteRepository.findByFolioAndInstitucion_IdIn(folio, alcanzables);
            // Excel se come los ceros a la izquierda al guardar un CSV.
            if (p.isEmpty() && folio.matches("\\d{1,5}")) {
                p = pacienteRepository.findByFolioAndInstitucion_IdIn(
                        String.format("%06d", Integer.parseInt(folio)), alcanzables);
            }
            p.ifPresent(value -> encontrados.put(clave, value));
        }
        return encontrados;
    }

    private static String claveFolio(String folio) {
        return folio.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private static String nombreCompleto(Paciente p) {
        if (p.getPersona() == null) return "";
        var per = p.getPersona();
        return (per.getNombre()
                + (per.getSegundoNombre() != null ? " " + per.getSegundoNombre() : "")
                + " " + per.getApellidoPaterno()
                + (per.getApellidoMaterno() != null ? " " + per.getApellidoMaterno() : "")).trim();
    }

    private static List<PrevisualizacionCargaExamenes.ColumnaExamen> columnasReconocidas(
            List<Columna> cols, Map<Long, Examen> examenPorId) {
        return cols.stream().map(c -> {
            Examen e = examenPorId.get(c.destino().id());
            return new PrevisualizacionCargaExamenes.ColumnaExamen(
                    c.indice(), c.encabezado(), e.getId(), e.getParametro(), e.getUnidad(), c.aliasUsado());
        }).toList();
    }

    private PrevisualizacionCargaExamenes soloEstructura(
            TablaLeida tabla, EmparejadorColumnas.Emparejado e, List<String> problemas) {
        return new PrevisualizacionCargaExamenes(
                problemas,
                e.conRol(Rol.IGNORADA).stream().map(Columna::encabezado).toList(),
                List.of(), null, false, List.of(),
                tabla, e.indiceDe(Rol.FOLIO), e.indiceDe(Rol.FECHA),
                List.of(),
                new PrevisualizacionCargaExamenes.Resumen(0, 0, 0, 0, 0, 0,
                        0, e.conRol(Rol.IGNORADA).size()));
    }

    /** Los mismos topes del lector; esta via no pasa por el. */
    private void verificarLimites(TablaLeida tabla) {
        if (tabla.encabezados() == null || tabla.encabezados().isEmpty()) {
            throw new ArchivoInvalidoException("La tabla no trae encabezados.");
        }
        if (tabla.encabezados().size() > LimitesArchivo.MAX_COLUMNAS) {
            throw new ArchivoInvalidoException("La tabla tiene demasiadas columnas.");
        }
        if (tabla.filas() == null || tabla.filas().size() > LimitesArchivo.MAX_FILAS) {
            throw new ArchivoInvalidoException(
                    "La tabla supera el maximo de " + LimitesArchivo.MAX_FILAS + " filas.");
        }
        for (int i = 0; i < tabla.filas().size(); i++) {
            List<String> fila = tabla.filas().get(i);
            if (fila == null || fila.size() != tabla.encabezados().size()) {
                throw new ArchivoInvalidoException(
                        "La fila " + (i + 1) + " no tiene el mismo numero de celdas que los encabezados.");
            }
            for (String celda : fila) {
                if (celda != null && celda.length() > LimitesArchivo.MAX_CARACTERES_CELDA) {
                    throw new ArchivoInvalidoException("Hay una celda desmesuradamente larga.");
                }
            }
        }
    }
}
