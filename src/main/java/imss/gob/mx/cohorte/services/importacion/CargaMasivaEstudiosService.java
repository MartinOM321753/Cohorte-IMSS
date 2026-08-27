package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedicoRepository;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.estudios.EstudioMedico;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudio;
import imss.gob.mx.cohorte.modules.estudios.resultados.ResultadoEstudioRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.estudios.EstudioService;
import imss.gob.mx.cohorte.services.estudios.TipoService;
import imss.gob.mx.cohorte.services.importacion.ConversorValor.ValorNoValidoException;
import imss.gob.mx.cohorte.services.importacion.EmparejadorColumnas.Columna;
import imss.gob.mx.cohorte.services.importacion.EmparejadorColumnas.Rol;
import imss.gob.mx.cohorte.services.importacion.NormalizadorFecha.FechaNoReconocidaException;
import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Prepara una carga masiva de resultados de estudios y la deja lista para que
 * alguien la revise.
 *
 * <p>Este servicio NO escribe nada. Lee el archivo, decide que significa cada
 * columna, resuelve a que participante corresponde cada fila y convierte cada
 * celda, dejando marcado lo que no cuadra. Guardar es un paso aparte y
 * deliberado: una carga masiva mal interpretada puede meter cientos de
 * mediciones equivocadas de una sola vez, y deshacerlas despues es mucho mas
 * caro que revisarlas antes.</p>
 */
@Service
@RequiredArgsConstructor
public class CargaMasivaEstudiosService {

    private final LectorArchivoTabular lector;
    private final TipoService tipoService;
    private final PacienteRepository pacienteRepository;
    private final ParticipanteAccesoService accesoService;
    private final EstudioMedicoRepository estudioMedicoRepository;
    private final EstudioService estudioService;
    private final InstitucionContextService institucionContextService;
    private final ResultadoEstudioRepository resultadoEstudioRepository;

    /** Los estudios de captura normal viven todos en el grupo raiz. */
    private static final String GRUPO_RAIZ = "ROOT";

    private static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    @Transactional(readOnly = true)
    public PrevisualizacionCarga previsualizar(MultipartFile archivo, Long idTipoEstudio) {
        TipoEstudio tipo = tipoService.getOneParaLectura(idTipoEstudio);
        TablaLeida tabla = lector.leer(archivo);
        return analizar(tabla, tipo);
    }

    /**
     * Vuelve a analizar una tabla que el usuario ya corrigio en pantalla.
     *
     * <p>Recibe la tabla en JSON en vez del archivo para no obligar a subirlo de
     * nuevo en cada correccion. Lo importante es que pasa por exactamente el
     * mismo analisis que la primera lectura: si la pantalla validara por su
     * cuenta, acabaria habiendo dos reglas distintas para el mismo dato y la que
     * manda —la del servidor— seria la que nadie ve.</p>
     */
    @Transactional(readOnly = true)
    public PrevisualizacionCarga revalidar(TablaLeida tabla, Long idTipoEstudio) {
        TipoEstudio tipo = tipoService.getOneParaLectura(idTipoEstudio);
        verificarLimites(tabla);
        return analizar(tabla, tipo);
    }

