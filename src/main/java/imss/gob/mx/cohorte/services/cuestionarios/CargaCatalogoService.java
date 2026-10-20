package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.PresentacionSeccion;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacion;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.ReglaValidacionRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.Severidad;
import imss.gob.mx.cohorte.modules.cuestionarios.validaciones.TipoRegla;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.*;
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
 * Carga el catálogo de un instrumento desde el libro que mantienen los
 * investigadores.
 *
 * <h3>Por qué esto existe</h3>
 *
 * <p>El instrumento principal tiene más de dos mil preguntas y más de doce mil
 * opciones. Capturarlas en una pantalla no es «trabajoso»: es inviable, y un catálogo
 * teclead a mano es precisamente de donde salieron los errores que hubo que cotejar
 * contra el papel. El catálogo vive en un libro que los investigadores mantienen, y la
 * forma correcta de que entre al sistema es leerlo.</p>
 *
 * <h3>Las dos hojas</h3>
 *
 * <ul>
 *   <li><b>Variables</b>: una fila por pregunta, con su tipo, la columna que le
 *       corresponde en el volcado, el bloque y la página.</li>
 *   <li><b>Catalogo</b>: una fila por opción —código y etiqueta—, que es el formato en
 *       el que los investigadores lo redactaron.</li>
 * </ul>
 *
 * <p>Hacen falta las dos y no se pueden deducir una de la otra: la hoja de opciones no
 * dice de qué tipo es la pregunta ni qué columna le toca, y la de variables no trae los
 * códigos, que son con los que se piden los datos.</p>
 *
 * <h3>Lo que no hace</h3>
 *
 * <p>No borra. Una pregunta que el archivo ya no trae se informa y se deja; podría ser
 * que se retiró del instrumento, pero también que la hoja se exportó incompleta, y las
 * dos cosas se ven igual desde aquí. Retirarla de uso es una decisión que se toma en el
 * catálogo, una por una, sabiendo lo que se hace.</p>
 */
@Service
@AllArgsConstructor
@Slf4j
public class CargaCatalogoService {

    private static final String HOJA_VARIABLES = "VARIABLES";

    /**
     * Hoja opcional con las comprobaciones del instrumento. Opcional a proposito: un
     * catalogo sin reglas sigue siendo un catalogo valido, y obligar a traerlas
     * impediria cargar lo que ya existe.
     */
    private static final String HOJA_VALIDACIONES = "VALIDACIONES";
    private static final String HOJA_OPCIONES = "CATALOGO";

    // Encabezados de la hoja de variables.
    private static final String V_CUESTIONARIO = "CUESTIONARIO";
    private static final String V_CAMPO = "CAMPO";
    private static final String V_COLUMNA = "COLUMNA EN EL VOLCADO";
    private static final String V_PREGUNTA = "PREGUNTA";
    private static final String V_TIPO = "TIPO";
    private static final String V_MULTIPLE = "RESPUESTA MULTIPLE";
    private static final String V_RANGO = "RANGO";
    private static final String V_BLOQUE = "BLOQUE";
    private static final String V_PAGINA = "PAGINA";

    // Opcionales. El catalogo que mantienen los investigadores no trae estas tres
    // todavia —los saltos del papel («si contesto no, pase a la 25») estan impresos,
    // no tabulados— y hasta que las traiga se configuran pregunta por pregunta. Se
    // leen si aparecen para que el dia que las agreguen no haya que tocar nada: sin
    // la pregunta llave, el sistema no puede distinguir «no contesto» de «no se le
    // pregunto», que es lo que hace util el conteo de completitud.
    private static final String V_DEPENDE = "DEPENDE DE";
    private static final String V_HABILITANTE = "VALOR HABILITANTE";
    private static final String V_OBLIGATORIA = "OBLIGATORIA";

    /**
     * Cómo se compara la llave: {@code IGUAL} (lo de siempre, y lo que se asume si la
     * columna no viene) o {@code DISTINTO}, que es la casilla de descarte —el
     * «Ninguna» de la 112—.
     */
    private static final String V_OPERADOR = "OPERADOR";

    /**
     * Lo que el papel imprime junto a la pregunta y lo que hay que seguir
     * enseñándole a quien la contesta cuando ya no haya papel: «considerando horario
     * de 24 hrs», «5 años equivalen a 60 meses».
     */
    private static final String V_INSTRUCCION = "INSTRUCCION";

    /** Ruta de la figura sin la cual la pregunta no significa nada. */
    private static final String V_FIGURA = "FIGURA";

    /** Título y texto de entrada del bloque, tal como los imprime el papel. */
    private static final String V_TITULO_BLOQUE = "TITULO DEL BLOQUE";
    private static final String V_TEXTO_BLOQUE = "TEXTO DEL BLOQUE";


    // Encabezados de la hoja de opciones.
    private static final String O_CODIGO = "CODIGO";
    private static final String O_VALOR = "VALOR";

    private final LectorArchivoTabular lector;
    private final TipoCuestionarioRepositorios repos;

