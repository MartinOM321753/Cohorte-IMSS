package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.respuestas.RespuestaCuestionarioRepository;
import imss.gob.mx.cohorte.modules.cuestionarios.secciones.SeccionCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.tipos.TipoCuestionario;
import imss.gob.mx.cohorte.modules.cuestionarios.variables.*;
import imss.gob.mx.cohorte.security.institucion.InstitucionContextService;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjConflictException;
import imss.gob.mx.cohorte.utils.Exceptions.exceptions.ObjNotFoundException;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** El catálogo de preguntas de un instrumento. */
@Service
@AllArgsConstructor
public class VariableCuestionarioService {

    private final VariableCuestionarioRepository variableRepository;
    private final OpcionVariableRepository opcionRepository;
    private final RespuestaCuestionarioRepository respuestaRepository;
    private final TipoCuestionarioService tipoService;
    private final InstitucionContextService contexto;

    public List<VariableCuestionario> todasDe(Long idTipo) {
        tipoService.uno(idTipo);
        return variableRepository.findTodasConOpciones(idTipo);
    }

    public List<VariableCuestionario> deSeccion(Long idSeccion) {
        tipoService.seccion(idSeccion);
        return variableRepository.findDeSeccionConOpciones(idSeccion);
    }

    public Page<VariableCuestionario> buscar(Long idTipo, String texto, Pageable pagina) {
        tipoService.uno(idTipo);
        if (texto == null || texto.isBlank()) {
            return variableRepository.findAllByTipoCuestionario_Id(idTipo, pagina);
        }
        return variableRepository.buscar(idTipo, texto.trim(), pagina);
    }

    /** Las que el volcado produce y ningún catálogo documenta. Es una bandeja de trabajo. */
    public List<VariableCuestionario> sinDocumentar(Long idTipo) {
        tipoService.uno(idTipo);
        return variableRepository
                .findAllByTipoCuestionario_IdAndDocumentadaFalseOrderByOrdenAsc(idTipo);
    }

    /**
     * Pone la misma dependencia a todas las preguntas de un bloque.
     *
     * <h3>Por qué de golpe y no una por una</h3>
     *
     * <p>Los saltos del papel son por bloque, no por pregunta. El «Ninguna» que
     * encabeza la 112 apaga las ciento treinta y cinco celdas de la rejilla, y
     * capturarlo pregunta por pregunta no es trabajoso: es que nadie lo haría, y quien
     * lo intentara se saltaría alguna sin que nada lo delatara.</p>
     *
     * <h3>Las dos cosas que no hace</h3>
     *
     * <p>No toca la propia pregunta llave —una pregunta que dependiera de sí misma no
     * se mostraría nunca, o siempre, según el operador, y en los dos casos por un
     * motivo que nadie podría ver—. Y no comprueba que la llave exista: una llave que
     * apunta a nada deja la pregunta habilitada, que es el lado seguro y ya está
     * resuelto en {@link EvaluadorDependencias}.</p>
     *
     * @param llave  código de la pregunta que manda, o null para quitar la dependencia
     * @return cuántas preguntas quedaron tocadas
     */
    @Transactional
    public int dependenciaDeBloque(Long idSeccion, String llave, String valor,
                                   OperadorDependencia operador) {
        SeccionCuestionario seccion = tipoService.seccion(idSeccion);
        List<VariableCuestionario> variables =
                variableRepository.findDeSeccionConOpciones(seccion.getId());

        String codigoLlave = llave == null || llave.isBlank() ? null : llave.trim();
        int tocadas = 0;
        for (VariableCuestionario v : variables) {
            if (codigoLlave != null && codigoLlave.equalsIgnoreCase(v.getCodigo())) continue;
            v.setDependeDe(codigoLlave);
            v.setValorHabilitante(codigoLlave == null || valor == null || valor.isBlank()
                    ? null : valor.trim());
            v.setOperadorDependencia(operador == null ? OperadorDependencia.IGUAL : operador);
            tocadas++;
        }
        variableRepository.saveAll(variables);
        return tocadas;
    }

    public VariableCuestionario una(Long id) {
        VariableCuestionario variable = variableRepository.findById(id)
                .orElseThrow(() -> new ObjNotFoundException("No se encontro la pregunta solicitada"));
        contexto.verificarPertenece(variable.getTipoCuestionario().getInstitucion());
        return variable;
    }