    /**
     * Los mismos topes que aplica el lector de archivos.
     *
     * <p>Hacen falta otra vez porque esta vía no pasa por el lector: sin esto,
     * mandar un JSON con un millon de filas seria una forma de tumbar el
     * servidor saltandose el limite del archivo.</p>
     */
    private void verificarLimites(TablaLeida tabla) {
        if (tabla.encabezados() == null || tabla.encabezados().isEmpty()) {
            throw new ArchivoInvalidoException("La tabla no trae encabezados.");
        }
        if (tabla.encabezados().size() > LimitesArchivo.MAX_COLUMNAS) {
            throw new ArchivoInvalidoException(
                    "La tabla tiene " + tabla.encabezados().size() + " columnas y el maximo es "
                            + LimitesArchivo.MAX_COLUMNAS + ".");
        }
        if (tabla.filas() == null) {
            throw new ArchivoInvalidoException("La tabla no trae filas.");
        }
        if (tabla.filas().size() > LimitesArchivo.MAX_FILAS) {
            throw new ArchivoInvalidoException(
                    "La tabla tiene " + tabla.filas().size() + " filas y el maximo es "
                            + LimitesArchivo.MAX_FILAS + ".");
        }
        for (int i = 0; i < tabla.filas().size(); i++) {
            List<String> fila = tabla.filas().get(i);
            if (fila == null || fila.size() != tabla.encabezados().size()) {
                throw new ArchivoInvalidoException(
                        "La fila " + (i + 1) + " no tiene el mismo numero de celdas que los encabezados.");
            }
            for (String celda : fila) {
                if (celda != null && celda.length() > LimitesArchivo.MAX_CARACTERES_CELDA) {
                    throw new ArchivoInvalidoException(
                            "Hay una celda con mas de " + LimitesArchivo.MAX_CARACTERES_CELDA + " caracteres.");
                }
            }
        }
    }

    private PrevisualizacionCarga analizar(TablaLeida tabla, TipoEstudio tipo) {

        // La captura por grupos repite el mismo cuadro de parametros varias veces
        // dentro de un estudio, y una tabla plana no tiene forma de expresar a que
        // repeticion pertenece cada fila. Se rechaza en vez de inventar un criterio.
        if ("GRUPOS".equalsIgnoreCase(tipo.getTipoCapturaDefecto())) {
            throw new ArchivoInvalidoException(
                    "\"" + tipo.getNombre() + "\" se captura por grupos y la carga masiva todavia no "
                            + "contempla ese modo. Registra estos estudios desde el formulario.");
        }

        List<ParametroEstudio> parametros = tipo.getParametros() == null
                ? List.of() : List.copyOf(tipo.getParametros());
        if (parametros.isEmpty()) {
            throw new ArchivoInvalidoException(
                    "\"" + tipo.getNombre() + "\" no tiene parametros configurados, asi que no hay nada que cargar.");
        }

        if (tabla.vacia()) {
            throw new ArchivoInvalidoException("El archivo no tiene ninguna fila de datos.");
        }

        var emparejado = EmparejadorColumnas.emparejar(tabla.encabezados(),
                parametros.stream().map(EmparejadorColumnas::desdeParametro).toList());

        // En un estudio todos los parametros son obligatorios, asi que un destino
        // sin columna detiene la carga igual que un conflicto. En examenes no es
        // asi, y por eso la regla vive aqui y no en el emparejador.
        //
        // Sin estructura valida no tiene sentido interpretar las filas: se
        // devolveria una lista de errores derivados que esconderia la causa real.
        if (!emparejado.sinConflictos() || !emparejado.destinosSinColumna().isEmpty()) {
            return soloEstructura(tipo, tabla, emparejado);
        }

        int colFolio = emparejado.indiceDe(Rol.FOLIO);
        int colFecha = emparejado.indiceDe(Rol.FECHA);
        List<Columna> deParametro = emparejado.conRol(Rol.PARAMETRO);
        Map<Long, ParametroEstudio> parametroPorId = parametros.stream()
                .collect(java.util.stream.Collectors.toMap(ParametroEstudio::getId, pa -> pa));

        // El orden dia/mes se decide mirando el archivo completo, nunca fila a
        // fila: leerlo distinto en dos filas del mismo archivo es lo que mueve
        // estudios de mes sin que nadie lo note.
        List<String> fechasCrudas = tabla.filas().stream().map(f -> f.get(colFecha)).toList();
        var interpretacion = NormalizadorFecha.inferirOrden(fechasCrudas);

        // Una sola consulta para todos los folios del archivo, en vez de una por
        // fila: un archivo de 500 filas haria 500 viajes a la base.
        Map<String, Paciente> porFolio = resolverParticipantes(tabla, colFolio);

        Map<String, Long> yaRegistrados = buscarYaRegistrados(tipo, porFolio.values());

        List<PrevisualizacionCarga.FilaPrevisualizada> filas = new ArrayList<>();
        int conProblemas = 0;
        int duplicadas = 0;

        for (int i = 0; i < tabla.filas().size(); i++) {
            var fila = interpretarFila(
                    tabla.filas().get(i), tabla.numerosDeFila().get(i),
                    colFolio, colFecha, deParametro, parametroPorId, porFolio,
                    interpretacion.orden(), yaRegistrados);
            if (fila.tieneProblemas()) conProblemas++;
            if (fila.idEstudioExistente() != null) duplicadas++;
            filas.add(fila);
        }

        return new PrevisualizacionCarga(
                tipo.getId(),
                tipo.getNombre(),
                List.of(),
                encabezadosIgnorados(emparejado),
                List.of(),
                interpretacion.orden().name(),
                interpretacion.ambiguo(),
                columnasReconocidas(deParametro, parametroPorId),
                tabla,
                colFolio,
                colFecha,
                filas,
                new PrevisualizacionCarga.Resumen(
                        filas.size(), filas.size() - conProblemas, conProblemas,
                        deParametro.size(), emparejado.conRol(Rol.IGNORADA).size(), duplicadas));
    }