    /**
     * Lee la hoja de comprobaciones, si viene.
     *
     * <p>Una regla mal escrita se salta con un aviso en la bitacora en lugar de
     * tumbar la carga: el catalogo es el dato principal y perderlo por una fila de
     * una hoja auxiliar seria desproporcionado.</p>
     */
    private List<ReglaValidacion> leerReglas(List<LectorArchivoTabular.HojaLeida> hojas,
                                             String nombreCatalogo) {
        TablaLeida tabla = null;
        for (LectorArchivoTabular.HojaLeida h : hojas) {
            if (HOJA_VALIDACIONES.equals(NormalizadorAlias.normalizar(h.nombre()))) {
                tabla = h.tabla();
            }
        }
        if (tabla == null) return null;

        int iCuestionario = indice(tabla, V_CUESTIONARIO);
        int iRegla = indice(tabla, "REGLA");
        int iPregunta = indice(tabla, "PREGUNTA");
        int iValor = indice(tabla, "VALOR");
        int iCompara = indice(tabla, "COMPARA CON");
        int iSeveridad = indice(tabla, "SEVERIDAD");
        int iMensaje = indice(tabla, "MENSAJE");
        int iOrigen = indice(tabla, "DE DONDE SALE");
        if (iRegla < 0 || iPregunta < 0) {
            log.warn("La hoja de validaciones no trae las columnas esperadas; se omite.");
            return null;
        }

        List<ReglaValidacion> reglas = new ArrayList<>();
        for (List<String> f : tabla.filas()) {
            if (iCuestionario >= 0 && !valor(f, iCuestionario).isEmpty()
                    && !nombreCatalogo.equals(valor(f, iCuestionario))) {
                continue;
            }
            String tipo = NormalizadorAlias.normalizar(valor(f, iRegla));
            if (tipo == null) continue;
            try {
                ReglaValidacion r = new ReglaValidacion();
                r.setRegla(TipoRegla.valueOf(tipo));
                r.setPregunta(valor(f, iPregunta));
                r.setValor(iValor < 0 ? null : nullSiVacio(valor(f, iValor)));
                r.setComparaCon(iCompara < 0 ? null : nullSiVacio(valor(f, iCompara)));
                r.setSeveridad("ERROR".equalsIgnoreCase(valor(f, iSeveridad))
                        ? Severidad.ERROR : Severidad.REVISION);
                r.setMensaje(iMensaje < 0 || valor(f, iMensaje).isEmpty()
                        ? "Hay que revisar esta respuesta." : limpiar(valor(f, iMensaje)));
                r.setOrigen(iOrigen < 0 ? null : limpiar(valor(f, iOrigen)));
                if (r.getPregunta().isEmpty()) continue;
                reglas.add(r);
            } catch (IllegalArgumentException noConocida) {
                log.warn("Regla \"{}\" desconocida en la hoja de validaciones; se omite.", tipo);
            }
        }
        return reglas;
    }

    /**
     * Los repositorios que hacen falta, en un solo componente.
     *
     * <p>Son cinco y van siempre juntos; pedirlos uno a uno en el constructor haría la
     * firma más larga que el método que los usa.</p>
     */
    @org.springframework.stereotype.Component
    @AllArgsConstructor
    @lombok.Getter
    public static class TipoCuestionarioRepositorios {
        private final VariableCuestionarioRepository variables;
        private final OpcionVariableRepository opciones;
        private final SeccionCuestionarioRepository secciones;
        private final RespuestaCuestionarioRepository respuestas;
        private final ReglaValidacionRepository reglas;
        private final TipoCuestionarioService tipos;
    }

    private static LectorArchivoTabular.Topes topes() {
        return new LectorArchivoTabular.Topes(
                20_000, LimitesArchivo.MAX_COLUMNAS);
    }

    // ── Paso 1: previsualizar ────────────────────────────────────────────────

    public PrevisualizacionCatalogo previsualizar(MultipartFile archivo, Long idTipo,
                                                 String nombreCatalogo) {
        Plan plan = planear(archivo, idTipo, nombreCatalogo);

        int opciones = plan.entradas.stream().mapToInt(e -> e.opciones.size()).sum();

        return new PrevisualizacionCatalogo(
                plan.nombreCatalogo,
                (int) plan.entradas.stream().filter(e -> e.estado == Estado.NUEVA).count(),
                (int) plan.entradas.stream().filter(e -> e.estado == Estado.ACTUALIZA).count(),
                (int) plan.entradas.stream().filter(e -> e.estado == Estado.IGUAL).count(),
                plan.sobran, plan.seccionesNuevas, opciones, plan.choques);
    }

    // ── Paso 2: confirmar ────────────────────────────────────────────────────