    @Transactional
    public VariableCuestionario crear(Long idTipo, VariableCuestionario datos, Long idSeccion,
                                     List<OpcionVariable> opciones) {
        TipoCuestionario tipo = tipoService.uno(idTipo);
        variableRepository.findByTipoCuestionario_IdAndCodigoIgnoreCase(idTipo, datos.getCodigo())
                .ifPresent(v -> {
                    throw new ObjConflictException("Ya existe la pregunta "
                            + datos.getCodigo() + " en este cuestionario.");
                });

        datos.setTipoCuestionario(tipo);
        datos.setSeccion(idSeccion == null ? null : tipoService.seccion(idSeccion));
        datos.setOpciones(new ArrayList<>());
        completarOmitidos(datos, idSeccion);
        VariableCuestionario guardada = variableRepository.save(datos);
        reemplazarOpciones(guardada, opciones);
        return variableRepository.save(guardada);
    }

    /**
     * Rellena lo que el alta no dijo.
     *
     * <p>Hace falta porque {@code null} significa «no lo dije» —es lo que permite que la
     * actualización deje en paz lo que no se tocó— y cuatro de esos campos son columnas
     * obligatorias. Sin esto, crear una pregunta sin mandarlas acababa en un error de la
     * base en lugar de en una pregunta con los valores razonables.</p>
     *
     * <p>El orden va al final del bloque y no en cero: en cero la pregunta nueva
     * aparecería <b>antes</b> de las que ya estaban, moviendo de sitio un cuestionario
     * que alguien ya ordenó como el papel.</p>
     *
     * <p>«Documentada» no se pone en true por omisión, aunque sea el valor que la
     * entidad declara: significa que el catálogo dice qué pregunta es, así que se deduce
     * de si trae enunciado. Una pregunta creada sin texto es precisamente una de las que
     * faltan por redactar, y marcarla como documentada la esconde de esa lista.</p>
     */
    private void completarOmitidos(VariableCuestionario datos, Long idSeccion) {
        if (datos.getOrden() == null) {
            int ultimo = idSeccion == null ? -1 : variableRepository.ultimoOrdenDe(idSeccion);
            datos.setOrden(ultimo + 1);
        }
        if (datos.getObligatoria() == null) datos.setObligatoria(false);
        if (datos.getActiva() == null) datos.setActiva(true);
        if (datos.getDocumentada() == null) {
            datos.setDocumentada(datos.getPregunta() != null && !datos.getPregunta().isBlank());
        }
    }

    @Transactional
    public VariableCuestionario actualizar(Long id, VariableCuestionario datos, Long idSeccion,
                                           List<OpcionVariable> opciones) {
        VariableCuestionario variable = una(id);

        // El codigo es con el que los investigadores piden informacion y con el que
        // estan guardadas las respuestas. Renombrarlo cuando ya hay datos dejaria
        // solicitudes antiguas apuntando a una pregunta que ya no existe, sin error.
        if (!variable.getCodigo().equalsIgnoreCase(datos.getCodigo())
                && respuestaRepository.existsByVariable_Id(id)) {
            throw new ObjConflictException("No se puede renombrar " + variable.getCodigo()
                    + ": ya tiene respuestas guardadas.");
        }

        variable.setCodigo(datos.getCodigo());
        variable.setColumnaVolcado(datos.getColumnaVolcado());
        variable.setPregunta(datos.getPregunta());
        variable.setTipo(datos.getTipo());
        variable.setPagina(datos.getPagina());
        variable.setGrupoMultiple(datos.getGrupoMultiple());
        variable.setDependeDe(datos.getDependeDe());
        variable.setValorHabilitante(datos.getValorHabilitante());
        variable.setOperadorDependencia(datos.getOperadorDependencia());
        variable.setFilaMatriz(datos.getFilaMatriz());
        variable.setColumnaMatriz(datos.getColumnaMatriz());
        variable.setValorMin(datos.getValorMin());
        variable.setValorMax(datos.getValorMax());
        variable.setLongitudMax(datos.getLongitudMax());
        variable.setUnidad(datos.getUnidad());
        variable.setObservaciones(datos.getObservaciones());
        variable.setInstruccion(datos.getInstruccion());
        variable.setFigura(datos.getFigura());
        if (datos.getObligatoria() != null) variable.setObligatoria(datos.getObligatoria());
        if (datos.getDocumentada() != null) variable.setDocumentada(datos.getDocumentada());
        if (datos.getActiva() != null) variable.setActiva(datos.getActiva());
        if (datos.getOrden() != null) variable.setOrden(datos.getOrden());
        if (idSeccion != null) variable.setSeccion(tipoService.seccion(idSeccion));

        if (opciones != null) reemplazarOpciones(variable, opciones);
        return variableRepository.save(variable);
    }