    /** Que hacer con las filas que chocan con un estudio ya registrado. */
    public enum PoliticaDuplicados {
        /** No se tocan; se cuentan y se informan. Es el comportamiento por defecto. */
        OMITIR,
        /** Se sustituyen los resultados del estudio que ya existe. */
        REEMPLAZAR
    }

    /**
     * Escribe la carga. Es el unico metodo de esta clase que modifica datos.
     *
     * <p>Vuelve a analizar la tabla desde cero en vez de fiarse de lo que la
     * pantalla dice que estaba bien. La previsualizacion la calculo el servidor,
     * pero viaja por el cliente y vuelve: darla por buena permitiria guardar
     * cualquier cosa manipulando la peticion. Ademas, entre la revision y la
     * confirmacion pueden haber cambiado los datos —un participante reasignado,
     * un parametro borrado del catalogo— y el analisis nuevo lo detecta.</p>
     *
     * <p>Todo ocurre en una transaccion. Una carga a medias es peor que una
     * fallida: nadie sabria donde se quedo, y reintentarla duplicaria la parte
     * que si entro.</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultadoCarga confirmar(TablaLeida tabla, Long idTipoEstudio, PoliticaDuplicados politica) {
        TipoEstudio tipo = tipoService.getOneParaLectura(idTipoEstudio);
        verificarLimites(tabla);
        PrevisualizacionCarga previa = analizar(tabla, tipo);

        if (!previa.problemasDeEstructura().isEmpty() || !previa.parametrosSinColumna().isEmpty()) {
            throw new ArchivoInvalidoException(
                    "El archivo no encaja con \"" + tipo.getNombre() + "\". Vuelve a revisarlo antes de guardar.");
        }
        if (previa.resumen().filasConProblemas() > 0) {
            throw new ArchivoInvalidoException(
                    "Todavia hay " + previa.resumen().filasConProblemas()
                            + " fila(s) con datos por corregir. Corrigelas antes de guardar.");
        }

        BeanUser usuarioActual = institucionContextService.getUsuarioActual();
        Map<Long, ParametroEstudio> porId = tipo.getParametros().stream()
                .collect(java.util.stream.Collectors.toMap(ParametroEstudio::getId, pa -> pa));

        List<ResultadoCarga.Detalle> detalle = new ArrayList<>();
        int registrados = 0, reemplazados = 0, omitidos = 0;

        for (PrevisualizacionCarga.FilaPrevisualizada fila : previa.filas()) {
            if (fila.idEstudioExistente() != null && politica == PoliticaDuplicados.OMITIR) {
                omitidos++;
                detalle.add(new ResultadoCarga.Detalle(fila.numeroDeFila(), fila.folio(),
                        fila.nombreParticipante(), fila.fecha(), "OMITIDO", fila.idEstudioExistente()));
                continue;
            }

            EstudioMedico estudio = fila.idEstudioExistente() != null
                    ? estudioService.getOne(fila.idEstudioExistente())
                    : new EstudioMedico();

            estudio.setPaciente(accesoService.resolver(fila.uuidParticipante()));
            estudio.setTipoEstudio(tipo);
            estudio.setFechaEstudio(java.time.LocalDateTime.parse(fila.fecha()));
            estudio.setUsuarioRealiza(usuarioActual);

            List<ResultadoEstudio> resultados = construirResultados(fila, porId, estudio);

            if (fila.idEstudioExistente() != null) {
                reemplazarResultados(estudio, resultados);
                estudioService.update(estudio);
                reemplazados++;
                detalle.add(new ResultadoCarga.Detalle(fila.numeroDeFila(), fila.folio(),
                        fila.nombreParticipante(), fila.fecha(), "REEMPLAZADO", estudio.getId()));
            } else {
                estudio.setResultadoEstudio(resultados);
                EstudioMedico guardado = estudioService.create(estudio);
                registrados++;
                detalle.add(new ResultadoCarga.Detalle(fila.numeroDeFila(), fila.folio(),
                        fila.nombreParticipante(), fila.fecha(), "REGISTRADO", guardado.getId()));
            }
        }

        return new ResultadoCarga(registrados, reemplazados, omitidos, detalle);
    }

    /**
     * Sustituye los resultados de un estudio que ya existia.
     *
     * <p>Se trabaja sobre la coleccion y no se cambia la referencia: con
     * orphanRemoval, sustituir la lista deja a Hibernate sin saber que borrar.</p>
     *
     * <p>El flush intermedio no es opcional. Hibernate ordena las sentencias por
     * tipo y emite los INSERT antes que los DELETE, asi que sin forzar el borrado
     * primero los resultados nuevos chocan con los viejos. Es el mismo patron
     * que usa la edicion de un estudio desde el formulario.</p>
     */
    private void reemplazarResultados(EstudioMedico estudio, List<ResultadoEstudio> nuevos) {
        if (estudio.getResultadoEstudio() == null) {
            estudio.setResultadoEstudio(new ArrayList<>());
        }

        // Se reemplaza lo que el archivo trae, no todo lo que habia. Un parametro
        // que el archivo ni siquiera menciona conserva su valor anterior: borrarlo
        // seria destruir un dato sobre el que la carga no dice nada. El caso real
        // son los parametros retirados del catalogo, cuya columna ya no viaja en el
        // archivo pero cuyo valor sigue siendo parte de esa captura.
        Set<Long> vienenEnLaCarga = nuevos.stream()
                .map(r -> r.getParametro().getId())
                .collect(Collectors.toSet());

        List<ResultadoEstudio> conservados = estudio.getResultadoEstudio().stream()
                .filter(r -> r.getParametro() != null
                        && !vienenEnLaCarga.contains(r.getParametro().getId()))
                .toList();

        estudio.getResultadoEstudio().clear();
        resultadoEstudioRepository.flush();
        estudio.getResultadoEstudio().addAll(conservados);
        estudio.getResultadoEstudio().addAll(nuevos);
    }

