package imss.gob.mx.cohorte.services.importacion;

import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenica;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.CajaCriogenicaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.EtiquetaPosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCaja;
import imss.gob.mx.cohorte.modules.almacenamiento.caja.PosicionCajaRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.EstadoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.Muestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.MuestraRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.historial.TipoEventoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucion;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.MuestraTipoInstitucionRepository;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TipoMuestra;
import imss.gob.mx.cohorte.modules.almacenamiento.muestra.tipo.TuboMuestra;
import imss.gob.mx.cohorte.modules.estudios.parametros.TipoParametro;
import imss.gob.mx.cohorte.modules.institucion.Institucion;
import imss.gob.mx.cohorte.modules.paciente.Paciente;
import imss.gob.mx.cohorte.modules.paciente.PacienteRepository;
import imss.gob.mx.cohorte.modules.usuarios.user.BeanUser;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.EtiquetaMuestra;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.HistorialCambioMuestraService;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.PlanificadorAlicuotas;
import imss.gob.mx.cohorte.services.almacenamiento.muestra.TipoMuestraService;
import imss.gob.mx.cohorte.services.importacion.ColumnaMuestra.Emparejado;
import imss.gob.mx.cohorte.services.importacion.ConversorValor.ValorNoValidoException;
import imss.gob.mx.cohorte.services.importacion.NormalizadorFecha.FechaNoReconocidaException;
import imss.gob.mx.cohorte.services.importacion.PrevisualizacionCargaMuestras.FilaPrevisualizada;
import imss.gob.mx.cohorte.services.importacion.PrevisualizacionCargaMuestras.LotePrevisualizado;
import imss.gob.mx.cohorte.services.importacion.PrevisualizacionCargaMuestras.Problema;
import imss.gob.mx.cohorte.services.pacientes.ParticipanteAccesoService;
import imss.gob.mx.cohorte.utils.texto.NormalizadorAlias;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Carga masiva de muestras y alícuotas desde una hoja de cálculo.
 *
 * <p>Este servicio solo escribe en {@link #confirmar}. Todo lo demás lee el
 * archivo, resuelve contra el catálogo y deja marcado lo que no cuadra. Es el
 * mismo reparto que en la carga de estudios y por el mismo motivo, aquí con más
 * razón: un archivo mal interpretado no solo mete datos equivocados, además
 * <b>ocupa huecos físicos</b> de las cajas, y deshacer eso significa ir a abrir
 * congeladores.</p>
 *
 * <h3>Qué se crea</h3>
 * <p>Una fila del archivo es un vial. Los viales se agrupan por participante,
 * tipo, tubo y día, y cada grupo estrena una <b>muestra padre</b>: el registro
 * del tubo del que salieron. Esa padre nace sin posición y, si todos sus viales
 * llegan ubicados, ya agotada — el tubo se repartió entero y se desechó.</p>
 *
 * <h3>La contabilidad de volumen, al revés de lo normal</h3>
 * <p>En el alta normal la padre nace con su volumen y lo va soltando. Aquí la
 * padre se deduce de sus hijas, así que su volumen se calcula como lo que
 * <em>queda por repartir</em>: la suma de los viales que llegan sin ubicar. Los
 * que llegan con hueco ya están materializados, así que no le quedan en el
 * cuerpo. Escribir en {@code valor} la suma de todas las hijas <em>y</em>
 * marcarlas materializadas contaría cada mililitro dos veces.</p>
 *
 * <p>Por lo mismo no se pasa por {@code MaterializacionAlicuotaService}: ese
 * descuenta de una padre que ya tenía el líquido, y aquí la padre nace con la
 * cuenta hecha. Llamarlo la dejaría en negativo.</p>
 */
@Service
@RequiredArgsConstructor
public class CargaMasivaMuestrasService {

    private final LectorArchivoTabular lector;
    private final TipoMuestraService tipoMuestraService;
    private final PacienteRepository pacienteRepository;
    private final ParticipanteAccesoService accesoService;
    private final MuestraRepository muestraRepository;
    private final CajaCriogenicaRepository cajaRepository;
    private final PosicionCajaRepository posicionCajaRepository;
    private final MuestraTipoInstitucionRepository muestraTipoInstitucionRepository;
    private final InstitucionContextService institucionContextService;
    private final HistorialCambioMuestraService historialService;

    private static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    // ── Entradas ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PrevisualizacionCargaMuestras previsualizar(MultipartFile archivo, String fechaPorOmision) {
        return analizar(lector.leer(archivo), fechaPorOmision).previa();
    }

    /**
     * Vuelve a analizar una tabla que el usuario ya corrigió en pantalla.
     *
     * <p>Recibe la tabla en JSON en vez del archivo para no obligar a subirlo de
     * nuevo en cada corrección, pero pasa por exactamente el mismo análisis. Si
     * la pantalla validara por su cuenta acabarían existiendo dos reglas para el
     * mismo dato, y la que manda —esta— sería la que nadie ve.</p>
     */
    @Transactional(readOnly = true)
    public PrevisualizacionCargaMuestras revalidar(TablaLeida tabla, String fechaPorOmision) {
        verificarLimites(tabla);
        return analizar(tabla, fechaPorOmision).previa();
    }

    /**
     * Escribe la carga. Es el único método que modifica datos.
     *
     * <p>Vuelve a analizar la tabla entera antes de escribir. La previsualización
     * la calculó este mismo servicio, pero pasó por el cliente y volvió: darla
     * por buena permitiría guardar cualquier cosa manipulando la petición.
     * Además, entre la revisión y la confirmación un hueco puede haberse ocupado
     * desde otra pantalla, y el análisis nuevo lo detecta.</p>
     *
     * <p>Todo ocurre en una transacción. Una carga a medias es peor que una
     * fallida: nadie sabría por dónde se quedó, reintentarla duplicaría lo que sí
     * entró, y los huecos ocupados quedarían reservados para viales que no
     * existen.</p>
     */
    @Transactional(rollbackFor = Exception.class)
    public ResultadoCargaMuestras confirmar(TablaLeida tabla, String fechaPorOmision) {
        verificarLimites(tabla);
        Analisis analisis = analizar(tabla, fechaPorOmision);
        PrevisualizacionCargaMuestras previa = analisis.previa();

        if (!previa.problemasDeEstructura().isEmpty()) {
            throw new ArchivoInvalidoException(
                    "El archivo no tiene la estructura que espera la carga de muestras. "
                            + "Vuelva a revisarlo antes de guardar.");
        }
        if (previa.resumen().filasConProblemas() > 0) {
            throw new ArchivoInvalidoException(
                    "Todavía hay " + previa.resumen().filasConProblemas()
                            + " fila(s) con datos por corregir. Corríjalas antes de guardar.");
        }
        if (previa.resumen().totalFilas() == 0) {
            throw new ArchivoInvalidoException("El archivo no tiene ninguna fila que cargar.");
        }

        return escribir(analisis);
    }

    // ── Límites ──────────────────────────────────────────────────────────────

    /**
     * Los mismos topes que aplica el lector de archivos.
     *
     * <p>Hacen falta otra vez porque revalidar y confirmar no pasan por el
     * lector: sin esto, mandar un JSON con un millón de filas sería una forma de
     * tumbar el servidor saltándose el límite del archivo.</p>
     */
    private void verificarLimites(TablaLeida tabla) {
        if (tabla == null || tabla.encabezados() == null || tabla.encabezados().isEmpty()) {
            throw new ArchivoInvalidoException("La tabla no trae encabezados.");
        }
        if (tabla.encabezados().size() > LimitesArchivo.MAX_COLUMNAS) {
            throw new ArchivoInvalidoException("La tabla tiene " + tabla.encabezados().size()
                    + " columnas y el máximo es " + LimitesArchivo.MAX_COLUMNAS + ".");
        }
        if (tabla.filas() == null) {
            throw new ArchivoInvalidoException("La tabla no trae filas.");
        }
        if (tabla.filas().size() > LimitesArchivo.MAX_FILAS) {
            throw new ArchivoInvalidoException("La tabla tiene " + tabla.filas().size()
                    + " filas y el máximo es " + LimitesArchivo.MAX_FILAS + ".");
        }
        for (int i = 0; i < tabla.filas().size(); i++) {
            List<String> fila = tabla.filas().get(i);
            if (fila == null || fila.size() != tabla.encabezados().size()) {
                throw new ArchivoInvalidoException("La fila " + (i + 1)
                        + " no tiene el mismo número de celdas que los encabezados.");
            }
            for (String celda : fila) {
                if (celda != null && celda.length() > LimitesArchivo.MAX_CARACTERES_CELDA) {
                    throw new ArchivoInvalidoException("Hay una celda con más de "
                            + LimitesArchivo.MAX_CARACTERES_CELDA + " caracteres.");
                }
            }
        }
    }

    // ── Análisis ─────────────────────────────────────────────────────────────

    /** Lo que el análisis deja listo: la previsualización y lo resuelto para escribir. */
    private record Analisis(PrevisualizacionCargaMuestras previa, List<Lote> lotes) {}

    /** Un grupo de viales que comparten participante, tipo, tubo y día. */
    private static final class Lote {
        String clave;
        Paciente paciente;
        TipoMuestra tipo;
        TuboMuestra tubo;
        LocalDateTime fecha;
        int numeroLote;
        String etiquetaPadre;
        final List<FilaResuelta> filas = new ArrayList<>();
    }

    /** Una fila ya resuelta contra el catálogo, lista para escribirse. */
    private static final class FilaResuelta {
        int numeroDeFila;
        Paciente paciente;
        TipoMuestra tipo;
        TuboMuestra tubo;
        LocalDateTime fecha;
        Integer numeroAlicuota;
        Double volumen;
        String unidad;
        boolean volumenHeredado;
        String codigoCaja;
        String posicionTexto;
        PosicionCaja posicion;
        String etiqueta;
        String claveLote;
        /** La fila traía día pero no hora, y se completó con la de omisión. */
        boolean horaPuesta;
        final List<Problema> errores = new ArrayList<>();
        final List<Problema> avisos = new ArrayList<>();

        void error(ColumnaMuestra campo, String mensaje) {
            errores.add(new Problema(campo == null ? null : campo.name(), mensaje));
        }

        void aviso(ColumnaMuestra campo, String mensaje) {
            avisos.add(new Problema(campo == null ? null : campo.name(), mensaje));
        }
    }

    private Analisis analizar(TablaLeida tabla, String fechaPorOmision) {
        if (tabla.vacia()) {
            throw new ArchivoInvalidoException("El archivo no tiene ninguna fila de datos.");
        }

        Emparejado emparejado = ColumnaMuestra.emparejar(tabla.encabezados());
        if (!emparejado.sinProblemas()) {
            // Sin estructura válida no tiene sentido interpretar las filas: se
            // devolvería una lista de errores derivados que escondería la causa.
            return new Analisis(soloEstructura(tabla, emparejado), List.of());
        }

        Long idInst = institucionContextService.getIdInstitucionActual();
        LocalDateTime porOmision = parsearFechaPorOmision(fechaPorOmision);

        // El orden día/mes se decide mirando el archivo completo, nunca fila a
        // fila: leerlo distinto en dos filas es lo que mueve muestras de mes sin
        // que nadie lo note.
        NormalizadorFecha.Interpretacion interpretacion = interpretarFechas(tabla, emparejado);

        Map<String, Paciente> porFolio = resolverParticipantes(tabla, emparejado.indiceDe(ColumnaMuestra.FOLIO));
        Catalogo catalogo = cargarCatalogo();
        Map<String, CajaCriogenica> cajas = resolverCajas(tabla, emparejado, idInst);

        // Huecos ya reclamados por una fila anterior del mismo archivo. El índice
        // único no salta hasta el commit, así que sin esto dos viales podrían
        // pedir la misma celda y el error aparecería al guardar, no al revisar.
        Set<Long> huecosDelArchivo = new HashSet<>();
        Set<String> vialesDelArchivo = new HashSet<>();

        List<FilaResuelta> resueltas = new ArrayList<>(tabla.totalFilas());
        for (int i = 0; i < tabla.filas().size(); i++) {
            resueltas.add(resolverFila(tabla.filas().get(i), tabla.numerosDeFila().get(i),
                    emparejado, porFolio, catalogo, cajas, interpretacion.orden(), porOmision,
                    huecosDelArchivo, vialesDelArchivo));
        }

        List<Lote> lotes = agrupar(resueltas, idInst);
        verificarEtiquetas(resueltas, lotes, idInst);

        return new Analisis(armar(tabla, emparejado, interpretacion, resueltas, lotes), lotes);
    }

    // ── Resolución de una fila ───────────────────────────────────────────────

    private FilaResuelta resolverFila(List<String> celdas, int numeroDeFila, Emparejado emparejado,
                                      Map<String, Paciente> porFolio, Catalogo catalogo,
                                      Map<String, CajaCriogenica> cajas,
                                      NormalizadorFecha.Orden orden, LocalDateTime porOmision,
                                      Set<Long> huecosDelArchivo, Set<String> vialesDelArchivo) {
        FilaResuelta f = new FilaResuelta();
        f.numeroDeFila = numeroDeFila;

        // ── Participante ──
        String folio = celda(celdas, emparejado, ColumnaMuestra.FOLIO);
        f.paciente = porFolio.get(claveFolio(folio));
        if (folio.isEmpty()) {
            f.error(ColumnaMuestra.FOLIO, "La fila no trae folio.");
        } else if (f.paciente == null) {
            f.error(ColumnaMuestra.FOLIO, "No hay ningún participante con folio \"" + folio
                    + "\" al que esta institución tenga acceso.");
        } else if (!Boolean.TRUE.equals(f.paciente.getActivo())) {
            // Misma regla que PacienteEstadoValidator: con el participante
            // inactivo sus muestras están en cuarentena y no se pueden crear
            // más. Detectarlo aquí evita que la escritura reviente a medias.
            f.error(ColumnaMuestra.FOLIO, "El participante \"" + folio + "\" está inactivo "
                    + "(consentimiento retirado o baja administrativa). Reactívelo antes de cargar.");
        }

        // ── Tipo y tubo ──
        String nombreTipo = celda(celdas, emparejado, ColumnaMuestra.TIPO_MUESTRA);
        f.tipo = catalogo.tipoPorNombre(nombreTipo);
        if (nombreTipo.isEmpty()) {
            f.error(ColumnaMuestra.TIPO_MUESTRA, "La fila no trae tipo de muestra.");
        } else if (f.tipo == null) {
            f.error(ColumnaMuestra.TIPO_MUESTRA, "No hay ningún tipo de muestra activo llamado \""
                    + nombreTipo + "\" en el catálogo de esta institución.");
        }

        String nombreTubo = celda(celdas, emparejado, ColumnaMuestra.TUBO);
        if (nombreTubo.isEmpty()) {
            f.error(ColumnaMuestra.TUBO, "La fila no trae tubo.");
        } else if (f.tipo != null) {
            f.tubo = catalogo.tuboPorNombre(f.tipo, nombreTubo);
            if (f.tubo == null) {
                f.error(ColumnaMuestra.TUBO, "El tipo \"" + f.tipo.getNombre()
                        + "\" no tiene ningún tubo activo llamado \"" + nombreTubo + "\".");
            }
        }

        // ── Fecha ──
        resolverFecha(f, celdas, emparejado, orden, porOmision);

        // ── Número de alícuota ──
        resolverNumeroAlicuota(f, celdas, emparejado);

        // ── Volumen y unidad ──
        resolverVolumen(f, celdas, emparejado);

        // ── Posición ──
        resolverPosicion(f, celdas, emparejado, cajas, huecosDelArchivo);

        // ── Vial repetido dentro del propio archivo ──
        if (f.paciente != null && f.tipo != null && f.tubo != null
                && f.fecha != null && f.numeroAlicuota != null) {
            String clave = claveLote(f) + "|" + f.numeroAlicuota;
            if (!vialesDelArchivo.add(clave)) {
                f.error(ColumnaMuestra.NUMERO_ALICUOTA, "El archivo ya trae otra fila con la "
                        + "alícuota " + f.numeroAlicuota + " de este mismo lote.");
            }
        }

        return f;
    }

    private void resolverFecha(FilaResuelta f, List<String> celdas, Emparejado emparejado,
                               NormalizadorFecha.Orden orden, LocalDateTime porOmision) {
        String crudo = celda(celdas, emparejado, ColumnaMuestra.FECHA);

        if (crudo.isEmpty()) {
            if (porOmision == null) {
                f.error(ColumnaMuestra.FECHA, "La fila no trae fecha de toma. Escríbala, o elija "
                        + "arriba una fecha para las filas que no la traen.");
                return;
            }
            // Este sí va fila a fila: una fila sin fecha cae en el día de la
            // fecha elegida, y eso puede separarla del lote de sus hermanas
            // —el día forma parte de lo que define una muestra padre—.
            f.fecha = porOmision;
            f.aviso(ColumnaMuestra.FECHA, "Sin fecha en el archivo: se usa la elegida arriba, "
                    + "así que este vial contará como un lote de ese día.");
            return;
        }

        try {
            f.fecha = NormalizadorFecha.parsear(crudo, orden);
        } catch (FechaNoReconocidaException e) {
            f.error(ColumnaMuestra.FECHA, e.getMessage());
            return;
        }

        // Una hoja de cálculo entrega las fechas sin hora a medianoche, y una
        // muestra tomada «a las 00:00» es casi siempre una hora que nadie
        // escribió. Se completa con la de la fecha por omisión, conservando
        // SIEMPRE el día de la fila: mover el día para hacerlo caer en horario
        // cambiaría el dato que el archivo sí trae.
        //
        // NO es un aviso por fila. Una hoja de cálculo no trae hora en NINGUNA
        // de sus filas, así que marcarlas una a una pintaría de ámbar el archivo
        // entero y enterraría los avisos que sí señalan algo raro —un volumen
        // con un cero de más—. Se cuenta en el resumen y se dice una sola vez.
        if (f.fecha.getHour() == 0 && f.fecha.getMinute() == 0 && porOmision != null) {
            f.fecha = f.fecha.toLocalDate().atTime(porOmision.toLocalTime());
            f.horaPuesta = true;
        }
    }

    private void resolverNumeroAlicuota(FilaResuelta f, List<String> celdas, Emparejado emparejado) {
        String crudo = celda(celdas, emparejado, ColumnaMuestra.NUMERO_ALICUOTA);
        if (crudo.isEmpty()) {
            f.error(ColumnaMuestra.NUMERO_ALICUOTA, "La fila no trae el número de alícuota.");
            return;
        }
        int numero;
        try {
            numero = (int) Math.round(aNumero(crudo));
        } catch (ValorNoValidoException e) {
            f.error(ColumnaMuestra.NUMERO_ALICUOTA, "\"" + crudo + "\" no es un número de alícuota.");
            return;
        }
        if (numero < 1) {
            f.error(ColumnaMuestra.NUMERO_ALICUOTA, "El número de alícuota empieza en 1.");
            return;
        }
        f.numeroAlicuota = numero;

        if (f.tubo != null) {
            int configuradas = cupo(f.tubo);
            if (configuradas <= 0) {
                f.error(ColumnaMuestra.TUBO, "El tubo \"" + f.tubo.getNombre()
                        + "\" está configurado con 0 alícuotas, así que no puede recibir viales.");
            } else if (numero > configuradas) {
                f.error(ColumnaMuestra.NUMERO_ALICUOTA, "El tubo \"" + f.tubo.getNombre()
                        + "\" define " + configuradas + " alícuota(s) y esta fila pide la " + numero + ".");
            }
        }
    }

    private void resolverVolumen(FilaResuelta f, List<String> celdas, Emparejado emparejado) {
        String crudoVolumen = celda(celdas, emparejado, ColumnaMuestra.VOLUMEN);
        String crudoUnidad = celda(celdas, emparejado, ColumnaMuestra.UNIDAD);
        // «N/A», «ND» o un guion son formas de escribir que la celda no trae
        // dato, y aquí eso no es un error: significa lo mismo que dejarla vacía,
        // o sea heredar el nominal del tubo. En un estudio sí sería un error,
        // porque allí todos los parámetros son obligatorios.
        if (ConversorValor.esAusencia(crudoVolumen)) {
            crudoVolumen = "";
        }
        String unidadTubo = f.tubo != null && f.tubo.getUnidadVolumen() != null
                && !f.tubo.getUnidadVolumen().isBlank() ? f.tubo.getUnidadVolumen().trim() : null;

        // La unidad no se convierte nunca. El sistema no tiene factores de
        // conversión, así que aceptar una unidad distinta significaría guardar un
        // número que dice otra cosa de la que parece.
        if (crudoUnidad.isEmpty()) {
            f.unidad = unidadTubo;
        } else if (unidadTubo != null && !PlanificadorAlicuotas.mismaUnidad(crudoUnidad, unidadTubo)) {
            f.error(ColumnaMuestra.UNIDAD, "La fila dice \"" + crudoUnidad + "\" y el tubo está "
                    + "configurado en \"" + unidadTubo + "\". No se convierten unidades: corrija una de las dos.");
            f.unidad = crudoUnidad;
        } else {
            f.unidad = crudoUnidad;
        }

        if (crudoVolumen.isEmpty()) {
            // Celda vacía significa «el estándar de este tubo». Es la decisión
            // del cliente y por eso no es un error, pero sí se cuenta: guardar
            // cientos de viales con un volumen que nadie escribió merece verse.
            if (f.tubo == null) {
                return; // ya hay un error de tubo; no tiene sentido encadenar otro
            }
            if (f.tubo.getVolumenAlicuota() == null) {
                f.error(ColumnaMuestra.VOLUMEN, "La fila no trae volumen y el tubo \""
                        + f.tubo.getNombre() + "\" no tiene un volumen por alícuota configurado. "
                        + "Escriba el volumen, o configúrelo en el catálogo.");
                return;
            }
            f.volumen = f.tubo.getVolumenAlicuota();
            f.volumenHeredado = true;
            return;
        }

        try {
            f.volumen = aNumero(crudoVolumen);
        } catch (ValorNoValidoException e) {
            f.error(ColumnaMuestra.VOLUMEN, e.getMessage());
            return;
        }
        if (f.volumen < 0) {
            f.error(ColumnaMuestra.VOLUMEN, "El volumen no puede ser negativo.");
            return;
        }

        // Por encima del nominal solo se avisa. El vial ya existe con lo que
        // tenga dentro y el catálogo no puede desmentir un hecho consumado; lo
        // que sí puede es delatar un cero de más al teclear. Por debajo no se
        // dice nada: un vial con menos no es un faltante, es lo que salió.
        if (f.tubo != null && f.tubo.getVolumenAlicuota() != null
                && PlanificadorAlicuotas.mayorQue(f.volumen, f.tubo.getVolumenAlicuota())) {
            f.aviso(ColumnaMuestra.VOLUMEN, "El tubo \"" + f.tubo.getNombre() + "\" está configurado en "
                    + PlanificadorAlicuotas.fmt(f.tubo.getVolumenAlicuota())
                    + (f.unidad == null ? "" : " " + f.unidad) + " por alícuota y esta fila trae "
                    + PlanificadorAlicuotas.fmt(f.volumen) + ". Se guardará lo del archivo.");
        }
    }

    private void resolverPosicion(FilaResuelta f, List<String> celdas, Emparejado emparejado,
                                  Map<String, CajaCriogenica> cajas, Set<Long> huecosDelArchivo) {
        f.codigoCaja = celda(celdas, emparejado, ColumnaMuestra.CODIGO_CAJA);
        f.posicionTexto = celda(celdas, emparejado, ColumnaMuestra.POSICION);

        boolean hayCaja = !f.codigoCaja.isEmpty();
        boolean hayPosicion = !f.posicionTexto.isEmpty();

        if (!hayCaja && !hayPosicion) {
            return; // vial sin ubicar: es un estado válido y frecuente
        }
        if (hayCaja != hayPosicion) {
            ColumnaMuestra falta = hayCaja ? ColumnaMuestra.POSICION : ColumnaMuestra.CODIGO_CAJA;
            f.error(falta, "Para ubicar un vial hacen falta las dos cosas: el código de la caja y el "
                    + "hueco dentro de ella. Complete la que falta, o deje las dos vacías para "
                    + "cargarlo sin posición.");
            return;
        }

        CajaCriogenica caja = cajas.get(claveCaja(f.codigoCaja));
        if (caja == null) {
            f.error(ColumnaMuestra.CODIGO_CAJA, "No hay ninguna caja con el código \"" + f.codigoCaja
                    + "\" en el biobanco de esta institución. Déla de alta antes de cargar.");
            return;
        }
        if (!Boolean.TRUE.equals(caja.getActivo())) {
            f.error(ColumnaMuestra.CODIGO_CAJA, "La caja \"" + caja.getCodigoCaja()
                    + "\" está desactivada; no se pueden colocar muestras en ella.");
            return;
        }

        EtiquetaPosicionCaja.Coordenada coord = EtiquetaPosicionCaja.parsear(f.posicionTexto);
        if (coord == null) {
            f.error(ColumnaMuestra.POSICION, "\"" + f.posicionTexto + "\" no es un hueco. Se escribe "
                    + "con la fila en letra y la columna en número, como A1 o B7.");
            return;
        }
        if (coord.fila() > caja.getFilas() || coord.columna() > caja.getColumnas()) {
            f.error(ColumnaMuestra.POSICION, "La caja \"" + caja.getCodigoCaja() + "\" es de "
                    + caja.getFilas() + "x" + caja.getColumnas() + " y \"" + f.posicionTexto
                    + "\" queda fuera de la rejilla.");
            return;
        }

        Optional<PosicionCaja> hueco = posicionCajaRepository
                .findByCaja_IdAndFilaAndColumna(caja.getId(), coord.fila(), coord.columna());
        if (hueco.isEmpty()) {
            f.error(ColumnaMuestra.POSICION, "La caja \"" + caja.getCodigoCaja() + "\" no tiene "
                    + "generado el hueco " + f.posicionTexto + ".");
            return;
        }

        PosicionCaja posicion = hueco.get();
        if (Boolean.TRUE.equals(posicion.getOcupada())) {
            f.error(ColumnaMuestra.POSICION, "El hueco " + f.posicionTexto + " de la caja \""
                    + caja.getCodigoCaja() + "\" ya está ocupado.");
            return;
        }
        if (!huecosDelArchivo.add(posicion.getId())) {
            f.error(ColumnaMuestra.POSICION, "Otra fila de este mismo archivo ya reclama el hueco "
                    + f.posicionTexto + " de la caja \"" + caja.getCodigoCaja() + "\".");
            return;
        }

        f.posicion = posicion;
    }

    // ── Agrupación en lotes ──────────────────────────────────────────────────

    /**
     * Reparte los viales en lotes y le pone etiqueta a cada uno.
     *
     * <p>El lote es (participante, tipo, tubo, día), que es lo que define una
     * muestra padre. El día entra en la clave porque dos extracciones del mismo
     * tubo en fechas distintas son dos tubos distintos, aunque en el archivo del
     * cliente cada participante tenga una sola fecha.</p>
     *
     * <p>El número de lote arranca en el mayor que ya exista para ese folio y
     * prefijo, y sigue subiendo dentro del propio archivo: si dos tubos comparten
     * prefijo —o ninguno lo define y los dos caen en el de omisión—, sus lotes
     * tienen que numerarse distinto o sus etiquetas chocarían.</p>
     */
    private List<Lote> agrupar(List<FilaResuelta> filas, Long idInst) {
        Map<String, Lote> porClave = new LinkedHashMap<>();
        Map<String, Integer> ultimoLote = new HashMap<>();

        for (FilaResuelta f : filas) {
            if (f.paciente == null || f.tipo == null || f.tubo == null || f.fecha == null) {
                continue; // ya está marcada; agruparla solo produciría lotes fantasma
            }
            String clave = claveLote(f);
            f.claveLote = clave;

            Lote lote = porClave.get(clave);
            if (lote == null) {
                lote = new Lote();
                lote.clave = clave;
                lote.paciente = f.paciente;
                lote.tipo = f.tipo;
                lote.tubo = f.tubo;
                lote.fecha = f.fecha;

                String prefijo = EtiquetaMuestra.prefijo(f.tubo.getPrefijoCodigo());
                String claveNumeracion = f.paciente.getFolio() + "|" + prefijo;
                int siguiente = ultimoLote.computeIfAbsent(claveNumeracion,
                        k -> muestraRepository.findMaxLoteByFolioAndTuboPrefix(
                                f.paciente.getFolio(), prefijo)) + 1;
                ultimoLote.put(claveNumeracion, siguiente);

                lote.numeroLote = siguiente;
                lote.etiquetaPadre = EtiquetaMuestra.padre(
                        prefijo, f.paciente.getFolio(), idInst, siguiente);
                porClave.put(clave, lote);
            }

            lote.filas.add(f);
            f.etiqueta = EtiquetaMuestra.alicuota(
                    lote.etiquetaPadre, f.numeroAlicuota == null ? 0 : f.numeroAlicuota, cupo(f.tubo));
        }

        return new ArrayList<>(porClave.values());
    }

    /**
     * Comprueba que ninguna etiqueta calculada choque con una ya registrada.
     *
     * <p>En una sola consulta, no una por vial. El choque tiene que salir en la
     * revisión: si se dejara a {@code uk_muestra_etiqueta_institucion}, saltaría
     * a mitad de la escritura y se llevaría por delante la carga entera sin decir
     * cuál era el vial culpable.</p>
     */
    private void verificarEtiquetas(List<FilaResuelta> filas, List<Lote> lotes, Long idInst) {
        List<String> candidatas = new ArrayList<>();
        for (Lote l : lotes) {
            candidatas.add(l.etiquetaPadre.toUpperCase(Locale.ROOT));
        }
        for (FilaResuelta f : filas) {
            if (f.etiqueta != null) {
                candidatas.add(f.etiqueta.toUpperCase(Locale.ROOT));
            }
        }
        if (candidatas.isEmpty()) {
            return;
        }

        Set<String> existentes = new HashSet<>();
        for (String e : muestraRepository.findEtiquetasExistentes(candidatas, idInst)) {
            existentes.add(e.toUpperCase(Locale.ROOT));
        }
        if (existentes.isEmpty()) {
            return;
        }

        for (FilaResuelta f : filas) {
            if (f.etiqueta != null && existentes.contains(f.etiqueta.toUpperCase(Locale.ROOT))) {
                f.error(null, "Ya existe una muestra con la etiqueta \"" + f.etiqueta
                        + "\". Puede que este lote ya se haya cargado antes.");
            }
        }
        for (Lote l : lotes) {
            if (existentes.contains(l.etiquetaPadre.toUpperCase(Locale.ROOT))) {
                for (FilaResuelta f : l.filas) {
                    f.error(null, "Ya existe una muestra padre con la etiqueta \"" + l.etiquetaPadre
                            + "\". Puede que este lote ya se haya cargado antes.");
                }
            }
        }
    }

    // ── Escritura ────────────────────────────────────────────────────────────

    private ResultadoCargaMuestras escribir(Analisis analisis) {
        Institucion miInstitucion = institucionContextService.getInstitucionActual();
        BeanUser usuario = institucionContextService.getUsuarioActual();
        Timestamp ahora = Timestamp.valueOf(LocalDateTime.now());

        List<ResultadoCargaMuestras.Detalle> detalle = new ArrayList<>();
        int padres = 0, alicuotas = 0, ubicadas = 0, agotadas = 0;

        for (Lote lote : analisis.lotes()) {
            int configuradas = cupo(lote.tubo);

            // Lo que le queda a la padre en el cuerpo es lo que todavía no se ha
            // repartido: los viales sin ubicar. Los que llegan con hueco ya están
            // materializados y su volumen salió del tubo hace tiempo.
            double pendiente = 0.0;
            double repartido = 0.0;
            for (FilaResuelta f : lote.filas) {
                double v = f.volumen == null ? 0.0 : f.volumen;
                repartido = PlanificadorAlicuotas.sumar(repartido, v);
                if (f.posicion == null) {
                    pendiente = PlanificadorAlicuotas.sumar(pendiente, v);
                }
            }

            Muestra padre = new Muestra();
            padre.setEtiqueta(lote.etiquetaPadre);
            padre.setPaciente(lote.paciente);
            padre.setUsuarioRecolecta(usuario);
            padre.setInstitucion(miInstitucion);
            padre.setInstitucionActual(miInstitucion);
            padre.setTipoMuestra(lote.tipo);
            padre.setTuboMuestra(lote.tubo);
            padre.setNumeroLote(lote.numeroLote);
            padre.setFechaRecoleccion(lote.fecha);
            padre.setFechaRegistro(ahora);
            padre.setUnidad(unidadDelLote(lote));
            padre.setValor(pendiente);
            padre.setValorComprometido(pendiente);
            padre.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
            // Sin volumen pendiente el tubo se repartió entero: eso es
            // agotamiento, no baja. La baja es una decisión —contaminación,
            // pérdida, retiro de consentimiento— y mezclarlas haría imposible
            // saber cuántas muestras se echaron a perder.
            boolean quedaAgotada = PlanificadorAlicuotas.agotado(pendiente);
            if (quedaAgotada) {
                padre.setFechaAgotamiento(ahora);
                agotadas++;
            }
            padre = muestraRepository.save(padre);
            padres++;

            registrarTipoTubo(padre, lote, miInstitucion, ahora);

            historialService.registrarEvento(padre, usuario, TipoEventoMuestra.REGISTRO,
                    null, PlanificadorAlicuotas.fmt(repartido) + sufijoUnidad(padre.getUnidad()),
                    "Carga masiva: " + lote.filas.size() + " vial(es) del tubo «"
                            + lote.tubo.getNombre() + "»" + (quedaAgotada ? ", repartido por completo" : ""),
                    null);
            if (quedaAgotada) {
                historialService.registrarEvento(padre, usuario, TipoEventoMuestra.MUESTRA_AGOTADA,
                        null, "0" + sufijoUnidad(padre.getUnidad()),
                        "Se repartió entera al cargarla. No es una baja: conserva su historial y "
                                + "sigue siendo el origen de sus alícuotas.", null);
            }

            for (FilaResuelta f : lote.filas) {
                Muestra alicuota = new Muestra();
                alicuota.setEtiqueta(f.etiqueta);
                alicuota.setPaciente(lote.paciente);
                alicuota.setUsuarioRecolecta(usuario);
                alicuota.setInstitucion(miInstitucion);
                alicuota.setInstitucionActual(miInstitucion);
                alicuota.setTipoMuestra(lote.tipo);
                alicuota.setTuboMuestra(lote.tubo);
                alicuota.setMuestraPadre(padre);
                alicuota.setNumeroAlicuota(f.numeroAlicuota);
                // El denominador de la etiqueta es el número CONFIGURADO del
                // tubo, no el tamaño de esta tanda: el lote no se cierra al
                // crearse y un «1-1» impreso impediría completarlo después.
                alicuota.setTotalAlicuotas(configuradas);
                alicuota.setNumeroLote(lote.numeroLote);
                alicuota.setFechaRecoleccion(f.fecha);
                alicuota.setFechaRegistro(ahora);
                alicuota.setValor(f.volumen);
                alicuota.setUnidad(f.unidad);
                alicuota.setValorComprometido(0.0);
                alicuota.setObservaciones(null);

                String donde = null;
                if (f.posicion != null) {
                    f.posicion.setOcupada(true);
                    posicionCajaRepository.save(f.posicion);
                    alicuota.setPosicionCaja(f.posicion);
                    alicuota.setEstadoMuestra(EstadoMuestra.EN_BIOBANCO);
                    // Ocupar un hueco es la prueba de que el vial existe de
                    // verdad, y eso no se puede despipetear: la marca se sella
                    // una sola vez y con ella el descuento ya está hecho.
                    alicuota.setFechaMaterializacion(ahora);
                    alicuota.setCantidadDescontadaPadre(f.volumen);
                    donde = f.codigoCaja + " " + f.posicionTexto;
                    ubicadas++;
                } else {
                    alicuota.setEstadoMuestra(EstadoMuestra.SIN_POSICION);
                }

                alicuota = muestraRepository.save(alicuota);
                alicuotas++;

                historialService.registrarEvento(alicuota, usuario, TipoEventoMuestra.REGISTRO,
                        null, PlanificadorAlicuotas.fmt(f.volumen == null ? 0.0 : f.volumen)
                                + sufijoUnidad(f.unidad),
                        "Carga masiva desde archivo" + (donde != null ? ". Ubicada en " + donde : "")
                                + ". Origen: " + padre.getEtiqueta(), null);

                detalle.add(new ResultadoCargaMuestras.Detalle(
                        f.numeroDeFila, lote.paciente.getFolio(), alicuota.getEtiqueta(),
                        lote.tipo.getNombre(), lote.tubo.getNombre(), donde, alicuota.getId()));
            }
        }

        return new ResultadoCargaMuestras(padres, alicuotas, ubicadas, agotadas, detalle);
    }

    /**
     * Deja constancia de con qué tipo y tubo alicuotó esta institución la padre.
     *
     * <p>Se escribe con el repositorio y no con {@code MuestraTipoInstitucionService}
     * porque ese vuelve a buscar la muestra, el tipo y el tubo por id para
     * validarlos: son cuatro consultas por lote que aquí sobran, porque las tres
     * entidades acaban de resolverse y la padre se creó hace dos líneas.</p>
     */
    private void registrarTipoTubo(Muestra padre, Lote lote, Institucion institucion, Timestamp ahora) {
        MuestraTipoInstitucion mapping = new MuestraTipoInstitucion();
        mapping.setMuestra(padre);
        mapping.setInstitucion(institucion);
        mapping.setTipoMuestra(lote.tipo);
        mapping.setTuboMuestra(lote.tubo);
        mapping.setFechaRegistro(ahora);
        muestraTipoInstitucionRepository.save(mapping);
    }

    // ── Armado de la previsualización ────────────────────────────────────────

    private PrevisualizacionCargaMuestras armar(TablaLeida tabla, Emparejado emparejado,
                                                NormalizadorFecha.Interpretacion interpretacion,
                                                List<FilaResuelta> resueltas, List<Lote> lotes) {
        Map<String, Lote> porClave = new HashMap<>();
        for (Lote l : lotes) {
            porClave.put(l.clave, l);
        }

        List<FilaPrevisualizada> filas = new ArrayList<>(resueltas.size());
        int conProblemas = 0, conAvisos = 0, conPosicion = 0, sinPosicion = 0;
        int heredados = 0, sinHora = 0;

        for (FilaResuelta f : resueltas) {
            if (!f.errores.isEmpty()) conProblemas++;
            if (!f.avisos.isEmpty()) conAvisos++;
            if (f.posicion != null) conPosicion++; else sinPosicion++;
            if (f.volumenHeredado) heredados++;
            if (f.horaPuesta) sinHora++;

            filas.add(new FilaPrevisualizada(
                    f.numeroDeFila,
                    f.paciente != null ? f.paciente.getFolio() : null,
                    f.paciente != null ? f.paciente.getUuid() : null,
                    nombreCompleto(f.paciente),
                    f.tipo != null ? f.tipo.getNombre() : null,
                    f.tipo != null ? f.tipo.getId() : null,
                    f.tubo != null ? f.tubo.getNombre() : null,
                    f.tubo != null ? f.tubo.getId() : null,
                    f.fecha != null ? f.fecha.format(SALIDA) : null,
                    f.numeroAlicuota,
                    f.volumen,
                    f.unidad,
                    f.volumenHeredado,
                    f.codigoCaja,
                    f.posicionTexto,
                    f.posicion != null ? f.posicion.getId() : null,
                    f.etiqueta,
                    f.claveLote,
                    List.copyOf(f.errores),
                    List.copyOf(f.avisos)));
        }

        List<LotePrevisualizado> lotesPrevios = new ArrayList<>(lotes.size());
        for (Lote l : lotes) {
            int configuradas = cupo(l.tubo);
            double total = 0.0;
            boolean todosUbicados = true;
            for (FilaResuelta f : l.filas) {
                total = PlanificadorAlicuotas.sumar(total, f.volumen == null ? 0.0 : f.volumen);
                if (f.posicion == null) todosUbicados = false;
            }

            List<String> avisos = new ArrayList<>();
            if (l.filas.size() < configuradas) {
                // No es un error: un lote no se cierra al crearse, y lo normal es
                // hacer las que alcanzan y completarlo cuando aparece más volumen.
                avisos.add("El tubo define " + configuradas + " alícuota(s) y el archivo trae "
                        + l.filas.size() + ". El lote quedará incompleto, que es normal.");
            }
            if (!todosUbicados) {
                avisos.add("Algún vial llega sin hueco: su volumen queda reservado en la muestra padre "
                        + "y se descontará cuando se ubique.");
            }

            lotesPrevios.add(new LotePrevisualizado(
                    l.clave, l.paciente.getFolio(), nombreCompleto(l.paciente),
                    l.tipo.getNombre(), l.tubo.getNombre(),
                    l.fecha != null ? l.fecha.format(SALIDA) : null,
                    l.filas.size(), configuradas, total, unidadDelLote(l),
                    l.etiquetaPadre, todosUbicados, avisos));
        }

        Map<String, Integer> indices = new LinkedHashMap<>();
        emparejado.indices().forEach((campo, indice) -> indices.put(campo.name(), indice));

        return new PrevisualizacionCargaMuestras(
                List.of(),
                emparejado.ignoradas(),
                interpretacion != null ? interpretacion.orden().name() : null,
                interpretacion != null && interpretacion.ambiguo(),
                emparejado.trae(ColumnaMuestra.FECHA),
                tabla,
                indices,
                filas,
                lotesPrevios,
                new PrevisualizacionCargaMuestras.Resumen(
                        filas.size(), filas.size() - conProblemas, conProblemas, conAvisos,
                        lotesPrevios.size(), conPosicion, sinPosicion, heredados, sinHora,
                        emparejado.ignoradas().size()));
    }

    /**
     * Cuando la estructura no encaja no se enseñan las filas: lo útil es el
     * motivo, y una lista de errores derivados invita a pensar que casi funciona.
     */
    private PrevisualizacionCargaMuestras soloEstructura(TablaLeida tabla, Emparejado emparejado) {
        Map<String, Integer> indices = new LinkedHashMap<>();
        emparejado.indices().forEach((campo, indice) -> indices.put(campo.name(), indice));
        return new PrevisualizacionCargaMuestras(
                emparejado.problemas(), emparejado.ignoradas(), null, false,
                emparejado.trae(ColumnaMuestra.FECHA), tabla, indices,
                List.of(), List.of(),
                new PrevisualizacionCargaMuestras.Resumen(0, 0, 0, 0, 0, 0, 0, 0, 0,
                        emparejado.ignoradas().size()));
    }

    // ── Resolución contra la base ────────────────────────────────────────────

    /** El catálogo de la institución, indexado por nombre normalizado. */
    private record Catalogo(Map<String, TipoMuestra> tipos, Map<String, TuboMuestra> tubos) {

        TipoMuestra tipoPorNombre(String nombre) {
            String clave = NormalizadorAlias.normalizar(nombre);
            return clave == null ? null : tipos.get(clave);
        }

        TuboMuestra tuboPorNombre(TipoMuestra tipo, String nombre) {
            String clave = NormalizadorAlias.normalizar(nombre);
            return clave == null ? null : tubos.get(tipo.getId() + "|" + clave);
        }
    }

    /**
     * Carga el catálogo entero de una vez.
     *
     * <p>Un archivo de seiscientas filas nombra dos o tres tipos; buscarlos fila
     * a fila serían seiscientas consultas para leer siempre lo mismo.</p>
     */
    private Catalogo cargarCatalogo() {
        Map<String, TipoMuestra> tipos = new HashMap<>();
        Map<String, TuboMuestra> tubos = new HashMap<>();
        for (TipoMuestra tipo : tipoMuestraService.getAllActivos()) {
            tipos.put(NormalizadorAlias.normalizar(tipo.getNombre()), tipo);
            if (tipo.getTubos() == null) continue;
            for (TuboMuestra tubo : tipo.getTubos()) {
                if (!Boolean.TRUE.equals(tubo.getActivo())) continue;
                tubos.put(tipo.getId() + "|" + NormalizadorAlias.normalizar(tubo.getNombre()), tubo);
            }
        }
        return new Catalogo(tipos, tubos);
    }

    /**
     * Los participantes del archivo, en una consulta por folio distinto.
     *
     * <p>Se busca dentro de las instituciones alcanzables, igual que el resto del
     * sistema: un participante fuera de alcance tiene que comportarse como uno
     * que no existe, o la carga masiva sería una forma de averiguar el padrón
     * ajeno probando folios.</p>
     */
    private Map<String, Paciente> resolverParticipantes(TablaLeida tabla, int colFolio) {
        if (colFolio < 0) return Map.of();
        List<Long> alcanzables = accesoService.institucionesAlcanzables();
        Map<String, Paciente> encontrados = new HashMap<>();

        for (List<String> fila : tabla.filas()) {
            String folio = fila.get(colFolio).trim();
            if (folio.isEmpty()) continue;
            String clave = claveFolio(folio);
            if (encontrados.containsKey(clave)) continue;

            Optional<Paciente> p = pacienteRepository.findByFolioAndInstitucion_IdIn(folio, alcanzables);
            // El folio del sistema tiene seis dígitos, y Excel se come los ceros
            // a la izquierda en cuanto el archivo pasa por un CSV: "1103" en la
            // hoja es el folio "001103". Se reintenta antes de darlo por perdido.
            if (p.isEmpty() && folio.matches("\\d{1,5}")) {
                p = pacienteRepository.findByFolioAndInstitucion_IdIn(
                        String.format("%06d", Integer.parseInt(folio)), alcanzables);
            }
            p.ifPresent(value -> encontrados.put(clave, value));
        }
        return encontrados;
    }

    /** Las cajas nombradas por el archivo, una consulta por código distinto. */
    private Map<String, CajaCriogenica> resolverCajas(TablaLeida tabla, Emparejado emparejado, Long idInst) {
        int col = emparejado.indiceDe(ColumnaMuestra.CODIGO_CAJA);
        if (col < 0) return Map.of();

        Map<String, CajaCriogenica> encontradas = new HashMap<>();
        Set<String> buscadas = new HashSet<>();
        for (List<String> fila : tabla.filas()) {
            String codigo = fila.get(col).trim();
            if (codigo.isEmpty() || !buscadas.add(claveCaja(codigo))) continue;
            cajaRepository.findByCodigoCajaAndInstitucion_Id(codigo, idInst)
                    .ifPresent(c -> encontradas.put(claveCaja(codigo), c));
        }
        return encontradas;
    }

    /**
     * Cómo leer las fechas de este archivo, y si de verdad había algo que elegir.
     *
     * <p>{@code ambiguo} por sí solo dice «no encontré desempate», y eso también
     * pasa cuando no hay ninguna fecha que desempatar —todas en ISO, o con el mes
     * en letra—. Avisar ahí de que «las fechas admiten dos lecturas» sería falso,
     * así que se exige además que alguna lo admita de verdad.</p>
     */
    private NormalizadorFecha.Interpretacion interpretarFechas(TablaLeida tabla, Emparejado emparejado) {
        int col = emparejado.indiceDe(ColumnaMuestra.FECHA);
        if (col < 0) return null;
        List<String> crudas = tabla.filas().stream().map(f -> f.get(col)).toList();
        NormalizadorFecha.Interpretacion i = NormalizadorFecha.inferirOrden(crudas);
        boolean ambiguaDeVerdad = i.ambiguo() && NormalizadorFecha.admiteDosLecturas(crudas);
        return new NormalizadorFecha.Interpretacion(i.orden(), ambiguaDeVerdad);
    }

    // ── Utilidades ───────────────────────────────────────────────────────────

    private static String celda(List<String> celdas, Emparejado emparejado, ColumnaMuestra campo) {
        int i = emparejado.indiceDe(campo);
        if (i < 0 || i >= celdas.size()) return "";
        String v = celdas.get(i);
        return v == null ? "" : v.trim();
    }

    /**
     * El mismo lector de números que usa la carga de estudios.
     *
     * <p>Se reutiliza en vez de escribir otro para que «300», «300,0» y «300 µL»
     * signifiquen lo mismo en las dos cargas. Un segundo lector acabaría
     * aceptando cosas distintas, y el usuario no tendría cómo saber cuál aplica.</p>
     */
    private static double aNumero(String crudo) {
        return ConversorValor.convertir(crudo, TipoParametro.NUMERICO, null).numerico();
    }

    private static int cupo(TuboMuestra tubo) {
        return tubo != null && tubo.getNumeroAlicuotas() != null ? tubo.getNumeroAlicuotas() : 0;
    }

    private static String unidadDelLote(Lote lote) {
        if (lote.tubo.getUnidadVolumen() != null && !lote.tubo.getUnidadVolumen().isBlank()) {
            return lote.tubo.getUnidadVolumen();
        }
        return lote.filas.stream().map(f -> f.unidad)
                .filter(u -> u != null && !u.isBlank()).findFirst().orElse(null);
    }

    private static String sufijoUnidad(String unidad) {
        return unidad == null || unidad.isBlank() ? "" : " " + unidad;
    }

    private static String claveLote(FilaResuelta f) {
        return f.paciente.getId() + "|" + f.tipo.getId() + "|" + f.tubo.getId()
                + "|" + f.fecha.toLocalDate();
    }

    private static String claveFolio(String folio) {
        return folio.trim().toUpperCase(Locale.ROOT);
    }

    private static String claveCaja(String codigo) {
        return codigo.trim().toUpperCase(Locale.ROOT);
    }

    private static String nombreCompleto(Paciente p) {
        if (p == null || p.getPersona() == null) return null;
        var per = p.getPersona();
        return (per.getNombre()
                + (per.getSegundoNombre() != null ? " " + per.getSegundoNombre() : "")
                + " " + per.getApellidoPaterno()
                + (per.getApellidoMaterno() != null ? " " + per.getApellidoMaterno() : "")).trim();
    }

    /**
     * La fecha que la pantalla eligió para las filas que no traen la suya.
     *
     * <p>Admite el día solo por comodidad, pero lo normal es que llegue con hora:
     * la pantalla la calcula con el horario configurado de la institución, que es
     * donde vive esa regla. Resolverla aquí obligaría a tener una segunda copia
     * del horario en Java y las dos acabarían diciendo cosas distintas.</p>
     */
    private static LocalDateTime parsearFechaPorOmision(String texto) {
        if (texto == null || texto.isBlank()) return null;
        String limpio = texto.trim();
        try {
            return LocalDateTime.parse(limpio);
        } catch (DateTimeParseException ignorada) {
            // sigue
        }
        try {
            return LocalDate.parse(limpio).atStartOfDay();
        } catch (DateTimeParseException e) {
            throw new ArchivoInvalidoException(
                    "No se entiende \"" + texto + "\" como fecha para las filas sin fecha.");
        }
    }
}