    @Transactional
    public PrevisualizacionCatalogo confirmar(MultipartFile archivo, Long idTipo,
                                              String nombreCatalogo) {
        Plan plan = planear(archivo, idTipo, nombreCatalogo);

        if (plan.tieneBloqueos()) {
            throw new ArchivoInvalidoException(
                    "El archivo cambia el significado de respuestas ya guardadas en "
                            + plan.choques.stream().filter(PrevisualizacionCatalogo.Choque::bloquea).count()
                            + " pregunta(s). Revisa la previsualizacion: hay que corregirlo antes de cargar.");
        }

        // ── Bloques ────────────────────────────────────────────────────────────
        Map<String, SeccionCuestionario> secciones = new LinkedHashMap<>();
        for (SeccionCuestionario s : repos.getSecciones()
                .findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(plan.tipo.getId())) {
            secciones.put(clave(s.getNombre()), s);
        }

        int ordenSeccion = secciones.size();
        for (EntradaPlan e : plan.entradas) {
            if (e.bloque == null) continue;
            String clave = clave(e.bloque);
            if (secciones.containsKey(clave)) continue;
            SeccionCuestionario s = new SeccionCuestionario();
            s.setTipoCuestionario(plan.tipo);
            s.setNombre(e.bloque);
            s.setTitulo(tituloDe(plan, clave));
            s.setDescripcion(textoDe(plan, clave));
            s.setPagina(e.pagina);
            s.setOrden(ordenSeccion++);
            s.setPresentacion(presentacionDe(plan, clave));
            secciones.put(clave, repos.getSecciones().save(s));
        }

        // Un bloque que ya existia solo puede SUBIR de lista a tabla, nunca bajar.
        // La deteccion es conservadora a proposito y se le escapan tablas; si al
        // recargar el catalogo bajara a lista todo lo que no detecta, borraria sin
        // aviso la correccion que alguien hizo a mano desde la pantalla.
        for (String nombre : secciones.keySet()) {
            SeccionCuestionario s = secciones.get(nombre);
            if (s == null) continue;
            boolean cambio = false;

            PresentacionSeccion propuesta = presentacionDe(plan, nombre);
            if (propuesta != PresentacionSeccion.LISTA
                    && s.getPresentacion() == PresentacionSeccion.LISTA) {
                s.setPresentacion(propuesta);
                cambio = true;
            }
            // El titulo y el texto si se sobreescriben cuando el archivo los trae: son
            // el enunciado del papel, y el archivo es la fuente. Lo que no se hace es
            // borrarlos cuando el archivo viene sin ellos.
            String titulo = tituloDe(plan, nombre);
            if (titulo != null && !titulo.equals(s.getTitulo())) {
                s.setTitulo(titulo);
                cambio = true;
            }
            String texto = textoDe(plan, nombre);
            if (texto != null && !texto.equals(s.getDescripcion())) {
                s.setDescripcion(texto);
                cambio = true;
            }
            if (cambio) repos.getSecciones().save(s);
        }

        // ── Reglas ──────────────────────────────────────────────────────
        //
        // Se reemplazan enteras, no se mezclan: una regla que el archivo ya no trae
        // es una regla que alguien quito, y dejarla viva seguiria marcando respuestas
        // buenas sin que nadie entienda por que.
        if (plan.reglas != null) {
            repos.getReglas().deleteAllByTipoCuestionario_Id(plan.tipo.getId());
            for (ReglaValidacion r : plan.reglas) r.setTipoCuestionario(plan.tipo);
            repos.getReglas().saveAll(plan.reglas);
        }

        // ── Preguntas ──────────────────────────────────────────────────────────
        // Lo que no cambio no se vuelve a escribir. Reescribir las dos mil preguntas y
        // sus siete mil opciones en cada recarga tardaba mas de lo que el navegador
        // espera, y no cambiaba nada: el archivo y la base ya decian lo mismo. Lo que
        // hace seguro saltarselas es que {@link #cambia} compare TODO lo que se escribe
        // aqui —incluidos el orden, la pagina y la rejilla—; si algun dia se guarda un
        // campo mas, tiene que entrar tambien en esa comparacion.
        int guardadas = 0;
        int intactas = 0;
        for (EntradaPlan e : plan.entradas) {
            if (e.estado == Estado.IGUAL) {
                intactas++;
                continue;
            }
            VariableCuestionario v = e.existente == null ? new VariableCuestionario() : e.existente;
            v.setTipoCuestionario(plan.tipo);
            v.setCodigo(e.codigo);
            v.setColumnaVolcado(e.columna);
            v.setPregunta(e.pregunta);
            v.setTipo(e.tipo);
            v.setPagina(e.pagina);
            v.setGrupoMultiple(e.grupoMultiple);
            v.setDependeDe(e.dependeDe);
            v.setValorHabilitante(e.valorHabilitante);
            v.setOperadorDependencia(e.operador);
            v.setObligatoria(e.obligatoria);
            v.setFilaMatriz(e.filaMatriz);
            v.setColumnaMatriz(e.columnaMatriz);
            v.setInstruccion(e.instruccion);
            v.setFigura(e.figura);
            v.setValorMin(e.min);
            v.setValorMax(e.max);
            v.setOrden(e.orden);
            v.setDocumentada(e.pregunta != null && !e.pregunta.isBlank());
            if (e.bloque != null) v.setSeccion(secciones.get(clave(e.bloque)));
            if (v.getOpciones() == null) v.setOpciones(new ArrayList<>());

            v = repos.getVariables().save(v);
            aplicarOpciones(v, e.opciones);
            repos.getVariables().save(v);
            guardadas++;
        }

        log.info("Catalogo \"{}\" cargado en {}: {} pregunta(s) escrita(s), {} sin cambios, "
                        + "{} bloque(s)",
                plan.nombreCatalogo, plan.tipo.getNombre(), guardadas, intactas, secciones.size());

        return previsualizarDesde(plan);
    }

    /**
     * Deja el juego de opciones igual al del archivo, conservando las filas de los
     * códigos que siguen existiendo.
     *
     * <p>Mismo criterio que al editar a mano: el código es el dato, así que una opción
     * que se mantiene mantiene su fila y solo se le corrige el texto y el orden.</p>
     */
    private void aplicarOpciones(VariableCuestionario variable, List<OpcionEntrante> entrantes) {
        List<OpcionVariable> actuales = variable.getOpciones();
        List<String> codigos = entrantes.stream().map(o -> o.codigo).toList();
        actuales.removeIf(o -> !codigos.contains(o.getCodigo()));

        for (int i = 0; i < entrantes.size(); i++) {
            OpcionEntrante entrante = entrantes.get(i);
            OpcionVariable existente = actuales.stream()
                    .filter(o -> o.getCodigo().equals(entrante.codigo))
                    .findFirst().orElse(null);
            if (existente == null) {
                OpcionVariable nueva = new OpcionVariable();
                nueva.setVariable(variable);
                nueva.setCodigo(entrante.codigo);
                nueva.setEtiqueta(entrante.etiqueta);
                nueva.setOrden(i);
                actuales.add(nueva);
            } else {
                existente.setEtiqueta(entrante.etiqueta);
                existente.setOrden(i);
            }
        }
    }

