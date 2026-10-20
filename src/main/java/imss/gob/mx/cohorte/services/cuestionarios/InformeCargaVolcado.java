package imss.gob.mx.cohorte.services.cuestionarios;

import imss.gob.mx.cohorte.modules.cuestionarios.carga.TareaCargaVolcado;

import java.time.LocalDateTime;
import java.util.List;

/**
 * En qué va —o cómo acabó— una carga de volcado.
 *
 * <p>Es la misma forma mientras corre y cuando termina, a propósito: la pantalla
 * pregunta por la carga cada pocos segundos y lo que recibe tiene que servirle igual
 * para pintar una barra de avance que para pintar el resultado final. Dos formas
 * distintas obligarían a adivinar cuál toca.</p>
 *
 * <p>Las cifras son de este archivo, no de la base: «cuestionarios creados» es
 * cuántos trajo esta carga, no cuántos hay.</p>
 *
 * @param totalFilas      cuántas filas trae el archivo. Es cero hasta que se lee la
 *                        hoja, lo que tarda lo que tarde el archivo en abrirse
 * @param filasProcesadas cuántas se han recorrido, se guardaran o se omitieran
 * @param mensajeError    por qué se cortó, si se cortó. Lo de una fila suelta no
 *                        viene aquí: va como fila omitida
 */
public record InformeCargaVolcado(
        Long id,
        String estado,
        boolean termino,
        String nombreArchivo,
        String nombreInstrumento,
        boolean sobrescribir,
        int totalFilas,
        int filasProcesadas,
        int cuestionariosCreados,
        int respuestasGuardadas,
        int pendientesDeLectura,
        int discrepanciasGuardadas,
        String mensajeError,
        LocalDateTime inicio,
        LocalDateTime fin,
        List<FilaOmitida> filasOmitidas
) {

    public record FilaOmitida(int numeroFila, String etiqueta, String motivo) {}

    /**
     * Se arma dentro de la transacción que leyó la tarea.
     *
     * <p>Tiene que ser ahí: el instrumento y las filas omitidas son relaciones
     * perezosas, y tocarlas después de cerrar la sesión de Hibernate reventaría al
     * pintar la respuesta, no al leerla.</p>
     */
    public static InformeCargaVolcado de(TareaCargaVolcado t) {
        return new InformeCargaVolcado(
                t.getId(),
                t.getEstado().name(),
                t.termino(),
                t.getNombreArchivo(),
                t.getTipoCuestionario().getNombre(),
                Boolean.TRUE.equals(t.getSobrescribir()),
                t.getTotalFilas(),
                t.getFilasProcesadas(),
                t.getCuestionariosCreados(),
                t.getRespuestasGuardadas(),
                t.getPendientesDeLectura(),
                t.getDiscrepanciasGuardadas(),
                t.getMensajeError(),
                t.getFechaInicio(),
                t.getFechaFin(),
                t.getFilasOmitidas().stream()
                        .map(f -> new FilaOmitida(f.getNumeroFila(), f.getEtiqueta(), f.getMotivo()))
                        .toList());
    }
}