    /**
     * Convierte los valores ya validados de una fila en resultados.
     *
     * <p>La conversion se repite aqui en vez de reutilizar la de la
     * previsualizacion porque alli solo interesaba saber si el valor se entendia;
     * aqui hace falta el valor en si. Es la misma funcion, asi que no puede dar
     * un resultado distinto.</p>
     */
    private List<ResultadoEstudio> construirResultados(
            PrevisualizacionCarga.FilaPrevisualizada fila,
            Map<Long, ParametroEstudio> porId,
            EstudioMedico estudio) {

        List<ResultadoEstudio> resultados = new ArrayList<>();
        int orden = 0;
        for (PrevisualizacionCarga.ValorPrevisualizado v : fila.valores()) {
            ParametroEstudio parametro = porId.get(v.idParametro());
            if (parametro == null) continue;

            var convertido = ConversorValor.convertir(v.crudo(), parametro.getTipo(), parametro.getOpciones());

            ResultadoEstudio r = new ResultadoEstudio();
            r.setParametro(parametro);
            r.setEstudio(estudio);
            r.setValorNumerico(convertido.numerico());
            r.setValorTexto(convertido.texto());
            r.setValorBooleano(convertido.booleano());
            // Los estudios de captura normal viven todos en el grupo raiz; la
            // carga masiva no admite grupos, asi que aqui siempre es ROOT.
            r.setGrupoCodigo(GRUPO_RAIZ);
            r.setOrdenResultado(orden++);
            resultados.add(r);
        }
        return resultados;
    }