    // ── Planeacion ───────────────────────────────────────────────────────────

    private enum Estado { NUEVA, ACTUALIZA, IGUAL }

    private record OpcionEntrante(String codigo, String etiqueta) {}

    /**
     * Lo que el archivo dice de una pregunta.
     *
     * <p>Es visible en el paquete para que la prueba de {@link #cambia} pueda armar una
     * entrada: esa comparación es una lista escrita a mano de catorce campos y ya se
     * quedó corta una vez —le faltaba el bloque—, así que tiene que poder probarse
     * sin levantar media aplicación.</p>
     */
    static final class EntradaPlan {
        String codigo, columna, pregunta, bloque, grupoMultiple;
        String dependeDe, valorHabilitante;
        OperadorDependencia operador = OperadorDependencia.IGUAL;
        String filaMatriz, columnaMatriz;
        int orden;
        String instruccion, figura, tituloBloque, textoBloque;
        boolean obligatoria;
        TipoVariable tipo;
        Integer pagina;
        Double min, max;
        List<OpcionEntrante> opciones = new ArrayList<>();
        VariableCuestionario existente;
        Estado estado;
    }

    private static final class Plan {
        TipoCuestionario tipo;
        String nombreCatalogo;
        List<EntradaPlan> entradas = new ArrayList<>();
        List<String> sobran = new ArrayList<>();
        List<String> seccionesNuevas = new ArrayList<>();
        List<PrevisualizacionCatalogo.Choque> choques = new ArrayList<>();
        Set<String> bloquesEnRejilla = new LinkedHashSet<>();
        Set<String> bloquesEnEscala = new LinkedHashSet<>();
        List<ReglaValidacion> reglas;

        boolean tieneBloqueos() {
            return choques.stream().anyMatch(PrevisualizacionCatalogo.Choque::bloquea);
        }
    }

    private PrevisualizacionCatalogo previsualizarDesde(Plan plan) {
        return new PrevisualizacionCatalogo(plan.nombreCatalogo,
                (int) plan.entradas.stream().filter(e -> e.estado == Estado.NUEVA).count(),
                (int) plan.entradas.stream().filter(e -> e.estado == Estado.ACTUALIZA).count(),
                (int) plan.entradas.stream().filter(e -> e.estado == Estado.IGUAL).count(),
                plan.sobran, plan.seccionesNuevas,
                plan.entradas.stream().mapToInt(e -> e.opciones.size()).sum(),
                plan.choques);
    }