    /**
     * Sustituye el juego de opciones.
     *
     * <p>Las opciones que siguen existiendo <b>conservan su fila</b>: se buscan por
     * código y solo se les cambia la etiqueta y el orden. Borrar todo y volver a
     * insertar sería más corto y estaría mal —las respuestas guardadas no apuntan a
     * la opción, pero el código sí es el dato, y reinsertar abre la puerta a que un
     * código desaparezca y vuelva con otro significado sin que quede rastro—.</p>
     *
     * <p>Una opción que se retira con respuestas que la usan no se elimina en
     * silencio: se rechaza el cambio. Un código de respuesta sin opción que lo
     * explique es un dato que ya no se puede leer.</p>
     */
    private void reemplazarOpciones(VariableCuestionario variable, List<OpcionVariable> nuevas) {
        List<OpcionVariable> actuales = variable.getOpciones();
        if (actuales == null) {
            actuales = new ArrayList<>();
            variable.setOpciones(actuales);
        }
        if (nuevas == null) nuevas = List.of();

        List<String> codigosNuevos = nuevas.stream().map(OpcionVariable::getCodigo).toList();

        if (respuestaRepository.existsByVariable_Id(variable.getId())) {
            List<String> enUso = respuestasConCodigo(variable.getId());
            for (String codigo : enUso) {
                if (!codigosNuevos.contains(codigo)) {
                    throw new ObjConflictException("No se puede quitar la opcion " + codigo
                            + " de " + variable.getCodigo() + ": hay respuestas que la usan. "
                            + "Cambia su etiqueta si lo que quieres es corregir el texto.");
                }
            }
        }

        actuales.removeIf(o -> !codigosNuevos.contains(o.getCodigo()));

        for (int i = 0; i < nuevas.size(); i++) {
            OpcionVariable entrante = nuevas.get(i);
            final int orden = i;
            OpcionVariable existente = actuales.stream()
                    .filter(o -> o.getCodigo().equals(entrante.getCodigo()))
                    .findFirst()
                    .orElse(null);
            if (existente == null) {
                entrante.setVariable(variable);
                entrante.setOrden(orden);
                actuales.add(entrante);
            } else {
                existente.setEtiqueta(entrante.getEtiqueta());
                existente.setOrden(orden);
                if (entrante.getEsEscape() != null) existente.setEsEscape(entrante.getEsEscape());
            }
        }
    }

    private List<String> respuestasConCodigo(Long idVariable) {
        return respuestaRepository.frecuenciaPorCodigo(idVariable).stream()
                .map(fila -> (String) fila[0])
                .filter(c -> c != null)
                .toList();
    }

    /**
     * Retira la pregunta de uso sin borrarla. Si no tiene respuestas, se borra de
     * verdad: una pregunta que nunca se contestó y que se catalogó por error no
     * tiene por qué quedarse estorbando en el catálogo.
     */
    @Transactional
    public void eliminar(Long id) {
        VariableCuestionario variable = una(id);
        if (respuestaRepository.existsByVariable_Id(id)) {
            variable.setActiva(false);
            variableRepository.save(variable);
            return;
        }
        opcionRepository.deleteAllByVariable_Id(id);
        variableRepository.delete(variable);
    }

    /**
     * Reparte el orden de las preguntas de un bloque.
     *
     * <p>Mismo criterio que los bloques: de una sola vez, porque el orden decide
     * cómo se pinta el formulario y a medio camino el formulario no corresponde al
     * papel.</p>
     */
    @Transactional
    public void reordenar(Long idSeccion, List<Long> idsEnOrden) {
        SeccionCuestionario seccion = tipoService.seccion(idSeccion);
        List<VariableCuestionario> variables =
                variableRepository.findDeSeccionConOpciones(idSeccion);
        if (idsEnOrden.size() != variables.size()) {
            throw new ObjConflictException("La lista trae " + idsEnOrden.size()
                    + " preguntas y el bloque tiene " + variables.size()
                    + ". Vuelve a cargar la pantalla antes de reordenar.");
        }

        // El orden es global dentro del instrumento, no dentro del bloque: las
        // preguntas de un bloque ocupan un tramo del instrumento y reordenarlas no
        // puede sacarlas de ese tramo. Se reutilizan los numeros que ya ocupaban.
        List<Integer> posiciones = variables.stream()
                .map(VariableCuestionario::getOrden)
                .sorted()
                .toList();

        for (int i = 0; i < idsEnOrden.size(); i++) {
            Long id = idsEnOrden.get(i);
            VariableCuestionario v = variables.stream()
                    .filter(x -> x.getId().equals(id))
                    .findFirst()
                    .orElseThrow(() -> new ObjConflictException(
                            "La pregunta " + id + " no pertenece al bloque \""
                                    + seccion.getNombre() + "\"."));
            v.setOrden(posiciones.get(i));
        }
        variableRepository.saveAll(variables);
    }
}