    // ── Filas ────────────────────────────────────────────────────────────────

    private PrevisualizacionCarga.FilaPrevisualizada interpretarFila(
            List<String> celdas, int numeroDeFila,
            int colFolio, int colFecha, List<Columna> deParametro,
            Map<Long, ParametroEstudio> parametroPorId,
            Map<String, Paciente> porFolio, NormalizadorFecha.Orden orden,
            Map<String, Long> yaRegistrados) {

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

        List<PrevisualizacionCarga.ValorPrevisualizado> valores = new ArrayList<>();
        for (Columna c : deParametro) {
            ParametroEstudio p = parametroPorId.get(c.destino().id());
            String crudo = celdas.get(c.indice());
            String error = null;
            String canonico = null;
            try {
                var convertido = ConversorValor.convertir(crudo, p.getTipo(), p.getOpciones());
                // Solo tiene sentido en los de seleccion; en el resto el propio
                // texto ya es el valor y devolverlo aqui seria ruido.
                if (p.getTipo() == imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro.TEXTO_OPCIONES) {
                    canonico = convertido.texto();
                }
            } catch (ValorNoValidoException e) {
                error = e.getMessage();
            }
            valores.add(new PrevisualizacionCarga.ValorPrevisualizado(p.getId(), crudo, error, canonico));
        }

        // Solo tiene sentido buscar duplicado si ya se sabe de quien y de cuando.
        Long idExistente = (paciente != null && fechaTexto != null)
                ? yaRegistrados.get(claveDia(paciente.getId(), fechaTexto))
                : null;

        return new PrevisualizacionCarga.FilaPrevisualizada(
                numeroDeFila, folio,
                paciente != null ? paciente.getUuid() : null,
                paciente != null ? nombreCompleto(paciente) : null,
                errorParticipante,
                fechaTexto, errorFecha, idExistente, valores);
    }