    private Plan planear(MultipartFile archivo, Long idTipo, String nombreCatalogo) {
        List<LectorArchivoTabular.HojaLeida> hojas = lector.leerHojas(archivo, topes());
        TablaLeida tablaVariables = hojaLlamada(hojas, HOJA_VARIABLES);
        TablaLeida tablaOpciones = hojaLlamada(hojas, HOJA_OPCIONES);

        Plan plan = new Plan();
        plan.tipo = repos.getTipos().uno(idTipo);

        int iCuestionario = indice(tablaVariables, V_CUESTIONARIO);
        int iCampo = exigir(tablaVariables, V_CAMPO, HOJA_VARIABLES);
        int iColumna = indice(tablaVariables, V_COLUMNA);
        int iPregunta = indice(tablaVariables, V_PREGUNTA);
        int iTipo = exigir(tablaVariables, V_TIPO, HOJA_VARIABLES);
        int iMultiple = indice(tablaVariables, V_MULTIPLE);
        int iRango = indice(tablaVariables, V_RANGO);
        int iBloque = indice(tablaVariables, V_BLOQUE);
        int iPagina = indice(tablaVariables, V_PAGINA);
        int iDepende = indice(tablaVariables, V_DEPENDE);
        int iHabilitante = indice(tablaVariables, V_HABILITANTE);
        int iObligatoria = indice(tablaVariables, V_OBLIGATORIA);
        int iOperador = indice(tablaVariables, V_OPERADOR);
        int iInstruccion = indice(tablaVariables, V_INSTRUCCION);
        int iFigura = indice(tablaVariables, V_FIGURA);
        int iTituloBloque = indice(tablaVariables, V_TITULO_BLOQUE);
        int iTextoBloque = indice(tablaVariables, V_TEXTO_BLOQUE);

        plan.nombreCatalogo = resolverCatalogo(tablaVariables, iCuestionario, nombreCatalogo);

        // ── Opciones, agrupadas por pregunta ───────────────────────────────────
        Map<String, List<OpcionEntrante>> opcionesPorCampo =
                leerOpciones(tablaOpciones, plan.nombreCatalogo);

        // ── Lo que ya hay ──────────────────────────────────────────────────────
        Map<String, VariableCuestionario> existentes = new HashMap<>();
        for (VariableCuestionario v : repos.getVariables().findTodasConOpciones(plan.tipo.getId())) {
            existentes.put(clave(v.getCodigo()), v);
        }
        Set<String> vistas = new HashSet<>();
        Set<String> bloquesExistentes = new HashSet<>();
        for (SeccionCuestionario s : repos.getSecciones()
                .findAllByTipoCuestionario_IdOrderByOrdenAscIdAsc(plan.tipo.getId())) {
            bloquesExistentes.add(clave(s.getNombre()));
        }

        // ── Una entrada por pregunta ───────────────────────────────────────────
        for (List<String> f : tablaVariables.filas()) {
            if (iCuestionario >= 0 && !plan.nombreCatalogo.equals(valor(f, iCuestionario))) continue;

            String codigo = valor(f, iCampo);
            if (codigo.isEmpty()) continue;

            EntradaPlan e = new EntradaPlan();
            e.codigo = codigo;
            e.columna = iColumna >= 0 && !valor(f, iColumna).isEmpty() ? valor(f, iColumna) : codigo;
            e.pregunta = iPregunta < 0 ? null : limpiar(valor(f, iPregunta));
            e.tipo = tipoDe(valor(f, iTipo), codigo);
            e.bloque = iBloque < 0 ? null : nullSiVacio(valor(f, iBloque));
            e.pagina = iPagina < 0 ? null : aEntero(valor(f, iPagina));
            aplicarRango(e, iRango < 0 ? "" : valor(f, iRango));
            e.dependeDe = iDepende < 0 ? null : nullSiVacio(valor(f, iDepende));
            e.valorHabilitante = iHabilitante < 0 ? null : nullSiVacio(valor(f, iHabilitante));
            e.obligatoria = iObligatoria >= 0 && esAfirmativo(valor(f, iObligatoria));
            e.operador = operadorDe(iOperador < 0 ? "" : valor(f, iOperador));
            e.instruccion = iInstruccion < 0 ? null : limpiar(valor(f, iInstruccion));
            e.figura = iFigura < 0 ? null : limpiar(valor(f, iFigura));
            e.tituloBloque = iTituloBloque < 0 ? null : limpiar(valor(f, iTituloBloque));
            e.textoBloque = iTextoBloque < 0 ? null : limpiar(valor(f, iTextoBloque));

            // Una casilla marcada como respuesta multiple es una de las varias que se
            // pueden marcar en la pregunta madre, y la madre es el bloque. El grupo no
            // se guarda en las demas: una opcion unica no se marca junto con otra.
            boolean multiple = iMultiple >= 0 && "SI".equalsIgnoreCase(valor(f, iMultiple));
            e.grupoMultiple = multiple ? (e.bloque != null ? e.bloque : raizDe(codigo)) : null;

            e.opciones = opcionesPorCampo.getOrDefault(clave(codigo), List.of());
            e.existente = existentes.get(clave(codigo));
            vistas.add(clave(codigo));

            if (e.bloque != null && !bloquesExistentes.contains(clave(e.bloque))
                    && !plan.seccionesNuevas.contains(e.bloque)) {
                plan.seccionesNuevas.add(e.bloque);
            }

            plan.entradas.add(e);
        }

        if (plan.entradas.isEmpty()) {
            throw new ArchivoInvalidoException("La hoja \"" + HOJA_VARIABLES
                    + "\" no trae ninguna pregunta del catalogo \"" + plan.nombreCatalogo + "\".");
        }

        for (Map.Entry<String, VariableCuestionario> entrada : existentes.entrySet()) {
            if (!vistas.contains(entrada.getKey())) {
                plan.sobran.add(entrada.getValue().getCodigo());
            }
        }

        derivarRejillas(plan);
        decidirEstados(plan);
        plan.reglas = leerReglas(hojas, plan.nombreCatalogo);

        return plan;
    }

    /**
     * Qué pregunta es nueva, cuál cambió y cuál se queda como está.
     *
     * <h3>Por qué va aquí y no dentro del bucle que lee las filas</h3>
     *
     * <p>Antes se decidía al leer cada fila, y eso era demasiado pronto: la fila y la
     * columna de una celda de rejilla se deducen cuando ya están todas las preguntas
     * del bloque, en {@link #derivarRejillas}, que corre después. Comparadas antes,
     * las dos eran siempre {@code null}, así que un bloque que <b>solo</b> gana su
     * rejilla se daba por «igual» en la previsualización. No se notaba porque al
     * confirmar se reescribían todas las preguntas, cambiadas o no; en el momento en
     * que se dejan de reescribir, ese reparto no se aplicaría nunca.</p>
     *
     * <h3>Y por qué los choques se preguntan aquí</h3>
     *
     * <p>Un choque es una opción que desaparece o un tipo que cambia: las dos cosas
     * son cambios, y a una pregunta que no cambió no hay nada que preguntarle. Antes
     * se le preguntaba a todas, y son dos consultas por pregunta: con el instrumento
     * principal cargado, cuatro mil consultas en cada previsualización y otras cuatro
     * mil al confirmar, que es lo que sacaba la carga del tiempo de espera del
     * navegador.</p>
     */
    private void decidirEstados(Plan plan) {
        for (int i = 0; i < plan.entradas.size(); i++) {
            EntradaPlan e = plan.entradas.get(i);
            e.orden = i;
            e.estado = e.existente == null ? Estado.NUEVA
                    : (cambia(e, e.existente) ? Estado.ACTUALIZA : Estado.IGUAL);
            if (e.existente != null && tocaRespuestas(e, e.existente)) {
                revisarChoques(plan, e);
            }
        }
    }

    // ── Rejillas ──────────────────────────────────────────────────────

    /**
     * Marca los bloques que en el papel son una rejilla y reparte sus preguntas en
     * filas y columnas. El criterio —y por qué es tan estricto— está en
     * {@link DetectorRejilla}; aquí solo se agrupa por bloque y se anota el resultado.
     */
    /**
     * El título del bloque sale de la primera de sus preguntas que lo traiga.
     *
     * <p>En el archivo se repite en todas las filas del bloque, que es como se deja
     * escribir cómodamente en una hoja de cálculo, pero pertenece al bloque y ahí se
     * guarda una sola vez.</p>
     */
    private String tituloDe(Plan plan, String clave) {
        for (EntradaPlan e : plan.entradas) {
            if (clave.equals(clave(e.bloque)) && e.tituloBloque != null) return e.tituloBloque;
        }
        return null;
    }

