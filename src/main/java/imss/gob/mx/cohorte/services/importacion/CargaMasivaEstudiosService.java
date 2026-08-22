package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.estudios.parametros.ParametroEstudio;
import imss.gob.mx.cohorte.modules.estudios.tipos.TipoEstudio;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
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

        var emparejado = EmparejadorColumnas.emparejar(tabla.encabezados(), parametros);

        // Sin estructura valida no tiene sentido interpretar las filas: se
        // devolveria una lista de errores derivados que esconderia la causa real.
        if (!emparejado.utilizable()) {
            return soloEstructura(tipo, tabla, emparejado);
        }

        int colFolio = emparejado.indiceDe(Rol.FOLIO);
        int colFecha = emparejado.indiceDe(Rol.FECHA);
        List<Columna> deParametro = emparejado.conRol(Rol.PARAMETRO);

        // El orden dia/mes se decide mirando el archivo completo, nunca fila a
        // fila: leerlo distinto en dos filas del mismo archivo es lo que mueve
        // estudios de mes sin que nadie lo note.
        List<String> fechasCrudas = tabla.filas().stream().map(f -> f.get(colFecha)).toList();
        var interpretacion = NormalizadorFecha.inferirOrden(fechasCrudas);

        // Una sola consulta para todos los folios del archivo, en vez de una por
        // fila: un archivo de 500 filas haria 500 viajes a la base.
        Map<String, Paciente> porFolio = resolverParticipantes(tabla, colFolio);

        List<PrevisualizacionCarga.FilaPrevisualizada> filas = new ArrayList<>();
        int conProblemas = 0;

        for (int i = 0; i < tabla.filas().size(); i++) {
            var fila = interpretarFila(
                    tabla.filas().get(i), tabla.numerosDeFila().get(i),
                    colFolio, colFecha, deParametro, porFolio, interpretacion.orden());
            if (fila.tieneProblemas()) conProblemas++;
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
                columnasReconocidas(deParametro),
                tabla,
                colFolio,
                colFecha,
                filas,
                new PrevisualizacionCarga.Resumen(
                        filas.size(), filas.size() - conProblemas, conProblemas,
                        deParametro.size(), emparejado.conRol(Rol.IGNORADA).size()));
    }

    // ── Filas ────────────────────────────────────────────────────────────────

    private PrevisualizacionCarga.FilaPrevisualizada interpretarFila(
            List<String> celdas, int numeroDeFila,
            int colFolio, int colFecha, List<Columna> deParametro,
            Map<String, Paciente> porFolio, NormalizadorFecha.Orden orden) {

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
            String crudo = celdas.get(c.indice());
            String error = null;
            try {
                ConversorValor.convertir(crudo, c.parametro().getTipo(), c.parametro().getOpciones());
            } catch (ValorNoValidoException e) {
                error = e.getMessage();
            }
            valores.add(new PrevisualizacionCarga.ValorPrevisualizado(
                    c.parametro().getId(), crudo, error));
        }

        return new PrevisualizacionCarga.FilaPrevisualizada(
                numeroDeFila, folio,
                paciente != null ? paciente.getUuid() : null,
                paciente != null ? nombreCompleto(paciente) : null,
                errorParticipante,
                fechaTexto, errorFecha, valores);
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

    private static String nombreCompleto(Paciente p) {
        if (p.getPersona() == null) return "";
        var per = p.getPersona();
        return (per.getNombre()
                + (per.getSegundoNombre() != null ? " " + per.getSegundoNombre() : "")
                + " " + per.getApellidoPaterno()
                + (per.getApellidoMaterno() != null ? " " + per.getApellidoMaterno() : "")).trim();
    }

    // ── Armado del resultado ─────────────────────────────────────────────────

    private PrevisualizacionCarga soloEstructura(TipoEstudio tipo, TablaLeida tabla,
                                                 EmparejadorColumnas.Emparejado e) {
        return new PrevisualizacionCarga(
                tipo.getId(), tipo.getNombre(),
                e.problemas(),
                encabezadosIgnorados(e),
                e.parametrosSinColumna().stream().map(ParametroEstudio::getNombre).toList(),
                null, false,
                columnasReconocidas(e.conRol(Rol.PARAMETRO)),
                tabla,
                e.indiceDe(Rol.FOLIO),
                e.indiceDe(Rol.FECHA),
                List.of(),
                new PrevisualizacionCarga.Resumen(0, 0, 0,
                        e.conRol(Rol.PARAMETRO).size(), e.conRol(Rol.IGNORADA).size()));
    }

    private static List<String> encabezadosIgnorados(EmparejadorColumnas.Emparejado e) {
        return e.conRol(Rol.IGNORADA).stream().map(Columna::encabezado).toList();
    }

    private static List<PrevisualizacionCarga.ColumnaReconocida> columnasReconocidas(List<Columna> cols) {
        return cols.stream()
                .map(c -> new PrevisualizacionCarga.ColumnaReconocida(
                        c.indice(), c.encabezado(), c.parametro().getId(), c.parametro().getNombre(),
                        c.parametro().getTipo().name(), c.aliasUsado()))
                .toList();
    }
}