    /**
     * Busca de golpe todos los folios del archivo, limitado a las instituciones
     * que esta sesion alcanza: un folio que existe pero pertenece a otra sede
     * tiene que comportarse igual que uno que no existe, o la carga masiva seria
     * una forma de averiguar el padron ajeno.
     */
    private Map<String, Paciente> resolverParticipantes(TablaLeida tabla, int colFolio) {
        List<Long> alcanzables = accesoService.institucionesAlcanzables();
        Map<String, Paciente> encontrados = new HashMap<>();

        for (List<String> fila : tabla.filas()) {
            String folio = fila.get(colFolio).trim();
            if (folio.isEmpty()) continue;
            String clave = claveFolio(folio);
            if (encontrados.containsKey(clave)) continue;

            Optional<Paciente> p = pacienteRepository.findByFolioAndInstitucion_IdIn(folio, alcanzables);
            // Excel se come los ceros a la izquierda al guardar un CSV, asi que
            // "502" en el archivo puede ser el folio "000502". Se reintenta con
            // el relleno antes de darlo por no encontrado.
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

    /**
     * Lo que ya esta registrado de este tipo para los participantes del archivo.
     *
     * <p>El choque se mide por DIA, no por instante. Un aparato puede exportar
     * la misma medicion con una hora distinta a la que se capturo a mano, y
     * comparar el instante exacto dejaria pasar como nuevo un estudio que
     * cualquiera reconoceria como el mismo.</p>
     */
    private Map<String, Long> buscarYaRegistrados(TipoEstudio tipo, java.util.Collection<Paciente> pacientes) {
        if (pacientes.isEmpty()) return Map.of();

        List<Long> ids = pacientes.stream().map(Paciente::getId).distinct().toList();
        Map<String, Long> mapa = new HashMap<>();
        for (Object[] fila : estudioMedicoRepository.buscarDeTipoParaPacientes(tipo.getId(), ids)) {
            Long idPaciente = (Long) fila[0];
            java.time.LocalDateTime fecha = (java.time.LocalDateTime) fila[1];
            Long idEstudio = (Long) fila[2];
            if (fecha == null) continue;
            // Si ya hay varios el mismo dia basta con senalar uno: la decision
            // que toma el usuario es la misma.
            mapa.putIfAbsent(claveDia(idPaciente, fecha.format(SALIDA)), idEstudio);
        }
        return mapa;
    }

    /** Participante + dia, que es la granularidad con la que se detecta el choque. */
    private static String claveDia(Long idPaciente, String fechaIso) {
        return idPaciente + "|" + fechaIso.substring(0, 10);
    }

    private static String nombreCompleto(Paciente p) {
        if (p.getPersona() == null) return "";
        var per = p.getPersona();
        return (per.getNombre()
                + (per.getSegundoNombre() != null ? " " + per.getSegundoNombre() : "")
                + " " + per.getApellidoPaterno()
                + (per.getApellidoMaterno() != null ? " " + per.getApellidoMaterno() : "")).trim();
    }

    // ── Armado del resultado ─────────────────────────────────────────────────

    /**
     * Cuando la estructura no encaja no se enseñan las columnas reconocidas: lo
     * util es el motivo, y una lista parcial invita a pensar que casi funciona.
     */
    private PrevisualizacionCarga soloEstructura(TipoEstudio tipo, TablaLeida tabla,
                                                 EmparejadorColumnas.Emparejado e) {
        return new PrevisualizacionCarga(
                tipo.getId(), tipo.getNombre(),
                e.problemas(),
                encabezadosIgnorados(e),
                e.destinosSinColumna().stream().map(EmparejadorColumnas.Destino::nombre).toList(),
                null, false,
                List.of(),
                tabla,
                e.indiceDe(Rol.FOLIO),
                e.indiceDe(Rol.FECHA),
                List.of(),
                new PrevisualizacionCarga.Resumen(0, 0, 0,
                        e.conRol(Rol.PARAMETRO).size(), e.conRol(Rol.IGNORADA).size(), 0));
    }

    private static List<String> encabezadosIgnorados(EmparejadorColumnas.Emparejado e) {
        return e.conRol(Rol.IGNORADA).stream().map(Columna::encabezado).toList();
    }

    private List<PrevisualizacionCarga.ColumnaReconocida> columnasReconocidas(
            List<Columna> cols, Map<Long, ParametroEstudio> parametroPorId) {
        return cols.stream()
                .map(c -> {
                    ParametroEstudio p = parametroPorId.get(c.destino().id());
                    return new PrevisualizacionCarga.ColumnaReconocida(
                            c.indice(), c.encabezado(), p.getId(), p.getNombre(),
                            p.getTipo().name(), c.aliasUsado(),
                            p.getOpciones() == null ? List.of()
                                    : p.getOpciones().stream().map(o -> o.getValor()).toList());
                })
                .toList();
    }
}