    private String textoDe(Plan plan, String clave) {
        for (EntradaPlan e : plan.entradas) {
            if (clave.equals(clave(e.bloque)) && e.textoBloque != null) return e.textoBloque;
        }
        return null;
    }

    private PresentacionSeccion presentacionDe(Plan plan, String clave) {
        if (plan.bloquesEnRejilla.contains(clave)) return PresentacionSeccion.MATRIZ;
        if (plan.bloquesEnEscala.contains(clave)) return PresentacionSeccion.ESCALA;
        return PresentacionSeccion.LISTA;
    }

    private void derivarRejillas(Plan plan) {
        Map<String, Map<EntradaPlan, String>> porBloque = new LinkedHashMap<>();
        for (EntradaPlan e : plan.entradas) {
            if (e.bloque == null || e.pregunta == null) continue;
            porBloque.computeIfAbsent(clave(e.bloque), k -> new LinkedHashMap<>())
                    .put(e, e.pregunta);
        }

        for (Map.Entry<String, Map<EntradaPlan, String>> bloque : porBloque.entrySet()) {
            Map<EntradaPlan, DetectorRejilla.Celda> reparto =
                    DetectorRejilla.repartir(bloque.getValue());
            if (reparto.isEmpty()) continue;

            for (Map.Entry<EntradaPlan, DetectorRejilla.Celda> celda : reparto.entrySet()) {
                celda.getKey().filaMatriz = celda.getValue().fila();
                celda.getKey().columnaMatriz = celda.getValue().columna();
            }
            plan.bloquesEnRejilla.add(bloque.getKey());
        }

        // Lo que no es una rejilla de celdas puede ser una tabla de escala, que es
        // la forma mucho mas comun. Se prueba despues y nunca encima: un bloque que
        // ya tiene sus celdas repartidas no se pinta ademas como escala.
        Map<String, List<EntradaPlan>> todos = new LinkedHashMap<>();
        for (EntradaPlan e : plan.entradas) {
            if (e.bloque == null) continue;
            todos.computeIfAbsent(clave(e.bloque), k -> new ArrayList<>()).add(e);
        }
        for (Map.Entry<String, List<EntradaPlan>> bloque : todos.entrySet()) {
            if (plan.bloquesEnRejilla.contains(bloque.getKey())) continue;
            List<List<String>> juegos = new ArrayList<>();
            for (EntradaPlan e : bloque.getValue()) {
                if (e.tipo != TipoVariable.OPCION_UNICA) continue;
                juegos.add(e.opciones.stream().map(o -> o.codigo).toList());
            }
            if (!DetectorEscala.columnas(juegos).isEmpty()) {
                plan.bloquesEnEscala.add(bloque.getKey());
            }
        }
    }

    /**
     * Si este cambio puede, siquiera, tocar una respuesta guardada.
     *
     * <h3>Por qué es esto y no «cambió algo»</h3>
     *
     * <p>Un choque solo puede venir de dos sitios: una opción que desaparece o un tipo
     * que cambia. Lo demás —la página, el bloque, la instrucción, el orden— no toca
     * ninguna respuesta, y preguntarle a la base por ella son dos consultas tiradas.</p>
     *
     * <p>Antes se preguntaba por todo lo que cambiaba, y eso parecía suficiente hasta
     * que una corrida real lo desmintió: insertar seis preguntas en medio del libro
     * corre el orden de las mil setecientas cuarenta y seis siguientes, ninguna de
     * ellas cambia nada que afecte a una respuesta, y aun así eran tres mil quinientas
     * consultas que volvieron a sacar la carga del tiempo de espera del navegador.</p>
     *
     * <p>De las opciones se comparan los <b>códigos</b> y no las etiquetas: reescribir
     * «si» como «Si» cambia lo que el participante lee, no lo que quedó guardado.</p>
     */
    private static boolean tocaRespuestas(EntradaPlan e, VariableCuestionario v) {
        if (v.getTipo() != e.tipo) return true;

        List<String> actuales = v.getOpciones() == null ? List.of()
                : v.getOpciones().stream().map(OpcionVariable::getCodigo).toList();
        List<String> entrantes = e.opciones.stream().map(o -> o.codigo).toList();
        return !actuales.equals(entrantes);
    }

    /**
     * Qué cambios de este archivo tocan respuestas ya guardadas.
     *
     * <p>Quitar una opción que alguien contestó bloquea la carga: el código seguiría en
     * la respuesta y ya no habría nada que lo explicara. Cambiar el tipo no bloquea pero
     * se avisa, porque el valor guardado puede dejar de interpretarse igual.</p>
     */
    private void revisarChoques(Plan plan, EntradaPlan e) {
        if (!repos.getRespuestas().existsByVariable_Id(e.existente.getId())) return;

        List<String> enUso = repos.getRespuestas().frecuenciaPorCodigo(e.existente.getId()).stream()
                .map(fila -> (String) fila[0])
                .filter(Objects::nonNull)
                .toList();

        List<String> nuevos = e.opciones.stream().map(o -> o.codigo).toList();
        List<String> perdidos = enUso.stream().filter(c -> !nuevos.contains(c)).toList();
        if (!perdidos.isEmpty() && !nuevos.isEmpty()) {
            plan.choques.add(new PrevisualizacionCatalogo.Choque(e.codigo,
                    "El archivo ya no trae la(s) opcion(es) " + String.join(", ", perdidos)
                            + ", y hay respuestas que las usan.", true));
        }

        if (e.existente.getTipo() != e.tipo) {
            plan.choques.add(new PrevisualizacionCatalogo.Choque(e.codigo,
                    "Cambia de " + e.existente.getTipo() + " a " + e.tipo
                            + " y ya tiene respuestas guardadas.", false));
        }
    }

    boolean cambia(EntradaPlan e, VariableCuestionario v) {
        // El bloque va primero porque es el que faltaba: una pregunta que solo cambia
        // de bloque se daba por «igual» y se quedaba donde estaba. Se notó al partir la
        // 183 en sus tres tablas —98 preguntas que solo cambian de bloque— y el archivo
        // nuevo no habría movido ninguna, sin que nada fallara.
        String bloqueActual = v.getSeccion() == null ? null : v.getSeccion().getNombre();
        if (!Objects.equals(bloqueActual, nullSiVacio(e.bloque))) return true;
        if (!Objects.equals(v.getTipo(), e.tipo)) return true;
        if (!Objects.equals(nullSiVacio(v.getPregunta()), e.pregunta)) return true;
        if (!Objects.equals(nullSiVacio(v.getColumnaVolcado()), nullSiVacio(e.columna))) return true;
        if (!Objects.equals(v.getGrupoMultiple(), e.grupoMultiple)) return true;
        if (!Objects.equals(v.getDependeDe(), e.dependeDe)) return true;
        if (!Objects.equals(v.getValorHabilitante(), e.valorHabilitante)) return true;
        if (v.getOperadorDependencia() != e.operador) return true;
        if (!Objects.equals(v.getInstruccion(), e.instruccion)) return true;
        if (!Objects.equals(v.getFigura(), e.figura)) return true;
        if (!Objects.equals(v.getFilaMatriz(), e.filaMatriz)) return true;
        if (!Objects.equals(v.getColumnaMatriz(), e.columnaMatriz)) return true;
        // Estos tres se escriben y antes no se comparaban. Daba igual mientras se
        // reescribia todo; ahora, lo que no se compara no se guarda.
        if (!Objects.equals(v.getPagina(), e.pagina)) return true;
        if (!Objects.equals(v.getOrden(), Integer.valueOf(e.orden))) return true;
        if (Boolean.TRUE.equals(v.getDocumentada())
                != (e.pregunta != null && !e.pregunta.isBlank())) return true;
        if (!Objects.equals(Boolean.TRUE.equals(v.getObligatoria()), e.obligatoria)) return true;
        if (!Objects.equals(v.getValorMin(), e.min) || !Objects.equals(v.getValorMax(), e.max)) return true;

        List<String> actuales = v.getOpciones() == null ? List.<String>of()
                : v.getOpciones().stream().map(o -> o.getCodigo() + "=" + o.getEtiqueta()).toList();
        List<String> entrantes = e.opciones.stream().map(o -> o.codigo + "=" + o.etiqueta).toList();
        return !actuales.equals(entrantes);
    }

    // ── Lectura de las hojas ─────────────────────────────────────────────────

    /**
     * Las opciones agrupadas por pregunta.
     *
     * <p>La hoja viene en formato largo: el código de la pregunta solo aparece en la
     * primera de sus filas y las siguientes lo dejan vacío, igual que se lee un listado
     * impreso. Así que se arrastra el último visto. Sin eso, todas las opciones menos la
     * primera de cada pregunta se perderían, y el catálogo quedaría con una sola opción
     * por pregunta sin que nada fallara.</p>
     */
    private Map<String, List<OpcionEntrante>> leerOpciones(TablaLeida tabla, String nombreCatalogo) {
        int iCuestionario = indice(tabla, V_CUESTIONARIO);
        int iCampo = exigir(tabla, V_CAMPO, HOJA_OPCIONES);
        int iCodigo = exigir(tabla, O_CODIGO, HOJA_OPCIONES);
        int iValor = indice(tabla, O_VALOR);

        Map<String, List<OpcionEntrante>> porCampo = new LinkedHashMap<>();
        String campoActual = null;

        for (List<String> f : tabla.filas()) {
            if (iCuestionario >= 0) {
                String cuestionario = valor(f, iCuestionario);
                if (!cuestionario.isEmpty() && !nombreCatalogo.equals(cuestionario)) {
                    campoActual = null;
                    continue;
                }
            }
            String campo = valor(f, iCampo);
            if (!campo.isEmpty()) campoActual = campo;
            if (campoActual == null) continue;

            String codigo = valor(f, iCodigo);
            String etiqueta = iValor < 0 ? "" : valor(f, iValor);

            // Las filas que describen como se captura en lugar de dar una opcion
            // —«Reconocimiento de texto», «dd mm aaaa», «999»— no son opciones. El tipo
            // ya salio de la hoja de variables; aqui solo estorbarian como opcion falsa.
            if (codigo.isEmpty() || !esCodigo(codigo)) continue;

            // La etiqueta se limpia al entrar: el libro trae cosas como
            // «5 -14           min. x semana», alineadas con espacios para que cuadren
            // en la celda de Excel, y en pantalla se verian con el hueco en medio.
            String limpia = limpiar(etiqueta);
            porCampo.computeIfAbsent(clave(campoActual), k -> new ArrayList<>())
                    .add(new OpcionEntrante(codigo, limpia == null ? codigo : limpia));
        }
        return porCampo;
    }

    /** Un código de opción es un número. Todo lo demás es una instrucción de captura. */
    private boolean esCodigo(String s) {
        return s.matches("-?\\d{1,6}");
    }

    private TipoVariable tipoDe(String texto, String codigo) {
        String t = NormalizadorAlias.normalizar(texto);
        if (t == null) {
            throw new ArchivoInvalidoException("La pregunta " + codigo + " no trae tipo.");
        }
        try {
            return TipoVariable.valueOf(t.replace(' ', '_'));
        } catch (IllegalArgumentException e) {
            throw new ArchivoInvalidoException("La pregunta " + codigo + " dice ser de tipo \""
                    + texto + "\", que no es ninguno de los tipos conocidos.");
        }
    }

    /** {@code 0-12} o {@code 1-9}: el rango impreso junto a la rejilla. */
    private void aplicarRango(EntradaPlan e, String rango) {
        if (rango == null || !rango.matches("-?\\d+(\\.\\d+)?\\s*-\\s*-?\\d+(\\.\\d+)?")) return;
        String[] partes = rango.split("(?<=\\d)\\s*-\\s*");
        if (partes.length != 2) return;
        try {
            e.min = Double.valueOf(partes[0].trim());
            e.max = Double.valueOf(partes[1].trim());
        } catch (NumberFormatException ignorada) {
            // Un rango que no se entiende se deja sin interpretar: inventar un limite
            // rechazaria respuestas validas, que es peor que no validar el rango.
        }
    }

    /** {@code P19_3} → {@code P19}. La pregunta madre de una casilla. */
    private String raizDe(String codigo) {
        int guion = codigo.indexOf('_');
        return guion > 0 ? codigo.substring(0, guion) : codigo;
    }

    private String resolverCatalogo(TablaLeida tabla, int iCuestionario, String pedido) {
        if (iCuestionario < 0) {
            if (pedido != null && !pedido.isBlank()) return pedido.trim();
            return "";
        }
        Set<String> nombres = new LinkedHashSet<>();
        for (List<String> f : tabla.filas()) {
            String v = valor(f, iCuestionario);
            if (!v.isEmpty()) nombres.add(v);
        }
        if (pedido != null && !pedido.isBlank()) {
            if (!nombres.contains(pedido.trim())) {
                throw new ArchivoInvalidoException("El archivo no trae el catalogo \""
                        + pedido.trim() + "\". Trae: " + String.join(", ", nombres) + ".");
            }
            return pedido.trim();
        }
        if (nombres.size() == 1) return nombres.iterator().next();
        // El libro trae los catalogos de todos los instrumentos. Elegir por nuestra
        // cuenta cargaria el catalogo de otro cuestionario sobre este, y los codigos no
        // significan lo mismo entre instrumentos.
        throw new ArchivoInvalidoException("El archivo trae " + nombres.size()
                + " catalogos (" + String.join(", ", nombres) + "). Indica cual se va a cargar.");
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private TablaLeida hojaLlamada(List<LectorArchivoTabular.HojaLeida> hojas, String nombre) {
        for (LectorArchivoTabular.HojaLeida h : hojas) {
            if (nombre.equals(NormalizadorAlias.normalizar(h.nombre()))) return h.tabla();
        }
        throw new ArchivoInvalidoException("El libro no tiene una hoja llamada \"" + nombre + "\".");
    }

    private int indice(TablaLeida tabla, String encabezado) {
        for (int c = 0; c < tabla.encabezados().size(); c++) {
            if (encabezado.equals(NormalizadorAlias.normalizar(tabla.encabezados().get(c)))) return c;
        }
        return -1;
    }

    private int exigir(TablaLeida tabla, String encabezado, String hoja) {
        int i = indice(tabla, encabezado);
        if (i < 0) {
            throw new ArchivoInvalidoException("La hoja \"" + hoja
                    + "\" no trae la columna \"" + encabezado + "\".");
        }
        return i;
    }

    private String valor(List<String> fila, int indice) {
        if (indice < 0 || indice >= fila.size()) return "";
        String v = fila.get(indice);
        return v == null ? "" : v.trim();
    }

    private String nullSiVacio(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /**
     * El texto sin espacios de sobra, o null si no quedaba nada.
     *
     * <p>Colapsa los blancos interiores, no solo los de las puntas: en el libro los
     * usan para alinear dentro de la celda y llegan hasta aquí. También entra el
     * espacio duro, que es el que deja una hoja de cálculo al pegar desde Word.</p>
     */
    private String limpiar(String s) {
        if (s == null) return null;
        String t = s.replaceAll("[\\s\\u00A0]+", " ").trim();
        return t.isEmpty() ? null : t;
    }

    private OperadorDependencia operadorDe(String texto) {
        String t = NormalizadorAlias.normalizar(texto);
        if (t == null) return OperadorDependencia.IGUAL;
        return switch (t) {
            case "DISTINTO", "DIFERENTE", "!=", "<>", "NO" -> OperadorDependencia.DISTINTO;
            case "EN", "ALGUNO", "ALGUNO DE", "IN" -> OperadorDependencia.EN;
            default -> OperadorDependencia.IGUAL;
        };
    }

    /** Admite las formas en que una hoja de calculo dice si: SI, X, 1, TRUE. */
    private boolean esAfirmativo(String s) {
        if (s == null) return false;
        String t = s.trim().toUpperCase();
        return t.equals("SI") || t.equals("SÍ") || t.equals("X") || t.equals("1") || t.equals("TRUE");
    }

    private String clave(String s) {
        return s == null ? "" : s.trim().toUpperCase();
    }

    private Integer aEntero(String s) {
        try {
            return Integer.valueOf(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
